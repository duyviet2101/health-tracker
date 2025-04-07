package com.example.healthtracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ImageView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.content.ContextCompat;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;
import com.example.healthtracker.database.DBHelper;
import com.example.healthtracker.models.StepHistory;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;

public class RunResultFragment extends Fragment {
    
    private static final String TAG = "RunResultFragment";
    
    // Thêm định dạng ngày tháng và thời gian
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    private TextView tvRunTitle;
    private TextView tvStepCount;
    private TextView tvTimeElapsed;
    private TextView tvPace;
    private TextView tvDistance;
    private TextView tvCalories;
    private TextView tvCompletionPercent;
    private ImageButton btnBack;
    private Button btnShareResult;
    private boolean isTargetCompleted = false;
    
    // Dữ liệu từ StepCounterFragment
    private int steps;
    private float distanceKm;
    private int calories;
    private long timeInMillis;
    private int completionPercentage;
    private float targetDistance;
    
    // Database helper
    private DBHelper dbHelper;

    public RunResultFragment() {
        // Required empty public constructor
    }
    
    // Tạo instance mới với dữ liệu từ StepCounterFragment
    public static RunResultFragment newInstance(int steps, float distanceKm, int calories, 
                                               long timeInMillis, int completionPercentage, 
                                               float targetDistance) {
        RunResultFragment fragment = new RunResultFragment();
        Bundle args = new Bundle();
        args.putInt("steps", steps);
        args.putFloat("distance", distanceKm);
        args.putInt("calories", calories);
        args.putLong("time", timeInMillis);
        args.putInt("completion", completionPercentage);
        args.putFloat("target_distance", targetDistance);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            steps = getArguments().getInt("steps", 0);
            distanceKm = getArguments().getFloat("distance", 0f);
            calories = getArguments().getInt("calories", 0);
            timeInMillis = getArguments().getLong("time", 0L);
            completionPercentage = getArguments().getInt("completion", 0);
            targetDistance = getArguments().getFloat("target_distance", 0f);
            
            // Kiểm tra xem đã hoàn thành mục tiêu hay chưa
            // Sửa lại logic: chỉ coi là hoàn thành nếu mục tiêu thực sự > 0 và khác với khoảng cách
            isTargetCompleted = (completionPercentage >= 100) && 
                               (targetDistance > 0) && 
                               (Math.abs(targetDistance - distanceKm) > 0.001f); // Thêm điều kiện mục tiêu phải khác với khoảng cách
        }
        
        // Khởi tạo DBHelper
        dbHelper = new DBHelper(getContext());
        
        // Loại bỏ việc lưu kết quả vào cơ sở dữ liệu vì đã được lưu trước đó
        // saveResultToDatabase();
        
        System.out.println("RunResultFragment onCreate - ẩn thanh navigation");
        // Ẩn thanh navigation khi hiển thị màn hình kết quả
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }
    
    // Lưu kết quả chạy vào cơ sở dữ liệu
    /*
    private void saveResultToDatabase() {
        try {
            DBHelper dbHelper = new DBHelper(requireContext());
            
            // Tạo đối tượng StepHistory
            StepHistory stepHistory = new StepHistory();
            stepHistory.setDate(DATE_FORMAT.format(new Date()));
            stepHistory.setTime(TIME_FORMAT.format(new Date()));
            stepHistory.setSteps(steps);
            stepHistory.setDistance(distanceKm);
            stepHistory.setCalories(calories);
            stepHistory.setDuration(timeInMillis);
            
            // Lưu vào database
            dbHelper.addStepHistory(stepHistory);
            
            Toast.makeText(requireContext(), "Đã lưu kết quả thành công", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lưu kết quả: " + e.getMessage());
            Toast.makeText(requireContext(), "Lỗi khi lưu kết quả", Toast.LENGTH_SHORT).show();
        }
    }
    */

    @Override
    public void onStart() {
        super.onStart();
        System.out.println("RunResultFragment onStart - ẩn thanh navigation");
        // Đảm bảo ẩn thanh navigation khi fragment bắt đầu
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        System.out.println("RunResultFragment onResume - ẩn thanh navigation");
        // Đảm bảo ẩn thanh navigation khi fragment được nhìn thấy
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate layout
        View view = inflater.inflate(R.layout.fragment_run_result, container, false);
        
        // Ánh xạ UI components
        tvRunTitle = view.findViewById(R.id.tvRunTitle);
        tvStepCount = view.findViewById(R.id.tvStepCount);
        tvTimeElapsed = view.findViewById(R.id.tvTimeElapsed);
        tvPace = view.findViewById(R.id.tvPace);
        tvDistance = view.findViewById(R.id.tvDistance);
        tvCalories = view.findViewById(R.id.tvCalories);
        tvCompletionPercent = view.findViewById(R.id.tvCompletionPercent);
        btnBack = view.findViewById(R.id.btnBack);
        btnShareResult = view.findViewById(R.id.btnShareResult);
        
        // Hiển thị dữ liệu
        updateUI();
        
        // Thiết lập nút chia sẻ kết quả
        setupShareButton();
        
        // Thiết lập nút quay lại
        setupBackButton();
        
        // Kiểm tra hoàn thành mục tiêu để hiển thị hiệu ứng - chỉ hiển thị khi thực sự hoàn thành mục tiêu có ý nghĩa
        Log.d(TAG, "Thông số hiển thị: distanceKm=" + distanceKm + ", targetDistance=" + targetDistance 
            + ", completionPercentage=" + completionPercentage + ", isTargetCompleted=" + isTargetCompleted);
        
        if (isTargetCompleted) {
            showModernCongratulations(view);
        }
        
        return view;
    }
    
    private void setupBackButton() {
        btnBack.setOnClickListener(v -> {
            // Chuyển trực tiếp về màn hình đếm bước chân chính (StepCounterFragment)
            if (getActivity() != null) {
                StepCounterFragment stepCounterFragment = new StepCounterFragment();
                getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, stepCounterFragment)
                    .commit();
            }
        });
    }
    
    // Hiển thị hiệu ứng chúc mừng hiện đại khi hoàn thành mục tiêu
    private void showModernCongratulations(View rootView) {
        try {
            // Hiển thị card chúc mừng
            androidx.cardview.widget.CardView cardCongratulations = rootView.findViewById(R.id.cardCongratulations);
            TextView tvCongratulationsTitle = rootView.findViewById(R.id.tvCongratulationsTitle);
            TextView tvCongratulationsMessage = rootView.findViewById(R.id.tvCongratulationsMessage);
            
            if (cardCongratulations != null) {
                // Cập nhật nội dung
                if (tvCongratulationsMessage != null) {
                    String message = String.format(
                            "Bạn đã hoàn thành %.1f km, đạt %d%% mục tiêu!", 
                            distanceKm, 
                            completionPercentage
                    ).replace(".", ",");
                    tvCongratulationsMessage.setText(message);
                }
                
                // Hiển thị card với animation
                cardCongratulations.setVisibility(View.VISIBLE);
                cardCongratulations.setAlpha(0f);
                cardCongratulations.animate()
                        .alpha(1f)
                        .setDuration(500)
                        .start();
                
                // Thêm animation cho title
                if (tvCongratulationsTitle != null) {
                    tvCongratulationsTitle.setScaleX(0.5f);
                    tvCongratulationsTitle.setScaleY(0.5f);
                    tvCongratulationsTitle.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(500)
                            .start();
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi hiển thị chúc mừng: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // Hiển thị lại thanh navigation khi rời khỏi màn hình kết quả
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).showBottomNavigation();
        }
    }
    
    private void updateUI() {
        // Cập nhật tiêu đề
        String titleText;
        if (targetDistance > 0) {
            titleText = String.format(Locale.getDefault(), 
                    "Chạy bộ %.2f/%.2f km", distanceKm, targetDistance).replace(".", ",");
        } else {
            titleText = String.format(Locale.getDefault(), 
                    "Chạy bộ %.2f km", distanceKm).replace(".", ",");
        }
        tvRunTitle.setText(titleText);
        
        // Cập nhật số bước
        tvStepCount.setText(String.format(Locale.getDefault(), "%,d", steps).replace(",", "."));
        
        // Cập nhật thời gian
        String formattedTime = formatTimeElapsed(timeInMillis);
        tvTimeElapsed.setText(formattedTime);
        
        // Tính và cập nhật pace (phút/km)
        String pace = calculatePace(distanceKm, timeInMillis);
        tvPace.setText(pace);
        
        // Cập nhật quãng đường
        tvDistance.setText(String.format(Locale.getDefault(), "%.2f", distanceKm).replace(".", ","));
        
        // Cập nhật calories
        tvCalories.setText(String.valueOf(calories));
        
        // Cập nhật phần trăm hoàn thành
        String percentText = String.format(Locale.getDefault(), "%d%%", completionPercentage);
        tvCompletionPercent.setText(percentText);
        
        // Đổi màu phần trăm hoàn thành dựa trên mức độ hoàn thành
        if (completionPercentage >= 100) {
            tvCompletionPercent.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_light));
        } else if (completionPercentage >= 75) {
            tvCompletionPercent.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        } else if (completionPercentage >= 50) {
            tvCompletionPercent.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light));
        } else if (completionPercentage >= 25) {
            tvCompletionPercent.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light));
        } else {
            tvCompletionPercent.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
        }
    }
    
    // Định dạng thời gian từ milliseconds sang phút:giây - sao chép từ StepCounterFragment
    private String formatTimeElapsed(long milliseconds) {
        long minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        long seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(milliseconds) - 
                java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes);
        return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, seconds);
    }
    
    // Tính tốc độ (km/phút)
    private String calculatePace(float distanceKm, long timeInMillis) {
        float speedKmPerMinute = 0;
        // Tránh chia cho 0 và đảm bảo luôn có giá trị hiển thị
        if (distanceKm > 0) {
            // Chuyển milliseconds thành phút và tính km/phút
            float timeInMinutes = Math.max(0.1f, timeInMillis / 60000f); // Đảm bảo ít nhất 0.1 phút
            speedKmPerMinute = distanceKm / timeInMinutes;
        } else if (steps > 0) {
            // Nếu có bước đi nhưng chưa đủ để tính khoảng cách, hiển thị giá trị nhỏ
            speedKmPerMinute = 0.01f;
        }
        
        // Hiển thị tốc độ km/phút với 2 chữ số thập phân
        return String.format("%.2f", speedKmPerMinute).replace(".", ",");
    }
    
    // Chia sẻ kết quả chạy
    private void setupShareButton() {
        if (btnShareResult != null) {
            btnShareResult.setOnClickListener(v -> shareResult());
        }
    }
    
    private void shareResult() {
        String resultMessage = String.format(
                "Tôi đã hoàn thành %d%% mục tiêu chạy bộ %s km!\n" +
                "- Số bước: %,d\n" +
                "- Quãng đường: %.2f km\n" +
                "- Thời gian: %s\n" +
                "- Calories: %d kcal",
                completionPercentage,
                String.format("%.2f", targetDistance).replace(".", ","),
                steps,
                distanceKm,
                formatTimeElapsed(timeInMillis),
                calories
        ).replace(",", ".");
        
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, resultMessage);
        startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ kết quả chạy bộ"));
    }
} 