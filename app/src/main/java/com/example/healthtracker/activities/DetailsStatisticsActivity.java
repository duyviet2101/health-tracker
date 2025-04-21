package com.example.healthtracker.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.healthtracker.R;
import com.example.healthtracker.adapters.WeekPagerAdapter;
import com.example.healthtracker.models.WeekStepData;

import java.util.ArrayList;
import java.util.List;

public class DetailsStatisticsActivity extends AppCompatActivity {

    private ViewPager2 weekViewPager;
    private WeekPagerAdapter adapter;
    private List<WeekStepData> allWeekData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_statistics);

        // Khởi tạo weekViewPager
        weekViewPager = findViewById(R.id.WeekViewPager);

        // Kiểm tra xem weekViewPager có null không
        if (weekViewPager == null) {
            Toast.makeText(this, "ViewPager không tìm thấy!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo dữ liệu giả (mock data)
        allWeekData = createMockWeekData();

        // Kiểm tra nếu allWeekData null
        if (allWeekData == null || allWeekData.isEmpty()) {
            Toast.makeText(this, "Dữ liệu không hợp lệ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Khởi tạo adapter và set cho ViewPager2 sau khi có dữ liệu
        adapter = new WeekPagerAdapter(this, allWeekData);
        weekViewPager.setAdapter(adapter);

        weekViewPager.setCurrentItem(allWeekData.size() - 1, false);
    }

    // Tạo dữ liệu giả (mock data) cho các tuần
    private List<WeekStepData> createMockWeekData() {
        List<WeekStepData> list = new ArrayList<>();

        WeekStepData week1 = new WeekStepData("Tuần 1");
        week1.stepsPerDay.put("T2", 1200);
        week1.stepsPerDay.put("T3", 1500);
        week1.stepsPerDay.put("T4", 800);
        week1.stepsPerDay.put("T5", 2000);
        week1.stepsPerDay.put("T6", 3000);
        week1.stepsPerDay.put("T7", 1000);
        week1.stepsPerDay.put("CN", 1800);

        WeekStepData week2 = new WeekStepData("Tuần 2");
        week2.stepsPerDay.put("T2", 1000);
        week2.stepsPerDay.put("T3", 1100);
        week2.stepsPerDay.put("T4", 1400);
        week2.stepsPerDay.put("T5", 900);
        week2.stepsPerDay.put("T6", 2500);
        week2.stepsPerDay.put("T7", 1300);
        week2.stepsPerDay.put("CN", 1700);

        list.add(week1);
        list.add(week2);

        return list;
    }
}
