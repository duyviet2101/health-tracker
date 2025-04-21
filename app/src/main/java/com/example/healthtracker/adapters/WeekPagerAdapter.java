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

    public WeekPagerAdapter(@NonNull FragmentActivity fragmentActivity, List<WeekStepData> weekDataList) {
        super(fragmentActivity);
        this.weekDataList = weekDataList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return WeekChartFragment.newInstance(weekDataList.get(position));
    }

    @Override
    public int getItemCount() {
        return weekDataList.size();
    }
}

