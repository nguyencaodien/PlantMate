package com.example.ungdungchiasecaycanh.ui.inbox;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.model.Message;
import com.example.ungdungchiasecaycanh.model.MessageType;

import org.junit.Test;

public class MessageAdapterTest {
    @Test public void senderDeterminesBubbleSide() {
        Message message = new Message("message", "owner", MessageType.TEXT, "Hi", null);
        assertEquals(MessageAdapter.SENT, MessageAdapter.viewTypeFor(message, "owner"));
        assertEquals(MessageAdapter.RECEIVED, MessageAdapter.viewTypeFor(message, "requester"));
    }
}
