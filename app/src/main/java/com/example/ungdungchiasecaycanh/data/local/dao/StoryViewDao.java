package com.example.ungdungchiasecaycanh.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.ungdungchiasecaycanh.data.local.entity.StoryViewEntity;

import java.util.List;

@Dao
public interface StoryViewDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(StoryViewEntity view);

    @Query("SELECT storyId FROM story_views WHERE ownerUid = :ownerUid")
    List<String> getViewedIds(String ownerUid);

    /** Stories live a day; older views are only clutter. */
    @Query("DELETE FROM story_views WHERE ownerUid = :ownerUid")
    void deleteAllForOwner(String ownerUid);

    @Query("DELETE FROM story_views WHERE ownerUid = :ownerUid AND viewedAt < :before")
    void deleteOlderThan(String ownerUid, long before);
}
