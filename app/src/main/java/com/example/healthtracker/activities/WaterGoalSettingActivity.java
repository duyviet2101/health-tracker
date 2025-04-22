package com.example.healthtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthtracker.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class WaterGoalSettingActivity extends AppCompatActivity {

    private static final String TAG = "WaterGoalSetting";
    private static final int MIN_WATER_GOAL = 40; // Giá trị tối thiểu 40ml
    private static final String KEY_GOAL = "goal";
    private static final String KEY_REMINDER = "reminder_enabled";
    
    private EditText goalAmountText;
    private Switch reminderSwitch;
    private Button saveButton;
    
    private int selectedGoal = 2000; // Default 2000ml
    private boolean reminderEnabled = false;
    
    // Firestore
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_goal_setting);
        
        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            userId = "anonymous";
        }

        // Get current goal from intent
        if (getIntent().hasExtra("current_goal")) {
            selectedGoal = getIntent().getIntExtra("current_goal", 2000);
        }

        // Initialize views
        goalAmountText = findViewById(R.id.goalAmountText);
        reminderSwitch = findViewById(R.id.reminderSwitch);
        saveButton = findViewById(R.id.saveButton);
        
        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        
        // Setup goal amount input
        setupGoalInput();
        
        // Setup reminder switch
        setupReminderSwitch();
        
        // Setup save button
        saveButton.setOnClickListener(v -> saveGoalSettings());
        
        // Load previously saved settings
        loadSavedSettings();
    }
    
    private void setupGoalInput() {
        // Set initial goal
        goalAmountText.setText(String.valueOf(selectedGoal));
        
        // Add text change listener
        goalAmountText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s)) {
                    try {
                        selectedGoal = Integer.parseInt(s.toString());
                    } catch (NumberFormatException e) {
                        // Reset to min value if not a valid number
                        selectedGoal = MIN_WATER_GOAL;
                    }
                } else {
                    // If empty, default to min value
                    selectedGoal = MIN_WATER_GOAL;
                }
            }
        });
    }
    
    private void setupReminderSwitch() {
        reminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            reminderEnabled = isChecked);
    }
    
    private void loadSavedSettings() {
        if (!"anonymous".equals(userId)) {
            db.collection("users").document(userId)
                    .collection("settings").document("water")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            // Load goal
                            Long goal = documentSnapshot.getLong(KEY_GOAL);
                            if (goal != null) {
                                selectedGoal = goal.intValue();
                                goalAmountText.setText(String.valueOf(selectedGoal));
                            }
                            
                            // Load reminder setting
                            Boolean reminder = documentSnapshot.getBoolean(KEY_REMINDER);
                            if (reminder != null) {
                                reminderEnabled = reminder;
                                reminderSwitch.setChecked(reminderEnabled);
                            }
                        }
                    });
        }
    }
    
    private void saveGoalSettings() {
        // Validate input
        validateAndFixGoalAmount();
        
        // Reset water amount in tracking screen to 0
        resetWaterAmount();
        
        // Save to Firestore if user is logged in
        if (!"anonymous".equals(userId)) {
            DocumentReference settingsRef = db.collection("users").document(userId)
                    .collection("settings").document("water");
            
            Map<String, Object> settings = new HashMap<>();
            settings.put(KEY_GOAL, selectedGoal);
            settings.put(KEY_REMINDER, reminderEnabled);
            
            settingsRef.set(settings)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: Không thể lưu cài đặt", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // For anonymous users, just save locally and reset water amount
            Toast.makeText(this, "Đã lưu cài đặt", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void validateAndFixGoalAmount() {
        // Get the current input
        String goalText = goalAmountText.getText().toString().trim();
        
        // Parse input, default to MIN_WATER_GOAL if invalid
        try {
            selectedGoal = Integer.parseInt(goalText);
            
            // Make sure it's at least the minimum
            if (selectedGoal < MIN_WATER_GOAL) {
                selectedGoal = MIN_WATER_GOAL;
                goalAmountText.setText(String.valueOf(selectedGoal));
                Toast.makeText(this, "Mục tiêu tối thiểu là " + MIN_WATER_GOAL + "ml", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            selectedGoal = MIN_WATER_GOAL;
            goalAmountText.setText(String.valueOf(selectedGoal));
            Toast.makeText(this, "Đã đặt mục tiêu mặc định: " + MIN_WATER_GOAL + "ml", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void resetWaterAmount() {
        // Reset current water amount to 0
        
        // Get current date in format yyyy-MM-dd
        String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        
        // Store 0 in SharedPreferences
        getSharedPreferences("WaterTrackerPrefs", 0)
                .edit()
                .putInt("waterAmount", 0)
                .putString("lastDate", currentDate)
                .apply();
        
        // Update Firestore if logged in
        if (!"anonymous".equals(userId)) {
            DocumentReference waterRef = db.collection("users").document(userId)
                    .collection("waterTracking").document(currentDate);
            
            Map<String, Object> waterData = new HashMap<>();
            waterData.put("amount", 0); // Reset to 0
            waterData.put("goal", selectedGoal); // Update with new goal
            waterData.put("date", currentDate);
            waterData.put("timestamp", new java.util.Date());
            
            waterRef.set(waterData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Successfully reset water amount to 0");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to reset water amount", e);
                    });
        }
    }
} 