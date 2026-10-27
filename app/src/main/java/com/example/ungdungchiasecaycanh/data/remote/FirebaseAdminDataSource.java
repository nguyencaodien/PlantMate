package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.AdminAction;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.Member;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportReview;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Firestore access for the admin area. Parent mutations and their audit entries are atomic.
 * Large child deletions finish in bounded batches first; rules check every write as the admin.
 */
public class FirebaseAdminDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public Task<Boolean> isAdmin(String uid) {
        if (uid == null) return Tasks.forResult(false);
        return firestore.collection("admins").document(uid).get()
                .continueWith(task -> task.isSuccessful() && task.getResult() != null && task.getResult().exists());
    }

    public Task<Long> count(String collection) {
        return OfflineCounts.count(firestore.collection(collection), "admin:" + collection)
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    return task.getResult();
                });
    }

    // ---------------- Listings ----------------

    public Task<List<Listing>> getAllListings() {
        return firestore.collection("listings").get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            List<Listing> listings = new ArrayList<>();
            for (DocumentSnapshot document : task.getResult().getDocuments()) {
                try {
                    listings.add(FirebaseListingDataSource.map(document));
                } catch (RuntimeException ignored) {
                    // A malformed document is skipped rather than failing the whole list.
                }
            }
            return listings;
        });
    }

    public Task<Void> updateListing(String adminId, String listingId, Map<String, Object> fields, String note) {
        Map<String, Object> update = new HashMap<>(fields);
        update.put("updatedAt", FieldValue.serverTimestamp());
        WriteBatch batch = firestore.batch();
        batch.update(firestore.collection("listings").document(listingId), update);
        log(batch, adminId, AdminAction.EDIT, "LISTING", listingId, note);
        return batch.commit();
    }

    public Task<Void> deleteListing(String adminId, String listingId, String note) {
        WriteBatch batch = firestore.batch();
        batch.delete(firestore.collection("listings").document(listingId));
        log(batch, adminId, AdminAction.DELETE, "LISTING", listingId, note);
        return batch.commit();
    }

    // ---------------- Posts and comments ----------------

    public Task<List<Post>> getAllPosts() {
        return new FirebaseCommunityDataSource().getPosts();
    }

    public Task<Post> getPost(String postId) {
        return new FirebaseCommunityDataSource().getPost(postId);
    }

    public Task<List<Comment>> getComments(String postId) {
        return new FirebaseCommunityDataSource().getComments(postId);
    }

    public Task<Void> updatePost(String adminId, String postId, String text, PostCategory category,
                                 String imageUrl, String note) {
        Map<String, Object> update = new HashMap<>();
        update.put("text", text);
        update.put("category", category.name());
        update.put("imageUrl", imageUrl == null ? "" : imageUrl);
        WriteBatch batch = firestore.batch();
        batch.update(firestore.collection("posts").document(postId), update);
        log(batch, adminId, AdminAction.EDIT, "POST", postId, note);
        return batch.commit();
    }

    /** Deletes the post with its comments and likes (Firestore does not cascade subcollections). */
    public Task<Void> deletePost(String adminId, String postId, String note) {
        CollectionReference post = firestore.collection("posts");
        Task<QuerySnapshot> comments = post.document(postId).collection("comments").get(Source.SERVER);
        Task<QuerySnapshot> likes = post.document(postId).collection("likes").get(Source.SERVER);
        return Tasks.whenAllSuccess(comments, likes).continueWithTask(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            List<DocumentReference> children = new ArrayList<>();
            for (DocumentSnapshot d : comments.getResult().getDocuments()) children.add(d.getReference());
            for (DocumentSnapshot d : likes.getResult().getDocuments()) children.add(d.getReference());
            return deleteChildrenThenParent(children, post.document(postId), adminId, "POST", postId, note);
        });
    }

    /** Deletes a comment and, for a top-level comment, its replies. */
    public Task<Void> deleteComment(String adminId, String postId, String commentId, String note) {
        CollectionReference comments = firestore.collection("posts").document(postId).collection("comments");
        return comments.whereEqualTo("replyToCommentId", commentId).get(Source.SERVER).continueWithTask(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            List<DocumentReference> children = new ArrayList<>();
            for (DocumentSnapshot reply : task.getResult().getDocuments()) children.add(reply.getReference());
            return deleteChildrenThenParent(children, comments.document(commentId), adminId,
                    "COMMENT", postId + "/" + commentId, note);
        });
    }

    /** A failed chunk stops the chain. Retrying queries the remaining children from the server. */
    private Task<Void> deleteChildrenThenParent(List<DocumentReference> children, DocumentReference parent,
                                               String adminId, String targetType, String targetId, String note) {
        Task<Void> chain = Tasks.forResult(null);
        for (List<DocumentReference> chunk : FirebaseAccountDataSource.chunks(
                children, FirebaseAccountDataSource.BATCH_SIZE)) {
            chain = chain.continueWithTask(previous -> {
                if (!previous.isSuccessful()) throw failure(previous.getException());
                WriteBatch batch = firestore.batch();
                for (DocumentReference child : chunk) batch.delete(child);
                return batch.commit();
            });
        }
        return chain.continueWithTask(deleted -> {
            if (!deleted.isSuccessful()) throw failure(deleted.getException());
            // Two writes, committed only after every child batch has succeeded.
            WriteBatch batch = firestore.batch();
            batch.delete(parent);
            log(batch, adminId, AdminAction.DELETE, targetType, targetId, note);
            return batch.commit();
        });
    }

    // ---------------- Members ----------------

    public Task<List<Member>> getMembers() {
        Task<QuerySnapshot> users = firestore.collection("users").get();
        Task<QuerySnapshot> suspended = firestore.collection("suspendedUsers").get();
        Task<QuerySnapshot> admins = firestore.collection("admins").get();
        return Tasks.whenAllSuccess(users, suspended, admins).continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            Set<String> suspendedIds = new HashSet<>();
            for (DocumentSnapshot d : suspended.getResult().getDocuments()) suspendedIds.add(d.getId());
            Set<String> adminIds = new HashSet<>();
            for (DocumentSnapshot d : admins.getResult().getDocuments()) adminIds.add(d.getId());
            List<Member> members = new ArrayList<>();
            for (DocumentSnapshot d : users.getResult().getDocuments()) {
                members.add(new Member(d.getId(), user(d), suspendedIds.contains(d.getId()), adminIds.contains(d.getId())));
            }
            return members;
        });
    }

    public Task<Member> getMember(String uid) {
        Task<DocumentSnapshot> profile = firestore.collection("users").document(uid).get();
        Task<DocumentSnapshot> suspended = firestore.collection("suspendedUsers").document(uid).get();
        Task<DocumentSnapshot> admin = firestore.collection("admins").document(uid).get();
        return Tasks.whenAllSuccess(profile, suspended, admin).continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            DocumentSnapshot d = profile.getResult();
            if (d == null || !d.exists()) throw new IllegalStateException("Member does not exist");
            return new Member(uid, user(d), suspended.getResult() != null && suspended.getResult().exists(),
                    admin.getResult() != null && admin.getResult().exists());
        });
    }

    public Task<Void> updateMember(String adminId, String uid, String displayName, String locationText,
                                   String bio, String note) {
        WriteBatch batch = firestore.batch();
        batch.update(firestore.collection("users").document(uid),
                FirebaseUserDataSource.buildProfileUpdate(displayName, locationText, bio));
        log(batch, adminId, AdminAction.EDIT, "USER", uid, note);
        return batch.commit();
    }

    public Task<Void> setSuspended(String adminId, String uid, boolean suspend, String note) {
        WriteBatch batch = firestore.batch();
        if (suspend) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("note", note);
            doc.put("adminId", adminId);
            doc.put("createdAt", FieldValue.serverTimestamp());
            batch.set(firestore.collection("suspendedUsers").document(uid), doc);
        } else {
            batch.delete(firestore.collection("suspendedUsers").document(uid));
        }
        log(batch, adminId, suspend ? AdminAction.SUSPEND : AdminAction.LIFT_SUSPENSION, "USER", uid, note);
        return batch.commit();
    }

    /** Grants or revokes the admin role (admins/{uid}); the rules refuse it for the admin's own UID. */
    public Task<Void> setAdmin(String adminId, String uid, boolean grant, String note) {
        WriteBatch batch = firestore.batch();
        if (grant) {
            Map<String, Object> doc = new HashMap<>();
            doc.put("adminId", adminId);
            doc.put("createdAt", FieldValue.serverTimestamp());
            batch.set(firestore.collection("admins").document(uid), doc);
        } else {
            batch.delete(firestore.collection("admins").document(uid));
        }
        log(batch, adminId, grant ? AdminAction.GRANT_ADMIN : AdminAction.REVOKE_ADMIN, "USER", uid, note);
        return batch.commit();
    }

    // ---------------- Reports ----------------

    public Task<List<Report>> getReports() {
        return firestore.collection("reports").get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            List<Report> reports = new ArrayList<>();
            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                try {
                    reports.add(new Report(d.getId(), d.getString("reporterId"),
                            ReportTargetType.valueOf(d.getString("targetType")), d.getString("targetId"),
                            string(d, "parentId"), ReportReason.valueOf(d.getString("reason")),
                            string(d, "details"), d.getTimestamp("createdAt")));
                } catch (RuntimeException ignored) {
                    // Skip a malformed report instead of hiding the whole queue.
                }
            }
            return reports;
        });
    }

    public Task<Map<String, ReportReview>> getReportReviews() {
        return firestore.collection("reportReviews").get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            Map<String, ReportReview> reviews = new HashMap<>();
            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                reviews.put(d.getId(), new ReportReview(d.getString("status"), string(d, "note"),
                        d.getString("adminId"), d.getTimestamp("reviewedAt")));
            }
            return reviews;
        });
    }

    public Task<Void> reviewReport(String adminId, String reportId, String status, String note) {
        Map<String, Object> review = new HashMap<>();
        review.put("status", status);
        review.put("note", note);
        review.put("adminId", adminId);
        review.put("reviewedAt", FieldValue.serverTimestamp());
        WriteBatch batch = firestore.batch();
        batch.set(firestore.collection("reportReviews").document(reportId), review);
        log(batch, adminId, ReportReview.DISMISSED.equals(status) ? AdminAction.DISMISS : AdminAction.RESOLVE,
                "REPORT", reportId, note);
        return batch.commit();
    }

    // ---------------- Audit log ----------------

    public Task<List<AdminAction>> getActions() {
        return firestore.collection("adminActions").get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            List<AdminAction> actions = new ArrayList<>();
            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                actions.add(new AdminAction(d.getId(), d.getString("adminId"), d.getString("action"),
                        d.getString("targetType"), d.getString("targetId"), string(d, "note"),
                        d.getTimestamp("createdAt")));
            }
            return actions;
        });
    }

    private void log(WriteBatch batch, String adminId, String action, String targetType, String targetId, String note) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("adminId", adminId);
        entry.put("action", action);
        entry.put("targetType", targetType);
        entry.put("targetId", targetId);
        entry.put("note", note == null ? "" : note);
        entry.put("createdAt", FieldValue.serverTimestamp());
        batch.set(firestore.collection("adminActions").document(), entry);
    }

    private static User user(DocumentSnapshot d) {
        return new User(string(d, "displayName"), string(d, "locationText"), string(d, "avatarUrl"),
                string(d, "bio"), d.getTimestamp("createdAt"), d.getTimestamp("updatedAt"));
    }

    private static String string(DocumentSnapshot d, String key) {
        String value = d.getString(key);
        return value == null ? "" : value;
    }

    private static Exception failure(Exception cause) {
        return cause == null ? new IllegalStateException("Admin request failed") : cause;
    }
}
