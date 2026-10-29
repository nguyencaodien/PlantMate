package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.Report;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FirebaseSafetyDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public Task<Void> createReport(Report report) {
        return firestore.collection("reports").document().set(reportFields(report));
    }

    public Task<Void> blockUser(String blockerUid, String blockedUid) {
        return firestore.collection("users").document(blockerUid)
                .collection("blockedUsers").document(blockedUid).set(blockFields());
    }

    public Task<Void> unblockUser(String blockerUid, String blockedUid) {
        return firestore.collection("users").document(blockerUid)
                .collection("blockedUsers").document(blockedUid).delete();
    }

    public Task<Set<String>> getBlockedUids(String uid) {
        return firestore.collection("users").document(uid).collection("blockedUsers")
                .get().continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    Set<String> ids = new HashSet<>();
                    QuerySnapshot result = task.getResult();
                    if (result != null) for (DocumentSnapshot document : result.getDocuments()) {
                        ids.add(document.getId());
                    }
                    return Collections.unmodifiableSet(ids);
                });
    }

    public Task<Boolean> isBlocked(String uid, String blockedUid) {
        return firestore.collection("users").document(uid).collection("blockedUsers")
                .document(blockedUid).get().continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    return task.getResult() != null && task.getResult().exists();
                });
    }

    public static Map<String, Object> reportFields(Report report) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("reporterId", report.getReporterId());
        fields.put("targetType", report.getTargetType().name());
        fields.put("targetId", report.getTargetId());
        fields.put("parentId", report.getParentId());
        fields.put("reason", report.getReason().name());
        fields.put("details", report.getDetails());
        fields.put("createdAt", FieldValue.serverTimestamp());
        return fields;
    }

    public static Map<String, Object> blockFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("createdAt", FieldValue.serverTimestamp());
        return fields;
    }

    private static Exception failure(Exception error) {
        return error == null ? new IllegalStateException("Safety lookup failed") : error;
    }
}
