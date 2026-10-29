package com.example.ungdungchiasecaycanh.ui.listing;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SimilarListingsTest {
    private static Listing listing(String id, String owner, ListingType type, String area, ListingStatus status) {
        return new Listing(id, owner, id, "", "", type, 0, "", Collections.emptyList(), area, status, null, null);
    }

    @Test public void sameTypeThenSameAreaNeverSelfBlockedOrClosed() {
        Listing target = listing("t", "o", ListingType.GIVE_AWAY, "Quận 3", ListingStatus.ACTIVE);
        List<Listing> out = ListingDetailViewModel.similarTo(Arrays.asList(
                target,
                listing("trade", "a", ListingType.TRADE, "Quận 3", ListingStatus.ACTIVE),
                listing("giveFar", "b", ListingType.GIVE_AWAY, "Quận 7", ListingStatus.ACTIVE),
                listing("giveNear", "c", ListingType.GIVE_AWAY, "quận 3", ListingStatus.ACTIVE),
                listing("blocked", "x", ListingType.GIVE_AWAY, "Quận 3", ListingStatus.ACTIVE),
                listing("gone", "d", ListingType.GIVE_AWAY, "Quận 3", ListingStatus.REHOMED)),
                target, Collections.singleton("x"));
        assertEquals(3, out.size());
        assertEquals("giveNear", out.get(0).getListingId());
        assertEquals("giveFar", out.get(1).getListingId());
        assertEquals("trade", out.get(2).getListingId());
    }

    @Test public void ownerOpenCountIsNotCappedByTheCarousel() {
        List<Listing> all = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) all.add(listing("a" + i, "o", ListingType.SELL, "D7", ListingStatus.ACTIVE));
        all.add(listing("done", "o", ListingType.GIVE_AWAY, "D7", ListingStatus.REHOMED));
        all.add(listing("hidden", "o", ListingType.SELL, "D7", ListingStatus.UNAVAILABLE));
        ListingDetailViewModel.OwnerStats stats = ListingDetailViewModel.ownerStatsOf(all, Collections.emptyList(), "a0");
        assertEquals(9, stats.open);
        assertEquals(ListingDetailViewModel.MAX_SIMILAR, stats.others.size());
        assertEquals(1, stats.given);
    }
}
