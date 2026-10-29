package com.example.ungdungchiasecaycanh.ui.inbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.Conversation;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatUnreadTest {
    private static Conversation chat(String id, String last, long updatedSec) {
        return new Conversation(id, "", "a", "b", Arrays.asList("a", "b"), last, new Timestamp(1, 0), new Timestamp(updatedSec, 0));
    }

    @Test public void newerThanLastOpenIsUnread() {
        assertTrue(ChatUnread.isUnread(chat("c", "Hi", 100), 50_000));
        assertFalse(ChatUnread.isUnread(chat("c", "Hi", 100), 100_000));
        assertFalse(ChatUnread.isUnread(chat("c", "", 100), 0)); // created but nothing said yet
    }

    @Test public void countsChatsNotMessages() {
        Map<String, Long> seen = new HashMap<>();
        seen.put("old", 200_000L);
        assertEquals(1, ChatUnread.count(Arrays.asList(chat("old", "x", 100), chat("new", "y", 300)), seen, 250_000));
        assertEquals(0, ChatUnread.count(Collections.<Conversation>emptyList(), seen, 0));
    }
}
