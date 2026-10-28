package com.example.ungdungchiasecaycanh.data.remote;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * "Was this diagnosis right?" answers, collected to measure and later train Plant Doctor. Only
 * admins can read them. A photo is stored (privately) only when the member chose to share it.
 */
public class FirebaseDoctorFeedbackDataSource {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** {@code feedback} holds the answer fields; {@code photo} is null unless the member agreed. */
    public Task<Void> submit(String ownerId, Map<String, Object> feedback,
                             @Nullable FirebaseImageDataSource.Encoded photo) {
        WriteBatch batch = db.batch();
        String photoId = "";
        if (photo != null) {
            DocumentReference photoDoc = db.collection("doctorFeedbackPhotos").document();
            photoId = photoDoc.getId();
            Map<String, Object> data = new HashMap<>();
            data.put("ownerId", ownerId);
            data.put("data", photo.base64);
            data.put("width", photo.width);
            data.put("height", photo.height);
            data.put("createdAt", FieldValue.serverTimestamp());
            batch.set(photoDoc, data);
        }
        Map<String, Object> answer = new HashMap<>(feedback);
        answer.put("ownerId", ownerId);
        answer.put("photoId", photoId);
        answer.put("createdAt", FieldValue.serverTimestamp());
        batch.set(db.collection("doctorFeedback").document(), answer);
        return batch.commit();
    }
}
