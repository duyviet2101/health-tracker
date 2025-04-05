package com.example.healthtracker.models;

import java.util.Date;

/**
 * Model class để lưu trữ dữ liệu lịch sử bước chân
 */
public class StepHistory {
    private int id;
    private String date;
    private String time;
    private int steps;
    private float distance;
    private int calories;
    private long duration;
    private float targetDistance; // Mục tiêu quãng đường cho đợt chạy này
    private int completionPercentage; // Phần trăm hoàn thành mục tiêu
    
    // Constructor mặc định
    public StepHistory() {
    }
    
    // Constructor đầy đủ tham số
    public StepHistory(int id, String date, String time, int steps, float distance, int calories, long duration) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
        this.duration = duration;
        this.targetDistance = distance; // Mặc định mục tiêu là quãng đường đã chạy
        this.completionPercentage = 100; // Mặc định đã hoàn thành 100%
    }
    
    // Constructor với đầy đủ tham số bao gồm cả mục tiêu
    public StepHistory(int id, String date, String time, int steps, float distance, int calories, long duration, float targetDistance, int completionPercentage) {
        this.id = id;
        this.date = date;
        this.time = time;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
        this.duration = duration;
        this.targetDistance = targetDistance;
        this.completionPercentage = completionPercentage;
    }
    
    // Getters và Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    public int getSteps() {
        return steps;
    }
    
    public void setSteps(int steps) {
        this.steps = steps;
    }
    
    public float getDistance() {
        return distance;
    }
    
    public void setDistance(float distance) {
        this.distance = distance;
    }
    
    public int getCalories() {
        return calories;
    }
    
    public void setCalories(int calories) {
        this.calories = calories;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public float getTargetDistance() {
        return targetDistance;
    }
    
    public void setTargetDistance(float targetDistance) {
        this.targetDistance = targetDistance;
        // Cập nhật phần trăm hoàn thành nếu mục tiêu thay đổi
        this.updateCompletionPercentage();
    }
    
    public int getCompletionPercentage() {
        return completionPercentage;
    }
    
    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }
    
    // Tính toán phần trăm hoàn thành dựa trên khoảng cách và mục tiêu
    private void updateCompletionPercentage() {
        if (targetDistance > 0) {
            this.completionPercentage = (int) ((distance / targetDistance) * 100);
        } else {
            this.completionPercentage = 0;
        }
    }
    
    @Override
    public String toString() {
        return "StepHistory{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", steps=" + steps +
                ", distance=" + distance +
                ", calories=" + calories +
                ", duration=" + duration +
                ", targetDistance=" + targetDistance +
                ", completionPercentage=" + completionPercentage +
                '}';
    }
} 