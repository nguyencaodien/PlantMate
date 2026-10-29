package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseCommunityDataSource;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.util.ImageRefs;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class CommunityRepository {
    private final FirebaseAuthDataSource auth = new FirebaseAuthDataSource();
    private final FirebaseCommunityDataSource data = new FirebaseCommunityDataSource();
    private final ListingRepository listings = new ListingRepository();

    public Task<String> createPost(String text, PostCategory category) {
        return createPost(text, category, "");
    }

    public Task<String> createPost(String text, PostCategory category, String listingId) {
        return createPost(text, category, listingId, "");
    }

    /** Creates a post with an optional already-hosted https image URL ("" for none). */
    public Task<String> createPost(String text, PostCategory category, String listingId, String imageUrl) {
        String uid = auth.getCurrentUserId();
        if (uid == null)
            return Tasks.forException(new IllegalStateException("Authentication required"));
        try {
            Post post = normalizeForCreate(uid, text, category, listingId, imageUrl);
            if (post.getListingId().isEmpty()) return data.createPost(post);
            return listings.getListing(post.getListingId()).continueWithTask(task -> {
                if (!task.isSuccessful())
                    return Tasks.forException(task.getException() == null ? new IllegalStateException("Listing unavailable") : task.getException());
                if (task.getResult() == null || task.getResult().getStatus() != ListingStatus.ACTIVE)
                    return Tasks.forException(new IllegalStateException("Only active listings can be shared"));
                return data.createPost(post);
            });
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
    }

    public static final int PAGE_SIZE = 20;

    /** A feed page; {@code category} null means every category, {@code cursor} null the first page. */
    public Task<FirebaseCommunityDataSource.Page> getPostsPage(PostCategory category, Object cursor) {
        return auth.getCurrentUserId() == null
                ? Tasks.forException(new IllegalStateException("Authentication required"))
                : data.getPostsPage(category, cursor, PAGE_SIZE);
    }

    public Task<List<Post>> getPostsByAuthor(String authorId) {
        if (auth.getCurrentUserId() == null)
            return Tasks.forException(new IllegalStateException("Authentication required"));
        if (authorId == null || authorId.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Author required"));
        return data.getPostsByAuthor(authorId.trim());
    }

    public Task<Post> getPost(String id) {
        if (id == null || id.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Post ID required"));
        return data.getPost(id.trim());
    }

    public Task<List<Comment>> getComments(String postId) {
        if (postId == null || postId.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Post ID required"));
        return data.getComments(postId.trim());
    }

    public Task<Comment> getComment(String postId, String commentId) {
        if (auth.getCurrentUserId() == null)
            return Tasks.forException(new IllegalStateException("Authentication required"));
        if (postId == null || postId.trim().isEmpty() || commentId == null || commentId.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Comment context required"));
        return data.getComment(postId.trim(), commentId.trim());
    }

    public Task<String> createComment(String postId, String text) {
        return createCommentInternal(postId, "", text);
    }

    public Task<String> createReply(String postId, String parentId, String text) {
        if (postId == null || postId.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Post ID required"));
        if (parentId == null || parentId.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Parent comment required"));
        return data.getComment(postId, parentId.trim()).continueWithTask(task -> {
            if (!task.isSuccessful())
                return Tasks.forException(task.getException() == null ? new IllegalStateException("Parent comment missing") : task.getException());
            Comment parent = task.getResult();
            if (!canReplyTo(parent))
                return Tasks.forException(new IllegalArgumentException("Replies can only target top-level comments"));
            return createCommentInternal(postId, parentId.trim(), text);
        });
    }

    private Task<String> createCommentInternal(String postId, String parentId, String text) {
        String uid = auth.getCurrentUserId();
        if (uid == null)
            return Tasks.forException(new IllegalStateException("Authentication required"));
        if (postId == null || postId.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Post ID required"));
        try {
            return data.createComment(postId.trim(), normalizeComment(uid, parentId, text));
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
    }

    public Task<Void> deletePost(String id) {
        if (auth.getCurrentUserId() == null)
            return Tasks.forException(new IllegalStateException("Authentication required"));
        if (id == null || id.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Post ID required"));
        return data.deletePost(id.trim());
    }

    public Task<Void> likePost(String id) {
        return withUid(id, true);
    }

    public Task<Void> unlikePost(String id) {
        return withUid(id, false);
    }

    private Task<Void> withUid(String id, boolean like) {
        String uid = auth.getCurrentUserId();
        if (uid == null)
            return Tasks.forException(new IllegalStateException("Authentication required"));
        if (id == null || id.trim().isEmpty())
            return Tasks.forException(new IllegalArgumentException("Post ID required"));
        return like ? data.likePost(id.trim(), uid) : data.unlikePost(id.trim(), uid);
    }

    public Task<Boolean> isPostLiked(String id) {
        String uid = auth.getCurrentUserId();
        return uid == null ? Tasks.forException(new IllegalStateException("Authentication required")) : data.isPostLiked(id, uid);
    }

    public Task<Integer> getLikeCount(String id) {
        return data.getLikeCount(id);
    }

    public Task<Integer> getCommentCount(String id) {
        return data.getCommentCount(id);
    }

    static Post normalizeForCreate(String uid, String text, PostCategory category) {
        return normalizeForCreate(uid, text, category, "");
    }

    static Post normalizeForCreate(String uid, String text, PostCategory category, String listingId) {
        return normalizeForCreate(uid, text, category, listingId, "");
    }

    static Post normalizeForCreate(String uid, String text, PostCategory category, String listingId,
                                   String imageUrl) {
        String normalized = text == null ? "" : text.trim();
        if (uid == null || uid.isEmpty() || category == null)
            throw new IllegalArgumentException("Author and category are required");
        if (normalized.isEmpty() || normalized.length() > 2000)
            throw new IllegalArgumentException("Post text must be 1 to 2000 characters");
        String listing = listingId == null ? "" : listingId.trim();
        if (!listing.isEmpty()) category = PostCategory.SHARING;
        return new Post(null, uid, normalized, category, listing, normalizeImageUrl(imageUrl), null);
    }

    static String normalizeImageUrl(String imageUrl) {
        String value = imageUrl == null ? "" : imageUrl.trim();
        if (value.isEmpty()) return "";
        if (value.length() > 2048) throw new IllegalArgumentException("Image URL is too long");
        if (ImageRefs.isUploaded(value)) return value;
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && !uri.getHost().isEmpty()) {
                return value;
            }
        } catch (URISyntaxException ignored) {
            // Fall through to the rejection below.
        }
        throw new IllegalArgumentException("Post images must be remote http(s) URLs");
    }

    static Comment normalizeComment(String uid, String parentId, String text) {
        String normalized = text == null ? "" : text.trim();
        if (uid == null || uid.isEmpty()) throw new IllegalArgumentException("Author required");
        if (normalized.isEmpty() || normalized.length() > 1000)
            throw new IllegalArgumentException("Comment text must be 1 to 1000 characters");
        return new Comment(null, uid, parentId == null ? "" : parentId.trim(), normalized, null);
    }

    static boolean canReplyTo(Comment parent) {
        return parent != null && parent.getCommentId() != null && !parent.getCommentId().trim().isEmpty() && parent.getReplyToCommentId().isEmpty();
    }
}
