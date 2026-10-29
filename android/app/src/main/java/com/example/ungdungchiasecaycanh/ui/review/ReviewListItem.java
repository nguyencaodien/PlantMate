package com.example.ungdungchiasecaycanh.ui.review;

import com.example.ungdungchiasecaycanh.model.Review;

public final class ReviewListItem {
    public final Review review;
    public final String reviewerName;

    public ReviewListItem(Review review, String reviewerName) {
        this.review = review;
        this.reviewerName = reviewerName;
    }
}
