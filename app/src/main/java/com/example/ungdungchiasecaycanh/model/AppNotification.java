package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

/**
 * One in-app notification under {@code users/{uid}/notifications}: someone commented on, replied to,
 * mentioned or liked something of the member's. Written by the actor's app (no server on Spark).
 */
public final class AppNotification {
    public enum Type { COMMENT, REPLY, MENTION, LIKE }

    public final String id;
    public final Type type;
    public final String actorId;
    public final String postId;
    /** Short excerpt of the comment, "" for likes. */
    public final String text;
    public final boolean read;
    public final Timestamp createdAt;

    public AppNotification(String id, Type type, String actorId, String postId, String text, boolean read,
                           Timestamp createdAt) {
        this.id = id;
        this.type = type;
        this.actorId = actorId;
        this.postId = postId;
        this.text = text == null ? "" : text;
        this.read = read;
        this.createdAt = createdAt;
    }
}
