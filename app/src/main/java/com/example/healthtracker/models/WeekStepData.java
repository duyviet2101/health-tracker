package com.example.healthtracker.models;

import java.io.Serializable;
import java.util.Map;

public class WeekStepData implements Serializable {
    public String weekLabel;
    public Map<String, Integer> stepsPerDay;

    public WeekStepData(String weekLabel, Map<String, Integer> stepsPerDay) {
        this.weekLabel = weekLabel;
        this.stepsPerDay = stepsPerDay;
    }
}
