package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Map;

public class FirebaseUserDataSourceTest {
    @Test public void profileUpdateContainsExactlyMutableFieldsAndServerTimestamp() {
        Map<String, Object> update = FirebaseUserDataSource.buildProfileUpdate(
                "Maya", "Hà Nội", "Plant collector");
        assertEquals(4, update.size());
        assertTrue(update.containsKey("displayName"));
        assertTrue(update.containsKey("locationText"));
        assertTrue(update.containsKey("bio"));
        assertTrue(update.containsKey("updatedAt"));
        assertFalse(update.containsKey("avatarUrl"));
        assertFalse(update.containsKey("createdAt"));
        assertFalse(update.containsKey("email"));
        assertFalse(update.containsKey("uid"));
        assertFalse(update.containsKey("settings"));
        assertFalse(update.containsKey("rating"));
    }
}
