package com.example.healthtracker.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.healthtracker.R;
import com.example.healthtracker.models.User;
import com.example.healthtracker.services.UserService;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class FirebaseUIActivity extends BaseActivity {

    private static final String TAG = "FirebaseUIActivity";
    CardView googleLoginButton;
    FirebaseAuth auth;
    UserService userService = new UserService();
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (o.getResultCode() == RESULT_OK) {
                Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(o.getData());
                try {
                    GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                    AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                    auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                String userId = auth.getCurrentUser().getUid();

                                // First check if user already exists in the database
                                userService.getUser(userId).addOnCompleteListener(getUserTask -> {
                                    if (getUserTask.isSuccessful()) {
                                        User existingUser = getUserTask.getResult();

                                        if (existingUser == null) {
                                            // New user - create new document
                                            User newUser = new User(
                                                    userId,
                                                    signInAccount.getDisplayName(),
                                                    signInAccount.getEmail(),
                                                    null,
                                                    signInAccount.getPhotoUrl() != null ? signInAccount.getPhotoUrl().toString() : null,
                                                    null,
                                                    null
                                            );

                                            userService.setUser(newUser).addOnCompleteListener(userTask -> {
                                                if (!userTask.isSuccessful()) {
                                                    Log.e(TAG, "Error saving new user data: " + userTask.getException().getMessage());
                                                }

                                                // New user always goes to RegisterInfo
                                                Intent intent = new Intent(FirebaseUIActivity.this, RegisterInfo.class);
                                                startActivity(intent);
                                                finish();
                                            });
                                        } else {
                                            // Existing user - update only name, email, and photo
                                            User updatedUser = new User(
                                                    userId,
                                                    signInAccount.getDisplayName(),
                                                    signInAccount.getEmail(),
                                                    null,
                                                    signInAccount.getPhotoUrl() != null ? signInAccount.getPhotoUrl().toString() : null,
                                                    existingUser.getWeight(),
                                                    existingUser.getHeight()
                                            );

                                            userService.updateUser(userId, updatedUser).addOnCompleteListener(userTask -> {
                                                if (!userTask.isSuccessful()) {
                                                    Log.e(TAG, "Error updating user data: " + userTask.getException().getMessage());
                                                }

                                                // Check if user has complete profile data
                                                if (existingUser.getWeight() == null || existingUser.getHeight() == null
                                                        || existingUser.getWeight().isEmpty() || existingUser.getHeight().isEmpty()) {
                                                    // Incomplete profile, go to RegisterInfo
                                                    Intent intent = new Intent(FirebaseUIActivity.this, RegisterInfo.class);
                                                    startActivity(intent);
                                                } else {
                                                    // Complete profile, go to MainActivity
                                                    Intent intent = new Intent(FirebaseUIActivity.this, MainActivity.class);
                                                    startActivity(intent);
                                                }
//                                                Intent intent = new Intent(FirebaseUIActivity.this, MainActivity.class);
//                                                startActivity(intent);
                                                finish();
                                            });
                                        }
                                    } else {
                                        // Error getting user data, default to creating new user
                                        Log.e(TAG, "Error checking if user exists: " + getUserTask.getException().getMessage());
                                        User newUser = new User(
                                                userId,
                                                signInAccount.getDisplayName(),
                                                signInAccount.getEmail(),
                                                null,
                                                signInAccount.getPhotoUrl() != null ? signInAccount.getPhotoUrl().toString() : null,
                                                null,
                                                null
                                        );

                                        userService.setUser(newUser).addOnCompleteListener(userTask -> {
                                            if (!userTask.isSuccessful()) {
                                                Log.e(TAG, "Error saving user data: " + userTask.getException().getMessage());
                                            }

                                            Intent intent = new Intent(FirebaseUIActivity.this, RegisterInfo.class);
                                            startActivity(intent);
                                            finish();
                                        });
                                    }
                                });
                            } else {
                                Toast.makeText(FirebaseUIActivity.this, "Đăng nhập thất bại.", Toast.LENGTH_LONG).show();
                                Log.e(TAG, "onActivityResult: " + task.getException().getMessage());
                            }
                        }
                    });
                } catch (ApiException e) {
                    Toast.makeText(FirebaseUIActivity.this, "Đăng nhập thất bại.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "onActivityResult: " + e.getMessage());
                }
            }
        }
    });
    GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_uiactivity);

        FirebaseApp.initializeApp(this);

        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(FirebaseUIActivity.this, options);

        auth = FirebaseAuth.getInstance();

        googleLoginButton = findViewById(R.id.googleLoginButton);
        googleLoginButton.setOnClickListener(v -> {
            Intent intent = googleSignInClient.getSignInIntent();
            activityResultLauncher.launch(intent);
        });
    }
}

