// DetailsStatisticsActivity.java
package com.example.healthtracker.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.healthtracker.R;
import com.example.healthtracker.adapters.WeekPagerAdapter;
import com.example.healthtracker.fragments.WeekChartFragment;
import com.example.healthtracker.models.StepsDataHelper;
import com.example.healthtracker.models.WeekStepData;

import java.text.SimpleDateFormat;
import java.util.*;

public class DetailsStatisticsActivity extends AppCompatActivity {

    private ViewPager2 weekViewPager;
    private WeekPagerAdapter adapter;
    private TextView tvSteps, tvDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_statistics);

        weekViewPager = findViewById(R.id.WeekViewPager);
        tvSteps = findViewById(R.id.tvSteps);
        tvDate = findViewById(R.id.tvDate);

        if (weekViewPager == null) {
            Toast.makeText(this, "ViewPager không tìm thấy!", Toast.LENGTH_SHORT).show();
            return;
        }

        loadAndDisplayData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAndDisplayData();
    }

    private void loadAndDisplayData() {
        StepsDataHelper helper = new StepsDataHelper(this);
        Map<String, List<String>> rawWeekData = helper.getStepsDataPerWeek();

        List<WeekStepData> allWeekData = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : rawWeekData.entrySet()) {
            Map<String, Integer> stepsMap = new HashMap<>();
            for (String dayInfo : entry.getValue()) {
                String[] parts = dayInfo.split(" ");
                if (parts.length == 3) {
                    String key = parts[0] + " " + parts[1];
                    int steps = Integer.parseInt(parts[2]);
                    stepsMap.put(key, steps);
                } else if (parts.length == 2) {
                    String key = parts[0] + " " + parts[1];
                    stepsMap.put(key, 0);
                }
            }
            allWeekData.add(new WeekStepData(entry.getKey(), stepsMap));
        }

        if (allWeekData.isEmpty()) {
            Toast.makeText(this, "Dữ liệu không hợp lệ hoặc trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter = new WeekPagerAdapter(this, allWeekData, new WeekChartFragment.OnBarSelectedListener() {
            @Override
            public void onBarSelected(String date, int steps) {
                tvSteps.setText(steps + " bước");

                String[] parts = date.split("-");
                if (parts.length == 3) {
                    String formattedDate = "ngày " + Integer.parseInt(parts[2]) +
                            " tháng " + Integer.parseInt(parts[1]) + ", " + parts[0];
                    tvDate.setText(formattedDate);
                }
            }
        });

        weekViewPager.setAdapter(adapter);
        weekViewPager.setCurrentItem(allWeekData.size() - 1, false);

        updateTodaySummary(allWeekData);
    }

    private void updateTodaySummary(List<WeekStepData> dataList) {
        if (dataList == null || dataList.isEmpty()) return;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(new Date());

        int todaySteps = 0;
        boolean found = false;

        for (WeekStepData week : dataList) {
            for (Map.Entry<String, Integer> entry : week.stepsPerDay.entrySet()) {
                String[] parts = entry.getKey().split(" ");
                if (parts.length == 2 && parts[1].equals(todayDate)) {
                    todaySteps = entry.getValue();
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        String[] dateParts = todayDate.split("-");
        String formattedDate = "ngày " + Integer.parseInt(dateParts[2])
                + " tháng " + Integer.parseInt(dateParts[1])
                + ", " + dateParts[0];

        tvSteps.setText(todaySteps + " bước");
        tvDate.setText(formattedDate);
    }
}
