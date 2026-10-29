package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.ListingStatus;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;

public class ListingStatusUpdateFieldsTest {
    @Test public void updateMapContainsOnlyStatusAndUpdatedAt() {
        assertEquals(new HashSet<>(Arrays.asList("status", "updatedAt")),
                FirebaseListingDataSource.statusUpdateFields(ListingStatus.RESERVED).keySet());
        assertTrue(FirebaseListingDataSource.statusUpdateFields(ListingStatus.RESERVED)
                .containsKey("updatedAt"));
    }
}
