package com.example.healthtracker.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
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

public class StepCounterActivity extends AppCompatActivity implements SensorEventListener {
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isCounting = false;
    private int stepCount = 0;
    private int lastStepCount = 0;
    private final float stepLength = 0.762f; // Chiều dài trung bình của một bước chân (mét)
    private final float caloriesPerStep = 0.04f; // Lượng calo đốt cháy trung bình cho mỗi bước

    private TextView tvSteps;
    private TextView tvDistance;
    private TextView tvCalories;
    private Button btnStartStop;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_counter);

        // Kiểm tra và xin quyền hiển thị thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }

        tvSteps = findViewById(R.id.tvSteps);
        tvDistance = findViewById(R.id.tvDistance);
        tvCalories = findViewById(R.id.tvCalories);
        btnStartStop = findViewById(R.id.btnStartStop);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

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
    }

    private void startCounting() {
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            isCounting = true;
            btnStartStop.setText("Dừng");
            
            // Start service
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            Toast.makeText(this, "Không tìm thấy cảm biến bước chân", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopCounting() {
        sensorManager.unregisterListener(this);
        isCounting = false;
        btnStartStop.setText("Bắt đầu");
        lastStepCount = stepCount;
        
        // Stop service
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            stepCount = (int) event.values[0] - lastStepCount;
            updateUI();
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
        if (isCounting) {
            // Không dừng service khi app bị pause
            // Chỉ unregister sensor listener
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCounting) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
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
        }
    }
} 