// DetailsStatisticsActivity.java
package com.example.healthtracker.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.healthtracker.R;
import com.example.healthtracker.adapters.WeekPagerAdapter;
import com.example.healthtracker.models.StepsDataHelper;
import com.example.healthtracker.models.WeekStepData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailsStatisticsActivity extends AppCompatActivity {

    private ViewPager2 weekViewPager;
    private WeekPagerAdapter adapter;
    private List<WeekStepData> allWeekData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_statistics);

        weekViewPager = findViewById(R.id.WeekViewPager);

        if (weekViewPager == null) {
            Toast.makeText(this, "ViewPager không tìm thấy!", Toast.LENGTH_SHORT).show();
            return;
        }

        allWeekData = loadWeekDataFromJson();

        if (allWeekData == null || allWeekData.isEmpty()) {
            Toast.makeText(this, "Dữ liệu không hợp lệ hoặc trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter = new WeekPagerAdapter(this, allWeekData);
        weekViewPager.setAdapter(adapter);
        weekViewPager.setCurrentItem(allWeekData.size() - 1, false);
    }

    private List<WeekStepData> loadWeekDataFromJson() {
        StepsDataHelper helper = new StepsDataHelper(this);
        Map<String, List<String>> rawWeekData = helper.getStepsDataPerWeek();

        List<WeekStepData> list = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : rawWeekData.entrySet()) {
            Map<String, Integer> stepsMap = new HashMap<>();
            for (String dayInfo : entry.getValue()) {
                String[] parts = dayInfo.split(" ");
                if (parts.length == 3) {
                    String key = parts[0] + " " + parts[1]; // ví dụ: T2 2025-04-21
                    int steps = Integer.parseInt(parts[2]);
                    stepsMap.put(key, steps);
                } else if (parts.length == 2) {
                    String key = parts[0] + " " + parts[1];
                    stepsMap.put(key, 0);
                }
            }

            WeekStepData week = new WeekStepData(entry.getKey(), stepsMap);
            list.add(week);
        }

        return list;
    }
}
