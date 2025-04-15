package com.example.healthtracker;

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

import com.example.healthtracker.activities.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

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
    public static final String ACTION_STEPS_UPDATED = "com.example.healthtracker.STEPS_UPDATED";
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
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HealthTracker:WakeLock");
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
                        // Kiểm tra xem người dùng có đăng nhập không
                        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                            Log.d(TAG, "Người dùng đã đăng xuất, tạm dừng mô phỏng bước chân");
                            return; // Dừng và không lập lịch lại
                        }
                        
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
                            emulatorHandler.postDelayed(this, 500); // Giảm thời gian xuống 500ms để cập nhật nhanh hơn
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
            // Kiểm tra người dùng đã đăng nhập chưa
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                Log.d(TAG, "Người dùng chưa đăng nhập, không bắt đầu đếm bước");
                return START_NOT_STICKY; // Không khởi động lại service tự động
            }
            
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
                Log.d(TAG, "Đã kích hoạt WakeLock");
            }
            
            // Khởi động việc kiểm tra đặt lại dữ liệu hàng ngày
            if (dailyResetHandler != null && dailyResetCheck != null) {
                dailyResetHandler.post(dailyResetCheck);
                Log.d(TAG, "Đã lên lịch kiểm tra đặt lại dữ liệu hàng ngày");
            }
            
            // Nếu khởi động lại service do hệ thống, khôi phục dữ liệu đã lưu
            currentSteps = stepData.getSteps();
            Log.d(TAG, "Khôi phục số bước đã lưu: " + currentSteps);
            
            // Gửi broadcast ngay để cập nhật UI
            broadcastStepCount();
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand: ", e);
        }
        
        // Yêu cầu hệ thống khởi động lại service nếu bị kill
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Step Counter Service Channel",
                        NotificationManager.IMPORTANCE_LOW);
                
                channel.setDescription("Step Counter Service");
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setShowBadge(false);
                
                NotificationManager notificationManager = getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification channel created");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error creating notification channel: ", e);
            }
        }
    }

    private Notification buildNotification() {
        try {
            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Đang đếm bước chân")
                    .setContentText("Đã đi được " + currentSteps + " bước")
                    .setSmallIcon(R.drawable.ic_footsteps)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setOngoing(true);

            return builder.build();
        } catch (Exception e) {
            Log.e(TAG, "Error building notification: ", e);
            return null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (event != null && event.sensor != null && event.sensor.getType() == Sensor.TYPE_STEP_COUNTER && event.values != null && event.values.length > 0) {
                // Lấy giá trị số bước từ cảm biến
                float sensorValue = event.values[0];
                
                // Khởi tạo lần đầu
                if (!isInitialized) {
                    initialSteps = (int) sensorValue;
                    isInitialized = true;
                    
                    // Nếu có số bước đã lưu từ trước, sử dụng số đó
                    int savedSteps = stepData.getSteps();
                    if (savedSteps > 0) {
                        // Số bước đã lưu lớn hơn 0, sử dụng lại để tránh mất dữ liệu
                        currentSteps = savedSteps;
                    } else {
                        // Không có số bước đã lưu, đặt lại thành 0
                        currentSteps = 0;
                    }
                    
                    Log.d(TAG, "Khởi tạo cảm biến với giá trị ban đầu: " + initialSteps);
                    Log.d(TAG, "Khởi tạo với số bước đã lưu: " + currentSteps);
                } else {
                    // Tính toán số bước hiện tại dựa trên sự khác biệt từ giá trị ban đầu
                    int calculatedSteps = (int) (sensorValue - initialSteps);
                    
                    // Nếu giá trị tính toán âm hoặc quá lớn, có thể là do thiết bị đã khởi động lại
                    if (calculatedSteps < 0 || calculatedSteps > 100000) {
                        // Đặt lại giá trị ban đầu
                        initialSteps = (int) sensorValue;
                        // Giữ lại số bước hiện tại để tránh mất dữ liệu
                        Log.d(TAG, "Phát hiện sự thiết lập lại cảm biến, điều chỉnh giá trị ban đầu mới: " + initialSteps);
                    } else {
                        // Cập nhật số bước hiện tại
                        currentSteps = calculatedSteps;
                        
                        // Lưu vào bộ nhớ
                        stepData.saveSteps(currentSteps);
                        
                        // Phát broadcast để cập nhật UI
                        broadcastStepCount();
                        
                        // Cập nhật thông báo
                        updateNotification();
                        
                        Log.d(TAG, "Cập nhật số bước từ cảm biến: " + currentSteps);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onSensorChanged: ", e);
        }
    }

    private void updateNotification() {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                Notification notification = buildNotification();
                if (notification != null) {
                    notificationManager.notify(NOTIFICATION_ID, notification);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification: ", e);
        }
    }

    private void broadcastStepCount() {
        try {
            Intent intent = new Intent(ACTION_STEPS_UPDATED);
            intent.putExtra(EXTRA_STEPS, currentSteps);
            
            // Tính toán khoảng cách đi được (mét)
            double distance = stepData.calculateDistance(currentSteps);
            intent.putExtra(EXTRA_DISTANCE, distance);
            
            // Tính toán calo đã đốt
            double calories = stepData.calculateCalories(currentSteps);
            intent.putExtra(EXTRA_CALORIES, calories);
            
            // Tính toán thời gian hoạt động
            long activeTime = stepData.calculateActiveTime();
            intent.putExtra(EXTRA_TIME, activeTime);
            
            // Đặt flags để đảm bảo broadcast được xử lý ngay lập tức
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
            
            // Đảm bảo broadcast chỉ được gửi trong ứng dụng để tăng tốc độ
            intent.setPackage(getPackageName());
            
            // Gửi broadcast
            sendBroadcast(intent);
            
            // Log chi tiết để debug
            Log.d(TAG, "Đã gửi broadcast với FLAG_RECEIVER_FOREGROUND và setPackage");
            Log.d(TAG, "Nội dung broadcast: Bước=" + currentSteps + ", khoảng cách=" + 
                    distance + "m, calo=" + calories + ", thời gian=" + activeTime + " phút");
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
        // Service này không hỗ trợ binding
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        try {
            super.onTaskRemoved(rootIntent);
            
            // Lưu số bước chân hiện tại khi ứng dụng bị đóng
            stepData.saveSteps(currentSteps);
            Log.d(TAG, "Ứng dụng bị đóng, đã lưu số bước: " + currentSteps);
            
            // Đảm bảo service sẽ được khởi động lại khi ứng dụng bị đóng
            Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
            restartIntent.setPackage(getPackageName());
            
            PendingIntent restartServicePendingIntent = PendingIntent.getService(
                    getApplicationContext(), 
                    1, 
                    restartIntent, 
                    PendingIntent.FLAG_IMMUTABLE);
                    
            android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext()
                    .getSystemService(Context.ALARM_SERVICE);
                    
            if (alarmService != null) {
                alarmService.set(android.app.AlarmManager.RTC_WAKEUP, 
                        System.currentTimeMillis() + 5000, 
                        restartServicePendingIntent);
                Log.d(TAG, "Service sẽ được khởi động lại sau 5 giây");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onTaskRemoved: ", e);
        }
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            Log.d(TAG, "onDestroy: Service bị hủy");
            
            // Lưu số bước hiện tại
            stepData.saveSteps(currentSteps);
            
            // Hủy đăng ký lắng nghe sự kiện từ cảm biến
            if (sensorManager != null && stepSensor != null) {
                sensorManager.unregisterListener(this);
            }
            
            // Hủy handler mô phỏng
            if (emulatorHandler != null && emulatorRunnable != null) {
                emulatorHandler.removeCallbacks(emulatorRunnable);
            }
            
            // Hủy handler kiểm tra đặt lại hàng ngày
            if (dailyResetHandler != null && dailyResetCheck != null) {
                dailyResetHandler.removeCallbacks(dailyResetCheck);
            }
            
            // Giải phóng WakeLock
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
            
            // Thử khởi động lại service
            Intent restartIntent = new Intent(getApplicationContext(), this.getClass());
            startService(restartIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: ", e);
        }
    }
} 