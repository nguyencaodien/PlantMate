package com.example.ungdungchiasecaycanh.ui.inbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseChatMediaDataSource;
import com.example.ungdungchiasecaycanh.model.Message;
import com.example.ungdungchiasecaycanh.model.MessageType;

import org.junit.Test;

public class ChatMediaTest {
    @Test public void chatMediaRefAcceptsOnlyPrivateIds() {
        assertEquals("abcDEF12345", FirebaseChatMediaDataSource.idOf("pmchat://abcDEF12345"));
        assertTrue(FirebaseChatMediaDataSource.isChatMedia("pmchat://abcDEF12345"));
        assertNull(FirebaseChatMediaDataSource.idOf("pmimg://abcDEF12345"));
        assertNull(FirebaseChatMediaDataSource.idOf("pmchat://short"));
        assertNull(FirebaseChatMediaDataSource.idOf("pmchat://../../users/x1234567"));
        assertFalse(FirebaseChatMediaDataSource.isChatMedia(null));
    }

    @Test public void voiceDurationLabelRoundsToSeconds() {
        assertEquals("0:00", VoiceRecorder.format(0));
        assertEquals("0:07", VoiceRecorder.format(6_600));
        assertEquals("1:00", VoiceRecorder.format(VoiceRecorder.MAX_MS));
        assertEquals("0:00", VoiceRecorder.format(-50));
    }

    @Test public void voiceLimitIsSixtySeconds() {
        assertEquals(60_000, VoiceRecorder.MAX_MS);
        assertEquals(500, VoiceRecorder.MIN_MS);
    }

    @Test public void sentAndReceivedFollowSender() {
        Message own = new Message("m1", "u1", MessageType.VOICE, "", null, "pmchat://abcDEF12345", 4_000);
        assertEquals(MessageAdapter.SENT, MessageAdapter.viewTypeFor(own, "u1"));
        assertEquals(MessageAdapter.RECEIVED, MessageAdapter.viewTypeFor(own, "u2"));
    }
}
