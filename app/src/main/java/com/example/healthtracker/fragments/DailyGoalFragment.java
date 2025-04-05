package com.example.healthtracker.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import java.text.DecimalFormat;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;
import com.example.healthtracker.utils.SharedPreferencesManager;

public class DailyGoalFragment extends Fragment {

    private TextView tvCurrentStepGoal;
    private SeekBar seekBarStepGoal;
    private Button btnSaveDailyGoal;
    private EditText etCustomStepGoal;
    private TextView tvStepGoalValue;
    private ImageButton btnDecreaseDailyGoal;
    private ImageButton btnIncreaseDailyGoal;
    private ImageButton btnBack;
    private int dailyGoal;
    private SharedPreferencesManager prefsManager;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_daily_goal, container, false);
        
        // Ẩn thanh navigation khi hiển thị màn hình mục tiêu hằng ngày
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }

        // Khởi tạo các thành phần giao diện
        tvCurrentStepGoal = rootView.findViewById(R.id.tvCurrentStepGoal);
        seekBarStepGoal = rootView.findViewById(R.id.seekBarStepGoal);
        btnSaveDailyGoal = rootView.findViewById(R.id.btnSaveDailyGoal);
        etCustomStepGoal = rootView.findViewById(R.id.etCustomStepGoal);
        tvStepGoalValue = rootView.findViewById(R.id.tvStepGoalValue);
        btnDecreaseDailyGoal = rootView.findViewById(R.id.btnDecreaseDailyGoal);
        btnIncreaseDailyGoal = rootView.findViewById(R.id.btnIncreaseDailyGoal);
        btnBack = rootView.findViewById(R.id.btnBack);

        // Khởi tạo SharedPreferencesManager
        prefsManager = new SharedPreferencesManager(requireContext());
        
        // Lấy giá trị mục tiêu hiện tại
        dailyGoal = prefsManager.getDailyGoalMeters();
        
        // Cập nhật UI
        updateUI(dailyGoal);
        
        // Thiết lập giá trị ban đầu cho seekbar
        setupSeekBar(dailyGoal);
        
        // Ngăn chặn sự kiện touch từ bên ngoài fragment
        rootView.setOnTouchListener((v, event) -> {
            // Chỉ xử lý sự kiện touch trong phạm vi của fragment
            return true;
        });
        
        // Xử lý sự kiện khi người dùng nhấn nút Lưu
        btnSaveDailyGoal.setOnClickListener(v -> {
            try {
                int newGoal = Integer.parseInt(etCustomStepGoal.getText().toString());
                if (newGoal > 0 && newGoal <= 50000) {
                    // Lưu mục tiêu mới
                    prefsManager.setDailyGoalMeters(newGoal);
                    
                    // Hiển thị thông báo
                    Toast.makeText(requireContext(), "Đã lưu mục tiêu mới: " + newGoal + " bước", Toast.LENGTH_SHORT).show();
                    
                    // Quay lại màn hình trước
                    getParentFragmentManager().popBackStack();
                } else {
                    Toast.makeText(requireContext(), "Mục tiêu phải từ 1 đến 50,000 bước", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Vui lòng nhập một số hợp lệ", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút giảm mục tiêu
        btnDecreaseDailyGoal.setOnClickListener(v -> {
            if (dailyGoal > 100) {
                dailyGoal -= 100;
                updateUI(dailyGoal);
            }
        });

        // Nút tăng mục tiêu
        btnIncreaseDailyGoal.setOnClickListener(v -> {
            if (dailyGoal < 10000) {
                dailyGoal += 100;
                updateUI(dailyGoal);
            }
        });

        // Nút quay lại
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        return rootView;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Đảm bảo thanh navigation vẫn bị ẩn khi quay lại fragment
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }

    private void updateUI(int currentGoal) {
        tvCurrentStepGoal.setText("Mục tiêu hiện tại: " + currentGoal + " bước");
        tvStepGoalValue.setText(String.valueOf(currentGoal));
        etCustomStepGoal.setText(String.valueOf(currentGoal));
    }
    
    private void setupSeekBar(int currentGoal) {
        // Thiết lập giới hạn tối đa cho seekbar (10,000 bước)
        seekBarStepGoal.setMax(10000);
        
        // Thiết lập giá trị hiện tại cho seekbar
        seekBarStepGoal.setProgress(currentGoal > 10000 ? 10000 : currentGoal);
        
        // Xử lý sự kiện thay đổi giá trị seekbar
        seekBarStepGoal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Làm tròn đến 100 bước
                    int roundedProgress = Math.max(100, Math.round(progress / 100f) * 100);
                    tvStepGoalValue.setText(String.valueOf(roundedProgress));
                    etCustomStepGoal.setText(String.valueOf(roundedProgress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Không cần xử lý
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Không cần xử lý
            }
        });
    }
} 