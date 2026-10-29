package com.example.ungdungchiasecaycanh.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;

import java.util.List;

@Dao
public interface DoctorHistoryDao {
    @Insert long insertHistory(DoctorHistoryEntity history);

    @Query("SELECT * FROM doctor_history WHERE ownerUid = :ownerUid ORDER BY createdAt DESC")
    LiveData<List<DoctorHistoryEntity>> observeHistoryForOwner(String ownerUid);

    @Query("SELECT * FROM doctor_history WHERE id = :historyId AND ownerUid = :ownerUid LIMIT 1")
    DoctorHistoryEntity getHistoryByIdForOwner(long historyId, String ownerUid);

    /** Whether the plant's latest Plant Doctor check looked healthy; null without a check or a verdict. */
    @Query("SELECT healthy FROM doctor_history WHERE ownerUid = :ownerUid AND plantId = :plantId "
            + "ORDER BY createdAt DESC LIMIT 1")
    LiveData<Boolean> observeLatestHealth(long plantId, String ownerUid);

    /** The plant's latest Plant Doctor check; null without one. */
    @Query("SELECT * FROM doctor_history WHERE ownerUid = :ownerUid AND plantId = :plantId "
            + "ORDER BY createdAt DESC LIMIT 1")
    LiveData<DoctorHistoryEntity> observeLatestCheck(long plantId, String ownerUid);

    /** Remembers the member's "was this right?" answer so the question is asked once. */
    @Query("UPDATE doctor_history SET feedback = :correct WHERE id = :historyId AND ownerUid = :ownerUid")
    int setFeedback(long historyId, String ownerUid, boolean correct);

    @Query("SELECT imageUri FROM doctor_history WHERE ownerUid = :ownerUid")
    List<String> getImageUrisForOwner(String ownerUid);

    @Query("DELETE FROM doctor_history WHERE ownerUid = :ownerUid")
    int deleteAllForOwner(String ownerUid);

    @Query("SELECT COUNT(*) FROM doctor_history WHERE imageUri = :imageUri")
    int countUsingImage(String imageUri);
}
