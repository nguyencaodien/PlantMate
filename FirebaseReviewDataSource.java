package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.Review;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseReviewDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public Task<Boolean> reviewExists(String reviewId) {
        return firestore.collection("reviews").document(reviewId).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    return task.getResult() != null && task.getResult().exists();
                });
    }

    public Task<Void> createReview(String reviewId, String listingId, String reviewerId,
                                   String revieweeId, int rating, String comment) {
        DocumentReference document = firestore.collection("reviews").document(reviewId);
        Map<String, Object> fields = createFields(
                listingId, reviewerId, revieweeId, rating, comment);
        return firestore.runTransaction(transaction -> {
            DocumentSnapshot existing = transaction.get(document);
            if (existing.exists()) throw new IllegalStateException("Review already exists");
            transaction.set(document, fields);
            return null;
        });
    }

    public Task<List<Review>> getReviewsForUser(String revieweeId) {
        return firestore.collection("reviews").whereEqualTo("revieweeId", revieweeId).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    List<Review> values = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        values.add(mapReview(document));
                    }
                    Collections.sort(values, Comparator.comparing(Review::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())));
                    return values;
                });
    }

    static Map<String, Object> createFields(String listingId, String reviewerId,
                                             String revieweeId, int rating, String comment) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("listingId", listingId);
        fields.put("reviewerId", reviewerId);
        fields.put("revieweeId", revieweeId);
        fields.put("rating", rating);
        fields.put("comment", comment);
        fields.put("createdAt", FieldValue.serverTimestamp());
        return fields;
    }

    private static Review mapReview(DocumentSnapshot document) {
        Long rating = document.getLong("rating");
        Timestamp createdAt = document.getTimestamp("createdAt");
        if (rating == null || rating < 1 || rating > 5 || createdAt == null) {
            throw new IllegalStateException("Invalid Review document");
        }
        return new Review(document.getId(), required(document, "listingId"),
                required(document, "reviewerId"), required(document, "revieweeId"),
                rating.intValue(), required(document, "comment"), createdAt);
    }

    private static String required(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        if (value == null) throw new IllegalStateException("Missing " + field);
        return value;
    }

    private static Exception failure(Exception cause) {
        return cause == null ? new IllegalStateException("Review request failed") : cause;
    }
}
