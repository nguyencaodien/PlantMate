package com.example.ungdungchiasecaycanh.ui.community;

import com.example.ungdungchiasecaycanh.model.Comment;

/** A comment row, or (when {@link #comment} is null) a "View N replies" row for a collapsed thread. */
public final class CommunityCommentItem {
    public final Comment comment;
    public final String authorName;
    public final boolean reply;
    /** Top-level comment this row belongs to (itself for a top-level comment). */
    public final String threadId;
    /** Hidden replies behind a "View N replies" row; 0 for a comment row. */
    public final int moreCount;
    /** Length of a leading "@Name" in the text (shown in green), 0 when there is none. */
    public final int mentionLength;
    /** Member the leading "@Name" points to, or "" when there is none. */
    public final String mentionUid;

    public CommunityCommentItem(Comment comment, String authorName, boolean reply) {
        this(comment, authorName, reply, comment.getCommentId(), 0, 0, "");
    }

    CommunityCommentItem(Comment comment, String authorName, boolean reply, String threadId, int moreCount,
                         int mentionLength, String mentionUid) {
        this.comment = comment;
        this.authorName = authorName;
        this.reply = reply;
        this.threadId = threadId;
        this.moreCount = moreCount;
        this.mentionLength = mentionLength;
        this.mentionUid = mentionUid == null ? "" : mentionUid;
    }

    static CommunityCommentItem more(String threadId, int count) {
        return new CommunityCommentItem(null, "", true, threadId, count, 0, "");
    }

    String key() {
        return comment == null ? "more:" + threadId : comment.getCommentId();
    }
}
