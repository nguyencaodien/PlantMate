package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public class ChatRepositoryTest {
    @Test public void deterministicConversationId() {
        assertEquals("listing123__member456",
                ChatRepository.buildConversationId(" listing123 ", " member456 "));
        assertThrows(IllegalArgumentException.class,
                () -> ChatRepository.buildConversationId(" ", "member456"));
        assertThrows(IllegalArgumentException.class,
                () -> ChatRepository.buildConversationId("listing123", " "));
    }

    /** One conversation per pair of members, whichever of them asks first. */
    @Test public void pairConversationIdIsTheSameBothWays() {
        assertEquals("alice__bob", ChatRepository.pairConversationId("bob", "alice"));
        assertEquals("alice__bob", ChatRepository.pairConversationId(" alice ", "bob"));
        assertEquals(java.util.Arrays.asList("alice", "bob"), ChatRepository.pairParticipants("bob", "alice"));
        assertThrows(IllegalArgumentException.class, () -> ChatRepository.pairConversationId("alice", "alice"));
        assertThrows(IllegalArgumentException.class, () -> ChatRepository.pairConversationId("alice", " "));
    }

    @Test public void textIsTrimmedAndBounded() {
        assertEquals("Hello", ChatRepository.normalizeMessageText("  Hello  "));
        assertEquals("x", ChatRepository.normalizeMessageText("x"));
        assertEquals(1000, ChatRepository.normalizeMessageText("a".repeat(1000)).length());
        assertThrows(IllegalArgumentException.class,
                () -> ChatRepository.normalizeMessageText("  "));
        assertThrows(IllegalArgumentException.class,
                () -> ChatRepository.normalizeMessageText(""));
        assertThrows(IllegalArgumentException.class,
                () -> ChatRepository.normalizeMessageText("a".repeat(1001)));
    }

    /** A chat opened from a listing but never written in stays out of both inboxes (B2). */
    @Test public void inboxKeepsOnlyConversationsWithAMessage() {
        java.util.List<com.example.ungdungchiasecaycanh.model.Conversation> all = java.util.Arrays.asList(
                conversation("empty", ""), conversation("blank", "   "), conversation("text", "Cây còn không?"),
                conversation("voice", "Tin nhắn thoại"), conversation("missing", null), null);
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (com.example.ungdungchiasecaycanh.model.Conversation c : ChatRepository.withMessages(all)) ids.add(c.getConversationId());
        assertEquals(java.util.Arrays.asList("text", "voice"), ids);
        assertEquals(0, ChatRepository.withMessages(null).size());
    }

    private static com.example.ungdungchiasecaycanh.model.Conversation conversation(String id, String last) {
        return new com.example.ungdungchiasecaycanh.model.Conversation(id, "listing", "owner", "asker",
                java.util.Arrays.asList("owner", "asker"), last, null, null);
    }
}
