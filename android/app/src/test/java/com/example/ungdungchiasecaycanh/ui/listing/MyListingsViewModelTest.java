package com.example.ungdungchiasecaycanh.ui.listing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyListingsViewModelTest {
    private List<Listing> listings() {
        return Arrays.asList(
                listing("active", ListingStatus.ACTIVE),
                listing("reserved", ListingStatus.RESERVED),
                listing("rehomed", ListingStatus.REHOMED),
                listing("unavailable", ListingStatus.UNAVAILABLE));
    }

    @Test public void allFilterReturnsAllAndDoesNotMutateSource() {
        List<Listing> source = new ArrayList<>(listings());
        List<Listing> result = MyListingsViewModel.filterListings(
                source, MyListingsViewModel.Filter.ALL);
        assertEquals(4, result.size());
        assertEquals(4, source.size());
        assertFalse(source == result);
    }

    @Test public void eachStatusFilterReturnsOnlyMatchingListings() {
        List<Listing> source = listings();
        assertFilter(source, MyListingsViewModel.Filter.ACTIVE, ListingStatus.ACTIVE);
        assertFilter(source, MyListingsViewModel.Filter.RESERVED, ListingStatus.RESERVED);
        assertFilter(source, MyListingsViewModel.Filter.REHOMED, ListingStatus.REHOMED);
        assertFilter(source, MyListingsViewModel.Filter.UNAVAILABLE, ListingStatus.UNAVAILABLE);
    }

    private void assertFilter(List<Listing> source, MyListingsViewModel.Filter filter,
                              ListingStatus expected) {
        List<Listing> result = MyListingsViewModel.filterListings(source, filter);
        assertEquals(1, result.size());
        assertEquals(expected, result.get(0).getStatus());
        assertEquals(4, source.size());
    }

    @Test public void statusTransitionsMatchFrozenContract() {
        allowed(ListingStatus.ACTIVE, ListingStatus.ACTIVE);
        allowed(ListingStatus.ACTIVE, ListingStatus.RESERVED);
        allowed(ListingStatus.ACTIVE, ListingStatus.REHOMED);
        allowed(ListingStatus.ACTIVE, ListingStatus.UNAVAILABLE);
        allowed(ListingStatus.RESERVED, ListingStatus.ACTIVE);
        allowed(ListingStatus.RESERVED, ListingStatus.RESERVED);
        allowed(ListingStatus.RESERVED, ListingStatus.REHOMED);
        allowed(ListingStatus.RESERVED, ListingStatus.UNAVAILABLE);
        allowed(ListingStatus.UNAVAILABLE, ListingStatus.ACTIVE);
        allowed(ListingStatus.UNAVAILABLE, ListingStatus.UNAVAILABLE);
        allowed(ListingStatus.REHOMED, ListingStatus.REHOMED);
        denied(ListingStatus.UNAVAILABLE, ListingStatus.RESERVED);
        denied(ListingStatus.UNAVAILABLE, ListingStatus.REHOMED);
        denied(ListingStatus.REHOMED, ListingStatus.ACTIVE);
        denied(ListingStatus.REHOMED, ListingStatus.RESERVED);
        denied(ListingStatus.REHOMED, ListingStatus.UNAVAILABLE);
    }

    private void allowed(ListingStatus from, ListingStatus to) {
        assertTrue(from + " -> " + to,
                MyListingsViewModel.canTransition(from, to));
    }

    private void denied(ListingStatus from, ListingStatus to) {
        assertFalse(from + " -> " + to,
                MyListingsViewModel.canTransition(from, to));
    }

    @Test public void emptyInputProducesEmptyFilteredList() {
        assertTrue(MyListingsViewModel.filterListings(Collections.emptyList(),
                MyListingsViewModel.Filter.RESERVED).isEmpty());
    }

    private static Listing listing(String id, ListingStatus status) {
        return new Listing(id, "owner", id, "", "description", ListingType.GIVE_AWAY,
                0, "", Collections.singletonList("https://image.invalid"), "Area", status,
                null, null);
    }
}
