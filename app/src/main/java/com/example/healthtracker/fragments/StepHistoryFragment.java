package com.example.healthtracker.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthtracker.R;
import com.example.healthtracker.adapters.StepHistoryAdapter;
import com.example.healthtracker.database.DBHelper;
import com.example.healthtracker.models.DailySummary;
import com.example.healthtracker.models.StepHistory;
import com.example.healthtracker.preferences.UserPreferences;
import com.example.healthtracker.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class StepHistoryFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "StepHistoryFragment";
    
    // UI Components
    private RecyclerView recyclerViewHistory;
    private TextView tvNoData;
    private ProgressBar progressLoading;
    private ImageView ivEmptyState;
    
    // Data
    private List<Object> historyItems; // Danh sách chứa cả StepHistory và DailySummary
    private StepHistoryAdapter adapter;
    private DBHelper dbHelper;
    private UserPreferences userPreferences;
    private SharedPreferences sharedPreferences;

    public StepHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo database helper
        dbHelper = new DBHelper(getContext());
        userPreferences = new UserPreferences(getContext());
        historyItems = new ArrayList<>();
        
        // Lấy SharedPreferences và đăng ký lắng nghe sự thay đổi
        sharedPreferences = getContext().getSharedPreferences(
                "HealthTrackerPrefs", // Sử dụng cùng tên với SharedPreferencesManager
                getContext().MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_step_history, container, false);
        
        // Khởi tạo UI components
        recyclerViewHistory = view.findViewById(R.id.recyclerViewStepHistory);
        tvNoData = view.findViewById(R.id.tvNoData);
        progressLoading = view.findViewById(R.id.progressLoading);
        ivEmptyState = view.findViewById(R.id.ivEmptyState);
        
        // Thiết lập RecyclerView
        setupRecyclerView();
        
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Tải lại dữ liệu khi quay lại fragment
        loadStepHistoryData();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Hủy đăng ký lắng nghe sự thay đổi để tránh rò rỉ bộ nhớ
        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        }
    }
    
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Nếu mục tiêu hằng ngày thay đổi, cập nhật lại dữ liệu
        if (key.equals("daily_goal")) {
            Log.d(TAG, "Mục tiêu hằng ngày đã thay đổi, cập nhật lại dữ liệu");
            loadStepHistoryData();
        }
    }
    
    // Thiết lập RecyclerView và Adapter
    private void setupRecyclerView() {
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StepHistoryAdapter(getContext(), historyItems);
        
        // Bỏ phần xử lý sự kiện click vì không cần chuyển đến màn hình chi tiết nữa
        
        recyclerViewHistory.setAdapter(adapter);
    }
    
    // Tải dữ liệu lịch sử bước chân từ cơ sở dữ liệu
    private void loadStepHistoryData() {
        // Hiển thị loading
        progressLoading.setVisibility(View.VISIBLE);
        tvNoData.setVisibility(View.GONE);
        ivEmptyState.setVisibility(View.GONE);
        
        // Thực hiện tải dữ liệu trong background thread
        new Thread(() -> {
            try {
                // Lấy mục tiêu hằng ngày
                SharedPreferencesManager prefsManager = new SharedPreferencesManager(requireContext());
                int dailyGoalMeters = prefsManager.getDailyGoalMeters();
                float currentDailyGoal = dailyGoalMeters / 1000f; // Chuyển từ mét sang km
                
                Log.d(TAG, "Đang tải dữ liệu với mục tiêu hằng ngày: " + dailyGoalMeters + " mét = " + currentDailyGoal + " km");
                
                // Đảm bảo database helper được khởi tạo
                if (dbHelper == null) {
                    dbHelper = new DBHelper(requireContext());
                }
                
                // Lấy danh sách lịch sử bước chân từ cơ sở dữ liệu
                List<StepHistory> allHistory = dbHelper.getAllStepHistory();
                
                // Log thông tin debug
                Log.d(TAG, "Đã tải được " + allHistory.size() + " bản ghi từ cơ sở dữ liệu");
                if (!allHistory.isEmpty()) {
                    StepHistory latest = allHistory.get(0);
                    Log.d(TAG, "Bản ghi mới nhất: " + latest.toString());
                }
                
                // Tổng hợp dữ liệu theo ngày
                Map<String, List<StepHistory>> historyByDate = groupHistoryByDate(allHistory);
                
                // Tạo danh sách kết hợp giữa tổng kết ngày và chi tiết hoạt động
                List<Object> combinedItems = createCombinedList(historyByDate, currentDailyGoal);
                
                // Cập nhật UI trong main thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Ẩn loading
                        progressLoading.setVisibility(View.GONE);
                        
                        // Cập nhật dataset
                        historyItems.clear();
                        historyItems.addAll(combinedItems);
                        adapter.notifyDataSetChanged();
                        
                        // Hiển thị trạng thái không có dữ liệu nếu cần
                        if (historyItems.isEmpty()) {
                            tvNoData.setVisibility(View.VISIBLE);
                            ivEmptyState.setVisibility(View.VISIBLE);
                            recyclerViewHistory.setVisibility(View.GONE);
                        } else {
                            tvNoData.setVisibility(View.GONE);
                            ivEmptyState.setVisibility(View.GONE);
                            recyclerViewHistory.setVisibility(View.VISIBLE);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading step history data: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressLoading.setVisibility(View.GONE);
                        tvNoData.setText("Đã xảy ra lỗi khi tải dữ liệu");
                        tvNoData.setVisibility(View.VISIBLE);
                        ivEmptyState.setVisibility(View.VISIBLE);
                    });
                }
            }
        }).start();
    }
    
    // Nhóm lịch sử theo ngày
    private Map<String, List<StepHistory>> groupHistoryByDate(List<StepHistory> history) {
        Map<String, List<StepHistory>> historyByDate = new HashMap<>();
        
        for (StepHistory item : history) {
            // Sử dụng trực tiếp trường date (đã là String) của StepHistory
            String dateStr = item.getDate();
            
            // Chuyển đổi định dạng từ yyyy-MM-dd sang dd/MM/yyyy nếu cần thiết
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date date = inputFormat.parse(dateStr);
                dateStr = outputFormat.format(date);
            } catch (Exception e) {
                // Nếu có lỗi khi parse, giữ nguyên dateStr
                Log.e(TAG, "Lỗi khi chuyển đổi định dạng ngày: " + e.getMessage());
            }
            
            if (!historyByDate.containsKey(dateStr)) {
                historyByDate.put(dateStr, new ArrayList<>());
            }
            historyByDate.get(dateStr).add(item);
        }
        
        return historyByDate;
    }
    
    // Tạo danh sách kết hợp giữa tổng kết ngày và chi tiết hoạt động
    private List<Object> createCombinedList(Map<String, List<StepHistory>> historyByDate, float currentDailyGoal) {
        List<Object> result = new ArrayList<>();
        
        // Duyệt qua từng ngày và thêm cả header lẫn items
        for (String date : historyByDate.keySet()) {
            List<StepHistory> dailyItems = historyByDate.get(date);
            
            // Tính tổng các chỉ số trong ngày
            int totalSteps = 0;
            float totalDistance = 0;
            int totalCalories = 0;
            long totalDuration = 0;
            
            for (StepHistory item : dailyItems) {
                totalSteps += item.getSteps();
                totalDistance += item.getDistance();
                totalCalories += item.getCalories();
                totalDuration += item.getDuration();
            }
            
            // Tính phần trăm hoàn thành mục tiêu dựa trên mục tiêu HIỆN TẠI
            int completionPercentage = 0;
            if (currentDailyGoal > 0) {
                completionPercentage = (int) (totalDistance / currentDailyGoal * 100);
            }
            
            // Tạo DailySummary với mục tiêu hiện tại
            DailySummary summary = new DailySummary();
            try {
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date dateObj = format.parse(date);
                summary.setDate(dateObj);
            } catch (Exception e) {
                Log.e(TAG, "Lỗi chuyển đổi ngày: " + e.getMessage());
                summary.setDate(new Date());
            }
            
            summary.setTotalSteps(totalSteps);
            summary.setTotalDistance(totalDistance);
            summary.setTotalCalories(totalCalories);
            summary.setTotalDuration(totalDuration);
            summary.setDailyGoalDistance(currentDailyGoal);
            summary.setCompletionPercentage(completionPercentage);
            summary.setActivities(dailyItems);
            
            // Thêm tiêu đề ngày vào danh sách kết quả
            result.add(summary);
            
            // Thêm chi tiết từng hoạt động trong ngày vào danh sách kết quả
            for (StepHistory activity : dailyItems) {
                result.add(activity);
            }
        }
        
        return result;
    }
} 