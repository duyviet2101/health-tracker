package com.example.healthtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver để nhận cập nhật số bước chân từ StepCounterService
 */
public class StepCountReceiver extends BroadcastReceiver {
    private static final String TAG = "StepCountReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (StepCounterService.ACTION_STEPS_UPDATED.equals(intent.getAction())) {
            int steps = intent.getIntExtra(StepCounterService.EXTRA_STEPS, 0);
            double distance = intent.getDoubleExtra(StepCounterService.EXTRA_DISTANCE, 0.0);
            double calories = intent.getDoubleExtra(StepCounterService.EXTRA_CALORIES, 0.0);
            long activeTime = intent.getLongExtra(StepCounterService.EXTRA_TIME, 0);

            Log.d(TAG, "Nhận được cập nhật số bước chân: " + steps);
            Log.d(TAG, "Khoảng cách: " + distance + " m");
            Log.d(TAG, "Calo: " + calories);
            Log.d(TAG, "Thời gian hoạt động: " + activeTime + " phút");

            // Gửi thông tin đến LocalBroadcastManager để các Activity/Fragment có thể nhận được
            Intent localIntent = new Intent("STEP_COUNT_UPDATED");
            localIntent.putExtra(StepCounterService.EXTRA_STEPS, steps);
            localIntent.putExtra(StepCounterService.EXTRA_DISTANCE, distance);
            localIntent.putExtra(StepCounterService.EXTRA_CALORIES, calories);
            localIntent.putExtra(StepCounterService.EXTRA_TIME, activeTime);
            
            // Sử dụng Context để sendBroadcast
            context.sendBroadcast(localIntent);
        }
    }
} 