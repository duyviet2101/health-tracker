package com.example.healthtracker.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;
import com.example.healthtracker.database.DBHelper;
import com.example.healthtracker.utils.SharedPreferencesManager;
import com.github.lzyzsd.circleprogress.ArcProgress;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class StepExerciseFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "StepExerciseFragment";
    
    // UI Components
    private TextView tvSteps;
    private TextView tvPercentage;
    private TextView tvDistance;
    private TextView tvCalories;
    private TextView tvTime;
    private TextView tvStepGoalRemaining;
    private ArcProgress circleProgress;
    private EditText etWeight;
    private Button btnStopRun;
    private ImageButton btnBack;
    private ImageButton btnRefresh;

    // Đếm bước chân
    private int currentSteps = 0;
    private float targetDistance = 2.75f;
    private long startTimeMillis = 0;
    private long pausedTime = 0;
    private boolean isRunning = false;
    private boolean isFirstStart = true;

    // Cảm biến
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int initialStepCount = 0;
    private float distanceInKm = 0;

    // Handler cho timer
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    
    // SharedPreferences
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "StepCounterPrefs";
    
    // Hằng số
    private static final float STEP_LENGTH_METERS = 0.8f;
    
    // Database
    private DBHelper dbHelper;

    // Interface để giao tiếp với MainActivity
    private MainActivity.OnStepCounterStateListener stateListener;
    
    private static final int STEPS_PER_SECOND = 2; // Tốc độ đếm giả lập: 2 bước/giây
    private boolean isEmulatorMode = false; // Cờ cho biết đang dùng chế độ giả lập
    private Handler simulationHandler = new Handler(Looper.getMainLooper());
    private Runnable simulationRunnable;
    
    public StepExerciseFragment() {
        // Required empty public constructor
    }
    
    // Tạo instance mới với target distance
    public static StepExerciseFragment newInstance(double targetDistance) {
        StepExerciseFragment fragment = new StepExerciseFragment();
        Bundle args = new Bundle();
        args.putDouble("targetDistance", targetDistance);
        fragment.setArguments(args);
        return fragment;
    }
    
    public void setStateListener(MainActivity.OnStepCounterStateListener listener) {
        this.stateListener = listener;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Khởi tạo SharedPreferences
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Khởi tạo DBHelper
        dbHelper = new DBHelper(requireContext());
        
        // Đọc target distance từ arguments
        Bundle args = getArguments();
        if (args != null && args.containsKey("targetDistance")) {
            targetDistance = (float) args.getDouble("targetDistance", 2.75);
            
            // Reset state cho phiên mới
            currentSteps = 0;
            pausedTime = 0;
            initialStepCount = 0;
            isFirstStart = true;
            
            Log.d(TAG, "Khởi tạo phiên chạy mới với mục tiêu: " + targetDistance + " km");
        } else {
            // Load state từ SharedPreferences
            loadStateFromPrefs();
        }
        
        // Ẩn thanh navigation khi hiển thị màn hình exercise
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_step_exercise, container, false);
        
        try {
            // Ẩn thanh navigation khi hiển thị màn hình exercise
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).hideBottomNavigation();
            }
            
            // Khởi tạo các view
            tvSteps = view.findViewById(R.id.tvSteps);
            tvPercentage = view.findViewById(R.id.tvPercentage);
            tvDistance = view.findViewById(R.id.tvDistance);
            tvCalories = view.findViewById(R.id.tvCalories);
            tvTime = view.findViewById(R.id.tvTime);
            tvStepGoalRemaining = view.findViewById(R.id.tvStepGoalRemaining);
            circleProgress = view.findViewById(R.id.circleProgress);
            etWeight = view.findViewById(R.id.etWeight);
            btnStopRun = view.findViewById(R.id.btnStopRun);
            btnBack = view.findViewById(R.id.btnBack);
            btnRefresh = view.findViewById(R.id.btnRefresh);
            
            // Khởi tạo SensorManager
            sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
            stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            
            // Hiển thị cân nặng từ SharedPreferences
            int savedWeight = prefs.getInt("userWeight", 70);
            etWeight.setText(String.valueOf(savedWeight));
            
            // Thiết lập listeners cho các nút
            setupButtons();
            
            // Cập nhật UI
            updateUI();
            
            // Tự động bắt đầu đếm bước chân ngay khi mở ứng dụng
            startStepCounter();
            
            // Thông báo cho MainActivity
            if (stateListener != null) {
                stateListener.onStepCounterStarted();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi trong onCreateView: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }
    
    private void setupButtons() {
        // Nút dừng/bắt đầu
        btnStopRun.setOnClickListener(v -> {
            if (isRunning) {
                // Dừng đếm bước chân
                stopStepCounter();
                
                // Chuyển sang màn hình kết quả
                showRunResult();
            } else {
                // Bắt đầu đếm bước
                startStepCounter();
            }
        });
        
        // Nút quay lại
        btnBack.setOnClickListener(v -> {
            // Dừng đếm bước chân
            if (isRunning) {
                stopStepCounter();
            }
            
            // Hiển thị dialog xác nhận nếu có dữ liệu
            if (currentSteps > 0) {
                // Hiển thị dialog xác nhận
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
                builder.setTitle("Xác nhận")
                       .setMessage("Bạn có muốn lưu kết quả trước khi thoát không?")
                       .setPositiveButton("Lưu", (dialog, id) -> {
                           showRunResult();
                       })
                       .setNegativeButton("Huỷ bỏ", (dialog, id) -> {
                           resetStepCounter();
                           navigateBack();
                       });
                builder.create().show();
            } else {
                // Không có dữ liệu, quay về màn hình trước
                navigateBack();
            }
        });
        
        // Nút làm mới
        btnRefresh.setOnClickListener(v -> {
            // Reset bước đếm
            resetStepCounter();
            
            // Bắt đầu đếm lại ngay lập tức
            startStepCounter();
            
            // Thông báo
            Toast.makeText(requireContext(), "Đã làm mới số bước", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void navigateBack() {
        // Quay về màn hình trước
        if (getActivity() != null) {
            // Đảm bảo thanh navigation được hiển thị khi quay về StepCounterFragment
            if (getActivity() instanceof MainActivity) {
                // Thông báo cho MainActivity hiển thị lại thanh navigation
                // nếu fragment tiếp theo là StepCounterFragment
                ((MainActivity) getActivity()).showBottomNavigation();
            }
            
            // Quay lại fragment trước đó
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
    
    private void startStepCounter() {
        try {
            // Kiểm tra xem có cảm biến bước không
            if (stepSensor == null) {
                // Thiết bị không hỗ trợ đếm bước chân, chuyển sang chế độ giả lập
                Toast.makeText(requireContext(), "Đang dùng chế độ giả lập (2 bước/giây)", Toast.LENGTH_SHORT).show();
                isEmulatorMode = true;
            } else {
                // Có cảm biến bước chân, đăng ký lắng nghe
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
                isEmulatorMode = false;
            }
            
            // Thiết lập thời gian bắt đầu
            startTimeMillis = SystemClock.elapsedRealtime() - pausedTime;
            
            // Bắt đầu timer
            timerHandler.post(timerRunnable);
            
            // Nếu ở chế độ giả lập, bắt đầu tăng bước giả lập
            if (isEmulatorMode) {
                startStepSimulation();
            }
            
            // Cập nhật UI
            isRunning = true;
            btnStopRun.setText("Dừng lại");
            btnStopRun.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorStopRed));
            
            // Lưu trạng thái
            saveRunningState(true);
            
            Log.d(TAG, "Bắt đầu đếm bước chân" + (isEmulatorMode ? " (Chế độ giả lập)" : ""));
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi bắt đầu đếm bước chân: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startStepSimulation() {
        simulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && isEmulatorMode) {
                    // Tăng 2 bước mỗi giây
                    currentSteps += STEPS_PER_SECOND;
                    
                    // Cập nhật UI
                    updateUI();
                    
                    // Lặp lại sau 1 giây
                    simulationHandler.postDelayed(this, 1000);
                }
            }
        };
        
        // Bắt đầu giả lập
        simulationHandler.post(simulationRunnable);
    }
    
    private void stopStepCounter() {
        try {
            // Hủy đăng ký cảm biến nếu không ở chế độ giả lập
            if (!isEmulatorMode && stepSensor != null) {
                sensorManager.unregisterListener(this);
            }
            
            // Dừng timer
            timerHandler.removeCallbacks(timerRunnable);
            
            // Dừng giả lập nếu đang chạy
            if (isEmulatorMode && simulationRunnable != null) {
                simulationHandler.removeCallbacks(simulationRunnable);
            }
            
            // Lưu thời gian đã trôi qua
            pausedTime = SystemClock.elapsedRealtime() - startTimeMillis;
            
            // Cập nhật UI
            isRunning = false;
            btnStopRun.setText("Tiếp tục");
            btnStopRun.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorStartGreen));
            
            // Lưu trạng thái
            saveRunningState(false);
            
            Log.d(TAG, "Dừng đếm bước chân");
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi dừng đếm bước chân: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showRunResult() {
        // Chỉ hiển thị kết quả nếu có bước chân
        if (currentSteps <= 0) {
            Toast.makeText(requireContext(), "Không có dữ liệu bước chân để hiển thị", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Chuẩn bị dữ liệu cho Result Fragment
        float distanceKm = calculateDistance(currentSteps);
        int calories = calculateCalories(distanceKm);
        int percentage = calculatePercentage(distanceKm);
        long elapsedTimeMillis = calculateElapsedTime();
        
        // Lưu dữ liệu vào database
        try {
            // Định dạng ngày và thời gian đúng chuẩn
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            
            // Lấy thời gian hiện tại
            Date currentDateTime = new Date();
            String currentDate = dateFormat.format(currentDateTime);
            String currentTime = timeFormat.format(currentDateTime);
            
            Log.d(TAG, "Đang lưu dữ liệu: ngày=" + currentDate + ", thời gian=" + currentTime + 
                      ", bước=" + currentSteps + ", khoảng cách=" + distanceKm + 
                      ", calories=" + calories + ", thời gian chạy=" + elapsedTimeMillis);
            
            // Lưu vào cơ sở dữ liệu
            dbHelper.addStepHistory(
                currentDate,
                currentTime,
                currentSteps,
                distanceKm,
                calories,
                elapsedTimeMillis,
                targetDistance,  // Sử dụng mục tiêu khoảng cách đã đặt
                percentage       // Phần trăm hoàn thành tính toán
            );
            
            Log.d(TAG, "Đã lưu dữ liệu bước chân vào database thành công");
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lưu dữ liệu vào database: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Tạo Result Fragment với dữ liệu
        RunResultFragment resultFragment = RunResultFragment.newInstance(
                currentSteps,
                distanceKm,
                calories,
                elapsedTimeMillis,
                percentage,
                targetDistance
        );
        
        // Reset state cho lần sau
        resetStepCounter();
        
        // Chuyển đến Result Fragment
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, resultFragment);
        transaction.commit();
    }
    
    private float calculateDistance(int steps) {
        // Lấy độ dài sải chân từ SharedPreferences
        float strideLength = prefs.getFloat("stride_length", STEP_LENGTH_METERS);
        return (steps * strideLength) / 1000f; // Chuyển từ m sang km
    }
    
    private int calculateCalories(float distanceKm) {
        int weight = 70;
        try {
            weight = Integer.parseInt(etWeight.getText().toString());
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi đọc cân nặng: " + e.getMessage());
        }
        return (int) (weight * distanceKm * 1.036);
    }
    
    private int calculatePercentage(float distanceKm) {
        if (targetDistance <= 0) return 0;
        int percentage = (int) (distanceKm / targetDistance * 100);
        return Math.min(percentage, 100);
    }
    
    private long calculateElapsedTime() {
        if (isRunning) {
            return SystemClock.elapsedRealtime() - startTimeMillis;
        } else {
            return pausedTime;
        }
    }
    
    private void updateUI() {
        try {
            // Cập nhật số bước
            tvSteps.setText(String.format(Locale.getDefault(), "%,d", currentSteps));
            
            // Tính và hiển thị khoảng cách
            float distanceKm = calculateDistance(currentSteps);
            tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", distanceKm));
            
            // Tính và hiển thị calo
            int calories = calculateCalories(distanceKm);
            tvCalories.setText(String.format(Locale.getDefault(), "%d kcal", calories));
            
            // Tính và hiển thị phần trăm hoàn thành
            int percentage = calculatePercentage(distanceKm);
            tvPercentage.setText(String.format(Locale.getDefault(), "%d%%", percentage));
            circleProgress.setProgress(percentage);
            
            // Hiển thị thời gian
            long elapsedTimeMillis = calculateElapsedTime();
            String timeText = formatTime(elapsedTimeMillis);
            tvTime.setText(timeText + " phút");
            
            // Hiển thị mục tiêu khoảng cách
            float remainingDistance = Math.max(0, targetDistance - distanceKm);
            
            // Hiển thị cả mục tiêu và khoảng cách còn lại
            tvStepGoalRemaining.setText(String.format(Locale.getDefault(), 
                    "Mục tiêu: %.2f km (còn %.2f km)", 
                    targetDistance, remainingDistance));
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi cập nhật UI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String formatTime(long millis) {
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    
    private void resetStepCounter() {
        currentSteps = 0;
        initialStepCount = 0;
        pausedTime = 0;
        isFirstStart = true;
        isRunning = false;
        
        // Lưu trạng thái
        saveStateToPrefs();
        
        // Cập nhật UI
        updateUI();
    }
    
    private void saveStateToPrefs() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("currentSteps", currentSteps);
        editor.putInt("initialStepCount", initialStepCount);
        editor.putLong("pausedTime", pausedTime);
        editor.putBoolean("isRunning", isRunning);
        editor.putFloat("targetDistance", targetDistance);
        editor.apply();
    }
    
    private void loadStateFromPrefs() {
        currentSteps = prefs.getInt("currentSteps", 0);
        initialStepCount = prefs.getInt("initialStepCount", 0);
        pausedTime = prefs.getLong("pausedTime", 0);
        isRunning = prefs.getBoolean("isRunning", false);
        targetDistance = prefs.getFloat("targetDistance", 2.75f);
    }
    
    private void saveRunningState(boolean running) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isRunning", running);
        editor.apply();
    }
    
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateUI();
            
            // Kiểm tra nếu đã đạt mục tiêu thì tự động dừng
            if (isRunning) {
                float distanceKm = calculateDistance(currentSteps);
                int percentage = calculatePercentage(distanceKm);
                
                // Nếu đạt 100% mục tiêu thì tự động dừng và hiển thị kết quả
                if (percentage >= 100) {
                    Log.d(TAG, "Đã đạt mục tiêu, tự động dừng đếm bước chân");
                    stopStepCounter();
                    showRunResult();
                    return;
                }
                
                // Tiếp tục cập nhật UI mỗi giây
                timerHandler.postDelayed(this, 1000);
            }
        }
    };
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isRunning && event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            if (initialStepCount == 0) {
                initialStepCount = (int) event.values[0];
            }
            
            int currentStepCount = (int) event.values[0];
            currentSteps = currentStepCount - initialStepCount;
            
            // Cập nhật UI
            updateUI();
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không cần xử lý
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        if (isRunning) {
            // Đăng ký cảm biến lại nếu đang chạy và không ở chế độ giả lập
            if (!isEmulatorMode && stepSensor != null) {
                sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
            
            // Bắt đầu lại giả lập nếu đang ở chế độ giả lập
            if (isEmulatorMode) {
                startStepSimulation();
            }
            
            // Bắt đầu lại timer
            timerHandler.post(timerRunnable);
        }
        
        // Thông báo cho MainActivity
        if (stateListener != null) {
            stateListener.onStepCounterStarted();
        }
        
        // Đảm bảo thanh navigation bị ẩn khi quay lại fragment
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Lưu trạng thái
        saveStateToPrefs();
        
        // Hủy đăng ký cảm biến để tiết kiệm pin
        if (!isEmulatorMode && stepSensor != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Dừng timer
        timerHandler.removeCallbacks(timerRunnable);
        
        // Dừng giả lập tạm thời khi ứng dụng ở background
        if (isEmulatorMode && simulationRunnable != null) {
            simulationHandler.removeCallbacks(simulationRunnable);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Hủy đăng ký cảm biến
        if (!isEmulatorMode && stepSensor != null) {
            sensorManager.unregisterListener(this);
        }
        
        // Dừng timer
        timerHandler.removeCallbacks(timerRunnable);
        
        // Dừng giả lập
        if (isEmulatorMode && simulationRunnable != null) {
            simulationHandler.removeCallbacks(simulationRunnable);
        }
    }
} 