package com.example.healthtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Lớp singleton quản lý lưu trữ và truy xuất dữ liệu bước chân
 */
public class StepCounterData {
    private static final String TAG = "StepCounterData";
    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String STEPS_KEY = "steps";
    private static final String START_TIME_KEY = "start_time";
    private static final String LAST_RESET_KEY = "last_reset";
    
    // Đơn vị đo và chuyển đổi
    private static final double STEP_LENGTH_METERS = 0.7; // 70cm cho mỗi bước đi
    private static final double CALORIES_PER_STEP = 0.05; // 0.05 calo cho mỗi bước
    
    private static StepCounterData instance;
    private SharedPreferences preferences;
    
    private StepCounterData(Context context) {
        try {
            preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing SharedPreferences: ", e);
            // Fallback to ensure preferences isn't null
            try {
                preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            } catch (Exception e2) {
                Log.e(TAG, "Critical error with SharedPreferences: ", e2);
            }
        }
    }
    
    public static synchronized StepCounterData getInstance(Context context) {
        if (instance == null) {
            try {
                instance = new StepCounterData(context.getApplicationContext());
            } catch (Exception e) {
                Log.e(TAG, "Error creating StepCounterData instance: ", e);
                // Fallback creation with try-catch
                try {
                    instance = new StepCounterData(context);
                } catch (Exception e2) {
                    Log.e(TAG, "Critical error creating instance: ", e2);
                    // Last resort - create with empty implementation
                    instance = new EmptyStepCounterData();
                }
            }
        }
        return instance;
    }
    
    /**
     * Lưu số bước hiện tại
     */
    public void saveSteps(int steps) {
        try {
            if (preferences != null) {
                preferences.edit().putInt(STEPS_KEY, steps).apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving steps: ", e);
        }
    }
    
    /**
     * Lấy số bước đã lưu
     */
    public int getSteps() {
        try {
            if (preferences != null) {
                return preferences.getInt(STEPS_KEY, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting steps: ", e);
        }
        return 0; // Giá trị mặc định nếu có lỗi
    }
    
    /**
     * Lưu thời gian bắt đầu
     */
    public void saveStartTime(long startTimeMillis) {
        try {
            if (preferences != null && !preferences.contains(START_TIME_KEY)) {
                preferences.edit().putLong(START_TIME_KEY, startTimeMillis).apply();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving start time: ", e);
        }
    }
    
    /**
     * Lấy thời gian bắt đầu
     */
    public long getStartTime() {
        try {
            if (preferences != null) {
                if (!preferences.contains(START_TIME_KEY)) {
                    // Nếu chưa có thời gian bắt đầu, sử dụng thời gian hiện tại
                    long currentTime = System.currentTimeMillis();
                    saveStartTime(currentTime);
                    return currentTime;
                }
                return preferences.getLong(START_TIME_KEY, System.currentTimeMillis());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting start time: ", e);
        }
        return System.currentTimeMillis(); // Sử dụng thời gian hiện tại nếu có lỗi
    }
    
    /**
     * Đặt lại dữ liệu hàng ngày lúc nửa đêm
     */
    public void resetIfNeeded() {
        try {
            if (preferences == null) return;
            
            long currentTime = System.currentTimeMillis();
            long lastReset = preferences.getLong(LAST_RESET_KEY, 0);
            
            // Kiểm tra nếu ngày mới đã bắt đầu
            if (isNewDay(lastReset, currentTime)) {
                resetData(currentTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in resetIfNeeded: ", e);
        }
    }
    
    /**
     * Kiểm tra nếu ngày mới đã bắt đầu (sau nửa đêm)
     */
    private boolean isNewDay(long lastResetTime, long currentTime) {
        try {
            // Chuyển đổi thành ngày (bỏ qua giờ, phút, giây)
            long lastDay = lastResetTime / (24 * 60 * 60 * 1000);
            long currentDay = currentTime / (24 * 60 * 60 * 1000);
            
            return currentDay > lastDay;
        } catch (Exception e) {
            Log.e(TAG, "Error in isNewDay: ", e);
            return false;
        }
    }
    
    /**
     * Đặt lại dữ liệu bước chân
     * Phương thức này được làm public để cho phép người dùng đặt lại thủ công
     */
    public void resetData(long currentTime) {
        try {
            if (preferences != null) {
                preferences.edit()
                        .putInt(STEPS_KEY, 0)
                        .putLong(START_TIME_KEY, currentTime)
                        .putLong(LAST_RESET_KEY, currentTime)
                        .apply();
                Log.d(TAG, "Đã đặt lại dữ liệu bước chân");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting data: ", e);
        }
    }
    
    /**
     * Tính toán khoảng cách đi được (mét)
     */
    public double calculateDistance(int steps) {
        try {
            return steps * STEP_LENGTH_METERS;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance: ", e);
            return 0;
        }
    }
    
    /**
     * Tính toán lượng calo đã đốt
     */
    public double calculateCalories(int steps) {
        try {
            return steps * CALORIES_PER_STEP;
        } catch (Exception e) {
            Log.e(TAG, "Error calculating calories: ", e);
            return 0;
        }
    }
    
    /**
     * Tính thời gian hoạt động (phút)
     */
    public long calculateActiveTime() {
        try {
            long elapsedMillis = System.currentTimeMillis() - getStartTime();
            return elapsedMillis / (60 * 1000); // Chuyển đổi sang phút
        } catch (Exception e) {
            Log.e(TAG, "Error calculating active time: ", e);
            return 0;
        }
    }
    
    /**
     * Class con kế thừa để xử lý trường hợp khởi tạo hoàn toàn thất bại
     */
    private static class EmptyStepCounterData extends StepCounterData {
        EmptyStepCounterData() {
            super(null);
        }
        
        @Override
        public void saveSteps(int steps) {
            // Không làm gì - triển khai trống
            Log.w(TAG, "Using empty implementation - saveSteps");
        }
        
        @Override
        public int getSteps() {
            Log.w(TAG, "Using empty implementation - getSteps");
            return 0;
        }
        
        @Override
        public void saveStartTime(long startTimeMillis) {
            // Không làm gì - triển khai trống
            Log.w(TAG, "Using empty implementation - saveStartTime");
        }
        
        @Override
        public long getStartTime() {
            Log.w(TAG, "Using empty implementation - getStartTime");
            return System.currentTimeMillis();
        }
        
        @Override
        public void resetIfNeeded() {
            // Không làm gì - triển khai trống
            Log.w(TAG, "Using empty implementation - resetIfNeeded");
        }
        
        @Override
        public void resetData(long currentTime) {
            // Không làm gì - triển khai trống
            Log.w(TAG, "Using empty implementation - resetData");
        }
    }
} 