package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.AppNotification;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** {@code users/{uid}/notifications}: the actor creates, only the recipient reads, marks read or deletes. */
public class FirebaseNotificationDataSource {
    public static final int LIMIT = 50;
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private CollectionReference of(String uid) {
        return firestore.collection("users").document(uid).collection("notifications");
    }

    /** {@code docId} is deterministic so the same like or comment never notifies twice. */
    public Task<Void> create(String toUid, String docId, AppNotification.Type type, String actorId,
                             String postId, String text) {
        Map<String, Object> values = new HashMap<>();
        values.put("type", type.name());
        values.put("actorId", actorId);
        values.put("postId", postId);
        values.put("text", text);
        values.put("read", false);
        values.put("createdAt", FieldValue.serverTimestamp());
        return of(toUid).document(docId).set(values);
    }

    public Task<List<AppNotification>> recent(String uid) {
        return of(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(LIMIT).get().continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException() == null ? new IllegalStateException("Notifications failed") : task.getException();
            List<AppNotification> out = new ArrayList<>();
            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                AppNotification n = from(d);
                if (n != null) out.add(n);
            }
            return out;
        });
    }

    public Task<Integer> unreadCount(String uid) {
        return OfflineCounts.count(of(uid).whereEqualTo("read", false), "unreadNotifications")
                .continueWith(task -> task.isSuccessful() ? task.getResult().intValue() : 0);
    }

    public Task<Void> markRead(String uid, List<String> ids) {
        WriteBatch batch = firestore.batch();
        for (String id : ids) batch.update(of(uid).document(id), "read", true);
        return batch.commit();
    }

    private static AppNotification from(DocumentSnapshot d) {
        AppNotification.Type type;
        try {
            type = AppNotification.Type.valueOf(String.valueOf(d.getString("type")));
        } catch (IllegalArgumentException unknown) {
            return null; // a type this version does not know
        }
        return new AppNotification(d.getId(), type, d.getString("actorId"), d.getString("postId"),
                d.getString("text"), Boolean.TRUE.equals(d.getBoolean("read")), d.getTimestamp("createdAt"));
    }
}
