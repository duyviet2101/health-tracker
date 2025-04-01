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

import com.bumptech.glide.Glide;
import com.example.healthtracker.R;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CardView cardView;
    private FirebaseUser currentUser;
    private ImageView imgAvatar;
    private TextView txtName;

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

        cardView = findViewById(R.id.avatarCard);
        imgAvatar = findViewById(R.id.imgAvatar);
        txtName = findViewById(R.id.txtName);

        cardView.setOnClickListener(v -> {
            // Show a logout option when the avatar is clicked
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("Account Options")
                    .setItems(new CharSequence[]{"Sign Out"}, (dialog, which) -> {
                        if (which == 0) {
                            signOut();
                        }
                    })
                    .show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Check auth
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, FirebaseUIActivity.class));
        } else {
            Log.d("MainActivity", "User is signed in: " + currentUser.getEmail());
            // Use Glide to load the image instead of setImageURI
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .into(imgAvatar);
            }
            txtName.setText(currentUser.getDisplayName());
        }
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