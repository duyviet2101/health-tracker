package com.example.healthtracker.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthtracker.R;
import com.google.android.material.button.MaterialButton;

public class WaterTrackingActivity extends AppCompatActivity {

    private MaterialButton addWaterButton;
    private TextView waterAmountText;
    private TextView goalReachedMessage;
    private View waterLevelView;
    private int currentWaterAmount = 0;
    private final int WATER_GOAL = 2000; // 2000ml goal
    private final int WATER_INCREMENT = 250; // 250ml per click

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_tracking);

        // Initialize views
        addWaterButton = findViewById(R.id.addWaterButton);
        waterAmountText = findViewById(R.id.waterAmount);
        waterLevelView = findViewById(R.id.waterLevelView);
        goalReachedMessage = findViewById(R.id.goalReachedMessage);

        // Set initial water level height (0)
        updateWaterLevel();

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Setup add water button
        addWaterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentWaterAmount < WATER_GOAL) {
                    addWater(WATER_INCREMENT);
                } else {
                    Toast.makeText(WaterTrackingActivity.this, "Đã đạt mục tiêu!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void addWater(int amount) {
        currentWaterAmount += amount;
        
        // Update the displayed amount
        waterAmountText.setText(String.valueOf(currentWaterAmount));
        
        // Update the water level visualization
        updateWaterLevel();
        
        // Check if goal is reached
        if (currentWaterAmount >= WATER_GOAL) {
            goalReached();
        }
    }
    
    private void updateWaterLevel() {
        // Calculate height percentage based on current amount vs goal
        float percentage = Math.min(1.0f, (float) currentWaterAmount / WATER_GOAL);
        
        // Get parent view height to calculate actual height
        View chartContainer = findViewById(R.id.chartContainer);
        int containerHeight = chartContainer.getHeight();
        if (containerHeight == 0) {
            // If chart container height is not yet measured, post a runnable to do it later
            chartContainer.post(new Runnable() {
                @Override
                public void run() {
                    updateWaterLevel();
                }
            });
            return;
        }
        
        // Calculate and set the height of the water level view
        int waterHeight = (int) (containerHeight * percentage);
        waterLevelView.getLayoutParams().height = waterHeight;
        waterLevelView.requestLayout();
    }
    
    private void goalReached() {
        // Show goal reached message
        goalReachedMessage.setVisibility(View.VISIBLE);
        
        // Disable add water button
        addWaterButton.setEnabled(false);
        addWaterButton.setAlpha(0.5f);
        
        // Optionally show a toast
        Toast.makeText(this, "Đã đạt mục tiêu!", Toast.LENGTH_SHORT).show();
    }
} 