package com.example.ungdungchiasecaycanh.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;

import java.util.List;

@Dao
public interface CareLogDao {
    @Insert long insert(CareLogEntity careLog);

    @Query("SELECT * FROM care_logs WHERE plantId = :plantId ORDER BY createdAt DESC")
    LiveData<List<CareLogEntity>> observeHistory(long plantId);

    /** The latest care entries of a plant (Plant Doctor context). */
    /** Every care log of the owner's plants since {@code since}, newest first (Garden today/week). */
    @Query("SELECT care_logs.* FROM care_logs INNER JOIN plants ON plants.id = care_logs.plantId "
            + "WHERE plants.ownerUid = :ownerUid AND care_logs.createdAt >= :since ORDER BY care_logs.createdAt DESC")
    LiveData<List<CareLogEntity>> observeForOwnerSince(String ownerUid, long since);

    @Query("SELECT * FROM care_logs WHERE plantId = :plantId ORDER BY createdAt DESC LIMIT :limit")
    List<CareLogEntity> getRecent(long plantId, int limit);

    /** When the plant was watered since {@code since}; used to refuse accidental repeat logs. */
    @Query("SELECT createdAt FROM care_logs WHERE plantId = :plantId AND type = 'WATER' AND createdAt >= :since")
    List<Long> waterTimesSince(long plantId, long since);
}
