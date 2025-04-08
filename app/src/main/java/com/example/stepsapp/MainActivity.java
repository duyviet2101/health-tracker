package com.example.stepsapp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import android.Manifest;
import com.example.stepsapp.activities.FirebaseUIActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    private TextView stepsCount;
    private TextView timeValue;
    private TextView caloriesValue;
    private TextView distanceValue;
    private TextView userNameText;
    private ImageButton shareButton;
    private ImageView profileImage;
    private MaterialCardView statsButton;
    
    private BroadcastReceiver stepUpdateReceiver;
    private boolean isReceiverRegistered = false;
    private StepCounterData stepData;
    private FirebaseAuth mAuth;

    // Định dạng số
    private DecimalFormat caloriesFormat = new DecimalFormat("#,##0");
    private DecimalFormat distanceFormat = new DecimalFormat("#,##0.0");
    
    // Lưu trữ dữ liệu cuối cùng nhận được để chia sẻ
    private int lastSteps = 0;
    private double lastDistance = 0;
    private double lastCalories = 0;
    private long lastActiveTime = 0;
    
    // Giúp yêu cầu quyền truy cập từ người dùng
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startStepCounterService();
                } else {
                    Toast.makeText(this, "Cần quyền theo dõi hoạt động để đếm bước chân", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Khởi tạo Firebase Auth ngay từ đầu
            mAuth = FirebaseAuth.getInstance();
            
            // Kiểm tra trạng thái đăng nhập trước khi tiếp tục
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                // Chưa đăng nhập, chuyển đến màn hình đăng nhập
                goToLoginScreen();
                return; // Dừng việc tạo UI chính
            }
            
            // Nếu đã đăng nhập, tiếp tục khởi tạo UI
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_main);
            
            // Khởi tạo quản lý dữ liệu
            stepData = StepCounterData.getInstance(this);
            
            // Khởi tạo các view
            initViews();
            
            // Thiết lập BroadcastReceiver để nhận thông báo từ service
            setupBroadcastReceiver();
            
            // Cấu hình sự kiện click
            setupClickEvents();
            
            // Kiểm tra và yêu cầu quyền ACTIVITY_RECOGNITION (cần thiết từ Android 10 trở lên)
            checkPermissionsAndStartService();
            
            // Hiển thị thông tin người dùng
            showUserInfo();
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            Toast.makeText(this, "Lỗi khởi tạo ứng dụng: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void initViews() {
        try {
            stepsCount = findViewById(R.id.stepsCount);
            timeValue = findViewById(R.id.timeValue);
            caloriesValue = findViewById(R.id.caloriesValue);
            distanceValue = findViewById(R.id.distanceValue);
            shareButton = findViewById(R.id.shareButton);
            statsButton = findViewById(R.id.stats_button);
            profileImage = findViewById(R.id.profileImage);
            
            // Thêm TextView cho tên người dùng
            userNameText = new TextView(this);
            userNameText.setTextColor(getResources().getColor(R.color.black, null));
            userNameText.setTextSize(16);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 8);
            userNameText.setLayoutParams(params);
            
            // Thêm TextView vào layout
            LinearLayout statsContainer = findViewById(R.id.statsContainer);
            statsContainer.addView(userNameText, 0);
            
            if (stepsCount == null || timeValue == null || caloriesValue == null || distanceValue == null) {
                Log.e(TAG, "One or more TextViews not found in layout");
                Toast.makeText(this, "Lỗi tìm giao diện", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Hiển thị số liệu ban đầu từ bộ nhớ lưu trữ
            int initialSteps = stepData.getSteps();
            stepsCount.setText(String.format("%,d", initialSteps));
            caloriesValue.setText(caloriesFormat.format(stepData.calculateCalories(initialSteps)));
            distanceValue.setText(formatDistance(stepData.calculateDistance(initialSteps)));
            timeValue.setText(formatTime(stepData.calculateActiveTime()));
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: ", e);
        }
    }
    
    private void showUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                userNameText.setText("Hi, " + displayName);
            } else {
                userNameText.setText("Hi, User");
            }
        } else {
            // Không có người dùng đăng nhập, quay lại màn hình đăng nhập
            goToLoginScreen();
        }
    }
    
    private void setupClickEvents() {
        try {
            // Cấu hình nút chia sẻ
            if (shareButton != null) {
                shareButton.setOnClickListener(v -> shareStepStats());
            }
            
            // Cấu hình nút thống kê/đặt lại
            if (statsButton != null) {
                statsButton.setOnClickListener(v -> showResetDialog());
            } else {
                Log.e(TAG, "statsButton not found");
            }
            
            // Cấu hình nút profile/đăng xuất
            if (profileImage != null) {
                profileImage.setOnClickListener(v -> showSignOutDialog());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in setupClickEvents: ", e);
        }
    }
    
    private void showSignOutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có muốn đăng xuất khỏi ứng dụng không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> signOut())
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private void signOut() {
        // Đăng xuất Firebase
        mAuth.signOut();
        
        // Quay lại màn hình đăng nhập
        goToLoginScreen();
        
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
    }
    
    private void goToLoginScreen() {
        Intent intent = new Intent(this, FirebaseUIActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void showResetDialog() {
        try {
            new AlertDialog.Builder(this)
                    .setTitle("Quản lý dữ liệu bước chân")
                    .setMessage("Bạn có muốn đặt lại số liệu bước chân không?")
                    .setPositiveButton("Đặt lại", (dialog, which) -> resetStepData())
                    .setNegativeButton("Hủy", null)
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing reset dialog: ", e);
            Toast.makeText(this, "Không thể hiển thị hộp thoại", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void resetStepData() {
        try {
            long currentTime = System.currentTimeMillis();
            stepData.resetData(currentTime);
            
            // Cập nhật UI với giá trị ban đầu
            if (stepsCount != null) stepsCount.setText("0");
            if (timeValue != null) timeValue.setText("0h 0m");
            if (caloriesValue != null) caloriesValue.setText("0");
            if (distanceValue != null) distanceValue.setText("0 m");
            
            // Thông báo cho người dùng
            Toast.makeText(this, "Đã đặt lại số liệu bước chân", Toast.LENGTH_SHORT).show();
            
            // Khởi động lại service để cập nhật dữ liệu
            restartStepService();
        } catch (Exception e) {
            Log.e(TAG, "Error in resetStepData: ", e);
            Toast.makeText(this, "Lỗi khi đặt lại dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void restartStepService() {
        try {
            // Dừng service hiện tại
            stopService(new Intent(this, StepCounterService.class));
            
            // Khởi động lại service sau 1 giây
            new android.os.Handler().postDelayed(this::startStepCounterService, 1000);
        } catch (Exception e) {
            Log.e(TAG, "Error restarting service: ", e);
        }
    }
    
    private void shareStepStats() {
        try {
            String shareText = String.format(
                    "Hôm nay tôi đã đi được %,d bước (%s), đốt %s calo trong khoảng thời gian %s! #StepsApp",
                    lastSteps,
                    formatDistance(lastDistance),
                    caloriesFormat.format(lastCalories),
                    formatTime(lastActiveTime)
            );
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ số liệu bước chân"));
        } catch (Exception e) {
            Log.e(TAG, "Error sharing stats: ", e);
            Toast.makeText(this, "Không thể chia sẻ dữ liệu", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void setupBroadcastReceiver() {
        try {
            stepUpdateReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // Nhận cả hai loại broadcast
                    if (StepCounterService.ACTION_STEPS_UPDATED.equals(intent.getAction()) ||
                        "com.example.stepsapp.LOCAL_STEPS_UPDATED".equals(intent.getAction())) {
                        updateUI(intent);
                    }
                }
            };
        } catch (Exception e) {
            Log.e(TAG, "Error setting up receiver: ", e);
        }
    }
    
    private void updateUI(Intent intent) {
        try {
            lastSteps = intent.getIntExtra(StepCounterService.EXTRA_STEPS, 0);
            lastDistance = intent.getDoubleExtra(StepCounterService.EXTRA_DISTANCE, 0.0);
            lastCalories = intent.getDoubleExtra(StepCounterService.EXTRA_CALORIES, 0.0);
            lastActiveTime = intent.getLongExtra(StepCounterService.EXTRA_TIME, 0);
            
            // Cập nhật UI
            if (stepsCount != null) {
                stepsCount.setText(String.format("%,d", lastSteps));
            }
            
            // Hiển thị thời gian theo format giờ:phút
            if (timeValue != null) {
                timeValue.setText(formatTime(lastActiveTime));
            }
            
            // Hiển thị calo
            if (caloriesValue != null) {
                caloriesValue.setText(caloriesFormat.format(lastCalories));
            }
            
            // Hiển thị khoảng cách (m hoặc km)
            if (distanceValue != null) {
                distanceValue.setText(formatDistance(lastDistance));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: ", e);
        }
    }
    
    private String formatTime(long minutes) {
        try {
            long hours = minutes / 60;
            long mins = minutes % 60;
            
            return String.format("%dh %dm", hours, mins);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting time: ", e);
            return "0h 0m";
        }
    }
    
    private String formatDistance(double distanceInMeters) {
        try {
            if (distanceInMeters < 1000) {
                return distanceFormat.format(distanceInMeters) + " m";
            } else {
                return distanceFormat.format(distanceInMeters / 1000) + " km";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting distance: ", e);
            return "0 m";
        }
    }
    
    private void checkPermissionsAndStartService() {
        try {
            // Kiểm tra quyền ACTIVITY_RECOGNITION (cần thiết từ Android 10 trở lên)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION);
                } else {
                    startStepCounterService();
                }
            } else {
                // Với các phiên bản Android cũ hơn, không cần quyền đặc biệt
                startStepCounterService();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions: ", e);
            Toast.makeText(this, "Lỗi khi kiểm tra quyền truy cập", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void startStepCounterService() {
        try {
            Intent serviceIntent = new Intent(this, StepCounterService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "Step counter service started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting service: ", e);
            Toast.makeText(this, "Không thể khởi động dịch vụ đếm bước chân", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        try {
            // Đăng ký BroadcastReceiver
            if (!isReceiverRegistered && stepUpdateReceiver != null) {
                IntentFilter filter = new IntentFilter();
                // Thêm cả hai action
                filter.addAction(StepCounterService.ACTION_STEPS_UPDATED);
                filter.addAction("com.example.stepsapp.LOCAL_STEPS_UPDATED");
                
                // Từ Android 14 (API level 34), cần chỉ định cờ RECEIVER_EXPORTED hoặc RECEIVER_NOT_EXPORTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
                    registerReceiver(stepUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
                } else {
                    registerReceiver(stepUpdateReceiver, filter);
                }
                
                isReceiverRegistered = true;
                Log.d(TAG, "BroadcastReceiver đã đăng ký thành công");
            }
            
            // Kiểm tra trạng thái đăng nhập khi quay lại màn hình
            showUserInfo();
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume: ", e);
        }
    }
    
    @Override
    protected void onPause() {
        try {
            super.onPause();
            // Hủy đăng ký BroadcastReceiver
            if (isReceiverRegistered && stepUpdateReceiver != null) {
                unregisterReceiver(stepUpdateReceiver);
                isReceiverRegistered = false;
                Log.d(TAG, "BroadcastReceiver đã hủy đăng ký");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause: ", e);
        }
    }
}