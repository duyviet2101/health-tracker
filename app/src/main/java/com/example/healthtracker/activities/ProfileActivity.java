package com.example.healthtracker.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.healthtracker.R;
import com.example.healthtracker.models.User;
import com.example.healthtracker.services.UserService;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextInputEditText nameEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText dobEditText;
    private TextInputEditText weightEditText;
    private TextInputEditText heightEditText;
    private TextView nameHeaderTextView;
    private TextView emailHeaderTextView;
    private Button saveButton;
    private CircleImageView profileImageView;

    private UserService userService;
    private FirebaseAuth firebaseAuth;
    private String userId;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize services
        userService = new UserService();
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            Log.e(TAG, "No authenticated user found");
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userId = user.getUid();

        // Initialize UI elements
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        dobEditText = findViewById(R.id.dobEditText);
        weightEditText = findViewById(R.id.weightEditText);
        heightEditText = findViewById(R.id.heightEditText);
        nameHeaderTextView = findViewById(R.id.nameHeaderTextView);
        emailHeaderTextView = findViewById(R.id.emailHeaderTextView);
        saveButton = findViewById(R.id.saveButton);
        profileImageView = findViewById(R.id.profileImageView);

        // Setup back button
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Setup save button
        saveButton.setOnClickListener(v -> saveUserProfile());

        // Load user data
        loadUserProfile();
    }

    private void loadUserProfile() {
        userService.getUser(userId)
                .addOnSuccessListener(user -> {
                    if (user != null) {
                        currentUser = user;

                        // Set header text
                        nameHeaderTextView.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
                        emailHeaderTextView.setText(user.getEmail() != null ? user.getEmail() : "");

                        // Fill form fields
                        nameEditText.setText(user.getDisplayName() != null ? user.getDisplayName() : "");
                        emailEditText.setText(user.getEmail() != null ? user.getEmail() : "");
                        dobEditText.setText(user.getDob() != null ? user.getDob() : "");
                        weightEditText.setText(user.getWeight() != null ? user.getWeight() : "");
                        heightEditText.setText(user.getHeight() != null ? user.getHeight() : "");

                        // Load profile image
                        loadProfileImage(user.getPhotoUrl());
                    } else {
                        Log.w(TAG, "User document does not exist");
                        Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user profile", e);
                    Toast.makeText(ProfileActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProfileImage(String photoUrl) {
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(profileImageView);
        } else {
            // Load default image if photo URL is null or empty
            Glide.with(this)
                    .load(R.drawable.ic_launcher_foreground)
                    .apply(RequestOptions.circleCropTransform())
                    .into(profileImageView);
        }
    }

    private void saveUserProfile() {
        if (currentUser == null) {
            currentUser = new User();
            currentUser.setId(userId);
        }

        // Update user object with form data
        currentUser.setDisplayName(nameEditText.getText().toString().trim());
        currentUser.setEmail(emailEditText.getText().toString().trim());
        currentUser.setDob(dobEditText.getText().toString().trim());

        String weight = weightEditText.getText().toString().trim();
        String height = heightEditText.getText().toString().trim();

        currentUser.setWeight(weight.isEmpty() ? null : weight);
        currentUser.setHeight(height.isEmpty() ? null : height);

        // Update user in Firestore
        userService.updateUserInfo(currentUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Update header with new values
                    nameHeaderTextView.setText(currentUser.getDisplayName());
                    emailHeaderTextView.setText(currentUser.getEmail());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating user profile", e);
                    Toast.makeText(ProfileActivity.this, "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}