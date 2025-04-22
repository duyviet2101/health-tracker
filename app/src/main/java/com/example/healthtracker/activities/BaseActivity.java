package com.example.healthtracker.activities;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.healthtracker.utils.LanguageUtils;

/**
 * Base activity that handles language configuration for all activities
 */
public class BaseActivity extends AppCompatActivity {

    private static final String TAG = "BaseActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply the saved language to this activity
        Context context = LanguageUtils.applyLanguage(newBase);
        Log.d(TAG, this.getClass().getSimpleName() + " - attachBaseContext - Current language: " 
              + context.getResources().getConfiguration().locale.getLanguage());
        super.attachBaseContext(context);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Apply saved language when configuration changes
        Log.d(TAG, this.getClass().getSimpleName() + " - onConfigurationChanged - Current language: " 
              + newConfig.locale.getLanguage());
        LanguageUtils.applyLanguage(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure language is applied
        LanguageUtils.applyLanguage(this);
        Log.d(TAG, this.getClass().getSimpleName() + " - onCreate - Current language: " 
              + getResources().getConfiguration().locale.getLanguage());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Apply language again in onResume to ensure it's consistent
        LanguageUtils.applyLanguage(this);
        Log.d(TAG, this.getClass().getSimpleName() + " - onResume - Current language: " 
              + getResources().getConfiguration().locale.getLanguage());
    }
} 