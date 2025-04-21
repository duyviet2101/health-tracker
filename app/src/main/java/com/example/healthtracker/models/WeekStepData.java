package com.example.healthtracker.models;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class WeekStepData implements Serializable {
    public String weekLabel;
    public Map<String, Integer> stepsPerDay;

    // Constructor khởi tạo tuần với tên và dữ liệu mặc định cho mỗi ngày
    public WeekStepData(String label) {
        this.weekLabel = label;
        this.stepsPerDay = new LinkedHashMap<>();

        // Khởi tạo số bước mặc định cho mỗi ngày trong tuần
        stepsPerDay.put("T2", 0);
        stepsPerDay.put("T3", 0);
        stepsPerDay.put("T4", 0);
        stepsPerDay.put("T5", 0);
        stepsPerDay.put("T6", 0);
        stepsPerDay.put("T7", 0);
        stepsPerDay.put("CN", 0);
    }

    // Thêm số bước cho một ngày cụ thể
    public void setStepsForDay(String day, int steps) {
        if (stepsPerDay.containsKey(day)) {
            stepsPerDay.put(day, steps);
        }
    }

    // Lấy số bước của một ngày cụ thể
    public int getStepsForDay(String day) {
        return stepsPerDay.getOrDefault(day, 0);
    }

    // Phương thức lấy tất cả số bước của tuần
    public Map<String, Integer> getAllSteps() {
        return stepsPerDay;
    }
}
