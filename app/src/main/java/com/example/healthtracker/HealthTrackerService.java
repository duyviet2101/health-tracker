package com.example.healthtracker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class HealthTrackerService extends Service {
    private HealthTrackerManager healthTrackerManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        healthTrackerManager = new HealthTrackerManager(this, new HealthTrackerManager.HealthDataListener() {
            @Override
            public void onStepCountUpdated(int steps) {
                sendBroadcastUpdate("STEPS_UPDATED", steps);
            }

            @Override
            public void onCaloriesUpdated(double calories) {
                sendBroadcastUpdate("CALORIES_UPDATED", calories);
            }

            @Override
            public void onDistanceUpdated(double distanceMeters) {
                sendBroadcastUpdate("DISTANCE_UPDATED", distanceMeters);
            }
        });

        healthTrackerManager.startTracking();
        return START_STICKY;
    }

    private void sendBroadcastUpdate(String action, Object value) {
        Intent intent = new Intent(action);
        intent.putExtra("data", value.toString());
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        healthTrackerManager.stopTracking();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
