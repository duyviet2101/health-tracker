// MonthPagerAdapter.java
package com.example.healthtracker.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.healthtracker.fragments.MonthChartFragment;

import java.util.List;
import java.util.Map;

public class MonthPagerAdapter extends FragmentStateAdapter {

    private List<Map<Integer, Integer>> listOfStepsPerMonth;
    private List<MonthChartFragment.OnDaySelectedListener> listeners;

    public MonthPagerAdapter(@NonNull FragmentActivity fa,
                             List<Map<Integer, Integer>> monthDataList,
                             List<MonthChartFragment.OnDaySelectedListener> listenerList) {
        super(fa);
        this.listOfStepsPerMonth = monthDataList;
        this.listeners = listenerList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        MonthChartFragment fragment = MonthChartFragment.newInstance(listOfStepsPerMonth.get(position));
        if (listeners != null && position < listeners.size()) {
            fragment.setOnDaySelectedListener(listeners.get(position));
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return listOfStepsPerMonth.size();
    }
}
