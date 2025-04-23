package com.example.healthtracker.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.healthtracker.fragments.WeekChartFragment;
import com.example.healthtracker.models.WeekStepData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

public class WeekPagerAdapter extends FragmentStateAdapter {

    private final List<WeekStepData> weekDataList;
    private final WeekChartFragment.OnBarSelectedListener barSelectedListener;

    public WeekPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                            List<WeekStepData> weekDataList,
                            WeekChartFragment.OnBarSelectedListener listener) {
        super(fragmentActivity);
        this.weekDataList = weekDataList;
        this.barSelectedListener = listener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        WeekStepData currentWeek = weekDataList.get(position);

        // Lấy ra tất cả các key "T2 yyyy-MM-dd" từ stepsPerDay và sắp xếp theo ngày tăng dần
        List<String> orderedKeys = new ArrayList<>(currentWeek.stepsPerDay.keySet());

        orderedKeys.sort(Comparator.comparing(k -> k.split(" ")[1])); // So sánh theo yyyy-MM-dd

        // Tạo fragment và gắn orderedKeys
        WeekChartFragment fragment = WeekChartFragment.newInstance(currentWeek, orderedKeys);
        fragment.setOnBarSelectedListener(barSelectedListener);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return weekDataList.size();
    }
}
