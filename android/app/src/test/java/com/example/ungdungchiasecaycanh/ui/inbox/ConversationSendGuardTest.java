package com.example.ungdungchiasecaycanh.ui.inbox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Text, photos and voice notes share one guard, so a blocked chat refuses all of them. */
public class ConversationSendGuardTest {
    @Test public void aLoadedChatWithAClearBlockStateCanSend() {
        assertTrue(ConversationViewModel.canSend(true, false, true, false));
    }

    @Test public void nothingIsSentBeforeTheChatLoads() {
        assertFalse(ConversationViewModel.canSend(false, false, true, false));
    }

    @Test public void aSecondSendWaitsForTheFirst() {
        assertFalse(ConversationViewModel.canSend(true, true, true, false));
    }

    @Test public void anUnknownBlockStateRefusesTheSend() {
        assertFalse(ConversationViewModel.canSend(true, false, false, false));
    }

    @Test public void aBlockedMemberCannotBeMessaged() {
        assertFalse(ConversationViewModel.canSend(true, false, true, true));
    }
}
