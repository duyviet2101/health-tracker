package com.example.healthtracker.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.healthtracker.R;
import com.example.healthtracker.services.StepCounterService;

import java.text.DecimalFormat;
import java.util.Random;

public class StepCounterActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "StepCounterActivity";
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    private static final int ACTIVITY_RECOGNITION_PERMISSION_CODE = 1002;
    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String LAST_STEP_COUNT_KEY = "lastStepCount";
    private static final String CURRENT_STEPS_KEY = "currentSteps";
    
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private Sensor accelerometer;
    private boolean isCounting = false;
    private int stepCount = 0;
    private int lastStepCount = 0;
    private float previousMagnitude = 0;
    private boolean isFirstStep = true;
    
    private final float stepLength = 0.762f; // Chiều dài trung bình của một bước chân (mét)
    private final float caloriesPerStep = 0.04f; // Lượng calo đốt cháy trung bình cho mỗi bước
    private final float STEP_THRESHOLD = 10.0f; // Ngưỡng để xác định bước chân

    private TextView tvSteps;
    private TextView tvDistance;
    private TextView tvCalories;
    private Button btnStartStop;
    private BottomNavigationView bottomNavigationView;
    
    // Handler và Runnable để mô phỏng cảm biến bước chân trên máy ảo
    private Handler simulationHandler;
    private Runnable simulationRunnable;
    private Random random;
    private boolean isEmulator = false;
    
    // Để xác định bước chân từ gia tốc kế
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];
    private static final float ALPHA = 0.8f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_counter);
        Log.d(TAG, "onCreate");
        
        // Kiểm tra và xin quyền hiển thị thông báo
        requestPermissions();
        
        // Khởi tạo UI
        tvSteps = findViewById(R.id.tvSteps);
        tvDistance = findViewById(R.id.tvDistance);
        tvCalories = findViewById(R.id.tvCalories);
        btnStartStop = findViewById(R.id.btnStartStop);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        // Khôi phục dữ liệu bước chân đã lưu
        loadStepData();

        // Khởi tạo cảm biến
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            if (stepSensor == null) {
                Log.d(TAG, "No step counter sensor, will use accelerometer instead");
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                if (accelerometer == null) {
                    Log.d(TAG, "No accelerometer available either");
                }
            } else {
                Log.d(TAG, "Step counter sensor available");
            }
        }
        
        // Kiểm tra xem có phải là máy ảo không
        isEmulator = isEmulator();
        if (isEmulator) {
            // Nếu là máy ảo, khởi tạo bộ mô phỏng
            Log.d(TAG, "Running on emulator. Using simulation.");
            simulationHandler = new Handler();
            random = new Random();
            simulationRunnable = new Runnable() {
                @Override
                public void run() {
                    // Tăng số bước ngẫu nhiên từ 1-3 bước mỗi 1-3 giây
                    stepCount += random.nextInt(3) + 1;
                    updateUI();
                    simulationHandler.postDelayed(this, 1000 + random.nextInt(2000));
                }
            };
        }

        btnStartStop.setOnClickListener(v -> {
            if (!isCounting) {
                startCounting();
            } else {
                stopCounting();
            }
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_steps) {
                // Đã ở trang bước chân, không cần làm gì
                return true;
            }
            return false;
        });
        
        // Hiển thị UI ban đầu
        updateUI();
    }
    
    private void requestPermissions() {
        // Xin quyền thông báo cho Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
        
        // Xin quyền ACTIVITY_RECOGNITION cho Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        ACTIVITY_RECOGNITION_PERMISSION_CODE);
            }
        }
    }
    
    private void loadStepData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        lastStepCount = prefs.getInt(LAST_STEP_COUNT_KEY, 0);
        stepCount = prefs.getInt(CURRENT_STEPS_KEY, 0);
        Log.d(TAG, "Loaded from prefs: lastStepCount=" + lastStepCount + ", stepCount=" + stepCount);
    }
    
    private void saveStepData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(LAST_STEP_COUNT_KEY, lastStepCount);
        editor.putInt(CURRENT_STEPS_KEY, stepCount);
        editor.apply();
        Log.d(TAG, "Saved to prefs: lastStepCount=" + lastStepCount + ", stepCount=" + stepCount);
    }

    // Phương thức kiểm tra xem có phải máy ảo không
    private boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

    private void startCounting() {
        try {
            isCounting = true;
            btnStartStop.setText("Dừng");
            
            if (isEmulator) {
                // Sử dụng bộ mô phỏng
                simulationHandler.post(simulationRunnable);
                Toast.makeText(this, "Đang mô phỏng đếm bước chân trên máy ảo", Toast.LENGTH_SHORT).show();
            } else if (stepSensor != null) {
                // Sử dụng cảm biến bước chân
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                Toast.makeText(this, "Đang sử dụng cảm biến bước chân", Toast.LENGTH_SHORT).show();
            } else if (accelerometer != null) {
                // Sử dụng gia tốc kế
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                Toast.makeText(this, "Đang sử dụng gia tốc kế để đếm bước chân", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không tìm thấy cảm biến phù hợp", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Start service 
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            serviceIntent.putExtra("INITIAL_STEP_COUNT", stepCount);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Started counting and service, stepCount=" + stepCount);
        } catch (Exception e) {
            Log.e(TAG, "Error starting counting: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi bắt đầu đếm bước chân", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopCounting() {
        try {
            isCounting = false;
            btnStartStop.setText("Bắt đầu");
            
            if (isEmulator) {
                // Dừng bộ mô phỏng
                simulationHandler.removeCallbacks(simulationRunnable);
            } else if (sensorManager != null) {
                // Dừng lắng nghe cảm biến
                sensorManager.unregisterListener(this);
            }
            
            // Stop service
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            stopService(serviceIntent);
            Log.d(TAG, "Stopped counting and service");
            
            // Lưu dữ liệu bước chân
            saveStepData();
        } catch (Exception e) {
            Log.e(TAG, "Error stopping counting: " + e.getMessage());
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                // Dùng cảm biến bước chân
                int steps = (int) event.values[0];
                if (isFirstStep) {
                    lastStepCount = steps;
                    isFirstStep = false;
                }
                stepCount = steps - lastStepCount;
                Log.d(TAG, "Step sensor: total=" + steps + ", counted=" + stepCount);
                updateUI();
            } else if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // Dùng gia tốc kế để đếm bước chân
                detectStepsFromAccelerometer(event);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onSensorChanged: " + e.getMessage());
        }
    }
    
    private void detectStepsFromAccelerometer(SensorEvent event) {
        // Lọc nhiễu sử dụng low-pass filter
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];

        // Loại bỏ trọng lực để lấy gia tốc thực
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
        
        // Tính độ lớn của gia tốc
        float magnitude = (float) Math.sqrt(
                linear_acceleration[0] * linear_acceleration[0] +
                linear_acceleration[1] * linear_acceleration[1] +
                linear_acceleration[2] * linear_acceleration[2]);
        
        float magnitudeDelta = magnitude - previousMagnitude;
        previousMagnitude = magnitude;
        
        // Nếu sự thay đổi đủ lớn và vượt qua ngưỡng, coi là một bước
        if (magnitudeDelta > STEP_THRESHOLD) {
            stepCount++;
            updateUI();
            Log.d(TAG, "Step detected from accelerometer, count=" + stepCount);
        }
    }

    private void updateUI() {
        tvSteps.setText(String.valueOf(stepCount));
        
        float distance = stepCount * stepLength / 1000; // Chuyển đổi sang km
        float calories = stepCount * caloriesPerStep;
        
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        tvDistance.setText(decimalFormat.format(distance));
        tvCalories.setText(decimalFormat.format(calories));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (isCounting) {
            // Không dừng service khi app bị pause
            if (isEmulator) {
                // Dừng bộ mô phỏng tạm thời
                simulationHandler.removeCallbacks(simulationRunnable);
            } else {
                // Chỉ unregister sensor listener
                try {
                    if (sensorManager != null) {
                        sensorManager.unregisterListener(this);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in onPause: " + e.getMessage());
                }
            }
        }
        
        // Lưu trạng thái hiện tại
        saveStepData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (isCounting) {
            if (isEmulator) {
                // Khởi động lại bộ mô phỏng
                simulationHandler.post(simulationRunnable);
            } else if (stepSensor != null) {
                try {
                    // Đăng ký lại step sensor
                    sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                } catch (Exception e) {
                    Log.e(TAG, "Error registering step sensor: " + e.getMessage());
                }
            } else if (accelerometer != null) {
                try {
                    // Đăng ký lại accelerometer
                    sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                } catch (Exception e) {
                    Log.e(TAG, "Error registering accelerometer: " + e.getMessage());
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (isEmulator) {
            // Dừng bộ mô phỏng hoàn toàn
            if (simulationHandler != null && simulationRunnable != null) {
                simulationHandler.removeCallbacks(simulationRunnable);
            }
        } else {
            try {
                if (sensorManager != null) {
                    sensorManager.unregisterListener(this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in onDestroy: " + e.getMessage());
            }
        }
        
        // Lưu trạng thái cuối cùng
        saveStepData();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền hiển thị thông báo", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần quyền hiển thị thông báo để đếm bước chân", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == ACTIVITY_RECOGNITION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền nhận diện hoạt động", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần quyền nhận diện hoạt động để đếm bước chân", Toast.LENGTH_LONG).show();
            }
        }
    }
} 