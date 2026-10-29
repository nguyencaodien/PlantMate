package com.example.ungdungchiasecaycanh.ui.review;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.Review;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReviewSummaryTest {
    @Test public void derivesAverageWithoutMutatingSource() {
        List<Review> source = new ArrayList<>(Arrays.asList(
                review(5), review(4), review(3)));
        ReviewSummary summary = ReviewSummary.from(source);
        assertTrue(summary.hasReviews);
        assertEquals(3, summary.reviewCount);
        assertEquals(4.0, summary.averageRating, 0.001);
        assertEquals(3, source.size());
    }

    @Test public void emptyIsExplicitNoReviewsState() {
        ReviewSummary summary = ReviewSummary.from(new ArrayList<>());
        assertFalse(summary.hasReviews);
        assertEquals(0, summary.reviewCount);
    }

    private static Review review(int rating) {
        return new Review("id" + rating, "listing", "reviewer", "reviewee",
                rating, "", null);
    }
}
