package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

public class FirebaseReviewDataSourceTest {
    @Test public void createMapHasSixCanonicalFieldsOnly() {
        Map<String, Object> fields = FirebaseReviewDataSource.createFields(
                "listing", "reviewer", "reviewee", 5, "Great");
        assertEquals(6, fields.size());
        assertTrue(fields.keySet().containsAll(Arrays.asList("listingId", "reviewerId",
                "revieweeId", "rating", "comment", "createdAt")));
        assertEquals("Great", fields.get("comment"));
        assertFalse(fields.containsKey("reviewId"));
        assertFalse(fields.containsKey("conversationId"));
        assertFalse(fields.containsKey("reviewerName"));
    }
}
