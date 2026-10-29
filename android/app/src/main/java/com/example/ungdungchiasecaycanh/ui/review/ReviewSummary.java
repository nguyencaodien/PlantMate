package com.example.ungdungchiasecaycanh.ui.review;

import com.example.ungdungchiasecaycanh.model.Review;

import java.util.List;

public final class ReviewSummary {
    public final int reviewCount;
    public final double averageRating;
    public final boolean hasReviews;

    private ReviewSummary(int count, double average) {
        reviewCount = count;
        averageRating = average;
        hasReviews = count > 0;
    }

    public static ReviewSummary from(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) return new ReviewSummary(0, 0);
        int sum = 0;
        for (Review review : reviews) sum += review.getRating();
        return new ReviewSummary(reviews.size(), (double) sum / reviews.size());
    }
}
