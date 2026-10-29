package com.example.ungdungchiasecaycanh.ui.doctor;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DoctorGiversTest {
    private static Listing listing(String id, String nickname, String species, ListingType type, ListingStatus status) {
        return new Listing(id, "o", nickname, species, "", type, type == ListingType.SELL ? 1000 : 0,
                type == ListingType.TRADE ? "x" : "", Collections.emptyList(), "Q3", status, null, null);
    }

    @Test public void matchesByNameOrSpeciesIgnoringAccentsAndSkipsSalesAndClosed() {
        List<Listing> out = DoctorViewModel.matching(Arrays.asList(
                listing("a", "Lưỡi hổ mini", "", ListingType.GIVE_AWAY, ListingStatus.ACTIVE),
                listing("b", "Cây xanh", "Dracaena trifasciata", ListingType.TRADE, ListingStatus.ACTIVE),
                listing("c", "Luoi ho", "", ListingType.SELL, ListingStatus.ACTIVE),
                listing("d", "Lưỡi hổ", "", ListingType.GIVE_AWAY, ListingStatus.REHOMED),
                listing("e", "Sen đá", "", ListingType.GIVE_AWAY, ListingStatus.ACTIVE)),
                "Cây Lưỡi Hổ", "Dracaena trifasciata");
        assertEquals(2, out.size());
        assertEquals("a", out.get(0).getListingId());
        assertEquals("b", out.get(1).getListingId());
    }

    @Test public void nothingWithoutANameToMatch() {
        assertEquals(0, DoctorViewModel.matching(Collections.singletonList(
                listing("a", "Lưỡi hổ", "", ListingType.GIVE_AWAY, ListingStatus.ACTIVE)), null, null).size());
    }
}
