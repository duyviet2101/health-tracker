package com.example.healthtracker.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StepsDataResponse {
    @SerializedName("stepsData")
    private List<DayData> stepsData;

    // Getter and Setter
    public List<DayData> getStepsData() {
        return stepsData;
    }

    public void setStepsData(List<DayData> stepsData) {
        this.stepsData = stepsData;
    }

    public static class DayData {
        private String date;
        private String dayOfWeek;
        private List<Activity> activities;

        // Getter and Setter
        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getDayOfWeek() {
            return dayOfWeek;
        }

        public void setDayOfWeek(String dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
        }

        public List<Activity> getActivities() {
            return activities;
        }

        public void setActivities(List<Activity> activities) {
            this.activities = activities;
        }
    }

    public static class Activity {
        private String startTime;
        private String duration;
        private int steps;
        private double distance;
        private int calories;

        // Getter and Setter
        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getDuration() {
            return duration;
        }

        public void setDuration(String duration) {
            this.duration = duration;
        }

        public int getSteps() {
            return steps;
        }

        public void setSteps(int steps) {
            this.steps = steps;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }

        public int getCalories() {
            return calories;
        }

        public void setCalories(int calories) {
            this.calories = calories;
        }
    }
}
