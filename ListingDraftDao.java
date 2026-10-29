package com.example.ungdungchiasecaycanh.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.ungdungchiasecaycanh.data.local.entity.ListingDraftEntity;

@Dao
public interface ListingDraftDao {
    @Query("SELECT * FROM listing_drafts WHERE ownerUid = :ownerUid LIMIT 1")
    ListingDraftEntity getForOwner(String ownerUid);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(ListingDraftEntity draft);

    @Query("DELETE FROM listing_drafts WHERE ownerUid = :ownerUid")
    void deleteForOwner(String ownerUid);
}
