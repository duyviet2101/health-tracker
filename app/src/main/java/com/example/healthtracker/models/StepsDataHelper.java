// StepsDataHelper.java
package com.example.healthtracker.models;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class StepsDataHelper {
    private final Context context;

    public StepsDataHelper(Context context) {
        this.context = context;
    }

    public Map<String, List<String>> getStepsDataPerWeek() {
        Map<String, List<String>> weekDataMap = new LinkedHashMap<>();
        List<StepsDataResponse.DayData> sortedList = new ArrayList<>();

        try {
            File file = new File(context.getFilesDir(), "activity_data.json");
            if (!file.exists()) {
                Log.e("StepsDataHelper", "File activity_data.json không tồn tại trong internal storage!");
                return new LinkedHashMap<>();
            }

            InputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            StepsDataResponse dataResponse = gson.fromJson(json, StepsDataResponse.class);

            sortedList = dataResponse.getStepsData();
            Collections.sort(sortedList, Comparator.comparing(StepsDataResponse.DayData::getDate));

        } catch (Exception e) {
            Log.e("StepsDataHelper", "Lỗi đọc dữ liệu: " + e.getMessage());
        }

        List<String> currentWeekData = new ArrayList<>();
        int weekIndex = 0;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        Set<String> processedDates = new HashSet<>();

        for (StepsDataResponse.DayData dayData : sortedList) {
            try {
                Date currentDate = dateFormat.parse(dayData.getDate());
                calendar.setTime(currentDate);

                Calendar weekStart = (Calendar) calendar.clone();
                int currentDayOfWeek = weekStart.get(Calendar.DAY_OF_WEEK);
                int daysFromMonday = (currentDayOfWeek + 5) % 7;
                weekStart.add(Calendar.DAY_OF_MONTH, -daysFromMonday);

                String weekKey = "week" + weekIndex;

                if (!processedDates.contains(dayData.getDate())) {
                    processedDates.add(dayData.getDate());

                    String dayLabel = getDayLabel(calendar.get(Calendar.DAY_OF_WEEK));
                    int steps = dayData.getActivities().stream().mapToInt(a -> a.getSteps()).sum();
                    currentWeekData.add(dayLabel + " " + dayData.getDate() + " " + steps);
                }

                if (currentWeekData.size() == 7) {
                    weekDataMap.put(weekKey, new ArrayList<>(currentWeekData));
                    currentWeekData.clear();
                    weekIndex++;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (!currentWeekData.isEmpty()) {
            for (int i = currentWeekData.size(); i < 7; i++) {
                Calendar weekStart = Calendar.getInstance();
                weekStart.add(Calendar.DAY_OF_MONTH, i);
                String dateStr = dateFormat.format(weekStart.getTime());
                String dayLabel = getDayLabel(weekStart.get(Calendar.DAY_OF_WEEK));
                currentWeekData.add(dayLabel + " " + dateStr + " 0");
            }
            weekDataMap.put("week" + weekIndex, new ArrayList<>(currentWeekData));
        }

        return weekDataMap;
    }

    private String getDayLabel(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "T2";
            case Calendar.TUESDAY: return "T3";
            case Calendar.WEDNESDAY: return "T4";
            case Calendar.THURSDAY: return "T5";
            case Calendar.FRIDAY: return "T6";
            case Calendar.SATURDAY: return "T7";
            case Calendar.SUNDAY: return "CN";
            default: return "";
        }
    }

    public StepsDataResponse readRawStepsDataFromFile() {
        File file = new File(context.getFilesDir(), "activity_data.json");
        if (!file.exists()) return null;

        try {
            InputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            return gson.fromJson(json, StepsDataResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Map<Integer, Integer>> getStepsDataPerMonthList() {
        List<Map<Integer, Integer>> listPerMonth = new ArrayList<>();
        Map<String, Map<Integer, Integer>> groupedByMonth = new LinkedHashMap<>();

        File file = new File(context.getFilesDir(), "activity_data.json");
        if (!file.exists()) return listPerMonth;

        try {
            InputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();

            String json = new String(buffer, "UTF-8");
            Gson gson = new Gson();
            StepsDataResponse response = gson.fromJson(json, StepsDataResponse.class);

            for (StepsDataResponse.DayData dayData : response.getStepsData()) {
                String[] parts = dayData.getDate().split("-");
                if (parts.length != 3) continue;

                String yearMonthKey = parts[0] + "-" + parts[1];
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);

                int steps = dayData.getActivities().stream().mapToInt(a -> a.getSteps()).sum();

                groupedByMonth.putIfAbsent(yearMonthKey, new TreeMap<>());
                groupedByMonth.get(yearMonthKey).put(day, steps);
            }

            for (String key : groupedByMonth.keySet()) {
                Map<Integer, Integer> monthData = groupedByMonth.get(key);

                String[] parts = key.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);

                Calendar cal = Calendar.getInstance();
                cal.set(year, month - 1, 1); // Calendar month starts from 0
                int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

                for (int i = 1; i <= maxDay; i++) {
                    monthData.putIfAbsent(i, 0);
                }

                listPerMonth.add(monthData);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return listPerMonth;
    }

    public Map<String, Map<Integer, Integer>> getStepsDataPerMonth() {
        Map<String, Map<Integer, Integer>> result = new LinkedHashMap<>();
        StepsDataResponse response = readRawStepsDataFromFile();
        if (response == null || response.getStepsData() == null) return result;

        for (StepsDataResponse.DayData dayData : response.getStepsData()) {
            String[] parts = dayData.getDate().split("-");
            if (parts.length != 3) continue;

            String monthKey = parts[0] + "-" + parts[1]; // ví dụ: 2025-04
            int day = Integer.parseInt(parts[2]);
            int steps = dayData.getActivities().stream().mapToInt(a -> a.getSteps()).sum();

            result.putIfAbsent(monthKey, new LinkedHashMap<>());
            result.get(monthKey).put(day, steps);
        }

        return result;
    }





}
