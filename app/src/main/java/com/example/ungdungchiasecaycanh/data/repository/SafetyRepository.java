package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseSafetyDataSource;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class SafetyRepository {
    private final FirebaseAuthDataSource auth = new FirebaseAuthDataSource();
    private final FirebaseSafetyDataSource data = new FirebaseSafetyDataSource();
    private final UserRepository users = new UserRepository();
    private final ListingRepository listings = new ListingRepository();
    private final CommunityRepository community = new CommunityRepository();
    private final FriendRepository friends = new FriendRepository();

    public String getCurrentUserId() { return auth.getCurrentUserId(); }

    public Task<Void> submitReport(ReportTargetType type, String targetId, String parentId,
                                   ReportReason reason, String details) {
        String uid = auth.getCurrentUserId();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        final Report report;
        try { report = normalizeReport(uid, type, targetId, parentId, reason, details); }
        catch (IllegalArgumentException error) { return Tasks.forException(error); }
        Task<String> author;
        switch (type) {
            case USER:
                if (isSelfTarget(uid, report.getTargetId())) return selfReport();
                author = users.getProfile(report.getTargetId())
                        .continueWith(task -> { requireSuccess(task); return report.getTargetId(); });
                break;
            case LISTING:
                author = listings.getListing(report.getTargetId())
                        .continueWith(task -> { requireSuccess(task); return task.getResult().getOwnerId(); });
                break;
            case POST:
                author = community.getPost(report.getTargetId())
                        .continueWith(task -> { requireSuccess(task); return task.getResult().getAuthorId(); });
                break;
            case COMMENT:
                author = community.getPost(report.getParentId()).continueWithTask(parent -> {
                    requireSuccess(parent);
                    return community.getComment(report.getParentId(), report.getTargetId());
                }).continueWith(task -> { requireSuccess(task); return task.getResult().getAuthorId(); });
                break;
            default: return Tasks.forException(new IllegalArgumentException("Unsupported target"));
        }
        return author.continueWithTask(task -> {
            requireSuccess(task);
            if (isSelfTarget(uid, task.getResult())) return selfReport();
            if (!uid.equals(auth.getCurrentUserId()))
                return Tasks.forException(new IllegalStateException("Authentication changed"));
            return data.createReport(report);
        });
    }

    public Task<Void> blockUser(String blockedUid) {
        String uid = auth.getCurrentUserId();
        final String target;
        try { target = requireId(blockedUid); }
        catch (IllegalArgumentException error) { return Tasks.forException(error); }
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        if (isSelfTarget(uid, target)) return Tasks.forException(new IllegalArgumentException("Cannot block self"));
        return users.getProfile(target).continueWithTask(read -> {
            requireSuccess(read);
            if (!uid.equals(auth.getCurrentUserId()))
                return Tasks.forException(new IllegalStateException("Authentication changed"));
            return data.isBlocked(uid, target).continueWithTask(existing -> {
                requireSuccess(existing);
                return Boolean.TRUE.equals(existing.getResult())
                        ? Tasks.forResult(null) : data.blockUser(uid, target);
            });
        }).onSuccessTask(blocked -> friends.removeAll(target)); // a block also ends the friendship
    }

    public Task<Void> unblockUser(String blockedUid) {
        String uid = auth.getCurrentUserId();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        try { return data.unblockUser(uid, requireId(blockedUid)); }
        catch (IllegalArgumentException error) { return Tasks.forException(error); }
    }

    public Task<Set<String>> getBlockedUids() {
        String uid = auth.getCurrentUserId();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        return data.getBlockedUids(uid).continueWith(task -> {
            requireSuccess(task);
            if (!uid.equals(auth.getCurrentUserId())) throw new IllegalStateException("Authentication changed");
            return task.getResult();
        });
    }

    public Task<Boolean> isBlockedByCurrentUser(String targetUid) {
        String uid = auth.getCurrentUserId();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        try { return data.isBlocked(uid, requireId(targetUid)).continueWith(task -> {
            requireSuccess(task);
            if (!uid.equals(auth.getCurrentUserId())) throw new IllegalStateException("Authentication changed");
            return task.getResult();
        }); }
        catch (IllegalArgumentException error) { return Tasks.forException(error); }
    }

    public static boolean isSelfTarget(String currentUid, String targetAuthorUid) {
        return currentUid != null && currentUid.equals(targetAuthorUid);
    }

    public static Report normalizeReport(String uid, ReportTargetType type, String targetId,
                                         String parentId, ReportReason reason, String details) {
        String reporter = requireId(uid);
        String target = requireId(targetId);
        if (type == null || reason == null) throw new IllegalArgumentException("Type and reason required");
        String parent = parentId == null ? "" : parentId.trim();
        if (type == ReportTargetType.COMMENT) requireId(parent);
        else if (!parent.isEmpty()) throw new IllegalArgumentException("Parent ID must be empty");
        String normalized = details == null ? "" : details.trim();
        if (normalized.length() > 500 || (reason == ReportReason.OTHER && normalized.isEmpty()))
            throw new IllegalArgumentException("Invalid report details");
        return new Report(null, reporter, type, target, parent, reason, normalized, null);
    }

    public static <T> List<T> filterByBlockedAuthors(List<T> source, Set<String> blocked,
                                                      Function<T, String> author) {
        List<T> result = new ArrayList<>();
        if (source == null) return result;
        Set<String> hidden = blocked == null ? Collections.emptySet() : blocked;
        for (T item : source) if (item != null && !hidden.contains(author.apply(item))) result.add(item);
        return result;
    }

    public static List<Comment> filterCommentTree(List<Comment> source, Set<String> blocked) {
        List<Comment> visible = filterByBlockedAuthors(source, blocked, Comment::getAuthorId);
        Set<String> visibleParents = new HashSet<>();
        for (Comment comment : visible) if (comment.getReplyToCommentId().isEmpty())
            visibleParents.add(comment.getCommentId());
        List<Comment> result = new ArrayList<>();
        for (Comment comment : visible) if (comment.getReplyToCommentId().isEmpty()
                || visibleParents.contains(comment.getReplyToCommentId())) result.add(comment);
        return result;
    }

    private static String requireId(String value) {
        String id = value == null ? "" : value.trim();
        if (id.isEmpty() || id.contains("/")) throw new IllegalArgumentException("Invalid ID");
        return id;
    }
    private static <T> void requireSuccess(Task<T> task) throws Exception {
        if (!task.isSuccessful()) throw task.getException() == null
                ? new IllegalStateException("Safety request failed") : task.getException();
    }
    private static <T> Task<T> selfReport() {
        return Tasks.forException(new IllegalArgumentException("Cannot report your own content"));
    }
}
