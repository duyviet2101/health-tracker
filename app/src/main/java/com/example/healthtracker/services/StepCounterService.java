package com.example.healthtracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.StepCounterActivity;

import java.util.Random;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String TAG = "StepCounterService";
    private static final String CHANNEL_ID = "StepCounterChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String LAST_STEP_COUNT_KEY = "lastStepCount";
    private static final String CURRENT_STEPS_KEY = "currentSteps";
    private static final float ALPHA = 0.8f; // Cho low-pass filter
    private static final float STEP_THRESHOLD = 10.0f; // Ngưỡng để xác định bước chân

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private Sensor accelerometer;
    private int stepCount = 0;
    private int lastStepCount = 0;
    private NotificationManager notificationManager;
    private boolean isFirstStep = true;
    private float previousMagnitude = 0;
    
    // Biến cho mô phỏng
    private Handler simulationHandler;
    private Runnable simulationRunnable;
    private Random random;
    private boolean isEmulator = false;
    
    // Để xác định bước chân từ gia tốc kế
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        try {
            // Khởi tạo sensor manager và step sensor
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                if (stepSensor == null) {
                    Log.d(TAG, "No step counter sensor, will use accelerometer");
                    accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                    if (accelerometer == null) {
                        Log.d(TAG, "No accelerometer available either");
                    }
                } else {
                    Log.d(TAG, "Step counter sensor available");
                }
            }
            
            // Khôi phục dữ liệu bước chân
            loadStepData();
            
            // Kiểm tra xem có phải máy ảo không
            isEmulator = isEmulator();
            Log.d(TAG, "isEmulator: " + isEmulator);
            
            // Nếu là máy ảo hoặc không có cảm biến, chuẩn bị mô phỏng
            if (isEmulator) {
                Log.d(TAG, "Initializing step simulation for emulator");
                simulationHandler = new Handler();
                random = new Random();
                simulationRunnable = new Runnable() {
                    @Override
                    public void run() {
                        // Tăng số bước ngẫu nhiên từ 1-3 bước mỗi 1-3 giây
                        stepCount += random.nextInt(3) + 1;
                        updateNotification();
                        simulationHandler.postDelayed(this, 1000 + random.nextInt(2000));
                    }
                };
            }
            
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            createNotificationChannel();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage());
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        try {
            // Lấy số bước chân ban đầu từ intent nếu có
            if (intent != null && intent.hasExtra("INITIAL_STEP_COUNT")) {
                int initialStepCount = intent.getIntExtra("INITIAL_STEP_COUNT", 0);
                Log.d(TAG, "Received initial step count: " + initialStepCount);
                stepCount = initialStepCount;
            }
            
            // Bắt đầu với thông báo foreground
            startForeground(NOTIFICATION_ID, createNotification());
            
            if (isEmulator) {
                // Sử dụng mô phỏng trên máy ảo
                Log.d(TAG, "Using step simulation on emulator");
                if (simulationHandler != null && simulationRunnable != null) {
                    simulationHandler.post(simulationRunnable);
                }
            } else if (stepSensor != null) {
                // Sử dụng cảm biến bước chân
                Log.d(TAG, "Using real step sensor");
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else if (accelerometer != null) {
                // Sử dụng gia tốc kế
                Log.d(TAG, "Using accelerometer for step counting");
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            } else {
                Log.e(TAG, "No suitable sensor available");
                stopSelf();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: " + e.getMessage());
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");
        try {
            if (isEmulator) {
                // Dừng mô phỏng
                if (simulationHandler != null && simulationRunnable != null) {
                    simulationHandler.removeCallbacks(simulationRunnable);
                }
            } else if (sensorManager != null) {
                // Dừng lắng nghe cảm biến
                sensorManager.unregisterListener(this);
            }
            
            // Lưu dữ liệu bước chân khi dừng service
            saveStepData();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
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
                updateNotification();
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
            updateNotification();
            Log.d(TAG, "Step detected from accelerometer, count=" + stepCount);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Step Counter Channel",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Channel for step counter notifications");
                notificationManager.createNotificationChannel(channel);
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: " + e.getMessage());
            }
        }
    }

    private Notification createNotification() {
        try {
            Intent notificationIntent = new Intent(this, StepCounterActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
            );

            String contentTitle = isEmulator ? "Mô phỏng bước chân" : "Đang đếm bước chân";
            String contentText = isEmulator ? "Mô phỏng: " + stepCount + " bước" : "Bước chân: " + stepCount;
            
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setSmallIcon(R.drawable.ic_walk)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification: " + e.getMessage());
            return null;
        }
    }

    private void updateNotification() {
        try {
            String contentTitle = isEmulator ? "Mô phỏng bước chân" : "Đang đếm bước chân";
            String contentText = isEmulator ? "Mô phỏng: " + stepCount + " bước" : "Bước chân: " + stepCount;
            
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setSmallIcon(R.drawable.ic_walk)
                    .setOngoing(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();
            
            notificationManager.notify(NOTIFICATION_ID, notification);
            
            // Lưu dữ liệu bước chân định kỳ
            saveStepData();
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: " + e.getMessage());
        }
    }
} 