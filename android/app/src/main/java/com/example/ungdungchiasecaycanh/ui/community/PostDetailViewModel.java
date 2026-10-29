package com.example.ungdungchiasecaycanh.ui.community;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.CommunityRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PostDetailViewModel extends ViewModel {
    public enum Status {LOADING, CONTENT, NOT_FOUND, ERROR, HIDDEN}

    public static final class State {
        public final Status status;
        public final Post post;
        public final String authorName;
        public final boolean liked;
        public final int likeCount;
        public final boolean likeBusy;
        public final Listing listing;
        public final boolean listingUnavailable;
        public final List<CommunityCommentItem> comments;
        public final int totalCommentCount;
        public final boolean submitting;
        public final String replyParentId;
        public final String replyAuthorName;
        public final int actionErrorVersion;
        public final int commentSuccessVersion;

        State(Status status, Post post, String authorName, boolean liked, int likeCount, boolean likeBusy, Listing listing, boolean listingUnavailable, List<CommunityCommentItem> comments, int totalCommentCount, boolean submitting, String replyParentId, String replyAuthorName, int errors, int success) {
            this.status = status;
            this.post = post;
            this.authorName = authorName;
            this.liked = liked;
            this.likeCount = likeCount;
            this.likeBusy = likeBusy;
            this.listing = listing;
            this.listingUnavailable = listingUnavailable;
            this.comments = comments;
            this.totalCommentCount = totalCommentCount;
            this.submitting = submitting;
            this.replyParentId = replyParentId;
            this.replyAuthorName = replyAuthorName;
            this.actionErrorVersion = errors;
            this.commentSuccessVersion = success;
        }
    }

    private final CommunityRepository repository = new CommunityRepository();
    private final UserRepository users = new UserRepository();
    private final ListingRepository listings = new ListingRepository();
    private final SafetyRepository safety = new SafetyRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private Set<String> blockedUids = Collections.emptySet();
    private int generation;
    private String hiddenAuthorUid;
    private String postId;
    private Post post;
    private String author = "";
    private String authorAvatar = "";

    /** Avatar ref of the post author ("" = initials). */
    public String getAuthorAvatar() { return authorAvatar; }
    private boolean liked, likeBusy, listingUnavailable, submitting;
    private int likeCount, errors, success, totalCommentCount;
    private Listing listing;
    private List<CommunityCommentItem> comments = Collections.emptyList();
    private String replyParent = "", replyAuthor = "";
    /** Threads whose replies are shown; others show "View N replies" (kept across rotation). */
    private final Set<String> expanded = new HashSet<>();
    private final com.example.ungdungchiasecaycanh.data.repository.NotificationRepository notifications =
            new com.example.ungdungchiasecaycanh.data.repository.NotificationRepository();
    private List<Comment> visibleComments = new ArrayList<>();
    private Map<String, String> commentNames = new HashMap<>();

    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);

    /** True once the post was deleted; the screen then closes. */
    public LiveData<Boolean> getDeleted() { return deleted; }

    public void deletePost() {
        State s = state.getValue();
        if (s == null || s.post == null) return;
        repository.deletePost(s.post.getPostId())
                .addOnSuccessListener(ignored -> deleted.postValue(true))
                .addOnFailureListener(error -> deleteFailed.postValue(deleteFailed.getValue() == null ? 1 : deleteFailed.getValue() + 1));
    }

    private final MutableLiveData<Integer> deleteFailed = new MutableLiveData<>(0);

    public LiveData<Integer> getDeleteFailed() { return deleteFailed; }

    public LiveData<State> getState() {
        return state;
    }

    public void load(String id) {
        postId = id;
        int request = ++generation;
        post = null;
        hiddenAuthorUid = null;
        author = "";
        listing = null;
        comments = Collections.emptyList();
        totalCommentCount = 0;
        liked = false;
        likeCount = 0;
        replyParent = "";
        replyAuthor = "";
        if (id == null || id.trim().isEmpty()) {
            publish(Status.NOT_FOUND);
            return;
        }
        publish(Status.LOADING);
        repository.getPost(id.trim()).addOnSuccessListener(p -> {
            if (request != generation) return;
            post = p;
            safety.getBlockedUids().addOnSuccessListener(blocked -> {
                if (request != generation) return;
                blockedUids = blocked;
                if (blocked.contains(p.getAuthorId())) {
                    hiddenAuthorUid = p.getAuthorId();
                    post = null;
                    publish(Status.HIDDEN);
                } else loadRelated();
            }).addOnFailureListener(error -> {
                if (request == generation) publish(Status.ERROR);
            });
        }).addOnFailureListener(e -> {
            if (request == generation) publish(isMissing(e) ? Status.NOT_FOUND : Status.ERROR);
        });
    }

    public void retry() {
        load(postId);
    }

    public void hideBlockedPost() {
        if (post != null) hiddenAuthorUid = post.getAuthorId();
        generation++;
        post = null;
        comments = Collections.emptyList();
        publish(Status.HIDDEN);
    }

    public String getHiddenAuthorUid() {
        return hiddenAuthorUid;
    }

    public void refreshComments() {
        if (post == null) return;
        int request = generation;
        safety.getBlockedUids().addOnSuccessListener(blocked -> {
            if (request != generation) return;
            blockedUids = blocked;
            repository.getComments(postId).addOnSuccessListener(values -> {
                if (request == generation) enrichComments(values);
            }).addOnFailureListener(e -> {
                if (request == generation) publish(Status.ERROR);
            });
        }).addOnFailureListener(e -> {
            if (request == generation) publish(Status.ERROR);
        });
    }

    public String getCurrentUserId() {
        return safety.getCurrentUserId();
    }

    private void loadRelated() {
        int request = generation;
        Task<User> authorTask = users.getProfile(post.getAuthorId());
        Task<Boolean> likedTask = repository.isPostLiked(post.getPostId());
        Task<Integer> countTask = repository.getLikeCount(post.getPostId());
        Task<List<Comment>> commentsTask = repository.getComments(post.getPostId());
        Task<Listing> listingTask = post.getListingId().isEmpty() ? Tasks.forResult(null) : listings.getListing(post.getListingId());
        List<Task<?>> tasks = new ArrayList<>();
        Collections.addAll(tasks, authorTask, likedTask, countTask, commentsTask, listingTask);
        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            if (request != generation) return;
            if (!likedTask.isSuccessful() || !countTask.isSuccessful() || !commentsTask.isSuccessful()) {
                publish(Status.ERROR);
                return;
            }
            if (authorTask.isSuccessful() && authorTask.getResult() != null) {
                author = authorTask.getResult().getDisplayName();
                authorAvatar = authorTask.getResult().getAvatarUrl() == null ? "" : authorTask.getResult().getAvatarUrl();
            }
            liked = Boolean.TRUE.equals(likedTask.getResult());
            likeCount = countTask.getResult() == null ? 0 : countTask.getResult();
            listing = listingTask.isSuccessful() ? listingTask.getResult() : null;
            listingUnavailable = !post.getListingId().isEmpty() && listing == null;
            enrichComments(commentsTask.getResult());
        });
    }

    private void enrichComments(List<Comment> source) {
        int request = generation;
        totalCommentCount = source == null ? 0 : source.size();
        source = SafetyRepository.filterCommentTree(source, blockedUids);
        if (source.isEmpty()) {
            comments = Collections.emptyList();
            publish(Status.CONTENT);
            return;
        }
        final List<Comment> visible = source;
        Map<String, Task<User>> tasks = new HashMap<>();
        for (Comment c : visible)
            if (!tasks.containsKey(c.getAuthorId()))
                tasks.put(c.getAuthorId(), users.getProfile(c.getAuthorId()));
        Tasks.whenAllComplete(new ArrayList<Task<?>>(tasks.values())).addOnCompleteListener(done -> {
            if (request != generation) return;
            Map<String, String> names = new HashMap<>();
            for (Map.Entry<String, Task<User>> e : tasks.entrySet())
                if (e.getValue().isSuccessful() && e.getValue().getResult() != null)
                    names.put(e.getKey(), e.getValue().getResult().getDisplayName());
            visibleComments = visible;
            commentNames = names;
            comments = group(visible, names, expanded);
            publish(Status.CONTENT);
        });
    }

    public void toggleLike() {
        if (post == null || likeBusy) return;
        boolean old = liked;
        int oldCount = likeCount;
        likeBusy = true;
        publish(Status.CONTENT);
        Task<Void> task = old ? repository.unlikePost(postId) : repository.likePost(postId);
        task.addOnSuccessListener(v -> {
            if (!old) notifications.notifyLike(postId, post.getAuthorId());
            liked = !old;
            likeCount = Math.max(0, oldCount + (old ? -1 : 1));
            likeBusy = false;
            publish(Status.CONTENT);
        }).addOnFailureListener(e -> {
            liked = old;
            likeCount = oldCount;
            likeBusy = false;
            errors++;
            publish(Status.CONTENT);
        });
    }

    public void beginReply(String parentId, String name) {
        replyParent = parentId == null ? "" : parentId;
        replyAuthor = name == null ? "" : name;
        publish(Status.CONTENT);
    }

    private String authorOf(String commentId) {
        if (commentId == null || commentId.isEmpty()) return null;
        for (Comment c : visibleComments) if (commentId.equals(c.getCommentId())) return c.getAuthorId();
        return null;
    }

    /** Shows the replies of a collapsed thread. */
    public void expandThread(String threadId) {
        if (threadId == null || !expanded.add(threadId)) return;
        comments = group(visibleComments, commentNames, expanded);
        publish(Status.CONTENT);
    }

    public void cancelReply() {
        replyParent = "";
        replyAuthor = "";
        publish(Status.CONTENT);
    }

    public void submitComment(String text) {
        if (submitting) return;
        String validation = validateComment(text);
        if (validation != null) {
            errors++;
            publish(Status.CONTENT);
            return;
        }
        submitting = true;
        publish(Status.CONTENT);
        Task<String> task = replyParent.isEmpty() ? repository.createComment(postId, text) : repository.createReply(postId, replyParent, text);
        String thread = replyParent;
        String threadAuthor = authorOf(thread);
        String mentioned = mentionedUid(text, commentNames);
        String postAuthor = post == null ? null : post.getAuthorId();
        task.addOnSuccessListener(id -> {
            submitting = false;
            notifications.notifyComment(postId, id, text, postAuthor, threadAuthor, mentioned);
            if (!thread.isEmpty()) expanded.add(thread); // the member sees their own reply
            replyParent = "";
            replyAuthor = "";
            success++;
            repository.getComments(postId).addOnSuccessListener(this::enrichComments).addOnFailureListener(e -> {
                errors++;
                publish(Status.CONTENT);
            });
        }).addOnFailureListener(e -> {
            submitting = false;
            errors++;
            publish(Status.CONTENT);
        });
    }

    private void publish(Status status) {
        state.postValue(new State(status, post, author, liked, likeCount, likeBusy, listing, listingUnavailable, comments, totalCommentCount, submitting, replyParent, replyAuthor, errors, success));
    }

    static String validateComment(String text) {
        String v = text == null ? "" : text.trim();
        if (v.isEmpty()) return "required";
        if (v.length() > 1000) return "too_long";
        return null;
    }

    /** Every thread expanded. */
    static List<CommunityCommentItem> group(List<Comment> source, Map<String, String> names) {
        return group(source, names, null);
    }

    /**
     * Top-level comments oldest first, each followed by its replies when its thread is in
     * {@code expanded} (null = all), else by one "View N replies" row.
     */
    static List<CommunityCommentItem> group(List<Comment> source, Map<String, String> names, Set<String> expanded) {
        List<Comment> top = new ArrayList<>();
        Map<String, List<Comment>> replies = new HashMap<>();
        Set<String> ids = new HashSet<>();
        for (Comment c : source)
            if (c.getReplyToCommentId().isEmpty()) {
                top.add(c);
                ids.add(c.getCommentId());
            }
        for (Comment c : source)
            if (!c.getReplyToCommentId().isEmpty() && ids.contains(c.getReplyToCommentId()))
                replies.computeIfAbsent(c.getReplyToCommentId(), k -> new ArrayList<>()).add(c);
        Comparator<Comment> order = (a, b) -> {
            if (a.getCreatedAt() == null) return -1;
            if (b.getCreatedAt() == null) return 1;
            return a.getCreatedAt().compareTo(b.getCreatedAt());
        };
        Collections.sort(top, order);
        List<CommunityCommentItem> out = new ArrayList<>();
        for (Comment parent : top) {
            out.add(new CommunityCommentItem(parent, names.getOrDefault(parent.getAuthorId(), ""), false));
            List<Comment> children = replies.getOrDefault(parent.getCommentId(), Collections.emptyList());
            if (children.isEmpty()) continue;
            if (expanded != null && !expanded.contains(parent.getCommentId())) {
                out.add(CommunityCommentItem.more(parent.getCommentId(), children.size()));
                continue;
            }
            Collections.sort(children, order);
            for (Comment child : children) {
                String mentioned = mentionedUid(child.getText(), names);
                out.add(new CommunityCommentItem(child, names.getOrDefault(child.getAuthorId(), ""), true,
                        parent.getCommentId(), 0, mentioned.isEmpty() ? 0 : names.get(mentioned).length() + 1, mentioned));
            }
        }
        return out;
    }

    /** The commenter (uid → name) whose longest name follows a leading "@", or "". */
    static String mentionedUid(String text, Map<String, String> names) {
        if (text == null || !text.startsWith("@")) return "";
        String best = "";
        int bestLength = 0;
        for (Map.Entry<String, String> e : names.entrySet()) {
            String name = e.getValue();
            if (name == null || name.isEmpty() || name.length() <= bestLength) continue;
            if (text.startsWith(name, 1)) {
                best = e.getKey();
                bestLength = name.length();
            }
        }
        return best;
    }

    static boolean hasListingContext(Post post) {
        return post != null && post.getListingId() != null && !post.getListingId().isEmpty();
    }

    private static boolean isMissing(Exception e) {
        return e != null && e.getMessage() != null && e.getMessage().toLowerCase(Locale.ROOT).contains("does not exist");
    }
}
