package com.example.ungdungchiasecaycanh.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;

/** A story this account has opened (the Community strip greys its ring). */
@Entity(tableName = "story_views", primaryKeys = {"ownerUid", "storyId"})
public class StoryViewEntity {
    @NonNull private final String ownerUid;
    @NonNull private final String storyId;
    private final long viewedAt;

    public StoryViewEntity(@NonNull String ownerUid, @NonNull String storyId, long viewedAt) {
        this.ownerUid = ownerUid;
        this.storyId = storyId;
        this.viewedAt = viewedAt;
    }

    @NonNull public String getOwnerUid() { return ownerUid; }
    @NonNull public String getStoryId() { return storyId; }
    public long getViewedAt() { return viewedAt; }
}
