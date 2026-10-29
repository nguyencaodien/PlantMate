package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

public final class Message {
    private final String messageId;
    private final String senderId;
    private final MessageType type;
    private final String text;
    private final Timestamp createdAt;
    /** IMAGE / VOICE: {@code pmchat://<id>} in chatMedia; "" for text. */
    private final String mediaRef;
    /** VOICE: length in milliseconds; 0 otherwise. */
    private final long durationMs;
    /** LISTING: the plant card's listing ID; "" otherwise. */
    private final String listingId;

    public Message(String messageId, String senderId, MessageType type,
                   String text, Timestamp createdAt) {
        this(messageId, senderId, type, text, createdAt, "", 0);
    }

    public Message(String messageId, String senderId, MessageType type, String text,
                   Timestamp createdAt, String mediaRef, long durationMs) {
        this(messageId, senderId, type, text, createdAt, mediaRef, durationMs, "");
    }

    public Message(String messageId, String senderId, MessageType type, String text,
                   Timestamp createdAt, String mediaRef, long durationMs, String listingId) {
        this.listingId = listingId == null ? "" : listingId;
        this.messageId = messageId;
        this.senderId = senderId;
        this.type = type;
        this.text = text;
        this.createdAt = createdAt;
        this.mediaRef = mediaRef == null ? "" : mediaRef;
        this.durationMs = durationMs;
    }

    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public MessageType getType() { return type; }
    public String getText() { return text; }
    public Timestamp getCreatedAt() { return createdAt; }
    public String getMediaRef() { return mediaRef; }
    public long getDurationMs() { return durationMs; }
    public String getListingId() { return listingId; }
}
