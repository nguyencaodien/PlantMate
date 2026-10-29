package com.example.ungdungchiasecaycanh.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Friendships. A request is friendRequests/{fromUid}_{toUid} (fromId, toId, createdAt); accepting it
 * writes users/{a}/friends/{b} and users/{b}/friends/{a} (createdAt) and removes the request in one
 * batch. Either friend can remove both friend documents.
 */
public class FirebaseFriendDataSource {
    static final String REQUESTS = "friendRequests";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static String requestId(String fromUid, String toUid) {
        return fromUid + "_" + toUid;
    }

    private DocumentReference request(String fromUid, String toUid) {
        return db.collection(REQUESTS).document(requestId(fromUid, toUid));
    }

    private DocumentReference friend(String uid, String friendUid) {
        return db.collection("users").document(uid).collection("friends").document(friendUid);
    }

    public Task<Void> sendRequest(String fromUid, String toUid) {
        Map<String, Object> data = new HashMap<>();
        data.put("fromId", fromUid);
        data.put("toId", toUid);
        data.put("createdAt", FieldValue.serverTimestamp());
        return request(fromUid, toUid).set(data);
    }

    /** Cancel (sender) or decline (receiver). */
    public Task<Void> deleteRequest(String fromUid, String toUid) {
        return request(fromUid, toUid).delete();
    }

    /** {@code me} accepts the request {@code requesterUid} sent. */
    public Task<Void> accept(String me, String requesterUid) {
        Map<String, Object> since = new HashMap<>();
        since.put("createdAt", FieldValue.serverTimestamp());
        WriteBatch batch = db.batch();
        batch.set(friend(me, requesterUid), since);
        batch.set(friend(requesterUid, me), since);
        batch.delete(request(requesterUid, me));
        return batch.commit();
    }

    public Task<Void> unfriend(String me, String otherUid) {
        WriteBatch batch = db.batch();
        batch.delete(friend(me, otherUid));
        batch.delete(friend(otherUid, me));
        return batch.commit();
    }

    public Task<List<String>> getFriendIds(String uid) {
        return db.collection("users").document(uid).collection("friends").get()
                .continueWith(task -> ids(task, null));
    }

    /** Members who asked {@code uid} to be friends. */
    public Task<List<String>> getIncomingIds(String uid) {
        return db.collection(REQUESTS).whereEqualTo("toId", uid).get()
                .continueWith(task -> ids(task, "fromId"));
    }

    /** Members {@code uid} asked to be friends. */
    public Task<List<String>> getOutgoingIds(String uid) {
        return db.collection(REQUESTS).whereEqualTo("fromId", uid).get()
                .continueWith(task -> ids(task, "toId"));
    }

    /** [are friends, me asked them, they asked me]. */
    public Task<boolean[]> relation(String me, String other) {
        Task<DocumentSnapshot> isFriend = friend(me, other).get();
        Task<DocumentSnapshot> outgoing = request(me, other).get();
        Task<DocumentSnapshot> incoming = request(other, me).get();
        return Tasks.whenAll(isFriend, outgoing, incoming).continueWith(done -> {
            if (!done.isSuccessful()) throw failure(done.getException());
            return new boolean[]{isFriend.getResult().exists(), outgoing.getResult().exists(),
                    incoming.getResult().exists()};
        });
    }

    private static List<String> ids(Task<QuerySnapshot> task, String field) throws Exception {
        if (!task.isSuccessful()) throw failure(task.getException());
        List<String> ids = new ArrayList<>();
        QuerySnapshot result = task.getResult();
        if (result != null) for (DocumentSnapshot doc : result.getDocuments()) {
            String id = field == null ? doc.getId() : doc.getString(field);
            if (id != null && !id.isEmpty()) ids.add(id);
        }
        return Collections.unmodifiableList(ids);
    }

    private static Exception failure(Exception error) {
        return error == null ? new IllegalStateException("Friend request failed") : error;
    }
}
