package com.example.ungdungchiasecaycanh.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReputationTest {
    @Test public void newMemberWithoutHistoryIsNew() {
        Reputation r = Reputation.of(0, 0, 0, 3);
        assertEquals(Reputation.Level.NEW, r.level);
        assertTrue(r.score < 10);
    }

    @Test public void oneGoodReviewOrTwoRehomedPlantsIsTrusted() {
        assertEquals(Reputation.Level.TRUSTED, Reputation.of(5, 1, 0, 10).level);
        assertEquals(Reputation.Level.TRUSTED, Reputation.of(0, 0, 2, 10).level);
    }

    @Test public void manyExcellentReviewsAndExchangesIsTop() {
        Reputation r = Reputation.of(4.8, 6, 4, 200, Reputation.VERIFIED_MAX);
        assertEquals(Reputation.Level.TOP, r.level);
        assertTrue(r.score >= 85);
    }

    @Test public void repeatedLowRatingsNeedCaution() {
        assertEquals(Reputation.Level.CAUTION, Reputation.of(2.3, 4, 5, 400).level);
    }

    @Test public void scoreStaysWithinBounds() {
        assertEquals(100, Reputation.of(5, 50, 50, 5000, 99).score);
        assertEquals(75, Reputation.of(5, 50, 50, 5000).score); // history alone tops out at 75
        assertEquals(0, Reputation.of(-3, -1, -2, -9).score);
    }

    @Test public void verificationAddsUpToTwentyFivePoints() {
        assertEquals(25, Reputation.verifiedPoints(true, true, true, true));
        assertEquals(15, Reputation.verifiedPoints(true, true, false, false));
        assertEquals(22, Reputation.of(0, 0, 0, 0, Reputation.verifiedPoints(true, true, true, false)).score);
    }
}
