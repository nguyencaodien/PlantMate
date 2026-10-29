package com.example.ungdungchiasecaycanh.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Deletes a member's shared data before their account is deleted (no backend on the Spark plan, so
 * the app does it, as the member). Everything is found first; deletion starts only when every
 * query succeeded, and the profile goes last so a failed run can simply be repeated.
 * <p>
 * Kept on purpose: messages in conversations (the other member's record of a hand-over; the sender
 * shows as a former member), reviews others wrote about them, reports and Doctor feedback (safety
 * and quality records), and post / story hearts, which are keyed by UID and have no field to query.
 */
public class FirebaseAccountDataSource {
    /** Firestore allows 500 writes per batch; stay below. */
    static final int BATCH_SIZE = 400;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<Integer> deleteMemberData(String uid) {
        DocumentReference user = db.collection("users").document(uid);
        return findMemberData(uid).continueWithTask(found -> {
            if (!found.isSuccessful()) throw failure(found.getException());
            List<DocumentReference> refs = found.getResult();
            return commitInBatches(refs).continueWithTask(deleted -> {
                if (!deleted.isSuccessful()) throw failure(deleted.getException());
                int count = deleted.getResult();
                return user.delete().continueWith(profile -> {
                    if (!profile.isSuccessful()) throw failure(profile.getException());
                    return count + 1;
                });
            });
        });
    }

    /**
     * Everything the member owns besides their profile, found with the same queries deletion uses
     * (read-only; also what the instrumented dry run checks against the real project).
     */
    public Task<List<DocumentReference>> findMemberData(String uid) {
        DocumentReference user = db.collection("users").document(uid);
        List<Query> owned = new ArrayList<>();
        owned.add(db.collection("listings").whereEqualTo("ownerId", uid));
        owned.add(db.collection("posts").whereEqualTo("authorId", uid));
        owned.add(db.collectionGroup("comments").whereEqualTo("authorId", uid));
        owned.add(db.collection("stories").whereEqualTo("authorId", uid));
        owned.add(db.collection("images").whereEqualTo("ownerId", uid));
        owned.add(db.collection("imageThumbs").whereEqualTo("ownerId", uid));
        owned.add(db.collection("chatMedia").whereEqualTo("ownerId", uid));
        owned.add(db.collection("friendRequests").whereEqualTo("fromId", uid));
        owned.add(db.collection("friendRequests").whereEqualTo("toId", uid));
        owned.add(db.collection("reviews").whereEqualTo("reviewerId", uid));
        owned.add(user.collection("savedListings"));
        owned.add(user.collection("blockedUsers"));
        owned.add(user.collection("notifications"));
        Task<QuerySnapshot> friends = user.collection("friends").get();

        List<Task<QuerySnapshot>> reads = new ArrayList<>();
        for (Query q : owned) reads.add(q.get());
        reads.add(friends);
        return Tasks.whenAllSuccess(reads).continueWithTask(done -> {
            if (!done.isSuccessful()) throw failure(done.getException());
            Set<DocumentReference> refs = new LinkedHashSet<>();
            for (Task<QuerySnapshot> read : reads) {
                for (DocumentSnapshot d : read.getResult().getDocuments()) refs.add(d.getReference());
            }
            // Each friendship has a mirror on the friend's side.
            for (DocumentSnapshot f : friends.getResult().getDocuments()) {
                refs.add(db.collection("users").document(f.getId()).collection("friends").document(uid));
            }
            return Tasks.forResult((List<DocumentReference>) new ArrayList<>(refs));
        });
    }

    private Task<Integer> commitInBatches(List<DocumentReference> refs) {
        Task<Void> chain = Tasks.forResult(null);
        for (List<DocumentReference> chunk : chunks(refs, BATCH_SIZE)) {
            chain = chain.continueWithTask(previous -> {
                if (!previous.isSuccessful()) throw failure(previous.getException());
                WriteBatch batch = db.batch();
                for (DocumentReference ref : chunk) batch.delete(ref);
                return batch.commit();
            });
        }
        return chain.continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            return refs.size();
        });
    }

    static <T> List<List<T>> chunks(List<T> items, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < items.size(); i += size) out.add(new ArrayList<>(items.subList(i, Math.min(items.size(), i + size))));
        return out;
    }

    private static Exception failure(Exception error) {
        return error == null ? new IllegalStateException("Account data deletion failed") : error;
    }
}
