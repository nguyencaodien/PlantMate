package com.example.ungdungchiasecaycanh.ui.inbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.ungdungchiasecaycanh.model.Message;
import com.example.ungdungchiasecaycanh.model.MessageType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** One chat per pair: a plant card goes in only when the plant asked about is not the latest card. */
public class ConversationCardTest {
    private static Message text(String id) {
        return new Message(id, "asker", MessageType.TEXT, "Còn cây không?", null);
    }

    private static Message card(String id, String listingId) {
        return new Message(id, "asker", MessageType.LISTING, "", null, "", 0, listingId);
    }

    @Test public void latestCardIsTheNewestPlantCard() {
        assertNull(ConversationViewModel.latestCardListingId(null));
        assertNull(ConversationViewModel.latestCardListingId(Collections.singletonList(text("m1"))));
        assertEquals("L2", ConversationViewModel.latestCardListingId(Arrays.asList(
                card("m1", "L1"), text("m2"), card("m3", "L2"), text("m4"))));
    }
}
