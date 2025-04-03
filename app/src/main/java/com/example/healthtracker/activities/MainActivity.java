package com.example.healthtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.example.healthtracker.R;
import com.example.healthtracker.fragments.MenuAccountFragment;
import com.firebase.ui.auth.AuthUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CardView avatarCard;
    private FirebaseUser currentUser;
    private ImageView imgAvatar;
    private TextView txtName;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        avatarCard = findViewById(R.id.avatarCard);
        imgAvatar = findViewById(R.id.imgAvatar);
        txtName = findViewById(R.id.txtName);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        avatarCard.setOnClickListener(v -> {
            // Show a logout option when the avatar is clicked
            MenuAccountFragment menuAccountFragment = new MenuAccountFragment();
            menuAccountFragment.show(getSupportFragmentManager(), "MenuAccountFragment");
        });

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                // Đã ở trang chủ, không cần làm gì
                return true;
            } else if (itemId == R.id.navigation_steps) {
                startActivity(new Intent(this, StepCounterActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Tạm thời bỏ qua kiểm tra đăng nhập
        // currentUser = mAuth.getCurrentUser();
        // if (currentUser == null) {
        //     Log.d("MainActivity", "No user signed in, redirecting to login");
        //     startActivity(new Intent(this, FirebaseUIActivity.class));
        // } else {
            Log.d("MainActivity", "Skipping login check for testing");
            // Set default values for testing
            txtName.setText("Test User");
            // Use a default avatar
            Glide.with(this)
                    .load(R.drawable.ic_avatar)
                    .circleCrop()
                    .into(imgAvatar);
        // }
    }

    public void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    // Start the auth flow after sign out
                    startActivity(new Intent(MainActivity.this, FirebaseUIActivity.class));
                    finish();
                });
    }
}