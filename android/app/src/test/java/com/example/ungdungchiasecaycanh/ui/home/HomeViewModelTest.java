package com.example.ungdungchiasecaycanh.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import com.google.firebase.Timestamp;

public class HomeViewModelTest {
    private Listing listing(String id, ListingType type) {
        return new Listing(id, "owner", id, "", "description", type,
                type == ListingType.SELL ? 180000 : 0,
                type == ListingType.TRADE ? "succulent" : "",
                Collections.singletonList("https://example.invalid/" + id),
                "District 7", ListingStatus.ACTIVE, null, null);
    }

    private List<Listing> all() {
        return Arrays.asList(
                listing("give", ListingType.GIVE_AWAY),
                listing("trade", ListingType.TRADE),
                listing("sell", ListingType.SELL));
    }

    @Test
    public void allReturnsEveryLoadedListing() {
        List<Listing> source = all();
        assertEquals(3, HomeViewModel.filterListings(source, HomeViewModel.Filter.ALL).size());
    }

    @Test
    public void eachTypeFilterReturnsOnlyMatchingListings() {
        List<Listing> source = all();
        assertEquals("give", HomeViewModel.filterListings(source, HomeViewModel.Filter.GIVE_AWAY).get(0).getListingId());
        assertEquals("trade", HomeViewModel.filterListings(source, HomeViewModel.Filter.TRADE).get(0).getListingId());
        assertEquals("sell", HomeViewModel.filterListings(source, HomeViewModel.Filter.SELL).get(0).getListingId());
    }

    @Test
    public void severalTypesKeepAnyOfThem() {
        List<Listing> out = HomeViewModel.filterListings(all(),
                EnumSet.of(HomeViewModel.Filter.GIVE_AWAY, HomeViewModel.Filter.TRADE));
        assertEquals(Arrays.asList("give", "trade"), ids(out));
        assertEquals(3, HomeViewModel.filterListings(all(), EnumSet.noneOf(HomeViewModel.Filter.class)).size());
    }

    @Test
    public void severalAreasKeepAnyOfThem() {
        List<Listing> in = Arrays.asList(placed("q7", "Quận 7", 3, true), placed("gv", "Gò Vấp", 2, true),
                placed("q1", "Quận 1", 1, true));
        List<Listing> out = HomeViewModel.refineListings(in, new HomeViewModel.Refine(
                new java.util.LinkedHashSet<>(Arrays.asList("quan 7", "Go Vap")), HomeViewModel.Sort.NEWEST, false));
        assertEquals(Arrays.asList("q7", "gv"), ids(out));
    }

    @Test
    public void emptyInputProducesEmptyOutput() {
        assertEquals(0, HomeViewModel.filterListings(Collections.emptyList(),
                HomeViewModel.Filter.ALL).size());
    }

    @Test
    public void filteringDoesNotMutateOriginalList() {
        List<Listing> source = all();
        List<Listing> filtered = HomeViewModel.filterListings(source, HomeViewModel.Filter.TRADE);
        assertEquals(3, source.size());
        assertSame(source.get(1), filtered.get(0));
    }

    private Listing placed(String id, String area, long seconds, boolean photo) {
        return new Listing(id, "owner", id, "", "description", ListingType.GIVE_AWAY, 0, "",
                photo ? Collections.singletonList("https://example.invalid/" + id) : Collections.<String>emptyList(),
                area, ListingStatus.ACTIVE, new Timestamp(seconds, 0), null);
    }

    @Test public void refineFiltersAreaIgnoringAccentsCaseAndSpacing() {
        List<Listing> in = Arrays.asList(placed("a", "Quận 7, HCM", 3, true),
                placed("b", "Tan Binh, HCM", 2, true), placed("c", "quan  7, hcm", 1, true));
        List<Listing> out = HomeViewModel.refineListings(in,
                new HomeViewModel.Refine(Collections.singleton("Quan 7, HCM"), HomeViewModel.Sort.NEWEST, false));
        assertEquals(Arrays.asList("a", "c"), ids(out));
    }

    @Test public void refineSortsAndKeepsOnlyListingsWithPhotos() {
        List<Listing> in = Arrays.asList(placed("new", "X", 30, true),
                placed("mid", "X", 20, false), placed("old", "X", 10, true));
        assertEquals(Arrays.asList("new", "mid", "old"), ids(HomeViewModel.refineListings(in,
                new HomeViewModel.Refine(Collections.<String>emptySet(), HomeViewModel.Sort.NEWEST, false))));
        assertEquals(Arrays.asList("new", "old"), ids(HomeViewModel.refineListings(in,
                new HomeViewModel.Refine(Collections.<String>emptySet(), HomeViewModel.Sort.NEWEST, true))));
    }

    @Test public void nearPutsMyAreaFirstAndMatchAlsoWeighsPhotos() {
        List<Listing> in = Arrays.asList(placed("far", "Quan 1", 30, true),
                placed("mineNoPhoto", "Go Vap", 20, false), placed("mine", "Gò Vấp", 10, true));
        HomeViewModel.Refine near = new HomeViewModel.Refine(Collections.<String>emptySet(), HomeViewModel.Sort.NEAR, false);
        assertEquals(Arrays.asList("mineNoPhoto", "mine", "far"), ids(HomeViewModel.refineListings(in, near, "go vap", false)));
        HomeViewModel.Refine match = new HomeViewModel.Refine(Collections.<String>emptySet(), HomeViewModel.Sort.MATCH, false);
        assertEquals(Arrays.asList("mine", "mineNoPhoto", "far"), ids(HomeViewModel.refineListings(in, match, "Go Vap", false)));
        assertEquals("tan phu", HomeViewModel.district("Tân Phú, TP.HCM"));
        assertEquals(HomeViewModel.district("Tan Phu, HCM"), HomeViewModel.district("Tân Phú, TP.HCM"));
        // No profile area: only kind and photo count, ties newest first.
        assertEquals(Arrays.asList("far", "mine", "mineNoPhoto"), ids(HomeViewModel.refineListings(in, match, "", false)));
    }

    @Test public void refineCountsOnlyNonDefaultChoices() {
        assertEquals(0, new HomeViewModel.Refine(Collections.singleton(" "), null, false).activeCount());
        // The order is not a filter: it never counts on the filter badge.
        assertEquals(2, new HomeViewModel.Refine(Collections.singleton("Go Vap"), HomeViewModel.Sort.MATCH, true).activeCount());
    }

    private static List<String> ids(List<Listing> listings) {
        List<String> out = new ArrayList<>();
        for (Listing l : listings) out.add(l.getListingId());
        return out;
    }

    @Test public void newestGiveAwaysAreNewestFirstAndCapped() {
        List<Listing> in = new ArrayList<>();
        for (int i = 0; i < 10; i++) in.add(placed("g" + i, "D7", i, true));
        in.add(listing("trade", ListingType.TRADE));
        List<Listing> out = HomeViewModel.newest(in, ListingType.GIVE_AWAY, HomeViewModel.NEWEST_GIVE_AWAYS);
        assertEquals(HomeViewModel.NEWEST_GIVE_AWAYS, out.size());
        assertEquals("g9", out.get(0).getListingId());
        assertEquals("g2", out.get(out.size() - 1).getListingId());
    }

    @Test public void latestPrefersNewestWithPhotoAcrossTypes() {
        List<Listing> in = Arrays.asList(placed("old", "D7", 1, true), placed("newNoPhoto", "D7", 9, false),
                placed("new", "D7", 5, true));
        assertEquals("new", HomeViewModel.latest(in).getListingId());
        assertEquals("newNoPhoto", HomeViewModel.latest(Collections.singletonList(placed("newNoPhoto", "D7", 9, false))).getListingId());
        assertEquals(null, HomeViewModel.latest(Collections.<Listing>emptyList()));
    }

    /** Older pages go after what is loaded and never repeat a listing. */
    @Test public void nextPageAppendsWithoutDuplicates() {
        java.util.List<Listing> current = java.util.Arrays.asList(listing("a", ListingType.GIVE_AWAY), listing("b", ListingType.TRADE));
        java.util.List<Listing> out = HomeViewModel.appendNew(current,
                java.util.Arrays.asList(listing("b", ListingType.TRADE), listing("c", ListingType.SELL)));
        org.junit.Assert.assertEquals(3, out.size());
        org.junit.Assert.assertEquals("c", out.get(2).getListingId());
    }

    private Listing sale(String id, ListingStatus status) {
        return new Listing(id, "owner", id, "", "description", ListingType.SELL, 100000, "",
                Collections.<String>emptyList(), "District 7", status, null, null);
    }

    @Test public void saleRowsPutAvailablePlantsBeforeReservedAndSold() {
        List<Listing> in = Arrays.asList(sale("sold", ListingStatus.REHOMED), sale("reserved", ListingStatus.RESERVED),
                listing("gift", ListingType.GIVE_AWAY), sale("a1", ListingStatus.ACTIVE), sale("a2", ListingStatus.ACTIVE));
        assertEquals(Arrays.asList("a1", "a2", "reserved"), ids(HomeViewModel.previewSales(in)));
    }

    @Test public void saleUpdatesReplaceTheOlderActiveCopy() {
        List<Listing> out = HomeViewModel.withSaleUpdates(
                Arrays.asList(sale("x", ListingStatus.ACTIVE), sale("y", ListingStatus.ACTIVE)),
                Collections.singletonList(sale("x", ListingStatus.REHOMED)));
        assertEquals(Arrays.asList("y", "x"), ids(out));
        assertEquals(ListingStatus.REHOMED, out.get(1).getStatus());
    }

    @Test public void heroTakesOnlyAvailableSales() {
        List<Listing> in = Arrays.asList(sale("sold", ListingStatus.REHOMED), sale("a", ListingStatus.ACTIVE),
                sale("reserved", ListingStatus.RESERVED));
        assertEquals(Collections.singletonList("a"), ids(HomeViewModel.activeOnly(in)));
    }

    @Test public void pagingStopsAfterThreeHundredDocumentsRead() {
        assertTrue(HomeViewModel.hasBudget(true, 5));
        assertFalse(HomeViewModel.hasBudget(true, 6)); // 6 x 50 = 300 read, even if stale ones were dropped
        assertFalse(HomeViewModel.hasBudget(false, 1));
    }
}
