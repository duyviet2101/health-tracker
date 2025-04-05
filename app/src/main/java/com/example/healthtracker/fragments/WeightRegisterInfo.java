package com.example.healthtracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.healthtracker.R;


public class WeightRegisterInfo extends Fragment {

    public WeightRegisterInfo() {
        // Required empty public constructor
    }

    public static WeightRegisterInfo newInstance(String param1, String param2) {
        WeightRegisterInfo fragment = new WeightRegisterInfo();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_weight_register_info, container, false);
    }
}