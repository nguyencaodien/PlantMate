package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

public final class Review {
    private final String reviewId;
    private final String listingId;
    private final String reviewerId;
    private final String revieweeId;
    private final int rating;
    private final String comment;
    private final Timestamp createdAt;

    public Review(String reviewId, String listingId, String reviewerId,
                  String revieweeId, int rating, String comment, Timestamp createdAt) {
        this.reviewId = reviewId;
        this.listingId = listingId;
        this.reviewerId = reviewerId;
        this.revieweeId = revieweeId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public String getReviewId() { return reviewId; }
    public String getListingId() { return listingId; }
    public String getReviewerId() { return reviewerId; }
    public String getRevieweeId() { return revieweeId; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public Timestamp getCreatedAt() { return createdAt; }
}
