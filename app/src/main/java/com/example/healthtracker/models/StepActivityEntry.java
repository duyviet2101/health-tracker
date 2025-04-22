package com.example.healthtracker.models;

public class StepActivityEntry {
    public String startTime;
    public String duration;
    public int steps;
    public double distance;
    public double calories;

    public StepActivityEntry(String startTime, String duration, int steps, double distance, double calories) {
        this.startTime = startTime;
        this.duration = duration;
        this.steps = steps;
        this.distance = distance;
        this.calories = calories;
    }
}
