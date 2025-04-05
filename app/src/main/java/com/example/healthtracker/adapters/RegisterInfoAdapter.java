package com.example.healthtracker.adapters;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class RegisterInfoAdapter extends FragmentStateAdapter {
    
    private final List<Fragment> fragmentList = new ArrayList<>();
    
    public RegisterInfoAdapter(FragmentActivity activity) {
        super(activity);
    }
    
    public void addFragment(Fragment fragment) {
        fragmentList.add(fragment);
    }
    
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }
    
    @Override
    public int getItemCount() {
        return fragmentList.size();
    }
} 