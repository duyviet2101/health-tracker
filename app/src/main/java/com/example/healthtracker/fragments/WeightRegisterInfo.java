package com.example.healthtracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.RegisterInfo;


public class WeightRegisterInfo extends Fragment {

    private ViewPager2 viewPager;
    private EditText editTextWeight;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Get reference to the ViewPager2 from parent activity
        viewPager = requireActivity().findViewById(R.id.viewPager);
        
        // Find the EditText for weight input
        editTextWeight = view.findViewById(R.id.editTextWeight);
        
        // Find the continue button
        Button continueButton = view.findViewById(R.id.button1);
        
        // Set click listener to navigate to the next fragment
        continueButton.setOnClickListener(v -> {
            // Get the weight value
            String weight = editTextWeight.getText().toString().trim();
            
            // Validate the weight input
            if (weight.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập cân nặng của bạn", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Save the weight in the parent activity
            if (getActivity() instanceof RegisterInfo) {
                ((RegisterInfo) getActivity()).setUserWeight(weight);
            }
            
            // Navigate to the next page (Height fragment)
            if (viewPager != null) {
                viewPager.setCurrentItem(1, true);
            }
        });
    }
}