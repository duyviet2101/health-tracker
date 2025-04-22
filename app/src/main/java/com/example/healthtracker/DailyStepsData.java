package com.example.healthtracker;

import com.example.healthtracker.models.StepActivityEntry;

import java.util.List;

public class DailyStepsData {
    public String date;
    public String dayOfWeek;
    public List<StepActivityEntry> activities;

    public DailyStepsData(String date, String dayOfWeek, List<StepActivityEntry> activities) {
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.activities = activities;
    }
}
