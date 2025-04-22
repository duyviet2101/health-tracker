package com.example.healthtracker.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthtracker.R;
import com.example.healthtracker.utils.WaterTracker;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class WaterTrackingActivity extends AppCompatActivity {

    private static final String TAG = "WaterTrackingActivity";

    private MaterialButton addWaterButton;
    private TextView waterAmountText;
    private TextView goalReachedMessage;
    private View waterLevelView;
    private int currentWaterAmount = 0;
    private final int WATER_GOAL = 2000; // 2000ml goal
    private final int WATER_INCREMENT = 250; // 250ml per click

    // Water tracker
    private WaterTracker waterTracker;
    
    // Date display
    private TextView currentDateText;
    private Handler timeHandler;
    private LinearLayout datesContainer;
    private final SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
    private final SimpleDateFormat weekdayFormat = new SimpleDateFormat("E", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_tracking);

        // Initialize water tracker
        waterTracker = new WaterTracker(this);

        // Initialize views
        addWaterButton = findViewById(R.id.addWaterButton);
        waterAmountText = findViewById(R.id.waterAmount);
        waterLevelView = findViewById(R.id.waterLevelView);
        goalReachedMessage = findViewById(R.id.goalReachedMessage);
        currentDateText = findViewById(R.id.currentDateText);
        datesContainer = findViewById(R.id.datesContainer);

        // Setup time updater
        setupDateTimeUpdater();

        // Load saved water amount
        loadWaterAmount();

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

    private void setupDateTimeUpdater() {
        // Initialize date display
        updateDateDisplay();
        
        // Setup periodic update every minute
        timeHandler = new Handler(Looper.getMainLooper());
        timeHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDateDisplay();
                timeHandler.postDelayed(this, 60000); // Update every minute
            }
        }, 60000);
    }
    
    private void updateDateDisplay() {
        // Get the current date
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        
        // Set the current date text
        currentDateText.setText(dayFormat.format(today));
        
        // Find any date change
        String currentDay = dayFormat.format(today);
        if (!currentDay.equals(currentDateText.getText().toString())) {
            // If date changed, reset water tracking for the new day
            currentWaterAmount = 0;
            updateUI();
            saveWaterAmount();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update date display when activity resumes
        updateDateDisplay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save water amount when app is paused
        saveWaterAmount();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove handler callbacks to prevent leaks
        if (timeHandler != null) {
            timeHandler.removeCallbacksAndMessages(null);
        }
    }

    private void loadWaterAmount() {
        // Load water amount using the water tracker
        waterTracker.loadWaterAmount(new WaterTracker.WaterAmountCallback() {
            @Override
            public void onWaterAmountLoaded(int amount) {
                currentWaterAmount = amount;
                updateUI();
            }
        });
    }

    private void saveWaterAmount() {
        // Save water amount using the water tracker
        waterTracker.saveWaterAmount(currentWaterAmount, WATER_GOAL);
    }

    private void updateUI() {
        // Update the displayed amount
        waterAmountText.setText(String.valueOf(currentWaterAmount));
        
        // Update the water level visualization
        updateWaterLevel();
        
        // Check if goal is reached
        if (currentWaterAmount >= WATER_GOAL) {
            goalReached();
        } else {
            // Re-enable button if below goal
            addWaterButton.setEnabled(true);
            addWaterButton.setAlpha(1.0f);
            goalReachedMessage.setVisibility(View.GONE);
        }
    }

    private void addWater(int amount) {
        currentWaterAmount += amount;
        
        // Update UI and save data
        updateUI();
        saveWaterAmount();
    }
    
    private void updateWaterLevel() {
        // Calculate height percentage based on current amount vs goal
        float percentage = Math.min(1.0f, (float) currentWaterAmount / WATER_GOAL);
        
        // Get parent view height to calculate actual height
        View chartContainer = findViewById(R.id.chartContainer);
        int containerHeight = chartContainer.getHeight();
        if (containerHeight == 0) {
            // If chart container height is not yet measured, post a runnable to do it later
            chartContainer.post(() -> updateWaterLevel());
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