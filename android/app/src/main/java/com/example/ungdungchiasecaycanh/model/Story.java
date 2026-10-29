package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

/**
 * A "green moment" (canvas Community "Khoảnh khắc xanh"): one photo with a short title and an
 * optional caption, seen by the author and their friends for {@link #LIFETIME_MS} after posting.
 */
public final class Story {
    public static final long LIFETIME_MS = 24L * 60 * 60 * 1000;
    public static final int TITLE_MAX = 24;
    public static final int CAPTION_MAX = 200;

    private final String storyId;
    private final String authorId;
    private final String title;
    private final String caption;
    private final String imageUrl;
    private final Timestamp createdAt;

    public Story(String storyId, String authorId, String title, String caption, String imageUrl,
                 Timestamp createdAt) {
        this.storyId = storyId;
        this.authorId = authorId;
        this.title = title == null ? "" : title;
        this.caption = caption == null ? "" : caption;
        this.imageUrl = imageUrl == null ? "" : imageUrl;
        this.createdAt = createdAt;
    }

    public String getStoryId() { return storyId; }
    public String getAuthorId() { return authorId; }
    public String getTitle() { return title; }
    public String getCaption() { return caption; }
    public String getImageUrl() { return imageUrl; }
    public Timestamp getCreatedAt() { return createdAt; }

    public long createdMillis() { return createdAt == null ? 0 : createdAt.toDate().getTime(); }

    /** Still shown at {@code nowMs}: posted within the last 24 hours (a pending write counts as now). */
    public boolean isActive(long nowMs) {
        return createdAt == null || nowMs - createdMillis() < LIFETIME_MS;
    }
}
