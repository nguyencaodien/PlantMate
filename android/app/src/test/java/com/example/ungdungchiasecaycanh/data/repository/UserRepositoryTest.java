package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UserRepositoryTest {
    @Test public void profileInputTrimsCanonicalValues() {
        UserRepository.ProfileInput input = UserRepository.normalizeProfileInput(
                "  Maya Lin  ", "  Quận 7, TP.HCM  ", "  Plant collector  ");
        assertEquals("Maya Lin", input.displayName);
        assertEquals("Quận 7, TP.HCM", input.locationText);
        assertEquals("Plant collector", input.bio);
    }

    @Test public void blankBioNormalizesToEmpty() {
        assertEquals("", UserRepository.normalizeProfileInput("Maya", "Hà Nội", "   ").bio);
    }

    @Test public void locationBoundaryIsEnforced() {
        UserRepository.normalizeProfileInput("Maya", repeat('a', 100), "");
        expectFailure(() -> UserRepository.normalizeProfileInput("Maya", "   ", ""));
        expectFailure(() -> UserRepository.normalizeProfileInput("Maya", repeat('a', 101), ""));
    }

    @Test public void bioBoundaryIsEnforced() {
        UserRepository.normalizeProfileInput("Maya", "Hà Nội", repeat('a', 500));
        expectFailure(() -> UserRepository.normalizeProfileInput("Maya", "Hà Nội", repeat('a', 501)));
    }

    @Test public void displayNameUsesRegistrationRequiredConstraint() {
        expectFailure(() -> UserRepository.normalizeProfileInput("   ", "Hà Nội", ""));
    }

    private static void expectFailure(Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected validation failure");
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }
}
