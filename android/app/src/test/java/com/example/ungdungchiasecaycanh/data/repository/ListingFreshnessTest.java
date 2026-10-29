package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Listing lifecycle: ask the owner after 14 idle days, leave Home after 30. */
public class ListingFreshnessTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long DAY = 24L * 60 * 60 * 1000;

    private static Listing listing(String id, ListingStatus status, long idleDays) {
        long at = NOW - idleDays * DAY;
        Timestamp t = new Timestamp(at / 1000, 0);
        return new Listing(id, "owner", id, "", "", ListingType.GIVE_AWAY, 0, "",
                Collections.emptyList(), "Quận 7", status, t, t);
    }

    @Test public void ownerIsAskedAfterFourteenIdleDays() {
        assertFalse(ListingRepository.needsCheck(listing("a", ListingStatus.ACTIVE, 13), NOW));
        assertTrue(ListingRepository.needsCheck(listing("a", ListingStatus.ACTIVE, 14), NOW));
        assertFalse(ListingRepository.needsCheck(listing("a", ListingStatus.RESERVED, 40), NOW));
    }

    @Test public void activeListingsIdleForThirtyDaysLeaveHome() {
        List<Listing> out = ListingRepository.withoutStale(Arrays.asList(
                listing("fresh", ListingStatus.ACTIVE, 29),
                listing("stale", ListingStatus.ACTIVE, 30)), NOW);
        assertEquals(1, out.size());
        assertEquals("fresh", out.get(0).getListingId());
    }

    @Test public void homeShowsReservedSalesAndOnesSoldInTheLastThreeDays() {
        List<Listing> out = ListingRepository.recentSaleUpdates(Arrays.asList(
                listing("reserved", ListingStatus.RESERVED, 20),
                listing("soldToday", ListingStatus.REHOMED, 0),
                listing("soldLongAgo", ListingStatus.REHOMED, 3),
                listing("hidden", ListingStatus.UNAVAILABLE, 0)), NOW);
        assertEquals(2, out.size());
        assertEquals("reserved", out.get(0).getListingId());
        assertEquals("soldToday", out.get(1).getListingId());
    }
}
