package com.example.healthtracker.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.healthtracker.R;
import com.example.healthtracker.StepCounterData;
import com.example.healthtracker.StepCounterService;
import com.example.healthtracker.fragments.MenuAccountFragment;
import com.example.healthtracker.services.UserService;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private FirebaseAuth mAuth;
    private CardView avatarCard;
    private FirebaseUser currentUser;
    private ImageView imgAvatar;
    private TextView txtName;

    private UserService userService;

    // Đếm bước chân
    private TextView stepCountText;
    private TextView timeValue;
    private TextView caloriesValue;
    private TextView distanceValue;
    private StepCounterData stepData;

    // Để nhận cập nhật từ StepCounterService
    private BroadcastReceiver stepUpdateReceiver;
    private int lastSteps = 0;
    private double lastDistance = 0.0;
    private double lastCalories = 0.0;
    private long lastActiveTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        userService = new UserService();

        // Khởi tạo quản lý dữ liệu bước chân
        stepData = StepCounterData.getInstance(this);

        // Khởi tạo các view đếm bước chân
        initStepCountViews();

        // Khởi tạo receiver để nhận cập nhật bước chân
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupStepUpdateReceiver();
        }

        avatarCard = findViewById(R.id.avatarCard);
        imgAvatar = findViewById(R.id.imgAvatar);
        txtName = findViewById(R.id.txtName);

        avatarCard.setOnClickListener(v -> {
            // Show a logout option when the avatar is clicked
            MenuAccountFragment menuAccountFragment = new MenuAccountFragment();
            menuAccountFragment.show(getSupportFragmentManager(), "MenuAccountFragment");
        });

        // Set up click listener for stats card
        MaterialCardView stepsCard = findViewById(R.id.stepsCard);
        stepsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DetailsChartActivity.class);
                startActivity(intent);
            }
        });
    }

    private void initStepCountViews() {
        try {
            stepCountText = findViewById(R.id.stepCountText);
            timeValue = findViewById(R.id.timeValue);
            caloriesValue = findViewById(R.id.caloriesValue);
            distanceValue = findViewById(R.id.distanceValue);

            // Khởi tạo giá trị ban đầu
            int initialSteps = stepData.getSteps();
            double initialDistance = stepData.calculateDistance(initialSteps);
            double initialCalories = stepData.calculateCalories(initialSteps);
            long initialTime = stepData.calculateActiveTime();

            // Hiển thị giá trị ban đầu
            if (stepCountText != null) stepCountText.setText(String.format("%,d", initialSteps));
            if (timeValue != null) timeValue.setText(formatTime(initialTime));
            if (caloriesValue != null)
                caloriesValue.setText(String.format("%.0f", initialCalories));
            if (distanceValue != null) {
                if (initialDistance >= 1000) {
                    distanceValue.setText(String.format("%.2f km", initialDistance / 1000));
                } else {
                    distanceValue.setText(String.format("%.0f m", initialDistance));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing step count views: ", e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void setupStepUpdateReceiver() {
        try {
            // Hủy đăng ký receiver cũ nếu đã tồn tại
            if (stepUpdateReceiver != null) {
                try {
                    unregisterReceiver(stepUpdateReceiver);
                } catch (Exception e) {
                    // Có thể receiver chưa được đăng ký
                    Log.e(TAG, "Error unregistering existing receiver: ", e);
                }
            }

            stepUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d(TAG, "Đã nhận broadcast với action: " + intent.getAction());
                    // Cập nhật UI ngay lập tức, không cần runOnUiThread vì BroadcastReceiver đã chạy trên main thread
                    updateStepCountUI(intent);
                }
            };

            // Đăng ký receiver với action chính xác
            IntentFilter filter = new IntentFilter(StepCounterService.ACTION_STEPS_UPDATED);

            // Thêm high priority để đảm bảo receiver được gọi nhanh
            filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);

            // Đăng ký receiver
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                registerReceiver(stepUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(stepUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            }

            Log.d(TAG, "Đã đăng ký broadcast receiver với action: " + StepCounterService.ACTION_STEPS_UPDATED);
        } catch (Exception e) {
            Log.e(TAG, "Error setting up step update receiver: ", e);
        }
    }

    private void updateStepCountUI(Intent intent) {
        try {
            // Log toàn bộ intent để kiểm tra
            Log.d(TAG, "Nhận được intent cập nhật bước: " + intent.toString());
            Log.d(TAG, "Intent có extras: " + (intent.getExtras() != null ? "Có" : "Không"));

            if (intent.getExtras() != null) {
                // Lấy dữ liệu từ intent
                lastSteps = intent.getIntExtra(StepCounterService.EXTRA_STEPS, lastSteps);
                lastDistance = intent.getDoubleExtra(StepCounterService.EXTRA_DISTANCE, lastDistance);
                lastCalories = intent.getDoubleExtra(StepCounterService.EXTRA_CALORIES, lastCalories);
                lastActiveTime = intent.getLongExtra(StepCounterService.EXTRA_TIME, lastActiveTime);

                Log.d(TAG, "Cập nhật UI với số bước: " + lastSteps);

                // Cập nhật giao diện
                if (stepCountText != null) {
                    stepCountText.setText(String.format("%,d", lastSteps));
                }
                if (timeValue != null) {
                    timeValue.setText(formatTime(lastActiveTime));
                }
                if (caloriesValue != null) {
                    caloriesValue.setText(String.format("%.0f", lastCalories));
                }
                if (distanceValue != null) {
                    if (lastDistance >= 1000) {
                        distanceValue.setText(String.format("%.2f km", lastDistance / 1000));
                    } else {
                        distanceValue.setText(String.format("%.0f m", lastDistance));
                    }
                }
            } else {
                Log.e(TAG, "Intent không có extra");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating step count UI: ", e);
        }
    }

    private String formatTime(long minutes) {
        try {
            if (minutes < 60) {
                return minutes + " m";
            } else {
                long hours = minutes / 60;
                long mins = minutes % 60;
                return hours + "h " + mins + "m";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time: ", e);
            return "0m";
        }
    }

    private void startStepCounterService() {
        try {
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Step counter service started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting step counter service: ", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký lại receiver để đảm bảo nhận broadcast
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setupStepUpdateReceiver();
        }

        // Nếu đã đăng nhập, đảm bảo service đang chạy
        if (mAuth.getCurrentUser() != null) {
            startStepCounterService();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Check auth
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, FirebaseUIActivity.class));
        } else {
            Log.d("MainActivity", "User is signed in: " + currentUser.getEmail());
            // Use Glide to load the image instead of setImageURI
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(imgAvatar);
            }
            txtName.setText(currentUser.getDisplayName());

            userService.getUser(currentUser.getUid())
                    .addOnSuccessListener(user -> {
                        if (user != null) {
                            txtName.setText(user.getDisplayName());
                        } else {
                            Log.w(TAG, "User document does not exist");
                        }
                    })
                    .addOnFailureListener(e -> {
                        txtName.setText(currentUser.getDisplayName());
                        Log.e(TAG, "Error loading user profile", e);
                    });

            // Chỉ khởi động service đếm bước sau khi đăng nhập thành công
            startStepCounterService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký receiver
        try {
            if (stepUpdateReceiver != null) {
                unregisterReceiver(stepUpdateReceiver);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering step update receiver: ", e);
        }
    }

    public void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    // Dừng service đếm bước chân trước khi chuyển đến màn hình đăng nhập
                    stopStepCounterService();

                    // Start the auth flow after sign out
                    startActivity(new Intent(MainActivity.this, FirebaseUIActivity.class));
                    finish();
                });
    }

    private void stopStepCounterService() {
        try {
            // Dừng service đếm bước chân
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            boolean stopped = stopService(serviceIntent);
            Log.d(TAG, "Dừng service đếm bước chân: " + (stopped ? "thành công" : "thất bại"));
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi dừng service đếm bước chân: ", e);
        }
    }
}