package com.example.healthtracker.activities;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
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
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.healthtracker.R;
import com.example.healthtracker.services.WaterReminderService;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaterGoalSettingActivity extends BaseActivity {

    private static final String TAG = "WaterGoalSetting";
    private static final int MIN_WATER_GOAL = 40; // Giá trị tối thiểu 40ml
    private static final String KEY_GOAL = "goal";
    private static final String KEY_REMINDER = "reminder_enabled";
    private static final String KEY_REMINDER_DAYS = "reminder_days";
    private static final String KEY_REMINDER_START_TIME = "reminder_start_time";
    private static final String KEY_REMINDER_END_TIME = "reminder_end_time";
    private static final String KEY_REMINDER_INTERVAL = "reminder_interval";
    
    private EditText goalAmountText;
    private Switch reminderSwitch;
    private Button saveButton;
    private LinearLayout reminderConfigLayout;
    private TextView timeRangeText;
    private TextView reminderFrequencyText;
    
    private int selectedGoal = 2000; // Default 2000ml
    private boolean reminderEnabled = false;
    private int startHour = 8;
    private int startMinute = 0;
    private int endHour = 23;
    private int endMinute = 0;
    private int reminderInterval = 2; // Mặc định 2 giờ
    private List<Integer> selectedDays = new ArrayList<>();
    private List<MaterialButton> dayButtons = new ArrayList<>();
    
    // Firestore
    private FirebaseFirestore db;
    private String userId;
    
    // Notification permission
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_goal_setting);
        
        // Khởi tạo permission launcher
        setupPermissionLauncher();
        
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
        reminderConfigLayout = findViewById(R.id.reminderConfigLayout);
        timeRangeText = findViewById(R.id.timeRangeText);
        reminderFrequencyText = findViewById(R.id.reminderFrequencyText);
        
        // Initialize day buttons
        initializeDayButtons();
        
        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        
        // Setup goal amount input
        setupGoalInput();
        
        // Setup reminder switch
        setupReminderSwitch();
        
        // Setup time range selection
        setupTimeRangeSelection();
        
        // Setup reminder frequency
        setupReminderFrequency();
        
        // Setup save button
        saveButton.setOnClickListener(v -> saveGoalSettings());
        
        // Load previously saved settings
        loadSavedSettings();
    }
    
    private void setupPermissionLauncher() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Quyền được cấp, lên lịch thông báo
                        scheduleReminderNotifications();
                    } else {
                        // Quyền bị từ chối, thông báo cho người dùng
                        Toast.makeText(this, 
                                "Bạn sẽ không nhận được thông báo vì không cấp quyền", 
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    
    private void initializeDayButtons() {
        // Add all day buttons
        dayButtons.add(findViewById(R.id.dayButtonC));
        dayButtons.add(findViewById(R.id.dayButton2));
        dayButtons.add(findViewById(R.id.dayButton3));
        dayButtons.add(findViewById(R.id.dayButton4));
        dayButtons.add(findViewById(R.id.dayButton5));
        dayButtons.add(findViewById(R.id.dayButton6));
        dayButtons.add(findViewById(R.id.dayButton7));
        
        // Set click listeners for each day button
        for (int i = 0; i < dayButtons.size(); i++) {
            final int day = i; // 0 = CN, 1 = T2, etc.
            dayButtons.get(i).setOnClickListener(v -> toggleDaySelection(day));
        }
        
        // Select all days by default
        for (int i = 0; i < 7; i++) {
            selectedDays.add(i);
            dayButtons.get(i).setStrokeWidth(0);
            dayButtons.get(i).setBackgroundTintList(getResources().getColorStateList(R.color.stats_card_dark));
            dayButtons.get(i).setTextColor(getResources().getColorStateList(R.color.white));
        }
    }
    
    private void toggleDaySelection(int day) {
        if (selectedDays.contains(day)) {
            // Deselect
            selectedDays.remove(Integer.valueOf(day));
            dayButtons.get(day).setStrokeWidth(1);
            dayButtons.get(day).setBackgroundTintList(null);
            dayButtons.get(day).setTextColor(getResources().getColorStateList(R.color.text_primary));
        } else {
            // Select
            selectedDays.add(day);
            dayButtons.get(day).setStrokeWidth(0);
            dayButtons.get(day).setBackgroundTintList(getResources().getColorStateList(R.color.stats_card_dark));
            dayButtons.get(day).setTextColor(getResources().getColorStateList(R.color.white));
        }
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
        // Set initial visibility
        reminderConfigLayout.setVisibility(reminderEnabled ? View.VISIBLE : View.GONE);
        
        reminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            reminderEnabled = isChecked;
            reminderConfigLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            
            // Kiểm tra quyền thông báo nếu người dùng bật nhắc nhở
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkNotificationPermission();
            }
        });
    }
    
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Yêu cầu quyền nếu chưa được cấp
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
    
    private void setupTimeRangeSelection() {
        // Set initial time range
        updateTimeRangeText();
        
        // Set click listener for time range
        timeRangeText.setOnClickListener(v -> showTimeRangePicker());
    }
    
    private void setupReminderFrequency() {
        // Set initial frequency text
        updateReminderFrequencyText();
        
        // Set click listener for frequency
        reminderFrequencyText.setOnClickListener(v -> showReminderFrequencyPicker());
    }
    
    private void showTimeRangePicker() {
        // Show start time picker first
        TimePickerDialog startTimePicker = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    startHour = hourOfDay;
                    startMinute = minute;
                    
                    // After selecting start time, show end time picker
                    TimePickerDialog endTimePicker = new TimePickerDialog(
                            this,
                            (endView, endHourOfDay, endMinute) -> {
                                // Kiểm tra thời gian kết thúc phải sau thời gian bắt đầu
                                if (endHourOfDay < startHour || (endHourOfDay == startHour && endMinute <= startMinute)) {
                                    Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                // Kiểm tra thời gian kết thúc không vượt quá 23:59
                                if (endHourOfDay > 23 || (endHourOfDay == 23 && endMinute > 59)) {
                                    Toast.makeText(this, "Thời gian kết thúc không được vượt quá 23:59", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                
                                endHour = endHourOfDay;
                                WaterGoalSettingActivity.this.endMinute = endMinute;
                                
                                // Update display
                                updateTimeRangeText();
                            },
                            endHour,
                            endMinute,
                            true
                    );
                    
                    endTimePicker.setTitle("Chọn giờ kết thúc");
                    endTimePicker.show();
                },
                startHour,
                startMinute,
                true
        );
        
        startTimePicker.setTitle("Chọn giờ bắt đầu");
        startTimePicker.show();
    }
    
    private void showReminderFrequencyPicker() {
        // Simple array of hour options
        final String[] hourOptions = new String[]{"1 giờ", "2 giờ", "3 giờ", "4 giờ", "6 giờ", "Tùy chỉnh"};
        final int[] hourValues = new int[]{1, 2, 3, 4, 6, -1}; // -1 for custom
        
        // Create dialog
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Chọn tần suất nhắc nhở")
                .setItems(hourOptions, (dialog, which) -> {
                    if (hourValues[which] != -1) {
                        // Sử dụng giá trị có sẵn
                        reminderInterval = hourValues[which];
                        updateReminderFrequencyText();
                    } else {
                        // Hiển thị dialog tùy chỉnh
                        showCustomIntervalDialog();
                    }
                });
        
        builder.create().show();
    }
    
    private void showCustomIntervalDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_custom_interval, null);
        final EditText hoursEditText = view.findViewById(R.id.hoursEditText);
        final EditText minutesEditText = view.findViewById(R.id.minutesEditText);
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Tùy chỉnh tần suất")
                .setView(view)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    try {
                        int hours = TextUtils.isEmpty(hoursEditText.getText()) ? 0 : 
                                Integer.parseInt(hoursEditText.getText().toString());
                        int minutes = TextUtils.isEmpty(minutesEditText.getText()) ? 0 : 
                                Integer.parseInt(minutesEditText.getText().toString());
                        
                        if (hours == 0 && minutes == 0) {
                            Toast.makeText(this, "Vui lòng nhập thời gian hợp lệ", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // Lưu dưới dạng phút để dễ tính toán
                        reminderInterval = hours * 60 + minutes;
                        
                        // Cập nhật giao diện
                        String displayText;
                        if (hours > 0 && minutes > 0) {
                            displayText = hours + " giờ " + minutes + " phút";
                        } else if (hours > 0) {
                            displayText = hours + " giờ";
                        } else {
                            displayText = minutes + " phút";
                        }
                        reminderFrequencyText.setText(displayText);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Định dạng không hợp lệ", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Hủy", null);
        
        builder.create().show();
    }
    
    private void updateTimeRangeText() {
        timeRangeText.setText(String.format("%d:%02d - %d:%02d", 
                startHour, startMinute, endHour, endMinute));
    }
    
    private void updateReminderFrequencyText() {
        if (reminderInterval >= 60) {
            int hours = reminderInterval / 60;
            int minutes = reminderInterval % 60;
            
            if (minutes > 0) {
                reminderFrequencyText.setText(hours + " giờ " + minutes + " phút");
            } else {
                reminderFrequencyText.setText(hours + " giờ");
            }
        } else {
            reminderFrequencyText.setText(reminderInterval + " phút");
        }
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
                                reminderConfigLayout.setVisibility(reminderEnabled ? View.VISIBLE : View.GONE);
                            }
                            
                            // Load reminder days
                            List<Long> days = (List<Long>) documentSnapshot.get(KEY_REMINDER_DAYS);
                            if (days != null && !days.isEmpty()) {
                                selectedDays.clear();
                                // Reset all buttons to unselected state
                                for (int i = 0; i < dayButtons.size(); i++) {
                                    dayButtons.get(i).setStrokeWidth(1);
                                    dayButtons.get(i).setBackgroundTintList(null);
                                    dayButtons.get(i).setTextColor(getResources().getColorStateList(R.color.text_primary));
                                }
                                
                                // Set selected days
                                for (Long day : days) {
                                    int dayIndex = day.intValue();
                                    selectedDays.add(dayIndex);
                                    dayButtons.get(dayIndex).setStrokeWidth(0);
                                    dayButtons.get(dayIndex).setBackgroundTintList(getResources().getColorStateList(R.color.stats_card_dark));
                                    dayButtons.get(dayIndex).setTextColor(getResources().getColorStateList(R.color.white));
                                }
                            }
                            
                            // Load time range
                            Long startTimeHour = documentSnapshot.getLong("reminder_start_hour");
                            Long startTimeMinute = documentSnapshot.getLong("reminder_start_minute");
                            Long endTimeHour = documentSnapshot.getLong("reminder_end_hour");
                            Long endTimeMinute = documentSnapshot.getLong("reminder_end_minute");
                            
                            if (startTimeHour != null && startTimeMinute != null && 
                                endTimeHour != null && endTimeMinute != null) {
                                startHour = startTimeHour.intValue();
                                startMinute = startTimeMinute.intValue();
                                endHour = endTimeHour.intValue();
                                endMinute = endTimeMinute.intValue();
                                updateTimeRangeText();
                            }
                            
                            // Load interval - hỗ trợ cả hai định dạng phút và giờ
                            Long intervalMinutes = documentSnapshot.getLong("reminder_interval_minutes");
                            if (intervalMinutes != null) {
                                // Định dạng mới - lưu dưới dạng phút
                                reminderInterval = intervalMinutes.intValue();
                            } else {
                                // Định dạng cũ - lưu dưới dạng giờ
                                Long interval = documentSnapshot.getLong(KEY_REMINDER_INTERVAL);
                                if (interval != null) {
                                    reminderInterval = interval.intValue() * 60; // Chuyển đổi giờ sang phút
                                }
                            }
                            updateReminderFrequencyText();
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
            
            // Save reminder settings if enabled
            if (reminderEnabled) {
                settings.put(KEY_REMINDER_DAYS, selectedDays);
                settings.put("reminder_start_hour", startHour);
                settings.put("reminder_start_minute", startMinute);
                settings.put("reminder_end_hour", endHour);
                settings.put("reminder_end_minute", endMinute);
                settings.put("reminder_interval_minutes", reminderInterval); // Lưu dưới dạng phút
            }
            
            settingsRef.set(settings)
                    .addOnSuccessListener(aVoid -> {
                        // Cài đặt thông báo nếu được bật
                        if (reminderEnabled) {
                            scheduleReminderNotifications();
                        } else {
                            // Hủy tất cả thông báo hiện tại nếu người dùng tắt nhắc nhở
                            new WaterReminderService(this).cancelAllReminders();
                        }
                        
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
    
    /**
     * Lên lịch thông báo nhắc nhở uống nước
     */
    private void scheduleReminderNotifications() {
        // Chỉ lên lịch nếu người dùng bật thông báo và chọn ít nhất một ngày
        if (reminderEnabled && !selectedDays.isEmpty()) {
            // Kiểm tra quyền hiển thị thông báo trên Android 13 trở lên
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == 
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Lên lịch thông báo
                    new WaterReminderService(this).scheduleReminders();
                } else {
                    // Yêu cầu quyền
                    requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            } else {
                // Phiên bản Android cũ hơn, không cần yêu cầu quyền riêng
                new WaterReminderService(this).scheduleReminders();
            }
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