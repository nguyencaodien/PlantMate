package com.example.ungdungchiasecaycanh.ui.inbox;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.model.Conversation;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class InboxViewModelTest {
    @Test public void searchFiltersPeerOrListingWithoutChangingSource() {
        List<InboxConversationItem> source = Arrays.asList(
                new InboxConversationItem("one", "Maya", "Pothos", "Hi", null),
                new InboxConversationItem("two", "Leo", "Fern", "Hello", null));
        assertEquals(2, InboxViewModel.filterItems(source, "").size());
        assertEquals("one", InboxViewModel.filterItems(source, "maya").get(0).conversationId);
        assertEquals("two", InboxViewModel.filterItems(source, "FERN").get(0).conversationId);
        assertEquals(0, InboxViewModel.filterItems(source, "orchid").size());
        assertEquals(2, source.size());
    }

    private static Conversation chat(String id, String owner, String requester, long seconds) {
        return new Conversation(id, "L", owner, requester, Arrays.asList(owner, requester), "",
                new Timestamp(seconds, 0), new Timestamp(seconds, 0));
    }

    @Test public void eachPersonGetsOneRowPreferringThePairChat() {
        List<Conversation> out = InboxViewModel.onePerPeer(Arrays.asList(
                chat("L1__vy", "me", "vy", 300),      // legacy, newer
                chat("me__vy", "vy", "me", 200),      // the pair chat wins anyway
                chat("L2__leo", "me", "leo", 100),
                chat("L3__leo", "me", "leo", 150)), "me");
        assertEquals(2, out.size());
        assertEquals("me__vy", out.get(0).getConversationId());
        assertEquals("L3__leo", out.get(1).getConversationId());
    }

    @Test public void searchIgnoresVietnameseAccents() {
        List<InboxConversationItem> source = Arrays.asList(
                new InboxConversationItem("vy", "Lê Thảo Vy", "Lá sen đá", "Hi", null));
        assertEquals(1, InboxViewModel.filterItems(source, "thao").size());
        assertEquals(1, InboxViewModel.filterItems(source, "la sen da").size());
        assertEquals(0, InboxViewModel.filterItems(source, "thai").size());
    }
}
