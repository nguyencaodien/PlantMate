package com.example.ungdungchiasecaycanh.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deletes a member's shared data before their account is deleted (no backend on the Spark plan, so
 * the app does it, as the member). Everything is found first; deletion starts only when every
 * query succeeded, and the profile goes last so a failed run can simply be repeated.
 * <p>
 * Kept on purpose: messages in conversations (the other member's record of a hand-over; the sender
 * shows as a former member), reviews others wrote about them, reports and Doctor feedback (safety
 * and quality records), and hearts on other members' posts / stories, which are keyed by UID.
 * Comments (including replies) and likes under the member's own posts are removed before the posts.
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
            Map<String, List<DocumentReference>> postChildren = new LinkedHashMap<>();
            List<DocumentReference> otherData = new ArrayList<>();
            List<DocumentReference> posts = new ArrayList<>();
            for (DocumentReference ref : refs) {
                String collection = ref.getParent().getPath();
                if ("posts".equals(collection)) posts.add(ref);
                else if (collection.startsWith("posts/")) {
                    postChildren.computeIfAbsent(collection, ignored -> new ArrayList<>()).add(ref);
                } else otherData.add(ref);
            }
            // Keep post documents available for isPostAuthor rules until every child is gone.
            // Each child collection has one parent: avoid exceeding the rules' document-access
            // budget by mixing children from hundreds of different posts in a single batch.
            Task<Integer> childChain = Tasks.forResult(0);
            for (List<DocumentReference> children : postChildren.values()) {
                childChain = childChain.continueWithTask(previous -> {
                    if (!previous.isSuccessful()) throw failure(previous.getException());
                    return commitInBatches(children);
                });
            }
            return childChain.continueWithTask(childrenDeleted -> {
                if (!childrenDeleted.isSuccessful()) throw failure(childrenDeleted.getException());
                return commitInBatches(otherData);
            }).continueWithTask(otherDeleted -> {
                if (!otherDeleted.isSuccessful()) throw failure(otherDeleted.getException());
                return commitInBatches(posts);
            }).continueWithTask(deleted -> {
                if (!deleted.isSuccessful()) throw failure(deleted.getException());
                return user.delete().continueWith(profile -> {
                    if (!profile.isSuccessful()) throw failure(profile.getException());
                    return refs.size() + 1;
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
        // Destructive cleanup must not treat an incomplete offline cache as the full data set.
        Task<QuerySnapshot> posts = db.collection("posts").whereEqualTo("authorId", uid).get(Source.SERVER);
        Task<QuerySnapshot> friends = user.collection("friends").get(Source.SERVER);

        List<Task<QuerySnapshot>> reads = new ArrayList<>();
        for (Query q : owned) reads.add(q.get(Source.SERVER));
        reads.add(friends);
        reads.add(posts);
        return Tasks.whenAllSuccess(reads).continueWithTask(done -> {
            if (!done.isSuccessful()) throw failure(done.getException());
            List<Task<QuerySnapshot>> children = new ArrayList<>();
            for (DocumentSnapshot post : posts.getResult().getDocuments()) {
                // Replies live in the same comments collection, not a nested subcollection.
                children.add(post.getReference().collection("comments").get(Source.SERVER));
                children.add(post.getReference().collection("likes").get(Source.SERVER));
            }
            return Tasks.whenAllSuccess(children).continueWith(childReads -> {
                if (!childReads.isSuccessful()) throw failure(childReads.getException());
                Set<DocumentReference> refs = new LinkedHashSet<>();
                for (Task<QuerySnapshot> child : children) {
                    for (DocumentSnapshot d : child.getResult().getDocuments()) refs.add(d.getReference());
                }
                for (Task<QuerySnapshot> read : reads) {
                    if (read == posts) continue;
                    for (DocumentSnapshot d : read.getResult().getDocuments()) refs.add(d.getReference());
                }
                // Each friendship has a mirror on the friend's side.
                for (DocumentSnapshot f : friends.getResult().getDocuments()) {
                    refs.add(db.collection("users").document(f.getId()).collection("friends").document(uid));
                }
                // A comment found both by author and by parent is deleted once; parents come last.
                for (DocumentSnapshot post : posts.getResult().getDocuments()) refs.add(post.getReference());
                return (List<DocumentReference>) new ArrayList<>(refs);
            });
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
