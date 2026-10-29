package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseNotificationDataSource;
import com.example.ungdungchiasecaycanh.model.AppNotification;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * In-app notifications. Sending is best effort: a failed notification never fails the comment or like
 * that caused it. Nobody is notified about their own actions.
 */
public class NotificationRepository {
    static final int TEXT_MAX = 140;
    private final FirebaseNotificationDataSource data = new FirebaseNotificationDataSource();
    private final FirebaseAuthDataSource auth = new FirebaseAuthDataSource();

    /**
     * After a comment is posted: a mention notifies the mentioned member, a reply the thread's author,
     * and any comment the post's author, each member once (mention first, then reply, then comment).
     */
    public void notifyComment(String postId, String commentId, String text, String postAuthor,
                              String threadAuthor, String mentioned) {
        String me = auth.getCurrentUserId();
        if (me == null || postId == null || commentId == null) return;
        for (Target t : commentTargets(me, postAuthor, threadAuthor, mentioned)) {
            data.create(t.uid, t.type.name() + "_" + commentId, t.type, me, postId, excerpt(text));
        }
    }

    public void notifyLike(String postId, String postAuthor) {
        String me = auth.getCurrentUserId();
        if (me == null || postId == null || postAuthor == null || postAuthor.equals(me)) return;
        // One per member and post: liking again after an unlike is refused by the rules (already there).
        data.create(postAuthor, "LIKE_" + postId + "_" + me, AppNotification.Type.LIKE, me, postId, "");
    }

    public Task<List<AppNotification>> recent() {
        String me = auth.getCurrentUserId();
        return me == null ? Tasks.forResult(Collections.emptyList()) : data.recent(me);
    }

    public Task<Integer> unreadCount() {
        String me = auth.getCurrentUserId();
        return me == null ? Tasks.forResult(0) : data.unreadCount(me);
    }

    public Task<Void> markRead(List<String> ids) {
        String me = auth.getCurrentUserId();
        return me == null || ids.isEmpty() ? Tasks.forResult(null) : data.markRead(me, ids);
    }

    static final class Target {
        final String uid;
        final AppNotification.Type type;

        Target(String uid, AppNotification.Type type) { this.uid = uid; this.type = type; }
    }

    static List<Target> commentTargets(String me, String postAuthor, String threadAuthor, String mentioned) {
        Set<String> seen = new LinkedHashSet<>();
        seen.add(me);
        java.util.ArrayList<Target> out = new java.util.ArrayList<>();
        if (valid(mentioned) && seen.add(mentioned)) out.add(new Target(mentioned, AppNotification.Type.MENTION));
        if (valid(threadAuthor) && seen.add(threadAuthor)) out.add(new Target(threadAuthor, AppNotification.Type.REPLY));
        if (valid(postAuthor) && seen.add(postAuthor)) out.add(new Target(postAuthor, AppNotification.Type.COMMENT));
        return out;
    }

    static String excerpt(String text) {
        String t = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        return t.length() <= TEXT_MAX ? t : t.substring(0, TEXT_MAX - 1) + "…";
    }

    private static boolean valid(String uid) {
        return uid != null && !uid.isEmpty();
    }
}
