package com.example.healthtracker.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.example.healthtracker.R;
import com.example.healthtracker.StepCounterData;
import com.example.healthtracker.StepCounterService;
import com.example.healthtracker.fragments.MenuAccountFragment;
import com.example.healthtracker.models.ScreenshotSharer;
import com.example.healthtracker.models.StepsDataHelper;
import com.example.healthtracker.services.UserService;
import com.firebase.ui.auth.AuthUI;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_ACTIVITY_RECOGNITION = 1001;
    private final int lastSteps = 0;
    private final double lastDistance = 0.0;
    private final double lastCalories = 0.0;
    private final long lastActiveTime = 0;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        userService = new UserService();

        new StepsDataHelper(this).copyJsonToInternalStorage();

        // Khởi tạo quản lý dữ liệu bước chân
        stepData = StepCounterData.getInstance(this);

        View outerRing2 = findViewById(R.id.outerRing2);
        outerRing2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Chuyển đến activity chi tiết thống kê
                Intent intent = new Intent(MainActivity.this, DetailsStatisticsActivity.class);
                startActivity(intent);
            }
        });

        View statistic = findViewById(R.id.statistic);
        statistic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DetailsStatisticsActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.shareButton).setOnClickListener(v -> ScreenshotSharer.captureAndShareScreen(MainActivity.this));


        // Khởi tạo các view đếm bước chân
        initStepCountViews();


        avatarCard = findViewById(R.id.avatarCard);
        imgAvatar = findViewById(R.id.imgAvatar);
        txtName = findViewById(R.id.txtName);

        avatarCard.setOnClickListener(v -> {
            // Show a logout option when the avatar is clicked
            MenuAccountFragment menuAccountFragment = new MenuAccountFragment();
            menuAccountFragment.show(getSupportFragmentManager(), "MenuAccountFragment");
        });

        checkAndRequestPermission();
        setupStepUpdateReceiver();
    }

    private void checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_ACTIVITY_RECOGNITION);
            } else {
                startStepCounterService();
            }
        } else {
            startStepCounterService();
        }
    }

    private void setupStepUpdateReceiver() {
        stepUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int steps = intent.getIntExtra(StepCounterService.EXTRA_STEPS, 0);
                double distance = intent.getDoubleExtra(StepCounterService.EXTRA_DISTANCE, 0);
                double calories = intent.getDoubleExtra(StepCounterService.EXTRA_CALORIES, 0);
                long activeTime = intent.getLongExtra(StepCounterService.EXTRA_TIME, 0);

                stepCountText.setText(String.format("%,d", steps));
                caloriesValue.setText(String.format("%.0f", calories));
                timeValue.setText(formatTime(activeTime));
                distanceValue.setText(distance >= 1000 ?
                        String.format("%.2f km", distance / 1000) :
                        String.format("%.0f m", distance));
            }
        };
        IntentFilter filter = new IntentFilter(StepCounterService.ACTION_STEPS_UPDATED);
        LocalBroadcastManager.getInstance(this).registerReceiver(stepUpdateReceiver, filter); // LocalBroadcast đảm bảo realtime
        // Setup water card
        MaterialCardView waterCard = findViewById(R.id.waterCard);
        waterCard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, WaterTrackingActivity.class);
            startActivity(intent);
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

    private void startStepCounterService() {
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
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


    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký lại receiver để đảm bảo nhận broadcast
        setupStepUpdateReceiver();

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
//                            txtName.setText(user.getDisplayName());
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
        if (stepUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(stepUpdateReceiver); // Gỡ đúng cách
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startStepCounterService();
        }
    }
}