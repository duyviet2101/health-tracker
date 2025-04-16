package com.example.healthtracker.services;

import com.example.healthtracker.models.User;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserService {
    FirebaseFirestore db;

    public UserService() {
        db = FirebaseFirestore.getInstance();
    }

    public Task<User> getUser(String id) {
        return db.collection("users").document(id).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        return document.toObject(User.class);
                    } else {
                        return null;
                    }
                });
    }

    public Task<Void> setUser(User user) {
        return db.collection("users").document(user.getId()).set(user);
    }

    public Task<Void> updateUser(String id, User user) {
        return db.collection("users").document(id).update(
                "displayName", user.getDisplayName(),
                "email", user.getEmail(),
                "photoUrl", user.getPhotoUrl(),
                "weight", user.getWeight(),
                "height", user.getHeight()
        );
    }

    // Method to update only non-null user information
    public Task<Void> updateUserInfo(User user) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();

        if (user.getDisplayName() != null) {
            updates.put("displayName", user.getDisplayName());
        }

        if (user.getEmail() != null) {
            updates.put("email", user.getEmail());
        }

        if (user.getDob() != null) {
            updates.put("dob", user.getDob());
        }

        if (user.getPhotoUrl() != null) {
            updates.put("photoUrl", user.getPhotoUrl());
        }

        if (user.getWeight() != null) {
            updates.put("weight", user.getWeight());
        }

        if (user.getHeight() != null) {
            updates.put("height", user.getHeight());
        }

        if (updates.isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forResult(null);
        }

        return db.collection("users").document(user.getId()).update(updates);
    }
}
