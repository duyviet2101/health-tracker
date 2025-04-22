package com.example.healthtracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class to handle water tracking data operations
 */
public class WaterTracker {
    private static final String TAG = "WaterTracker";
    private static final String PREFS_NAME = "WaterTrackerPrefs";
    private static final String KEY_WATER_AMOUNT = "waterAmount";
    private static final String KEY_LAST_DATE = "lastDate";

    private final Context context;
    private final FirebaseFirestore db;
    private final String userId;
    private final String currentDate;

    /**
     * Constructor for WaterTracker
     * @param context Application context
     */
    public WaterTracker(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
        
        // Get user ID
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        userId = (currentUser != null) ? currentUser.getUid() : "anonymous";
        
        // Set current date in format yyyy-MM-dd
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        currentDate = sdf.format(new Date());
    }

    /**
     * Save water amount to both Firestore and SharedPreferences
     * @param amount The water amount to save
     * @param goal The daily water goal
     */
    public void saveWaterAmount(int amount, int goal) {
        // Save to SharedPreferences
        saveToLocalStorage(amount);
        
        // Save to Firestore if user is logged in
        if (!"anonymous".equals(userId)) {
            saveToFirestore(amount, goal);
        }
    }

    /**
     * Load water amount from Firestore or SharedPreferences
     * @param callback Callback to handle the loaded amount
     */
    public void loadWaterAmount(WaterAmountCallback callback) {
        // If user is logged in, try to load from Firestore first
        if (!"anonymous".equals(userId)) {
            db.collection("users").document(userId)
                    .collection("waterTracking").document(currentDate)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("amount")) {
                            Long amount = documentSnapshot.getLong("amount");
                            if (amount != null) {
                                callback.onWaterAmountLoaded(amount.intValue());
                            } else {
                                // If Firestore has no data, try local storage
                                int localAmount = loadFromLocalStorage();
                                callback.onWaterAmountLoaded(localAmount);
                            }
                        } else {
                            // If Firestore has no data, try local storage
                            int localAmount = loadFromLocalStorage();
                            callback.onWaterAmountLoaded(localAmount);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading from Firestore", e);
                        // If Firestore fails, use local storage
                        int localAmount = loadFromLocalStorage();
                        callback.onWaterAmountLoaded(localAmount);
                    });
        } else {
            // If user is not logged in, just use local storage
            int localAmount = loadFromLocalStorage();
            callback.onWaterAmountLoaded(localAmount);
        }
    }

    /**
     * Save water data to Firestore
     */
    private void saveToFirestore(int amount, int goal) {
        DocumentReference waterRef = db.collection("users").document(userId)
                .collection("waterTracking").document(currentDate);
        
        Map<String, Object> waterData = new HashMap<>();
        waterData.put("amount", amount);
        waterData.put("goal", goal);
        waterData.put("date", currentDate);
        waterData.put("timestamp", new Date());
        
        waterRef.set(waterData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Water data saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving to Firestore", e));
    }

    /**
     * Save water amount to SharedPreferences
     */
    private void saveToLocalStorage(int amount) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(KEY_WATER_AMOUNT, amount);
        editor.putString(KEY_LAST_DATE, currentDate);
        editor.apply();
    }

    /**
     * Load water amount from SharedPreferences
     * @return The stored water amount, or 0 if not found or different date
     */
    private int loadFromLocalStorage() {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        String savedDate = settings.getString(KEY_LAST_DATE, "");
        
        // Only return saved amount if it's the same day
        if (currentDate.equals(savedDate)) {
            return settings.getInt(KEY_WATER_AMOUNT, 0);
        }
        
        // If it's a different day, return 0
        return 0;
    }

    /**
     * Callback interface for async water amount loading
     */
    public interface WaterAmountCallback {
        void onWaterAmountLoaded(int amount);
    }
} 