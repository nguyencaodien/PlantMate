package com.example.ungdungchiasecaycanh.ui.listing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Collections;

public class ListingDetailViewModelTest {
    private Listing listing(ListingType type, long price, String lookingFor,
                            String species) {
        return new Listing("id", "owner", "Mochi", species, "Healthy plant", type,
                price, lookingFor, Collections.singletonList("https://example.invalid/cover"),
                "District 7", ListingStatus.ACTIVE, null, null);
    }

    @Test
    public void giveAwayDoesNotNeedPriceOrLookingFor() {
        assertEquals("Give Away", ListingDetailViewModel.typeValue(
                listing(ListingType.GIVE_AWAY, 0, "", "")));
    }

    @Test
    public void tradeUsesLookingFor() {
        assertEquals("Looking for: small pothos", ListingDetailViewModel.typeValue(
                listing(ListingType.TRADE, 0, "small pothos", "Monstera")));
    }

    @Test
    public void sellUsesWholeVndPrice() {
        assertEquals("180.000 ₫", ListingDetailViewModel.typeValue(
                listing(ListingType.SELL, 180000, "", "Monstera")));
    }

    @Test
    public void missingSpeciesIsSafeForTypeFormatting() {
        assertEquals("Give Away", ListingDetailViewModel.typeValue(
                listing(ListingType.GIVE_AWAY, 0, "", "")));
    }

    @Test
    public void initialSavedStateIsKnownWithoutOperation() {
        ListingDetailViewModel.SavedState state =
                ListingDetailViewModel.finishSavedAction(false, false, true);
        assertFalse(state.saved);
        assertFalse(state.inProgress);
    }

    @Test
    public void successfulSaveAndUnsaveApplyTargetState() {
        assertTrue(ListingDetailViewModel.finishSavedAction(false, true, true).saved);
        assertFalse(ListingDetailViewModel.finishSavedAction(true, false, true).saved);
    }

    @Test
    public void failedSaveActionPreservesPreviousState() {
        assertFalse(ListingDetailViewModel.finishSavedAction(false, true, false).saved);
        assertTrue(ListingDetailViewModel.finishSavedAction(true, false, false).saved);
    }

    @Test
    public void saveActionBeginsInProgressWithoutChangingState() {
        ListingDetailViewModel.SavedState state =
                ListingDetailViewModel.beginSavedAction(true);
        assertTrue(state.saved);
        assertTrue(state.inProgress);
    }
}
