package com.example.healthtracker.preferences;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lớp hỗ trợ quản lý cài đặt người dùng sử dụng SharedPreferences
 */
public class UserPreferences {
    private static final String PREFS_NAME = "com.example.healthtracker.preferences";
    
    // Các key cho SharedPreferences
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_WEIGHT = "user_weight";
    private static final String KEY_USER_HEIGHT = "user_height";
    private static final String KEY_DAILY_GOAL_STEPS = "daily_goal_steps";
    private static final String KEY_DAILY_GOAL_DISTANCE = "daily_goal_distance";
    private static final String KEY_STRIDE_LENGTH = "stride_length";
    private static final String KEY_IS_FIRST_LAUNCH = "is_first_launch";
    
    // Giá trị mặc định
    private static final String DEFAULT_USER_NAME = "Người dùng";
    private static final float DEFAULT_USER_WEIGHT = 60.0f; // kg
    private static final float DEFAULT_USER_HEIGHT = 165.0f; // cm
    private static final int DEFAULT_DAILY_GOAL_STEPS = 5000; // bước
    private static final float DEFAULT_DAILY_GOAL_DISTANCE = 3.0f; // km
    private static final float DEFAULT_STRIDE_LENGTH = 0.65f; // m
    private static final boolean DEFAULT_IS_FIRST_LAUNCH = true;
    
    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;
    
    public UserPreferences(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = preferences.edit();
    }
    
    /**
     * Lưu tên người dùng
     */
    public void setUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }
    
    /**
     * Lấy tên người dùng
     */
    public String getUserName() {
        return preferences.getString(KEY_USER_NAME, DEFAULT_USER_NAME);
    }
    
    /**
     * Lưu cân nặng người dùng (kg)
     */
    public void setUserWeight(float weight) {
        editor.putFloat(KEY_USER_WEIGHT, weight);
        editor.apply();
    }
    
    /**
     * Lấy cân nặng người dùng (kg)
     */
    public float getUserWeight() {
        return preferences.getFloat(KEY_USER_WEIGHT, DEFAULT_USER_WEIGHT);
    }
    
    /**
     * Lưu chiều cao người dùng (cm)
     */
    public void setUserHeight(float height) {
        editor.putFloat(KEY_USER_HEIGHT, height);
        editor.apply();
    }
    
    /**
     * Lấy chiều cao người dùng (cm)
     */
    public float getUserHeight() {
        return preferences.getFloat(KEY_USER_HEIGHT, DEFAULT_USER_HEIGHT);
    }
    
    /**
     * Lưu mục tiêu số bước hằng ngày
     */
    public void setDailyGoalSteps(int steps) {
        editor.putInt(KEY_DAILY_GOAL_STEPS, steps);
        editor.apply();
    }
    
    /**
     * Lấy mục tiêu số bước hằng ngày
     */
    public int getDailyGoalSteps() {
        return preferences.getInt(KEY_DAILY_GOAL_STEPS, DEFAULT_DAILY_GOAL_STEPS);
    }
    
    /**
     * Lưu mục tiêu khoảng cách hằng ngày (km)
     */
    public void setDailyGoalDistance(float distance) {
        editor.putFloat(KEY_DAILY_GOAL_DISTANCE, distance);
        editor.apply();
    }
    
    /**
     * Lấy mục tiêu khoảng cách hằng ngày (km)
     */
    public float getDailyGoalDistance() {
        return preferences.getFloat(KEY_DAILY_GOAL_DISTANCE, DEFAULT_DAILY_GOAL_DISTANCE);
    }
    
    /**
     * Lưu độ dài sải chân (m)
     */
    public void setStrideLength(float length) {
        editor.putFloat(KEY_STRIDE_LENGTH, length);
        editor.apply();
    }
    
    /**
     * Lấy độ dài sải chân (m)
     */
    public float getStrideLength() {
        return preferences.getFloat(KEY_STRIDE_LENGTH, DEFAULT_STRIDE_LENGTH);
    }
    
    /**
     * Kiểm tra đây có phải lần đầu khởi chạy ứng dụng
     */
    public boolean isFirstLaunch() {
        return preferences.getBoolean(KEY_IS_FIRST_LAUNCH, DEFAULT_IS_FIRST_LAUNCH);
    }
    
    /**
     * Đánh dấu đã khởi chạy ứng dụng
     */
    public void setFirstLaunchComplete() {
        editor.putBoolean(KEY_IS_FIRST_LAUNCH, false);
        editor.apply();
    }
    
    /**
     * Xóa tất cả cài đặt người dùng
     */
    public void clearAll() {
        editor.clear();
        editor.apply();
    }
} 