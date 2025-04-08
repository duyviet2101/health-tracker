package com.example.stepsapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.stepsapp.MainActivity;
import com.example.stepsapp.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class FirebaseUIActivity extends AppCompatActivity {
    private static final String TAG = "FirebaseUIActivity";
    
    private CardView googleLoginButton;
    private ConstraintLayout loadingOverlay;
    
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    
    // Launcher cho đăng nhập Google
    private final ActivityResultLauncher<Intent> signInLauncher = 
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Intent data = result.getData();
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    // Đăng nhập Google thành công, xác thực với Firebase
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    // Đăng nhập Google thất bại
                    Log.w(TAG, "Google sign in failed", e);
                    hideLoading();
                    Toast.makeText(this, "Đăng nhập không thành công, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }
            } else {
                hideLoading();
                Log.d(TAG, "Sign in cancelled by user");
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firebase_ui);
        
        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        
        // Khởi tạo Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Ánh xạ các view
        googleLoginButton = findViewById(R.id.googleLoginButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        
        // Thiết lập sự kiện click cho nút đăng nhập Google
        googleLoginButton.setOnClickListener(v -> signIn());
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // Kiểm tra nếu người dùng đã đăng nhập (không null) và cập nhật UI tương ứng
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Người dùng đã đăng nhập, chuyển đến MainActivity
            goToMainActivity();
        }
    }
    
    private void signIn() {
        showLoading();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }
    
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Đăng nhập thành công
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            Toast.makeText(FirebaseUIActivity.this, 
                                    "Xin chào " + user.getDisplayName(), 
                                    Toast.LENGTH_SHORT).show();
                            goToMainActivity();
                        } else {
                            // Đăng nhập thất bại
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            hideLoading();
                            Toast.makeText(FirebaseUIActivity.this, 
                                    "Xác thực không thành công.", 
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
    
    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
    }
    
    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
    }
} 