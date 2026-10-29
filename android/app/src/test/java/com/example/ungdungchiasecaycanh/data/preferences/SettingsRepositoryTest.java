package com.example.ungdungchiasecaycanh.data.preferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SettingsRepositoryTest {
    @Test public void namespacesAreStableAndAccountScoped() {
        String a = SettingsRepository.namespaceForUid("uid-a");
        assertEquals(a, SettingsRepository.namespaceForUid("uid-a"));
        assertNotEquals(a, SettingsRepository.namespaceForUid("uid-b"));
    }

    @Test public void surroundingUidWhitespaceIsNormalized() {
        assertEquals(SettingsRepository.namespaceForUid("uid-a"),
                SettingsRepository.namespaceForUid("  uid-a  "));
    }

    @Test public void nullOrBlankUidIsRejected() {
        expectFailure(() -> SettingsRepository.namespaceForUid(null));
        expectFailure(() -> SettingsRepository.namespaceForUid("   "));
    }

    @Test public void careNotificationsDefaultToEnabled() {
        assertTrue(SettingsRepository.defaultCareNotificationsEnabled());
    }

    private static void expectFailure(Runnable action) {
        try { action.run(); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("Expected authentication failure");
    }
}
