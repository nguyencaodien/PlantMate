package com.example.ungdungchiasecaycanh.ui.inbox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** A deleted conversation shows "not found"; any other failure stays a retryable error. */
public class ConversationMissingTest {
    @Test public void aMissingDocumentIsNotFound() {
        assertTrue(ConversationViewModel.isMissing(new IllegalStateException("Conversation does not exist")));
    }

    @Test public void theMessageIsMatchedWhateverItsCase() {
        assertTrue(ConversationViewModel.isMissing(new IllegalStateException("Document DOES NOT EXIST")));
    }

    @Test public void aWrappedMissingDocumentIsNotFound() {
        assertTrue(ConversationViewModel.isMissing(
                new RuntimeException("Load failed", new IllegalStateException("does not exist"))));
    }

    @Test public void otherFailuresStayRetryable() {
        assertFalse(ConversationViewModel.isMissing(new IllegalStateException("Network unavailable")));
        assertFalse(ConversationViewModel.isMissing(new IllegalStateException()));
    }
}
