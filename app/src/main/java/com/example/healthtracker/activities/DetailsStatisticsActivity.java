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
import com.example.healthtracker.models.StepsDataHelper;
import com.example.healthtracker.models.WeekStepData;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.*;

public class DetailsStatisticsActivity extends AppCompatActivity {

    private ViewPager2 chartViewPager;
    private TextView tvSteps, tvDate;
    private TextView tvTotalLabel;
    private TabLayout tabLayout;

    private ViewPager2.OnPageChangeCallback monthPageChangeCallback;

    private WeekPagerAdapter weekAdapter;
    private MonthPagerAdapter monthAdapter;

    private List<Integer> yearList;
    private List<Integer> monthList;
    private List<Map<Integer, Integer>> stepsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_statistics);

        chartViewPager = findViewById(R.id.WeekViewPager);
        tvSteps = findViewById(R.id.tvSteps);
        tvDate = findViewById(R.id.tvDate);
        tvTotalLabel = findViewById(R.id.tvTotalLabel);
        tabLayout = findViewById(R.id.tabLayout);

        setupTabLayout();
        showWeekChart(); // Mặc định
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
                    case 3:
                        Toast.makeText(DetailsStatisticsActivity.this, "Chưa hỗ trợ hiển thị", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showWeekChart() {
        tvTotalLabel.setText("TỔNG");

        // Gỡ bỏ callback tháng nếu có
        if (monthPageChangeCallback != null) {
            chartViewPager.unregisterOnPageChangeCallback(monthPageChangeCallback);
            monthPageChangeCallback = null;
        }

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
            tvTotalLabel.setText("TỔNG");
            tvSteps.setText(steps + " bước");
            tvDate.setText(formatDate(date));
        });

        chartViewPager.setAdapter(weekAdapter);
        chartViewPager.setCurrentItem(allWeekData.size() - 1, false);
        updateTodaySummary(allWeekData);
    }



    private void showMonthChartViewPager() {
        StepsDataHelper helper = new StepsDataHelper(this);
        Map<String, Map<Integer, Integer>> fullMonthData = helper.getStepsDataPerMonth();

        if (fullMonthData.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu tháng", Toast.LENGTH_SHORT).show();
            return;
        }

        yearList = new ArrayList<>();
        monthList = new ArrayList<>();
        stepsList = new ArrayList<>();
        List<MonthChartFragment.OnDaySelectedListener> listeners = new ArrayList<>();

        for (String key : fullMonthData.keySet()) {
            String[] parts = key.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            yearList.add(year);
            monthList.add(month);
            Map<Integer, Integer> stepsMap = fullMonthData.get(key);
            stepsList.add(stepsMap);

            listeners.add((day, steps) -> {
                tvTotalLabel.setText("TỔNG");
                tvSteps.setText(steps + " bước");
                tvDate.setText("ngày " + day + " tháng " + month + ", " + year);
            });
        }

        monthAdapter = new MonthPagerAdapter(this, yearList, monthList, stepsList, listeners);
        chartViewPager.setAdapter(monthAdapter);
        chartViewPager.setCurrentItem(monthList.size() - 1, false);

        // Gỡ callback cũ nếu có
        if (monthPageChangeCallback != null) {
            chartViewPager.unregisterOnPageChangeCallback(monthPageChangeCallback);
        }

        // Tạo mới callback
        monthPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position >= 0 && position < stepsList.size()) {
                    Map<Integer, Integer> selectedMonthData = stepsList.get(position);
                    int year = yearList.get(position);
                    int month = monthList.get(position);

                    int total = 0;
                    int count = 0;
                    for (int val : selectedMonthData.values()) {
                        total += val;
                        count++;
                    }

                    int avg = count == 0 ? 0 : total / count;

                    tvTotalLabel.setText("Trung bình");
                    tvSteps.setText(avg + " bước/ngày");
                    tvDate.setText("tháng " + month + ", " + year);
                }
            }
        };

        chartViewPager.registerOnPageChangeCallback(monthPageChangeCallback);
        updateAverageDisplay(monthList.size() - 1);



    }


    @Override
    protected void onResume() {
        super.onResume();
        // Mỗi lần quay lại activity, cập nhật lại dữ liệu
        if (tabLayout.getSelectedTabPosition() == 0) {
            showWeekChart();
        } else if (tabLayout.getSelectedTabPosition() == 1) {
            showMonthChartViewPager();
        }
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

    private void updateAverageDisplay(int position) {
        if (position >= 0 && position < stepsList.size()) {
            Map<Integer, Integer> selectedMonthData = stepsList.get(position);
            int year = yearList.get(position);
            int month = monthList.get(position);

            int total = 0;
            int count = 0;
            for (int val : selectedMonthData.values()) {
                total += val;
                count++;
            }

            int avg = count == 0 ? 0 : total / count;

            tvTotalLabel.setText("Trung bình");
            tvSteps.setText(avg + " bước/ngày");
            tvDate.setText("tháng " + month + ", " + year);
        }
    }

}
