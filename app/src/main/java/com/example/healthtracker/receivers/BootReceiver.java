package com.example.healthtracker.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.healthtracker.services.StepCounterService;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "Boot completed, checking if step counter was running");
            
            // Kiểm tra xem service có đang chạy khi thiết bị tắt không
            SharedPreferences prefs = context.getSharedPreferences("HealthTrackerPrefs", Context.MODE_PRIVATE);
            boolean wasRunning = prefs.getBoolean("isRunning", false);
            
            if (wasRunning) {
                Log.d(TAG, "Step counter was running, restarting service");
                
                // Khởi động lại service
                Intent serviceIntent = new Intent(context, StepCounterService.class);
                serviceIntent.setAction("START");
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
}