package com.example.healthtracker.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.example.healthtracker.R;
import com.example.healthtracker.fragments.MenuAccountFragment;
import com.example.healthtracker.HealthTrackerService;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    // Avatar
    private ImageView imgAvatar;
    private TextView txtName;
    private CardView avatarCard;

    // Step tracker values
    private TextView stepCountText, caloriesValue, distanceValue;

    // BroadcastReceiver
    private BroadcastReceiver stepUpdateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase auth
        mAuth = FirebaseAuth.getInstance();

        // Bind views
        stepCountText = findViewById(R.id.stepCountText);
        caloriesValue = findViewById(R.id.caloriesValue);
        distanceValue = findViewById(R.id.distanceValue);
        txtName = findViewById(R.id.txtName);
        imgAvatar = findViewById(R.id.imgAvatar);
        avatarCard = findViewById(R.id.avatarCard);

        // Avatar click = mở menu đăng xuất
        avatarCard.setOnClickListener(v -> {
            MenuAccountFragment menuAccountFragment = new MenuAccountFragment();
            menuAccountFragment.show(getSupportFragmentManager(), "MenuAccountFragment");
        });

        setupBroadcastReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, FirebaseUIActivity.class));
        } else {
            txtName.setText(currentUser.getDisplayName());

            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(imgAvatar);
            }

            // Bắt đầu service theo dõi bước
            startStepCounterService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stepUpdateReceiver, getStepUpdateFilter(), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(stepUpdateReceiver, getStepUpdateFilter());
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(stepUpdateReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver was not registered");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStepCounterService();
    }

    public void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    stopStepCounterService();
                    startActivity(new Intent(MainActivity.this, FirebaseUIActivity.class));
                    finish();
                });
    }

    private void startStepCounterService() {
        Intent serviceIntent = new Intent(this, HealthTrackerService.class);
        startService(serviceIntent);
    }

    private void stopStepCounterService() {
        Intent serviceIntent = new Intent(this, HealthTrackerService.class);
        stopService(serviceIntent);
    }

    private void setupBroadcastReceiver() {
        stepUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String data = intent.getStringExtra("data");

                if (action == null || data == null) return;

                switch (action) {
                    case "STEPS_UPDATED":
                        stepCountText.setText(data);
                        break;
                    case "CALORIES_UPDATED":
                        caloriesValue.setText(data);
                        break;
                    case "DISTANCE_UPDATED":
                        distanceValue.setText(data + " m");
                        break;
                }
            }
        };
    }

    private IntentFilter getStepUpdateFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("STEPS_UPDATED");
        filter.addAction("CALORIES_UPDATED");
        filter.addAction("DISTANCE_UPDATED");
        return filter;
    }
}
