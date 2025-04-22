package com.example.healthtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.healthtracker.R;
import com.example.healthtracker.utils.WaterTracker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import android.widget.EditText;

public class WaterTrackingActivity extends AppCompatActivity {

    private static final String TAG = "WaterTrackingActivity";

    private MaterialButton addWaterButton;
    private TextView waterAmountText;
    private TextView goalReachedMessage;
    private TextView waterGoalText;
    private View waterLevelView;
    private int currentWaterAmount = 0;
    private int waterGoal = 2000; // Default 2000ml goal

    // Water tracker
    private WaterTracker waterTracker;
    private FirebaseFirestore db;
    private String userId;

    // New variable for the input field
    private EditText waterAmountInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_water_tracking);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            userId = "anonymous";
        }

        // Initialize water tracker
        waterTracker = new WaterTracker(this);

        // Initialize views
        addWaterButton = findViewById(R.id.addWaterButton);
        waterAmountText = findViewById(R.id.waterAmount);
        waterLevelView = findViewById(R.id.waterLevelView);
        goalReachedMessage = findViewById(R.id.goalReachedMessage);
        waterGoalText = findViewById(R.id.waterGoal);
        waterAmountInput = findViewById(R.id.waterAmountInput);
        waterAmountInput.setText("0"); // Đặt giá trị mặc định là 0

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Setup menu button
        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });

        // Setup add water button
        addWaterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentWaterAmount < waterGoal) {
                    // Lấy giá trị từ EditText
                    String inputText = waterAmountInput.getText().toString().trim();
                    int waterAmount = 0;
                    
                    // Parse giá trị đầu vào hoặc sử dụng 0 nếu trống/không hợp lệ
                    if (!inputText.isEmpty()) {
                        try {
                            waterAmount = Integer.parseInt(inputText);
                        } catch (NumberFormatException e) {
                            // Không cần thông báo lỗi, chỉ cần sử dụng giá trị 0
                            waterAmountInput.setText("0");
                        }
                    } else {
                        // Nếu trống, sử dụng giá trị mặc định 0 và cập nhật UI
                        waterAmountInput.setText("0");
                    }
                    
                    if (waterAmount <= 0) {
                        // Hiển thị thông báo lỗi cho số lượng không hợp lệ
                        Toast.makeText(WaterTrackingActivity.this, "Vui lòng nhập số lượng lớn hơn 0", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Thêm lượng nước đã nhập
                    addWater(waterAmount);
                    
                    // KHÔNG xóa giá trị trong input field sau khi thêm
                    // Giữ nguyên giá trị cho đến khi người dùng thay đổi
                } else {
                    Toast.makeText(WaterTrackingActivity.this, "Đã đạt mục tiêu!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Load water goal and data
        loadWaterGoal();
    }

    private void loadWaterGoal() {
        // First try to load from Firestore
        if (!"anonymous".equals(userId)) {
            db.collection("users").document(userId)
                    .collection("settings").document("water")
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("goal")) {
                            Long goal = documentSnapshot.getLong("goal");
                            if (goal != null) {
                                waterGoal = goal.intValue();
                                updateWaterGoalDisplay();
                            }
                        }
                        // After loading goal, load water amount
                        loadWaterAmount();
                    })
                    .addOnFailureListener(e -> {
                        // On failure, just load water amount with default goal
                        loadWaterAmount();
                    });
        } else {
            // If not logged in, just load water amount with default goal
            loadWaterAmount();
        }
    }

    private void updateWaterGoalDisplay() {
        // Update the waterGoal TextView
        waterGoalText.setText("/" + waterGoal + " ml");
    }

    private void showPopupMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.water_tracking_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.menu_set_goal) {
                    // Open goal setting activity
                    openGoalSettingScreen();
                    return true;
                }
                return false;
            }
        });
        
        popupMenu.show();
    }
    
    private void openGoalSettingScreen() {
        Intent intent = new Intent(this, WaterGoalSettingActivity.class);
        intent.putExtra("current_goal", waterGoal);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload water goal and amount when returning to this screen
        loadWaterGoal();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save water amount when app is paused
        saveWaterAmount();
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
        waterTracker.saveWaterAmount(currentWaterAmount, waterGoal);
    }

    private void updateUI() {
        // Update the displayed amount
        waterAmountText.setText(String.valueOf(currentWaterAmount));
        
        // Update the water level visualization
        updateWaterLevel();
        
        // Check if goal is reached
        if (currentWaterAmount >= waterGoal) {
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
        float percentage = Math.min(1.0f, (float) currentWaterAmount / waterGoal);
        
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