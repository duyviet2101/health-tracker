package com.example.healthtracker.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.healthtracker.fragments.MonthChartFragment;

import java.util.List;
import java.util.Map;

public class MonthPagerAdapter extends FragmentStateAdapter {

    private final List<Integer> yearList;
    private final List<Integer> monthList;
    private final List<Map<Integer, Integer>> stepsList;
    private final List<MonthChartFragment.OnDaySelectedListener> listeners;

    public MonthPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                             List<Integer> yearList,
                             List<Integer> monthList,
                             List<Map<Integer, Integer>> stepsList,
                             List<MonthChartFragment.OnDaySelectedListener> listeners) {
        super(fragmentActivity);
        this.yearList = yearList;
        this.monthList = monthList;
        this.stepsList = stepsList;
        this.listeners = listeners;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        int year = yearList.get(position);
        int month = monthList.get(position);
        Map<Integer, Integer> stepsMap = stepsList.get(position);
        MonthChartFragment fragment = MonthChartFragment.newInstance(year, month, stepsMap);
        fragment.setOnDaySelectedListener(listeners.get(position));
        return fragment;
    }

    @Override
    public int getItemCount() {
        return stepsList.size();
    }
}
