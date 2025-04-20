package com.example.healthtracker.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
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

    //Cảm biến
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;

    // Giá trị ban đầu của biến đếm bước chân
    private int stepCount = 0;

    // Độ dài 1 bước chân
    private double stepLengthInMeter = 0.4f;

    // Thời gian bắt đầu đi
    private long startTime;


    // Để nhận cập nhật từ StepCounterService
    private BroadcastReceiver stepUpdateReceiver;
    private int lastSteps = 0;
    private double lastDistance = 0.0;
    private double lastCalories = 0.0;
    private long lastActiveTime = 0;

    private Handler timerHandler = new Handler();

    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long milis = System.currentTimeMillis() - startTime;
            int seconds = (int)(milis/1000);
            int mins = seconds/60;
            int hours = mins/60;
            timeValue.setText(String.format(Locale.getDefault(), "%02d:%02d", hours, mins));
            timerHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stepCountText = findViewById(R.id.stepCountText);
        distanceValue = findViewById(R.id.distanceValue);
        timeValue = findViewById(R.id.timeValue);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        startTime = System.currentTimeMillis();

        if (stepCounterSensor == null) {
            Toast.makeText(this, "Không có cảm biến đếm bước chân", Toast.LENGTH_LONG).show();
        }

        mAuth = FirebaseAuth.getInstance();
        userService = new UserService();



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
        // Nếu có cảm biến đếm bước chân thì nghe hành động từ cảm biến
        if (stepCounterSensor != null) {
            sensorManager.registerListener((SensorEventListener) this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);
            timerHandler.postDelayed(timerRunnable, 0);
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
//            startStepCounterService();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (stepCounterSensor != null) {
            sensorManager.unregisterListener((SensorEventListener) this);
            timerHandler.removeCallbacks(timerRunnable);
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepCount = (int) sensorEvent.values[0];
            stepCountText.setText(stepCount);

            double distanceInMeters = stepCount * stepLengthInMeter;

            double calories = stepCount * 0.045;    // Mỗi bước tiêu tốn 0.045 calo
            caloriesValue.setText(String.format(Locale.getDefault(), "%.2f", calories));

            if (distanceInMeters >= 1000) {
                distanceValue.setText(String.format(Locale.getDefault(), "%.2f km", distanceInMeters/1000));
            }
            else {
                distanceValue.setText(String.format(Locale.getDefault(), "%.2f m", distanceInMeters));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}