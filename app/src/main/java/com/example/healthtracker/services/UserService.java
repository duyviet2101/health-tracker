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
    
    // Method to update only weight and height fields
    public Task<Void> updateUserInfo(String userId, String weight, String height) {
        return db.collection("users").document(userId).update(
                "weight", weight,
                "height", height
        );
    }
}
