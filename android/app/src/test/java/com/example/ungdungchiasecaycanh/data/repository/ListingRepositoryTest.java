package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ListingRepositoryTest {
    private Listing normalize(ListingType type, long price, String lookingFor,
                              List<String> images) {
        return ListingRepository.normalizeForCreate("owner", " Pothos ", " ",
                " Healthy cutting ", type, price, lookingFor, images, " District 7 ");
    }

    @Test
    public void giveAwayForcesZeroPriceAndEmptyTradeRequest() {
        Listing listing = normalize(ListingType.GIVE_AWAY, 9000, "other plant",
                Collections.singletonList("https://example.invalid/image"));
        assertEquals(0, listing.getPrice());
        assertEquals("", listing.getLookingFor());
        assertEquals("Pothos", listing.getNickname());
        assertEquals("", listing.getSpecies());
        assertEquals(ListingStatus.ACTIVE, listing.getStatus());
    }

    @Test
    public void tradeRequiresLookingForAndForcesZeroPrice() {
        assertThrows(IllegalArgumentException.class, () ->
                normalize(ListingType.TRADE, 0, "  ",
                        Collections.singletonList("https://example.invalid/image")));
        Listing listing = normalize(ListingType.TRADE, 9000, " Mint ",
                Collections.singletonList("https://example.invalid/image"));
        assertEquals(0, listing.getPrice());
        assertEquals("Mint", listing.getLookingFor());
    }

    @Test
    public void sellRequiresPositivePriceAndClearsTradeRequest() {
        assertThrows(IllegalArgumentException.class, () ->
                normalize(ListingType.SELL, 0, "",
                        Collections.singletonList("https://example.invalid/image")));
        Listing listing = normalize(ListingType.SELL, 10000, "Mint",
                Collections.singletonList("https://example.invalid/image"));
        assertEquals(10000, listing.getPrice());
        assertEquals("", listing.getLookingFor());
    }

    @Test
    public void acceptsZeroToFiveImagesAndRejectsSix() {
        assertEquals(Collections.emptyList(),
                normalize(ListingType.GIVE_AWAY, 0, "", Collections.emptyList()).getImageUrls());
        normalize(ListingType.GIVE_AWAY, 0, "",
                Collections.singletonList("https://example.invalid/one"));
        normalize(ListingType.GIVE_AWAY, 0, "",
                Arrays.asList("https://example.invalid/1", "https://example.invalid/2",
                        "https://example.invalid/3", "https://example.invalid/4",
                        "https://example.invalid/5"));
        assertThrows(IllegalArgumentException.class, () ->
                normalize(ListingType.GIVE_AWAY, 0, "",
                        Arrays.asList("https://example.invalid/1", "https://example.invalid/2",
                                "https://example.invalid/3", "https://example.invalid/4",
                                "https://example.invalid/5", "https://example.invalid/6")));
    }

    @Test
    public void localImageLocationsAreRejectedForRemoteListings() {
        assertThrows(IllegalArgumentException.class, () -> normalize(
                ListingType.GIVE_AWAY, 0, "", Collections.singletonList("content://photo/1")));
        assertThrows(IllegalArgumentException.class, () -> normalize(
                ListingType.GIVE_AWAY, 0, "", Collections.singletonList("file:///photo.jpg")));
    }

    @Test
    public void emptyImagesSelectPlaceholderStateSafely() {
        Listing listing = normalize(ListingType.GIVE_AWAY, 0, "", Collections.emptyList());
        assertEquals(null, listing.getCoverImageUrl());
    }
}
