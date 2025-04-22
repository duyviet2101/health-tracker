// WeekPagerAdapter.java
package com.example.healthtracker.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.healthtracker.fragments.WeekChartFragment;
import com.example.healthtracker.models.WeekStepData;

import java.util.List;

public class WeekPagerAdapter extends FragmentStateAdapter {

    private List<WeekStepData> weekDataList;
    private WeekChartFragment.OnBarSelectedListener barSelectedListener;

    public WeekPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<WeekStepData> weekDataList,
                            WeekChartFragment.OnBarSelectedListener listener) {
        super(fragmentActivity);
        this.weekDataList = weekDataList;
        this.barSelectedListener = listener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        WeekChartFragment fragment = WeekChartFragment.newInstance(weekDataList.get(position));
        fragment.setOnBarSelectedListener(barSelectedListener);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return weekDataList.size();
    }
}
