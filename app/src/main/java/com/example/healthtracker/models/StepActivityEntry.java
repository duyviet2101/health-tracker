package com.example.healthtracker.models;

public class StepActivityEntry {
    public String startTime;
    public String duration;
    public int steps;
    public double distance;
    public int calories;

    public StepActivityEntry(String startTime, String duration, int steps, double distance, double calories) {
        this.startTime = startTime;
        this.duration = duration;
        this.steps = Math.max(0, steps);
        this.distance = Math.max(0.0, distance);
        this.calories = (int) Math.ceil(Math.max(0.0, calories));
    }
}
