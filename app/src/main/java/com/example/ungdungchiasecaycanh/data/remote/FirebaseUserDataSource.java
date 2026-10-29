package com.example.ungdungchiasecaycanh.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.ungdungchiasecaycanh.model.User;

import java.util.HashMap;
import java.util.Map;

public class FirebaseUserDataSource {

    private final FirebaseFirestore firestore;

    public FirebaseUserDataSource() {
        firestore = FirebaseFirestore.getInstance();
    }

    public Task<Void> createInitialProfile(String uid, String displayName, String locationText) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("displayName", displayName);
        profile.put("locationText", locationText);
        profile.put("avatarUrl", "");
        profile.put("bio", "");
        profile.put("createdAt", FieldValue.serverTimestamp());
        profile.put("updatedAt", FieldValue.serverTimestamp());
        return firestore.collection("users").document(uid).set(profile);
    }

    public Task<User> getProfile(String uid) {
        return firestore.collection("users").document(uid).get().continueWith(task -> {
            if (!task.isSuccessful()) {
                Exception exception = task.getException();
                throw exception == null ? new IllegalStateException("Profile load failed") : exception;
            }
            DocumentSnapshot document = task.getResult();
            if (document == null || !document.exists()) {
                throw new IllegalStateException("Profile does not exist");
            }
            return new User(
                    document.getString("displayName"),
                    document.getString("locationText") == null ? "" : document.getString("locationText"),
                    document.getString("avatarUrl"),
                    document.getString("bio"),
                    document.getTimestamp("createdAt"),
                    document.getTimestamp("updatedAt"),
                    Boolean.TRUE.equals(document.getBoolean("emailVerified")),
                    Boolean.TRUE.equals(document.getBoolean("phoneVerified")),
                    Boolean.TRUE.equals(document.getBoolean("vneidVerified")));
        });
    }

    /** Shows the "email verified" badge; the rules accept it only with a verified sign-in token. */
    public Task<Void> markEmailVerified(String uid) {
        Map<String, Object> update = new HashMap<>();
        update.put("emailVerified", true);
        update.put("updatedAt", FieldValue.serverTimestamp());
        return firestore.collection("users").document(uid).update(update);
    }

    /** Publishes a passed demo check ("phoneVerified" or "vneidVerified"); the rules only let it turn on. */
    public Task<Void> markVerified(String uid, String field) {
        Map<String, Object> update = new HashMap<>();
        update.put(field, true);
        update.put("updatedAt", FieldValue.serverTimestamp());
        return firestore.collection("users").document(uid).update(update);
    }

    public Task<Void> updateProfile(String uid, String displayName,
                                    String locationText, String bio) {
        return firestore.collection("users").document(uid)
                .update(buildProfileUpdate(displayName, locationText, bio));
    }

    /** Profile text and a new avatar ref ("", preset:key or pmimg://id). */
    public Task<Void> updateProfile(String uid, String displayName, String locationText, String bio,
                                    String avatarUrl) {
        Map<String, Object> update = buildProfileUpdate(displayName, locationText, bio);
        update.put("avatarUrl", avatarUrl == null ? "" : avatarUrl);
        return firestore.collection("users").document(uid).update(update);
    }

    public static Map<String, Object> buildProfileUpdate(String displayName,
                                                         String locationText,
                                                         String bio) {
        Map<String, Object> update = new HashMap<>();
        update.put("displayName", displayName);
        update.put("locationText", locationText);
        update.put("bio", bio);
        update.put("updatedAt", FieldValue.serverTimestamp());
        return update;
    }
}
