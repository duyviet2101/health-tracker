package com.example.healthtracker.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;
import com.example.healthtracker.activities.RegisterInfo;
import com.example.healthtracker.models.User;
import com.example.healthtracker.services.UserService;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class HeightRegisterInfo extends Fragment {
    private static final String TAG = "HeightRegisterInfo";
    private EditText editTextHeight;
    private UserService userService;

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
        userService = new UserService();
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

        // Find the height input field
        editTextHeight = view.findViewById(R.id.editTextHeight);

        // Find the save button
        Button saveButton = view.findViewById(R.id.button1);

        // Set click listener to save data and navigate to MainActivity
        saveButton.setOnClickListener(v -> {
            // Get the height value
            String height = editTextHeight.getText().toString().trim();

            // Validate height input
            if (height.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập chiều cao của bạn", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get weight from parent activity
            String weight = "";
            if (getActivity() instanceof RegisterInfo) {
                weight = ((RegisterInfo) getActivity()).getUserWeight();
            }

            // Save user data to Firebase
            saveUserData(weight, height);
        });
    }

    private void saveUserData(String weight, String height) {
        // Show loading indicator or disable the button

        // Get current user ID
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        User user = new User();
        user.setId(userId);
        user.setWeight(weight);
        user.setHeight(height);

        // Update user data in Firestore
        userService.updateUserInfo(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User data updated successfully");

                    // Navigate to MainActivity
                    Intent intent = new Intent(requireActivity(), MainActivity.class);
                    startActivity(intent);

                    // Finish current activity to prevent going back to register screens
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user data", e);
                    Toast.makeText(requireContext(), "Lỗi khi lưu dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}