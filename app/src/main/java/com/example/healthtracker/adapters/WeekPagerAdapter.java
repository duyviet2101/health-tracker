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
import java.util.function.Consumer;

public class WeekPagerAdapter extends FragmentStateAdapter {

    private final List<WeekStepData> weekDataList;
    private final WeekChartFragment.OnBarSelectedListener barSelectedListener;
    private final Consumer<WeekChartFragment> fragmentInitializer;

    public WeekPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                            List<WeekStepData> weekDataList,
                            WeekChartFragment.OnBarSelectedListener listener) {
        super(fragmentActivity);
        this.weekDataList = weekDataList;
        this.barSelectedListener = listener;
        this.fragmentInitializer = null;
    }
    
    public WeekPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                            List<WeekStepData> weekDataList,
                            WeekChartFragment.OnBarSelectedListener listener,
                            Consumer<WeekChartFragment> fragmentInitializer) {
        super(fragmentActivity);
        this.weekDataList = weekDataList;
        this.barSelectedListener = listener;
        this.fragmentInitializer = fragmentInitializer;
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
        
        // Gọi consumer để khởi tạo fragment nếu có
        if (fragmentInitializer != null) {
            fragmentInitializer.accept(fragment);
        }
        
        return fragment;
    }

    @Override
    public int getItemCount() {
        return weekDataList.size();
    }
}
