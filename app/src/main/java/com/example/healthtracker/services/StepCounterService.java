package com.example.healthtracker.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;
import com.example.healthtracker.database.DBHelper;
import com.example.healthtracker.utils.SharedPreferencesManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String TAG = "StepCounterService";
    private static final String CHANNEL_ID = "step_counter_channel";
    private static final int NOTIFICATION_ID = 1001;

    private SensorManager sensorManager;
    private Sensor stepSensor;
    private NotificationManager notificationManager;
    private SharedPreferences prefs;
    private DBHelper dbHelper;
    private Handler handler;
    private Runnable updateRunnable;
    private PowerManager.WakeLock wakeLock;

    private int initialStepCount = 0;
    private int currentStepCount = 0;
    private float distanceInKm = 0;
    private int caloriesBurned = 0;
    private long startTimeMillis = 0;

    private int stepGoal = 2000; // Default step goal
    private float strideLength = 0.75f; // Default stride length in meters
    private int weight = 70; // Default weight in kg

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        prefs = getSharedPreferences("HealthTrackerPrefs", MODE_PRIVATE);
        dbHelper = new DBHelper(this);
        handler = new Handler();

        // Load user settings
        loadUserSettings();

        // Initialize sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // Initialize notification manager
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        // Create wake lock to keep CPU running
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "HealthTracker:StepCounterWakelockTag");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "START":
                    startStepCounting();
                    break;
                case "STOP":
                    stopStepCounting();
                    break;
            }
        } else {
            // Check if we should restore state
            boolean isRunning = prefs.getBoolean("isRunning", false);
            if (isRunning) {
                restoreState();
            } else {
                // No explicit start command and no previous state, stop self
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        // Return sticky to restart service if it gets killed
        return START_STICKY;
    }

    private void startStepCounting() {
        // Acquire wake lock to keep CPU running
        if (!wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        // Register step counter sensor
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.e(TAG, "Step sensor not available on this device");
            stopSelf();
            return;
        }

        // Initialize counting
        startTimeMillis = SystemClock.elapsedRealtime();
        
        // Schedule periodic updates for notification
        handler.postDelayed(getUpdateRunnable(), 1000);

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, buildNotification(0, 0, 0, "0:00"));

        // Save state
        prefs.edit()
                .putBoolean("isRunning", true)
                .putLong("startTime", startTimeMillis)
                .putInt("initialSteps", initialStepCount)
                .apply();
    }

    private void stopStepCounting() {
        // Unregister sensor listener
        sensorManager.unregisterListener(this);

        // Save activity data to database
        saveActivityToDatabase();

        // Remove callback
        handler.removeCallbacks(updateRunnable);

        // Release wake lock
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Clear running state
        prefs.edit().putBoolean("isRunning", false).apply();

        // Stop foreground service
        stopForeground(true);
        stopSelf();
    }

    private void restoreState() {
        // Restore saved state
        startTimeMillis = prefs.getLong("startTime", SystemClock.elapsedRealtime());
        initialStepCount = prefs.getInt("initialSteps", 0);

        // Start counting again
        startStepCounting();
    }

    private Runnable getUpdateRunnable() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                updateNotification();
                handler.postDelayed(this, 1000);
            }
        };
        return updateRunnable;
    }

    private void updateNotification() {
        int steps = currentStepCount - initialStepCount;
        long elapsedTime = SystemClock.elapsedRealtime() - startTimeMillis;
        String timeFormatted = formatTime(elapsedTime);

        // Calculate distance and calories
        updateCalculations(steps);

        Notification notification = buildNotification(
                steps, distanceInKm, caloriesBurned, timeFormatted);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification buildNotification(int steps, float distance, int calories, String time) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, StepCounterService.class);
        stopIntent.setAction("STOP");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        int percentComplete = (int) ((float) steps / stepGoal * 100);
        if (percentComplete > 100) percentComplete = 100;

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Đang theo dõi hoạt động")
                .setContentText(steps + " bước (" + percentComplete + "%) - " + String.format(Locale.getDefault(), "%.2f", distance) + " km")
                .setSmallIcon(R.drawable.steps_24px)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_pause, "Dừng lại", stopPendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(steps + " bước (" + percentComplete + "%)\n" +
                                String.format(Locale.getDefault(), "%.2f", distance) + " km - " + calories + " kcal\n" +
                                "Thời gian: " + time))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Counter Service",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Hiển thị tiến trình đếm bước chân");
            channel.enableLights(false);
            channel.setLightColor(Color.BLUE);
            channel.enableVibration(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void updateCalculations(int steps) {
        // Calculate distance based on steps and stride length
        distanceInKm = steps * strideLength / 1000;
        
        // Calculate calories burned based on steps, weight and average calorie burn per step
        // A rough estimate: calories = weight(kg) * distance(km) * 1.036
        caloriesBurned = (int) (weight * distanceInKm * 1.036);
    }

    private void saveActivityToDatabase() {
        int steps = currentStepCount - initialStepCount;
        if (steps <= 0) return;
        
        long duration = SystemClock.elapsedRealtime() - startTimeMillis;
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        
        dbHelper.addStepHistory(date, time, steps, distanceInKm, caloriesBurned, duration);
    }

    private String formatTime(long millis) {
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private void loadUserSettings() {
        SharedPreferencesManager prefsManager = new SharedPreferencesManager(this);
        stepGoal = prefsManager.getDailyStepGoal();
        weight = prefs.getInt("userWeight", 70);
        strideLength = prefs.getFloat("strideLength", 0.75f);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount == 0) {
                // First reading - initialize
                initialStepCount = (int) event.values[0];
                prefs.edit().putInt("initialSteps", initialStepCount).apply();
            }
            currentStepCount = (int) event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for step counter
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroyed");
        
        // Unregister sensor listener
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Remove callbacks
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
        
        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // We don't support binding
    }
} 