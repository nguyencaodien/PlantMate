package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

public class FirebaseListingDataSourceTest {
    @Test
    public void createMapContainsExactlyCanonicalTwelveFieldsWithNoImages() {
        Listing listing = new Listing(null, "owner", "Mochi", "", "Healthy plant",
                ListingType.GIVE_AWAY, 0, "", Collections.emptyList(),
                "Quận 7, TP.HCM", ListingStatus.ACTIVE, null, null);

        Map<String, Object> fields = FirebaseListingDataSource.listingCreateFields(listing);

        assertEquals(12, fields.size());
        assertEquals(new HashSet<>(Arrays.asList("ownerId", "nickname", "species",
                "description", "type", "price", "lookingFor", "imageUrls",
                "locationText", "status", "createdAt", "updatedAt")), fields.keySet());
        assertEquals(Collections.emptyList(), fields.get("imageUrls"));
        assertEquals("ACTIVE", fields.get("status"));
        assertTrue(fields.get("createdAt") != null);
        assertTrue(fields.get("updatedAt") != null);
        assertTrue(!fields.containsKey("listingId"));
        assertTrue(!fields.containsKey("localImageUris"));
        assertTrue(!fields.containsKey("contentUri"));
        assertTrue(!fields.containsKey("ownerName"));
        assertTrue(!fields.containsKey("imageUri"));
    }

    @Test
    public void detailsAreWrittenOnlyWhenGivenAsEnumNames() {
        ListingDetails details = new ListingDetails(
                Arrays.asList(ListingDetails.Condition.WITH_POT, ListingDetails.Condition.HEALTHY,
                        ListingDetails.Condition.HEALTHY),
                70, LightLevel.BRIGHT_INDIRECT, Collections.singletonList(ListingDetails.PickupTime.SAT_MORNING));
        Listing listing = new Listing(null, "owner", "Mochi", "", "Healthy plant",
                ListingType.GIVE_AWAY, 0, "", Collections.emptyList(),
                "Quận 7, TP.HCM", ListingStatus.ACTIVE, null, null, details);

        Map<String, Object> fields = FirebaseListingDataSource.listingCreateFields(listing);

        assertEquals(16, fields.size());
        assertEquals(Arrays.asList("HEALTHY", "WITH_POT"), fields.get("condition"));
        assertEquals(70L, fields.get("heightCm"));
        assertEquals("BRIGHT_INDIRECT", fields.get("light"));
        assertEquals(Collections.singletonList("SAT_MORNING"), fields.get("pickupTimes"));
    }

    @Test
    public void unknownStoredDetailValuesAreSkipped() {
        assertEquals(Collections.singletonList(ListingDetails.Condition.ROOTED),
                ListingDetails.parse(ListingDetails.Condition.class, Arrays.asList("ROOTED", "GLOWING", 3)));
        assertEquals(null, LightLevel.fromName("MOONLIGHT"));
        assertEquals(0, new ListingDetails(null, 900, null, null).getHeightCm());
    }

    @Test
    public void savedReferenceContainsOnlySavedAt() {
        Map<String, Object> fields = FirebaseListingDataSource.savedReferenceFields();
        assertEquals(Collections.singleton("savedAt"), fields.keySet());
    }

    @Test
    public void emptyReferencesProduceEmptyResult() {
        assertTrue(FirebaseListingDataSource.selectExistingListings(
                Collections.emptyList(), Collections.emptyMap()).isEmpty());
    }

    @Test
    public void existingReferenceLoadsCanonicalListingAndMissingIsSkipped() {
        Listing listing = listing("present", ListingStatus.REHOMED);
        Map<String, Listing> listings = new HashMap<>();
        listings.put("present", listing);

        List<Listing> result = FirebaseListingDataSource.selectExistingListings(
                Arrays.asList("missing", "present"), listings);

        assertEquals(1, result.size());
        assertSame(listing, result.get(0));
        assertEquals(ListingStatus.REHOMED, result.get(0).getStatus());
    }

    private static Listing listing(String id, ListingStatus status) {
        return new Listing(id, "owner", "Mochi", "", "Healthy plant",
                ListingType.GIVE_AWAY, 0, "", Collections.singletonList("https://valid.test/image"),
                "Quận 7, TP.HCM", status, null, null);
    }
}
