package com.example.ungdungchiasecaycanh.ui.inbox;

import com.example.ungdungchiasecaycanh.model.Conversation;

import java.util.List;
import java.util.Map;

/**
 * "New messages" without read receipts: a chat is unread when it changed after this member last had it
 * open on this device. Seen times live in the account's preferences (SettingsRepository).
 */
public final class ChatUnread {
    private ChatUnread() { }

    public static boolean isUnread(Conversation c, long seenAt) {
        return c != null && isUnread(c.getLastMessage(), c.getUpdatedAt(), seenAt);
    }

    public static boolean isUnread(String lastMessage, com.google.firebase.Timestamp updatedAt, long seenAt) {
        if (lastMessage == null || lastMessage.isEmpty() || updatedAt == null) return false;
        return updatedAt.toDate().getTime() > seenAt;
    }

    /** Chats with something new; a chat never opened here counts from {@code fallbackSeenAt}. */
    public static int count(List<Conversation> chats, Map<String, Long> seen, long fallbackSeenAt) {
        int n = 0;
        for (Conversation c : chats) {
            Long at = seen.get(c.getConversationId());
            if (isUnread(c, at == null ? fallbackSeenAt : at)) n++;
        }
        return n;
    }
}
