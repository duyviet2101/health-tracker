package com.example.healthtracker.models;

import android.content.Context;
import android.util.Log;

import com.example.healthtracker.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReadActivity_data {
    public static List<WeekStepData> getWeekStepData(Context context) {
        List<WeekStepData> weekDataList = new ArrayList<>();
        Map<String, Integer> stepsByDate = new LinkedHashMap<>();
        Map<String, String> dayOfWeekByDate = new HashMap<>();

        try {
            // Mở file JSON từ raw
            InputStream is = context.getResources().openRawResource(R.raw.activity_data);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            // Parse JSON
            JSONObject root = new JSONObject(sb.toString());
            JSONArray stepsDataArray = root.getJSONArray("stepsData");

            for (int i = 0; i < stepsDataArray.length(); i++) {
                JSONObject dayObj = stepsDataArray.getJSONObject(i);
                String date = dayObj.getString("date");
                String dayOfWeek = dayObj.getString("dayOfWeek");
                dayOfWeekByDate.put(date, dayOfWeek);

                JSONArray activities = dayObj.getJSONArray("activities");
                int totalSteps = 0;
                for (int j = 0; j < activities.length(); j++) {
                    JSONObject activity = activities.getJSONObject(j);
                    totalSteps += activity.getInt("steps");
                }

                stepsByDate.put(date, totalSteps);
            }

            // Nhóm theo tuần (7 ngày 1 nhóm)
            List<String> dates = new ArrayList<>(stepsByDate.keySet());
            int index = 0;
            while (index < dates.size()) {
                WeekStepData weekData = new WeekStepData("Tuần " + (weekDataList.size() + 1));

                for (int i = 0; i < 7 && index < dates.size(); i++, index++) {
                    String date = dates.get(index);
                    String day = dayOfWeekByDate.get(date);
                    int steps = stepsByDate.get(date);

                    weekData.stepsPerDay.put(day, steps);
                }

                weekDataList.add(weekData);
            }

            reader.close();
            is.close();

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("JSON_ERROR", "Lỗi đọc dữ liệu tuần: " + e.getMessage());
        }

        return weekDataList;
    }


}
