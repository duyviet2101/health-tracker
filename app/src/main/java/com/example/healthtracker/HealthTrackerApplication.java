package com.example.healthtracker;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import com.example.healthtracker.utils.LanguageUtils;

public class HealthTrackerApplication extends Application {
    
    private static final String TAG = "HealthTrackerApp";
    
    @Override
    protected void attachBaseContext(Context base) {
        // Apply saved language before attachBaseContext
        Context context = LanguageUtils.applyLanguage(base);
        Log.d(TAG, "attachBaseContext - Current language: " + context.getResources().getConfiguration().locale.getLanguage());
        super.attachBaseContext(context);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate - Current language: " + getResources().getConfiguration().locale.getLanguage());
        // Initialize application-wide components here
    }
    
    @Override
    public Context getApplicationContext() {
        // This ensures consistent language throughout the app
        Context context = LanguageUtils.applyLanguage(super.getApplicationContext());
        Log.d(TAG, "getApplicationContext - Current language: " + context.getResources().getConfiguration().locale.getLanguage());
        return context;
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged - Current language: " + newConfig.locale.getLanguage());
        LanguageUtils.applyLanguage(this);
    }
}