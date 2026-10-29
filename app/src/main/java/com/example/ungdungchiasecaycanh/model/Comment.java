package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

public final class Comment {
    private final String commentId;
    private final String authorId;
    private final String replyToCommentId;
    private final String text;
    private final Timestamp createdAt;

    public Comment(String commentId, String authorId, String replyToCommentId,
                   String text, Timestamp createdAt) {
        this.commentId=commentId; this.authorId=authorId;
        this.replyToCommentId=replyToCommentId; this.text=text; this.createdAt=createdAt;
    }
    public String getCommentId(){return commentId;}
    public String getAuthorId(){return authorId;}
    public String getReplyToCommentId(){return replyToCommentId==null?"":replyToCommentId;}
    public String getText(){return text;}
    public Timestamp getCreatedAt(){return createdAt;}
}
