package com.example.healthtracker.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import java.text.DecimalFormat;
import android.widget.EditText;

import com.example.healthtracker.R;
import com.example.healthtracker.activities.MainActivity;
import com.example.healthtracker.utils.SharedPreferencesManager;

public class RunTargetFragment extends Fragment {

    private TextView tvCurrentDailyGoal;
    private ImageButton btnIncreaseDistance;
    private ImageButton btnDecreaseDistance;
    private Button btnStartRun;
    private Button btnChangeDailyGoal;
    private double targetDistance;
    private EditText etDistance;
    private SharedPreferencesManager prefsManager;
    private View rootView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_run_target, container, false);

        // Ẩn thanh navigation khi hiển thị màn hình điều chỉnh mục tiêu
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }

        // Khởi tạo các thành phần giao diện
        tvCurrentDailyGoal = rootView.findViewById(R.id.tvCurrentDailyGoal);
        btnIncreaseDistance = rootView.findViewById(R.id.btnIncreaseDistance);
        btnDecreaseDistance = rootView.findViewById(R.id.btnDecreaseDistance);
        btnStartRun = rootView.findViewById(R.id.btnStartRun);
        btnChangeDailyGoal = rootView.findViewById(R.id.btnChangeDailyGoal);

        // Tìm EditText khoảng cách
        etDistance = rootView.findViewById(R.id.etDistance);

        // Khởi tạo SharedPreferencesManager
        prefsManager = new SharedPreferencesManager(requireContext());
        
        // Lấy dữ liệu đã lưu
        targetDistance = prefsManager.getTargetDistance();
        updateDistanceDisplay();

        // Cập nhật hiển thị mục tiêu hằng ngày
        updateDailyGoalDisplay();

        // Ngăn chặn sự kiện touch từ bên ngoài fragment
        rootView.setOnTouchListener((v, event) -> {
            // Chỉ xử lý sự kiện touch trong phạm vi của fragment
            return true;
        });

        // Nút điều chỉnh mục tiêu hằng ngày
        btnChangeDailyGoal.setOnClickListener(v -> {
            // Mở DailyGoalFragment để thiết lập mục tiêu hằng ngày
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, new DailyGoalFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Thiết lập listener cho nút tăng giảm khoảng cách
        btnIncreaseDistance.setOnClickListener(v -> {
            if (targetDistance < 10) {
                targetDistance += 0.25;
                updateDistanceDisplay();
            }
        });

        btnDecreaseDistance.setOnClickListener(v -> {
            if (targetDistance > 0.25) {
                targetDistance -= 0.25;
                updateDistanceDisplay();
            }
        });

        // Lưu và bắt đầu chạy khi nhấn nút Start
        btnStartRun.setOnClickListener(v -> {
            try {
                // Lấy khoảng cách mục tiêu từ EditText
                String input = etDistance.getText().toString().trim();
                if (!input.isEmpty()) {
                    targetDistance = Double.parseDouble(input);
                }
                
                // Lưu khoảng cách mục tiêu
                prefsManager.setTargetDistance((float) targetDistance);
                
                // Tạo Bundle để truyền dữ liệu
                Bundle bundle = new Bundle();
                bundle.putDouble("targetDistance", targetDistance);
                
                // Chuyển sang màn hình đếm bước
                StepCounterFragment stepCounterFragment = new StepCounterFragment();
                stepCounterFragment.setArguments(bundle);
                
                // Thực hiện thay thế fragment
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, stepCounterFragment);
                transaction.addToBackStack(null);
                transaction.commit();
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật giá trị mục tiêu hằng ngày khi quay lại fragment này
        updateDailyGoalDisplay();
        
        // Đảm bảo thanh navigation vẫn bị ẩn khi quay lại fragment
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }

    // Phương thức hiển thị mục tiêu hằng ngày
    private void updateDailyGoalDisplay() {
        // Lấy mục tiêu hằng ngày
        int dailyGoal = prefsManager.getDailyGoalMeters();
        
        // Định dạng số với dấu phân cách hàng nghìn
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedDailyGoal = decimalFormat.format(dailyGoal);
        tvCurrentDailyGoal.setText(formattedDailyGoal + " mét");
    }

    // Phương thức cập nhật hiển thị khoảng cách
    private void updateDistanceDisplay() {
        // Định dạng số thập phân với 2 chữ số thập phân và dấu phẩy
        DecimalFormat df = new DecimalFormat("0.00");
        String formattedDistance = df.format(targetDistance).replace('.', ',');
        etDistance.setText(formattedDistance.replace(',', '.'));
    }
}