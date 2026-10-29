package com.example.ungdungchiasecaycanh.ui.listing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ListingPreviewViewModelTest {
    @Test
    public void publishCanStartInitiallyAndRetryAfterFailure() {
        assertTrue(ListingPreviewViewModel.canStart(ListingPreviewViewModel.Status.IDLE));
        assertTrue(ListingPreviewViewModel.canStart(ListingPreviewViewModel.Status.ERROR));
    }

    @Test
    public void publishingAndSuccessRejectDuplicateSubmit() {
        assertFalse(ListingPreviewViewModel.canStart(
                ListingPreviewViewModel.Status.PUBLISHING));
        assertFalse(ListingPreviewViewModel.canStart(ListingPreviewViewModel.Status.SUCCESS));
    }
}
