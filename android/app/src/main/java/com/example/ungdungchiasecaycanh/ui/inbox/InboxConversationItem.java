package com.example.ungdungchiasecaycanh.ui.inbox;

import com.google.firebase.Timestamp;

public final class InboxConversationItem {
    public final String conversationId;
    public final String peerDisplayName;
    public final String listingNickname;
    public final String lastMessage;
    public final Timestamp updatedAt;
    /** Started from a member's profile: there is no plant to show. */
    public final boolean direct;

    public InboxConversationItem(String conversationId, String peerDisplayName,
                                 String listingNickname, String lastMessage, Timestamp updatedAt) {
        this(conversationId, peerDisplayName, listingNickname, lastMessage, updatedAt, false);
    }

    public InboxConversationItem(String conversationId, String peerDisplayName,
                                 String listingNickname, String lastMessage, Timestamp updatedAt, boolean direct) {
        this.direct = direct;
        this.conversationId = conversationId;
        this.peerDisplayName = peerDisplayName;
        this.listingNickname = listingNickname;
        this.lastMessage = lastMessage;
        this.updatedAt = updatedAt;
    }
}
