package com.example.healthtracker.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.healthtracker.R;
import com.example.healthtracker.fragments.RunTargetFragment;
import com.example.healthtracker.fragments.StepCounterFragment;
import com.example.healthtracker.fragments.StepHistoryFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private FrameLayout fragmentContainer;
    private BottomNavigationView bottomNavigationView;
    
    // Interface để lắng nghe sự kiện từ StepCounterFragment
    public interface OnStepCounterStateListener {
        void onStepCounterStarted();
        void onStepCounterStopped();
    }
    
    private OnStepCounterStateListener stepCounterStateListener = new OnStepCounterStateListener() {
        @Override
        public void onStepCounterStarted() {
            // Ẩn thanh navigation khi bắt đầu đếm bước chân
            hideBottomNavigation();
        }

        @Override
        public void onStepCounterStopped() {
            // Hiện thanh navigation khi dừng đếm bước chân
            showBottomNavigation();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo các view
        fragmentContainer = findViewById(R.id.fragment_container);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Thiết lập listener cho bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Mặc định hiển thị fragment đầu tiên
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_home);
        }
        
        // Theo dõi việc thay đổi Fragment để xử lý hiển thị bottom navigation
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(
            new FragmentManager.FragmentLifecycleCallbacks() {
                @Override
                public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                    super.onFragmentResumed(fm, f);
                    if (f instanceof StepCounterFragment) {
                        // Khi StepCounterFragment hiển thị, ẩn thanh navigation
                        hideBottomNavigation();
                        
                        // Truyền listener cho StepCounterFragment
                        ((StepCounterFragment) f).setStateListener(stepCounterStateListener);
                    } else {
                        // Các fragment khác, hiện thanh navigation
                        showBottomNavigation();
                    }
                }
            }, 
            false
        );
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;

        int itemId = item.getItemId();
        if (itemId == R.id.navigation_home) {
            selectedFragment = showPlaceholderFragment("Màn hình nhập");
        } else if (itemId == R.id.navigation_calendar) {
            // Sử dụng StepHistoryFragment cho tab "Lịch sử"
            selectedFragment = new StepHistoryFragment();
        } else if (itemId == R.id.navigation_steps) {
            // Sử dụng RunTargetFragment cho tab "Báo cáo"
            selectedFragment = new RunTargetFragment();
        } else if (itemId == R.id.navigation_weight) {
            selectedFragment = showPlaceholderFragment("Màn hình cân nặng");
        } else if (itemId == R.id.navigation_settings) {
            selectedFragment = showPlaceholderFragment("Màn hình cài đặt");
        }

        // Thay thế fragment hiện tại bằng fragment mới
        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            return true;
        }

        return false;
    }
    
    // Ẩn thanh navigation
    public void hideBottomNavigation() {
        if (bottomNavigationView != null) {
            System.out.println("MainActivity: Ẩn thanh navigation");
            bottomNavigationView.setVisibility(View.GONE);
        }
    }
    
    // Hiện thanh navigation
    public void showBottomNavigation() {
        if (bottomNavigationView != null) {
            System.out.println("MainActivity: Hiện thanh navigation");
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
    }

    // Tạo placeholder fragment cho các tab chưa có fragment riêng
    private Fragment showPlaceholderFragment(String message) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle args = new Bundle();
        args.putString("message", message);
        fragment.setArguments(args);
        return fragment;
    }

    // Fragment placeholder tạm thời
    public static class PlaceholderFragment extends Fragment {
        public PlaceholderFragment() {
            // Required empty public constructor
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Sửa lại layout và thêm TextView với ID cụ thể
            View view = inflater.inflate(R.layout.fragment_placeholder, container, false);
            TextView textView = view.findViewById(R.id.tvPlaceholder);
            
            // Lấy thông điệp từ Arguments
            Bundle args = getArguments();
            if (args != null) {
                String message = args.getString("message", "Placeholder");
                textView.setText(message);
            }
            
            return view;
        }
    }
}