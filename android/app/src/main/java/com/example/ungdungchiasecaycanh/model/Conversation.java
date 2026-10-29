package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Conversation {
    private final String conversationId;
    private final String listingId;
    private final String ownerId;
    private final String requesterId;
    private final List<String> participantIds;
    private final String lastMessage;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;

    public Conversation(String conversationId, String listingId, String ownerId,
                        String requesterId, List<String> participantIds,
                        String lastMessage, Timestamp createdAt, Timestamp updatedAt) {
        this.conversationId = conversationId;
        this.listingId = listingId == null ? "" : listingId; // "" = chat started from a profile
        this.ownerId = ownerId;
        this.requesterId = requesterId;
        this.participantIds = Collections.unmodifiableList(new ArrayList<>(participantIds));
        this.lastMessage = lastMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getConversationId() { return conversationId; }
    public String getListingId() { return listingId; }
    public String getOwnerId() { return ownerId; }
    public String getRequesterId() { return requesterId; }
    public List<String> getParticipantIds() { return participantIds; }
    public String getLastMessage() { return lastMessage; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
}
