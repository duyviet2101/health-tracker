package com.example.stepsapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class StepCounterService extends Service implements SensorEventListener {
    private static final String TAG = "StepCounterService";
    private static final String CHANNEL_ID = "StepCounterChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final long WAKELOCK_TIMEOUT = 12 * 60 * 60 * 1000L; // 12 giờ

    // Quản lý cảm biến
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private boolean isStepSensorPresent = false;

    // Đếm bước chân
    private int currentSteps = 0;
    private int initialSteps = 0;
    private boolean isInitialized = false;

    // Mô phỏng đếm bước cho máy ảo
    private boolean isEmulatorMode = false;
    private Handler emulatorHandler;
    private Runnable emulatorRunnable;
    
    // Thời điểm bước cuối cùng được đếm trong chế độ giả lập
    private long lastEmulatedStepTime = 0;
    // Khoảng thời gian giữa các bước (ms) - mặc định 1 giây
    private static final long STEP_INTERVAL = 1000;

    // Giữ cho service chạy khi màn hình tắt
    private PowerManager.WakeLock wakeLock;
    
    // Quản lý dữ liệu
    private StepCounterData stepData;
    
    // Để cập nhật UI
    public static final String ACTION_STEPS_UPDATED = "com.example.stepsapp.STEPS_UPDATED";
    public static final String EXTRA_STEPS = "extra_steps";
    public static final String EXTRA_DISTANCE = "extra_distance";
    public static final String EXTRA_CALORIES = "extra_calories";
    public static final String EXTRA_TIME = "extra_time";
    
    // Handler cho việc kiểm tra đặt lại dữ liệu hàng ngày
    private Handler dailyResetHandler;
    private Runnable dailyResetCheck;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Khởi tạo service");
        
        try {
            // Khởi tạo quản lý dữ liệu
            stepData = StepCounterData.getInstance(this);
            currentSteps = stepData.getSteps();
            
            // Khởi tạo cảm biến
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                // Tìm kiếm cảm biến đếm bước chân
                stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                
                if (stepSensor != null) {
                    // Thiết bị có cảm biến đếm bước chân thực
                    isStepSensorPresent = true;
                    Log.d(TAG, "Đã tìm thấy cảm biến bước chân thật, sử dụng cảm biến thật để đếm");
                } else {
                    // Không tìm thấy cảm biến bước chân, kiểm tra xem có phải máy ảo không
                    Log.d(TAG, "Không tìm thấy cảm biến bước chân, chuyển sang chế độ mô phỏng");
                    isEmulatorMode = true;
                }
            }

            // Xác định có phải là máy ảo không bằng nhiều phương pháp
            if (isEmulator()) {
                isEmulatorMode = true;
                Log.d(TAG, "Phát hiện đang chạy trên máy ảo, ép buộc chế độ mô phỏng");
            }

            // Thiết lập chế độ mô phỏng cho máy ảo
            if (isEmulatorMode) {
                Log.d(TAG, "Đang chạy ở chế độ mô phỏng - mỗi giây sẽ tăng 1 bước");
                setupEmulatorMode();
            }

            // Khởi tạo WakeLock để giữ service chạy khi màn hình tắt
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "StepsApp:WakeLock");
            }
            
            // Thiết lập kiểm tra đặt lại dữ liệu hàng ngày
            setupDailyReset();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
        }
    }

    /**
     * Phương thức kiểm tra có phải đang chạy trên máy ảo không
     * Kiểm tra bằng nhiều điều kiện khác nhau
     */
    private boolean isEmulator() {
        // Kiểm tra các đặc điểm phổ biến của máy ảo
        return (Build.BRAND.startsWith("generic") || 
                Build.DEVICE.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.PRODUCT.contains("sdk_google") ||
                Build.PRODUCT.contains("google_sdk") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("sdk_x86") ||
                Build.PRODUCT.contains("vbox86p") ||
                Build.PRODUCT.contains("emulator") ||
                Build.PRODUCT.contains("simulator"));
    }

    /**
     * Thiết lập chế độ mô phỏng đếm bước cho máy ảo
     * Sẽ tăng 1 bước mỗi giây
     */
    private void setupEmulatorMode() {
        try {
            // Lưu lại thời điểm bắt đầu
            lastEmulatedStepTime = System.currentTimeMillis();
            
            emulatorHandler = new Handler(Looper.getMainLooper());
            emulatorRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        // Lấy thời gian hiện tại
                        long currentTime = System.currentTimeMillis();
                        
                        // Chỉ tăng bước nếu đã đủ khoảng thời gian
                        if (currentTime - lastEmulatedStepTime >= STEP_INTERVAL) {
                            // Tăng 1 bước
                            currentSteps++;
                            
                            // Lưu thời điểm tăng bước mới nhất
                            lastEmulatedStepTime = currentTime;
                            
                            // Lưu số bước mới vào bộ nhớ
                            stepData.saveSteps(currentSteps);
                            
                            // Gửi broadcast để cập nhật UI
                            broadcastStepCount();
                            
                            // Cập nhật thông báo
                            updateNotification();
                            
                            Log.d(TAG, "Mô phỏng: Tăng 1 bước, tổng số bước hiện tại = " + currentSteps);
                        }
                        
                        // Lập lịch chạy lại sau 1 giây
                        if (emulatorHandler != null) {
                            emulatorHandler.postDelayed(this, 1000);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in emulator runnable: ", e);
                    }
                }
            };
        } catch (Exception e) {
            Log.e(TAG, "Error setting up emulator mode: ", e);
        }
    }
    
    private void setupDailyReset() {
        try {
            dailyResetHandler = new Handler(Looper.getMainLooper());
            dailyResetCheck = new Runnable() {
                @Override
                public void run() {
                    try {
                        // Kiểm tra và đặt lại dữ liệu nếu cần
                        stepData.resetIfNeeded();
                        // Cập nhật lại dữ liệu từ bộ nhớ
                        currentSteps = stepData.getSteps();
                        // Phát lại thông tin cập nhật
                        broadcastStepCount();
                        // Lên lịch kiểm tra tiếp theo (mỗi giờ)
                        if (dailyResetHandler != null) {
                            dailyResetHandler.postDelayed(this, 60 * 60 * 1000);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in daily reset check: ", e);
                    }
                }
            };
        } catch (Exception e) {
            Log.e(TAG, "Error setting up daily reset: ", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: Bắt đầu service");
        
        try {
            // Tạo notification channel (chỉ cho Android 8.0+)
            createNotificationChannel();
            
            // Khởi chạy service ở foreground với notification
            Notification notification = buildNotification();
            if (notification != null) {
                startForeground(NOTIFICATION_ID, notification);
            }
            
            // Bắt đầu đếm bước
            if (isStepSensorPresent && sensorManager != null && stepSensor != null) {
                // CHẾ ĐỘ MÁY THẬT: Đăng ký lắng nghe sự kiện từ cảm biến bước chân
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Đã kích hoạt chế độ đếm bước thật với cảm biến");
            } else if (isEmulatorMode && emulatorHandler != null && emulatorRunnable != null) {
                // CHẾ ĐỘ MÁY ẢO: Bắt đầu mô phỏng đếm bước
                emulatorHandler.post(emulatorRunnable);
                Log.d(TAG, "Đã kích hoạt chế độ mô phỏng đếm bước");
            }
            
            // Giữ CPU chạy ngay cả khi màn hình tắt
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(WAKELOCK_TIMEOUT);
                Log.d(TAG, "Acquired wake lock");
            }
            
            // Bắt đầu kiểm tra đặt lại dữ liệu hàng ngày
            if (dailyResetHandler != null && dailyResetCheck != null) {
                dailyResetHandler.post(dailyResetCheck);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: ", e);
        }
        
        // Đảm bảo service sẽ khởi động lại nếu bị kill
        return START_STICKY;
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Step Counter Service Channel",
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Kênh thông báo cho dịch vụ đếm bước chân");
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel: ", e);
        }
    }

    private Notification buildNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Đang đếm bước chân")
                    .setContentText("Số bước: " + currentSteps)
                    .setSmallIcon(R.drawable.ic_footsteps)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Error building notification: ", e);
            // Tạo thông báo đơn giản nhất nếu có lỗi
            return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Đang đếm bước chân")
                    .setContentText("Đang chạy")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .setOngoing(true)
                    .build();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // XỬ LÝ CHO MÁY THẬT - Được gọi mỗi khi cảm biến bước chân ghi nhận thay đổi
        if (event != null && event.sensor != null && event.sensor.getType() == Sensor.TYPE_STEP_COUNTER && event.values != null && event.values.length > 0) {
            try {
                int totalSteps = (int) event.values[0];
                
                // Khởi tạo giá trị ban đầu khi service mới chạy
                if (!isInitialized) {
                    initialSteps = totalSteps;
                    isInitialized = true;
                    
                    // Nếu đã lưu số bước trước đó, sử dụng giá trị đó
                    if (currentSteps > 0) {
                        initialSteps = totalSteps - currentSteps;
                    }
                }
                
                // Tính toán số bước hiện tại
                currentSteps = totalSteps - initialSteps;
                
                // Lưu vào dữ liệu
                stepData.saveSteps(currentSteps);
                
                // Cập nhật notification
                updateNotification();
                
                // Gửi broadcast để cập nhật UI
                broadcastStepCount();
                
                Log.d(TAG, "Cảm biến thật: Nhận sự kiện bước chân, tổng số bước = " + currentSteps);
            } catch (Exception e) {
                Log.e(TAG, "Error in onSensorChanged: ", e);
            }
        }
    }
    
    private void updateNotification() {
        try {
            NotificationManager notificationManager = 
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, buildNotification());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: ", e);
        }
    }

    private void broadcastStepCount() {
        try {
            Intent intent = new Intent(ACTION_STEPS_UPDATED);
            intent.putExtra(EXTRA_STEPS, currentSteps);
            
            // Tính toán khoảng cách (mét)
            double distance = stepData.calculateDistance(currentSteps);
            intent.putExtra(EXTRA_DISTANCE, distance);
            
            // Tính toán calo
            double calories = stepData.calculateCalories(currentSteps);
            intent.putExtra(EXTRA_CALORIES, calories);
            
            // Tính thời gian hoạt động (phút)
            long activeTime = stepData.calculateActiveTime();
            intent.putExtra(EXTRA_TIME, activeTime);
            
            // Từ Android 14, gửi broadcast theo cả hai cách để đảm bảo tương thích
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Gửi cho StepCountReceiver đã đăng ký trong manifest
                intent.setPackage(getPackageName());
                sendBroadcast(intent);
                Log.d(TAG, "Đã gửi broadcast cho receiver đã đăng ký trong manifest");
                
                // Gửi cho receivers đã đăng ký động (trong activity)
                Intent localIntent = new Intent("com.example.stepsapp.LOCAL_STEPS_UPDATED");
                localIntent.putExtras(intent);
                localIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
                sendBroadcast(localIntent);
                Log.d(TAG, "Đã gửi local broadcast với FLAG_RECEIVER_REGISTERED_ONLY");
            } else {
                // Với Android phiên bản cũ, gửi theo cách thông thường
                sendBroadcast(intent);
                Log.d(TAG, "Đã gửi broadcast theo cách thông thường");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error broadcasting step count: ", e);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không làm gì khi độ chính xác thay đổi
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Service không hỗ trợ binding
    }
    
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Đảm bảo service tiếp tục chạy khi người dùng xóa ứng dụng khỏi recent apps
        try {
            Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
            restartServiceIntent.setPackage(getPackageName());
            PendingIntent restartServicePendingIntent = PendingIntent.getService(
                    getApplicationContext(), 1, restartServiceIntent, PendingIntent.FLAG_IMMUTABLE);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Log.d(TAG, "Using AlarmManager not supported on Android 13+");
            } else {
                android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext()
                        .getSystemService(Context.ALARM_SERVICE);
                if (alarmService != null) {
                    alarmService.set(android.app.AlarmManager.ELAPSED_REALTIME,
                            android.os.SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onTaskRemoved: ", e);
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Dừng service");
        
        try {
            // Hủy đăng ký listener cảm biến
            if (isStepSensorPresent && sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
            
            // Dừng mô phỏng nếu đang chạy
            if (isEmulatorMode && emulatorHandler != null && emulatorRunnable != null) {
                emulatorHandler.removeCallbacks(emulatorRunnable);
                emulatorRunnable = null;
            }
            
            // Dừng kiểm tra đặt lại dữ liệu hàng ngày
            if (dailyResetHandler != null && dailyResetCheck != null) {
                dailyResetHandler.removeCallbacks(dailyResetCheck);
                dailyResetCheck = null;
            }
            
            // Giải phóng WakeLock
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "Released wake lock");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: ", e);
        } finally {
            super.onDestroy();
        }
    }
} 