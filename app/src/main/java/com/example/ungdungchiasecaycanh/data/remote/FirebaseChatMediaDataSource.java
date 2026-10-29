package com.example.ungdungchiasecaycanh.data.remote;

import android.util.Base64;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Private chat media in Firestore (no Cloud Storage on this project): chatMedia/{id} holds a
 * compressed photo or an AAC voice note as base64. The rules let only the two conversation
 * participants read it.
 */
public class FirebaseChatMediaDataSource {
    public static final String SCHEME = "pmchat://";
    static final String COLLECTION = "chatMedia";
    public static final int MAX_BASE64_CHARS = 690_000;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static String idOf(String ref) {
        if (ref == null || !ref.startsWith(SCHEME)) return null;
        String id = ref.substring(SCHEME.length());
        return id.matches("[A-Za-z0-9]{10,40}") ? id : null;
    }

    public static boolean isChatMedia(String ref) { return idOf(ref) != null; }

    /** Saves media for a conversation and resolves to its {@code pmchat://} ref. */
    public Task<String> save(String ownerId, String conversationId, String kind, String mime,
                             byte[] bytes, long durationMs) {
        String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
        if (base64.length() > MAX_BASE64_CHARS) {
            return Tasks.forException(new IllegalArgumentException("Media too large"));
        }
        DocumentReference doc = db.collection(COLLECTION).document();
        Map<String, Object> data = new HashMap<>();
        data.put("ownerId", ownerId);
        data.put("conversationId", conversationId);
        data.put("kind", kind);
        data.put("data", base64);
        data.put("mime", mime);
        data.put("durationMs", durationMs);
        data.put("createdAt", FieldValue.serverTimestamp());
        return doc.set(data).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return SCHEME + doc.getId();
        });
    }

    /** Raw bytes of a chat media document (blocking; call off the main thread). */
    public static byte[] loadBlocking(String ref) throws Exception {
        String id = idOf(ref);
        if (id == null) return null;
        DocumentSnapshot snapshot = Tasks.await(FirebaseFirestore.getInstance()
                .collection(COLLECTION).document(id).get(), 20, TimeUnit.SECONDS);
        String base64 = snapshot.getString("data");
        return base64 == null ? null : Base64.decode(base64, Base64.DEFAULT);
    }
}
