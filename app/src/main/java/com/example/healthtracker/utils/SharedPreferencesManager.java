package com.example.healthtracker.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Lớp tiện ích để quản lý các tác vụ SharedPreferences
 */
public class SharedPreferencesManager {
    private static final String PREFS_NAME = "HealthTrackerPrefs";
    private static final String KEY_DAILY_GOAL_METERS = "daily_goal_meters";
    private static final String KEY_DAILY_STEP_GOAL = "daily_step_goal";
    private static final String KEY_TARGET_DISTANCE = "target_distance";
    private static final String KEY_EXERCISE_TYPE = "exercise_type";
    private static final String KEY_USER_WEIGHT = "user_weight";
    private static final String KEY_STRIDE_LENGTH = "stride_length";
    private static final String KEY_TOTAL_STEPS_TODAY = "total_steps_today";
    private static final String KEY_TOTAL_DISTANCE_TODAY = "total_distance_today";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";

    private final SharedPreferences prefs;

    public SharedPreferencesManager() {
        // Constructor hiện có
        prefs = null;
    }

    public SharedPreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Lưu mục tiêu hằng ngày (đơn vị: mét)
     */
    public static void saveDailyGoal(Context context, int dailyGoal) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_DAILY_GOAL_METERS, dailyGoal).apply();
    }

    /**
     * Lấy mục tiêu hằng ngày (đơn vị: mét)
     * Mặc định là 3000 mét nếu chưa được thiết lập
     */
    public int getDailyGoalMeters() {
        if (prefs != null) {
            return prefs.getInt(KEY_DAILY_GOAL_METERS, 3000);
        }
        return 3000;
    }

    /**
     * Cài đặt mục tiêu hằng ngày (đơn vị: mét)
     */
    public void setDailyGoalMeters(int meters) {
        if (prefs != null) {
            prefs.edit().putInt(KEY_DAILY_GOAL_METERS, meters).apply();
        }
    }

    /**
     * Lưu tổng số bước chân trong ngày
     */
    public static void saveTotalStepsToday(Context context, int steps) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_TOTAL_STEPS_TODAY, steps).apply();
    }

    /**
     * Lấy tổng số bước chân trong ngày
     */
    public static int getTotalStepsToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_TOTAL_STEPS_TODAY, 0);
    }

    /**
     * Lưu tổng khoảng cách trong ngày (đơn vị: mét)
     */
    public static void saveTotalDistanceToday(Context context, int distanceM) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_TOTAL_DISTANCE_TODAY, distanceM).apply();
    }

    /**
     * Lấy tổng khoảng cách trong ngày (đơn vị: mét)
     */
    public static int getTotalDistanceToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_TOTAL_DISTANCE_TODAY, 0);
    }

    /**
     * Lưu ngày reset cuối cùng
     */
    public static void saveLastResetDate(Context context, String date) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_LAST_RESET_DATE, date).apply();
    }

    /**
     * Lấy ngày reset cuối cùng
     */
    public static String getLastResetDate(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LAST_RESET_DATE, "");
    }

    public int getDailyStepGoal() {
        return prefs.getInt(KEY_DAILY_STEP_GOAL, 2000);
    }

    public void setDailyStepGoal(int steps) {
        prefs.edit().putInt(KEY_DAILY_STEP_GOAL, steps).apply();
    }

    public float getTargetDistance() {
        return prefs.getFloat(KEY_TARGET_DISTANCE, 5.0f);
    }

    public void setTargetDistance(float distance) {
        prefs.edit().putFloat(KEY_TARGET_DISTANCE, distance).apply();
    }

    public String getExerciseType() {
        return prefs.getString(KEY_EXERCISE_TYPE, "running");
    }

    public void setExerciseType(String type) {
        prefs.edit().putString(KEY_EXERCISE_TYPE, type).apply();
    }

    public int getUserWeight() {
        return prefs.getInt(KEY_USER_WEIGHT, 70);
    }

    public void setUserWeight(int weight) {
        prefs.edit().putInt(KEY_USER_WEIGHT, weight).apply();
    }

    public float getStrideLength() {
        return prefs.getFloat(KEY_STRIDE_LENGTH, 0.75f);
    }

    public void setStrideLength(float length) {
        prefs.edit().putFloat(KEY_STRIDE_LENGTH, length).apply();
    }
}