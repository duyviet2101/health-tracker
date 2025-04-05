package com.example.healthtracker.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Lớp đại diện cho tổng kết dữ liệu bước chân theo ngày
 */
public class DailySummary {
    private Date date;
    private int totalSteps;
    private float totalDistance;
    private int totalCalories;
    private long totalDuration;
    private float dailyGoalDistance;
    private int completionPercentage;
    private List<StepHistory> activities;

    /**
     * Constructor mặc định
     */
    public DailySummary() {
        this.date = new Date();
        this.totalSteps = 0;
        this.totalDistance = 0;
        this.totalCalories = 0;
        this.totalDuration = 0;
        this.dailyGoalDistance = 0;
        this.completionPercentage = 0;
        this.activities = new ArrayList<>();
    }
    
    /**
     * Constructor chỉ với ngày
     */
    public DailySummary(Date date) {
        this.date = date;
        this.totalSteps = 0;
        this.totalDistance = 0;
        this.totalCalories = 0;
        this.totalDuration = 0;
        this.dailyGoalDistance = 0;
        this.completionPercentage = 0;
        this.activities = new ArrayList<>();
    }

    /**
     * Constructor đầy đủ tham số
     */
    public DailySummary(Date date, int totalSteps, float totalDistance, 
                     int totalCalories, long totalDuration, 
                     float dailyGoalDistance, List<StepHistory> activities) {
        this.date = date;
        this.totalSteps = totalSteps;
        this.totalDistance = totalDistance;
        this.totalCalories = totalCalories;
        this.totalDuration = totalDuration;
        this.dailyGoalDistance = dailyGoalDistance;
        this.activities = activities;
        
        // Tính phần trăm hoàn thành mục tiêu
        updateCompletionPercentage();
    }

    /**
     * Cập nhật phần trăm hoàn thành dựa trên mục tiêu
     */
    public void updateCompletionPercentage() {
        if (dailyGoalDistance > 0) {
            this.completionPercentage = (int) (totalDistance / dailyGoalDistance * 100);
        } else {
            this.completionPercentage = 0;
        }
    }

    /**
     * Thêm một hoạt động mới vào tổng kết ngày
     */
    public void addActivity(StepHistory activity) {
        if (activities == null) {
            activities = new ArrayList<>();
        }
        
        activities.add(activity);
        
        // Cập nhật tổng
        totalSteps += activity.getSteps();
        totalDistance += activity.getDistance();
        totalCalories += activity.getCalories();
        totalDuration += activity.getDuration();
        
        // Cập nhật phần trăm hoàn thành
        updateCompletionPercentage();
    }

    // Getters và Setters
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        this.totalDistance = totalDistance;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public float getDailyGoalDistance() {
        return dailyGoalDistance;
    }

    public void setDailyGoalDistance(float dailyGoalDistance) {
        this.dailyGoalDistance = dailyGoalDistance;
        updateCompletionPercentage();
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public List<StepHistory> getActivities() {
        return activities;
    }

    public void setActivities(List<StepHistory> activities) {
        this.activities = activities;
    }

    @Override
    public String toString() {
        return "DailySummary{" +
                "date=" + date +
                ", totalSteps=" + totalSteps +
                ", totalDistance=" + totalDistance +
                ", totalCalories=" + totalCalories +
                ", totalDuration=" + totalDuration +
                ", dailyGoalDistance=" + dailyGoalDistance +
                ", completionPercentage=" + completionPercentage +
                ", activitiesCount=" + (activities != null ? activities.size() : 0) +
                '}';
    }
} 