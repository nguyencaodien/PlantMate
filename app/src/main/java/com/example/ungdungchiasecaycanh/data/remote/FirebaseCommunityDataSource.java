package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseCommunityDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public Task<String> createPost(Post post) {
        DocumentReference reference = firestore.collection("posts").document();
        return reference.set(postCreateFields(post)).continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Post creation failed");
            return reference.getId();
        });
    }

    /** Admin moderation list: the newest posts in one read. */
    public static final int FEED_LIMIT = 100;

    public Task<List<Post>> getPosts() {
        return getPostsPage(null, null, FEED_LIMIT).continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Post load failed");
            return task.getResult().posts;
        });
    }

    /** One page of the feed, newest first. {@code cursor} is the previous page's cursor (null for the first). */
    public static final class Page {
        public final List<Post> posts;
        /** Opaque: pass back to load the next page. */
        public final Object cursor;
        public final boolean hasMore;

        Page(List<Post> posts, Object cursor, boolean hasMore) {
            this.posts = posts; this.cursor = cursor; this.hasMore = hasMore;
        }
    }

    /**
     * Newest posts, optionally of one category (uses the (category, createdAt) index in
     * firestore.indexes.json), starting after {@code cursor}.
     */
    public Task<Page> getPostsPage(PostCategory category, Object cursor, int size) {
        Query query = firestore.collection("posts");
        if (category != null) query = query.whereEqualTo("category", category.name());
        query = query.orderBy("createdAt", Query.Direction.DESCENDING);
        if (cursor instanceof DocumentSnapshot) query = query.startAfter((DocumentSnapshot) cursor);
        return query.limit(size).get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Post load failed");
            List<DocumentSnapshot> documents = task.getResult() == null
                    ? Collections.emptyList() : task.getResult().getDocuments();
            List<Post> posts = new ArrayList<>();
            for (DocumentSnapshot document : documents) posts.add(mapPost(document));
            Object next = documents.isEmpty() ? cursor : documents.get(documents.size() - 1);
            return new Page(posts, next, documents.size() == size);
        });
    }

    /** Every post by one member, newest first (single-field filter: no composite index needed). */
    public Task<List<Post>> getPostsByAuthor(String authorId) {
        return firestore.collection("posts").whereEqualTo("authorId", authorId).get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Post load failed");
            List<Post> posts = new ArrayList<>();
            QuerySnapshot snapshot = task.getResult();
            if (snapshot != null) for (DocumentSnapshot document : snapshot.getDocuments()) posts.add(mapPost(document));
            Collections.sort(posts, (a, b) -> {
                if (a.getCreatedAt() == null) return 1;
                if (b.getCreatedAt() == null) return -1;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            });
            return posts;
        });
    }

    public Task<Post> getPost(String postId) {
        return firestore.collection("posts").document(postId).get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Post load failed");
            DocumentSnapshot document = task.getResult();
            if (document == null || !document.exists())
                throw new IllegalStateException("Post does not exist");
            return mapPost(document);
        });
    }

    public Task<List<Comment>> getComments(String postId) {
        return firestore.collection("posts").document(postId).collection("comments").get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Comments load failed");
            List<Comment> comments = new ArrayList<>();
            QuerySnapshot snapshot = task.getResult();
            if (snapshot != null)
                for (DocumentSnapshot d : snapshot.getDocuments()) comments.add(mapComment(d));
            return comments;
        });
    }

    public Task<Comment> getComment(String postId, String commentId) {
        return firestore.collection("posts").document(postId).collection("comments").document(commentId).get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Comment load failed");
            DocumentSnapshot d = task.getResult();
            if (d == null || !d.exists()) throw new IllegalStateException("Comment does not exist");
            return mapComment(d);
        });
    }

    public Task<String> createComment(String postId, Comment comment) {
        DocumentReference ref = firestore.collection("posts").document(postId).collection("comments").document();
        return ref.set(commentCreateFields(comment)).continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Comment creation failed");
            return ref.getId();
        });
    }

    /** Deletes a post with its comments and likes in one batch (the rules allow it for the author). */
    public Task<Void> deletePost(String postId) {
        DocumentReference post = firestore.collection("posts").document(postId);
        Task<com.google.firebase.firestore.QuerySnapshot> comments = post.collection("comments").get();
        Task<com.google.firebase.firestore.QuerySnapshot> likes = post.collection("likes").get();
        return com.google.android.gms.tasks.Tasks.whenAllSuccess(comments, likes).continueWithTask(task -> {
            if (!task.isSuccessful()) throw failure(task.getException(), "Post deletion failed");
            com.google.firebase.firestore.WriteBatch batch = firestore.batch();
            for (com.google.firebase.firestore.DocumentSnapshot d : comments.getResult().getDocuments()) batch.delete(d.getReference());
            for (com.google.firebase.firestore.DocumentSnapshot d : likes.getResult().getDocuments()) batch.delete(d.getReference());
            batch.delete(post);
            return batch.commit();
        });
    }

    public Task<Void> likePost(String postId, String uid) {
        return firestore.collection("posts").document(postId).collection("likes")
                .document(uid).set(likeCreateFields());
    }

    public Task<Void> unlikePost(String postId, String uid) {
        return firestore.collection("posts").document(postId).collection("likes")
                .document(uid).delete();
    }

    public Task<Boolean> isPostLiked(String postId, String uid) {
        return firestore.collection("posts").document(postId).collection("likes")
                .document(uid).get().continueWith(task -> {
                    // Offline and never read before: show "not liked" rather than failing the feed.
                    if (!task.isSuccessful()) return false;
                    return task.getResult() != null && task.getResult().exists();
                });
    }

    /** Server-side count: one aggregate read instead of downloading every like document. */
    public Task<Integer> getLikeCount(String postId) {
        return OfflineCounts.count(firestore.collection("posts").document(postId).collection("likes"), "likes:" + postId)
                .continueWith(task -> {
                    if (!task.isSuccessful())
                        throw failure(task.getException(), "Like count failed");
                    return task.getResult().intValue();
                });
    }

    /** Number of comments (replies included) on a post, from a server-side count query. */
    public Task<Integer> getCommentCount(String postId) {
        return OfflineCounts.count(firestore.collection("posts").document(postId).collection("comments"), "comments:" + postId)
                .continueWith(task -> {
                    if (!task.isSuccessful())
                        throw failure(task.getException(), "Comment count failed");
                    return task.getResult().intValue();
                });
    }

    static Map<String, Object> postCreateFields(Post post) {
        Map<String, Object> values = new HashMap<>();
        values.put("authorId", post.getAuthorId());
        values.put("text", post.getText());
        values.put("category", post.getCategory().name());
        values.put("listingId", post.getListingId());
        values.put("imageUrl", post.getImageUrl());
        values.put("createdAt", FieldValue.serverTimestamp());
        return values;
    }

    static Map<String, Object> likeCreateFields() {
        Map<String, Object> values = new HashMap<>();
        values.put("createdAt", FieldValue.serverTimestamp());
        return values;
    }

    static Map<String, Object> commentCreateFields(Comment comment) {
        Map<String, Object> values = new HashMap<>();
        values.put("authorId", comment.getAuthorId());
        values.put("replyToCommentId", comment.getReplyToCommentId());
        values.put("text", comment.getText());
        values.put("createdAt", FieldValue.serverTimestamp());
        return values;
    }

    static Post mapPost(DocumentSnapshot document) {
        PostCategory category = PostCategory.fromFirestore(document.getString("category"));
        if (category == null) throw new IllegalStateException("Invalid Post category");
        return new Post(document.getId(), document.getString("authorId"), document.getString("text"), category, string(document, "listingId"), string(document, "imageUrl"), document.getTimestamp("createdAt"));
    }

    static Comment mapComment(DocumentSnapshot d) {
        String author = d.getString("authorId"), text = d.getString("text");
        if (author == null || text == null) throw new IllegalStateException("Invalid Comment");
        return new Comment(d.getId(), author, string(d, "replyToCommentId"), text, d.getTimestamp("createdAt"));
    }

    private static String string(DocumentSnapshot d, String key) {
        String v = d.getString(key);
        return v == null ? "" : v;
    }

    private static Exception failure(Exception error, String message) {
        return error == null ? new IllegalStateException(message) : error;
    }
}
