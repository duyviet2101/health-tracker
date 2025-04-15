package com.example.healthtracker.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;

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
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Find the save button
        Button saveButton = view.findViewById(R.id.button1);
        
        // Set click listener to navigate to MainActivity
        saveButton.setOnClickListener(v -> {
            // Create intent to MainActivity
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            startActivity(intent);
            
            // Finish current activity to prevent going back to register screens
            requireActivity().finish();
        });
    }
}