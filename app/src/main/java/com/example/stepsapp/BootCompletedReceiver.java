package com.example.stepsapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * BroadcastReceiver để khởi động service đếm bước chân khi thiết bị khởi động xong
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null &&
                (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                        intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
                        intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON"))) {
            
            Log.d(TAG, "Thiết bị đã khởi động xong, đang khởi động dịch vụ đếm bước chân");
            
            // Khởi động StepCounterService
            Intent serviceIntent = new Intent(context, StepCounterService.class);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
} 