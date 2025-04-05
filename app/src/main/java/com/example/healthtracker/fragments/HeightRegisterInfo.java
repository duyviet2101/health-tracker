package com.example.healthtracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.example.healthtracker.R;

public class HeightRegisterInfo extends Fragment {
    public HeightRegisterInfo() {
        // Required empty public constructor
    }

    public static HeightRegisterInfo newInstance(String param1, String param2) {
        HeightRegisterInfo fragment = new HeightRegisterInfo();
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
        return inflater.inflate(R.layout.fragment_height_register_info, container, false);
    }
}