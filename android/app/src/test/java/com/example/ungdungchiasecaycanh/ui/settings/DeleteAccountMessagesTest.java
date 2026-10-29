package com.example.ungdungchiasecaycanh.ui.settings;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;

/** A failed identity check says nothing was deleted; a failure after it asks to finish the job. */
public class DeleteAccountMessagesTest {
    @Test public void confirmationAndDeletionFailuresDiffer() {
        assertEquals(R.string.delete_account_confirm_failed,
                DeleteAccountViewModel.messageFor(new DeleteAccountViewModel.Confirmation(new IllegalStateException())));
        assertEquals(R.string.delete_account_failed, DeleteAccountViewModel.messageFor(new IllegalStateException()));
    }
}
