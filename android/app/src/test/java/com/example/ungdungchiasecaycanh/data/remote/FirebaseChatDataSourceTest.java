package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class FirebaseChatDataSourceTest {
    @Test public void conversationCreateHasSevenCanonicalFieldsOnly() {
        Map<String, Object> fields = FirebaseChatDataSource.conversationCreateFields(
                "listing", "owner", "requester", Arrays.asList("owner", "requester"));
        assertEquals(7, fields.size());
        assertTrue(fields.keySet().containsAll(Arrays.asList("listingId", "ownerId",
                "requesterId", "participantIds", "lastMessage", "createdAt", "updatedAt")));
        assertEquals(Arrays.asList("owner", "requester"), fields.get("participantIds"));
        assertEquals("", fields.get("lastMessage"));
        assertFalse(fields.containsKey("conversationId"));
    }

    @Test public void messageCreateHasFourCanonicalFieldsOnly() {
        Map<String, Object> fields = FirebaseChatDataSource.messageCreateFields("sender", "Hello");
        assertEquals(4, fields.size());
        assertTrue(fields.keySet().containsAll(Arrays.asList("senderId", "type", "text", "createdAt")));
        assertEquals("TEXT", fields.get("type"));
        assertEquals("sender", fields.get("senderId"));
        assertFalse(fields.containsKey("messageId"));
    }
}
