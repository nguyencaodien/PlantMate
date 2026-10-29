package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

public class Post {
    private final String postId;
    private final String authorId;
    private final String text;
    private final PostCategory category;
    private final String listingId;
    private final String imageUrl;
    private final Timestamp createdAt;

    public Post(String postId, String authorId, String text, PostCategory category,
                String listingId, String imageUrl, Timestamp createdAt) {
        this.postId = postId; this.authorId = authorId; this.text = text;
        this.category = category; this.listingId = listingId; this.imageUrl = imageUrl;
        this.createdAt = createdAt;
    }
    public String getPostId() { return postId; }
    public String getAuthorId() { return authorId; }
    public String getText() { return text; }
    public PostCategory getCategory() { return category; }
    public String getListingId() { return listingId; }
    public String getImageUrl() { return imageUrl; }
    public Timestamp getCreatedAt() { return createdAt; }
}
