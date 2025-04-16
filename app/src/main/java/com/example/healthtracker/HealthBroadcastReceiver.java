package com.example.healthtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HealthBroadcastReceiver extends BroadcastReceiver {
    private HealthTrackerManager.HealthDataListener listener;

    public HealthBroadcastReceiver(HealthTrackerManager.HealthDataListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && intent.getExtras() != null) {
            double data = Double.parseDouble(intent.getExtras().getString("data", "0"));

            switch (action) {
                case "STEPS_UPDATED":
                    listener.onStepCountUpdated((int) data);
                    break;
                case "CALORIES_UPDATED":
                    listener.onCaloriesUpdated(data);
                    break;
                case "DISTANCE_UPDATED":
                    listener.onDistanceUpdated(data);
                    break;
            }
        }
    }
}
