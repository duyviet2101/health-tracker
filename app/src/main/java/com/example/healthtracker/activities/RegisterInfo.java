package com.example.healthtracker.activities;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.healthtracker.R;
import com.example.healthtracker.adapters.RegisterInfoAdapter;
import com.example.healthtracker.fragments.HeightRegisterInfo;
import com.example.healthtracker.fragments.WeightRegisterInfo;

import lombok.Setter;

public class RegisterInfo extends BaseActivity {

    private ViewPager2 viewPager;
    // Method to set user weight from WeightRegisterInfo fragment
    @Setter
    private String userWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_info);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize ViewPager and fragments
        setupViewPager();
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.viewPager);

        // Set up adapter
        RegisterInfoAdapter adapter = new RegisterInfoAdapter(this);
        adapter.addFragment(new WeightRegisterInfo());
        adapter.addFragment(new HeightRegisterInfo());

        viewPager.setAdapter(adapter);

        // Disable swipe gesture
        viewPager.setUserInputEnabled(false);
    }

    // Method to get user weight for HeightRegisterInfo fragment
    public String getUserWeight() {
        return userWeight != null ? userWeight : "";
    }
}