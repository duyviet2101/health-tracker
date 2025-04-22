package com.example.healthtracker.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.healthtracker.R;
import com.example.healthtracker.adapters.MonthPagerAdapter;
import com.example.healthtracker.adapters.WeekPagerAdapter;
import com.example.healthtracker.fragments.MonthChartFragment;
import com.example.healthtracker.fragments.WeekChartFragment;
import com.example.healthtracker.models.StepsDataHelper;
import com.example.healthtracker.models.StepsDataResponse;
import com.example.healthtracker.models.WeekStepData;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.text.SimpleDateFormat;
import java.util.*;

public class DetailsStatisticsActivity extends AppCompatActivity {

    private ViewPager2 chartViewPager;
    private TextView tvSteps, tvDate;
    private TabLayout tabLayout;

    private WeekPagerAdapter weekAdapter;
    private MonthPagerAdapter monthAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_statistics);

        chartViewPager = findViewById(R.id.WeekViewPager);
        tvSteps = findViewById(R.id.tvSteps);
        tvDate = findViewById(R.id.tvDate);
        tabLayout = findViewById(R.id.tabLayout);

        setupTabLayout();
        showWeekChart(); // mặc định
    }

    private void setupTabLayout() {
        String[] tabs = {"Tuần", "Tháng", "6 Tháng", "Năm"};

        for (String title : tabs) {
            tabLayout.addTab(tabLayout.newTab().setText(title));
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0:
                        showWeekChart();
                        break;
                    case 1:
                        showMonthChartViewPager();
                        break;
                    case 2:
                        Toast.makeText(DetailsStatisticsActivity.this, "Chưa hỗ trợ hiển thị 6 tháng", Toast.LENGTH_SHORT).show();
                        break;
                    case 3:
                        Toast.makeText(DetailsStatisticsActivity.this, "Chưa hỗ trợ hiển thị theo năm", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showWeekChart() {
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
                }
            }
            allWeekData.add(new WeekStepData(entry.getKey(), stepsMap));
        }

        if (allWeekData.isEmpty()) {
            Toast.makeText(this, "Dữ liệu không hợp lệ hoặc trống!", Toast.LENGTH_SHORT).show();
            return;
        }

        weekAdapter = new WeekPagerAdapter(this, allWeekData, (date, steps) -> {
            tvSteps.setText(steps + " bước");
            tvDate.setText(formatDate(date));
        });

        chartViewPager.setAdapter(weekAdapter);
        chartViewPager.setCurrentItem(allWeekData.size() - 1, false);
        updateTodaySummary(allWeekData);
    }

    private void showMonthChartViewPager() {
        StepsDataHelper helper = new StepsDataHelper(this);
        List<Map<Integer, Integer>> monthDataList = helper.getStepsDataPerMonthList();

        if (monthDataList.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu tháng", Toast.LENGTH_SHORT).show();
            return;
        }

        List<MonthChartFragment.OnDaySelectedListener> listeners = new ArrayList<>();
        for (Map<Integer, Integer> monthData : monthDataList) {
            listeners.add((day, steps) -> {
                tvSteps.setText(steps + " bước");
                Calendar calendar = Calendar.getInstance();
                tvDate.setText("ngày " + day + " tháng " + (calendar.get(Calendar.MONTH) + 1) + ", " + calendar.get(Calendar.YEAR));
            });
        }

        monthAdapter = new MonthPagerAdapter(this, monthDataList, listeners);
        chartViewPager.setAdapter(monthAdapter);
        chartViewPager.setCurrentItem(monthDataList.size() - 1, false);
    }

    private void updateTodaySummary(List<WeekStepData> dataList) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (WeekStepData week : dataList) {
            for (Map.Entry<String, Integer> entry : week.stepsPerDay.entrySet()) {
                String[] parts = entry.getKey().split(" ");
                if (parts.length == 2 && parts[1].equals(today)) {
                    tvSteps.setText(entry.getValue() + " bước");
                    tvDate.setText(formatDate(today));
                    return;
                }
            }
        }

        tvSteps.setText("0 bước");
        tvDate.setText(formatDate(today));
    }

    private String formatDate(String date) {
        String[] parts = date.split("-");
        if (parts.length == 3) {
            return "ngày " + Integer.parseInt(parts[2]) +
                    " tháng " + Integer.parseInt(parts[1]) + ", " + parts[0];
        }
        return date;
    }
}
