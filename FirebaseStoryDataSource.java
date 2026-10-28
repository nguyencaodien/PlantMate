package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.Story;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * stories/{id}: authorId, title, caption, imageUrl (pmimg:// photo of the author), createdAt. Rules let
 * only the author and their friends read a story; expiry (24 h) is applied when reading.
 */
public class FirebaseStoryDataSource {
    static final String COLLECTION = "stories";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<String> create(String authorId, String title, String caption, String imageRef) {
        DocumentReference doc = db.collection(COLLECTION).document();
        Map<String, Object> data = new HashMap<>();
        data.put("authorId", authorId);
        data.put("title", title);
        data.put("caption", caption);
        data.put("imageUrl", imageRef);
        data.put("createdAt", FieldValue.serverTimestamp());
        return doc.set(data).continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            return doc.getId();
        });
    }

    /**
     * Every story of one author (one equality filter, so no composite index).
     * ponytail: expired stories are filtered on the device; add a createdAt range + index if authors
     * ever keep hundreds of old stories.
     */
    public Task<List<Story>> getForAuthor(String authorId) {
        return db.collection(COLLECTION).whereEqualTo("authorId", authorId).get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            List<Story> stories = new ArrayList<>();
            QuerySnapshot result = task.getResult();
            if (result != null) for (DocumentSnapshot doc : result.getDocuments()) stories.add(from(doc));
            return stories;
        });
    }

    public Task<Story> get(String storyId) {
        return db.collection(COLLECTION).document(storyId).get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            return task.getResult() != null && task.getResult().exists() ? from(task.getResult()) : null;
        });
    }

    public Task<Void> delete(String storyId) {
        return db.collection(COLLECTION).document(storyId).delete();
    }

    /** stories/{id}/likes/{uid}: one heart per friend. */
    public Task<Void> setLiked(String storyId, String uid, boolean liked) {
        DocumentReference doc = db.collection(COLLECTION).document(storyId).collection("likes").document(uid);
        if (!liked) return doc.delete();
        Map<String, Object> data = new HashMap<>();
        data.put("createdAt", FieldValue.serverTimestamp());
        return doc.set(data);
    }

    public Task<Boolean> isLiked(String storyId, String uid) {
        return db.collection(COLLECTION).document(storyId).collection("likes").document(uid).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    return task.getResult() != null && task.getResult().exists();
                });
    }

    /** Hearts on a story; only its author may read them all. */
    public Task<Integer> getLikeCount(String storyId) {
        return db.collection(COLLECTION).document(storyId).collection("likes").get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            return task.getResult() == null ? 0 : task.getResult().size();
        });
    }

    static Story from(DocumentSnapshot doc) {
        return new Story(doc.getId(), doc.getString("authorId"), doc.getString("title"),
                doc.getString("caption"), doc.getString("imageUrl"), doc.getTimestamp("createdAt"));
    }

    private static Exception failure(Exception error) {
        return error == null ? new IllegalStateException("Story request failed") : error;
    }
}
