package com.example.healthtracker.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver để khởi động lại các thông báo nhắc nhở uống nước sau khi thiết bị khởi động lại
 */
public class WaterReminderBootReceiver extends BroadcastReceiver {
    private static final String TAG = "WaterReminderBootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && (
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON") ||
                intent.getAction().equals("android.intent.action.LOCKED_BOOT_COMPLETED"))) {
            
            Log.d(TAG, "Thiết bị khởi động lại, thiết lập lại thông báo uống nước");
            
            // Khởi động lại các thông báo
            new WaterReminderService(context).scheduleReminders();
        }
    }
} 