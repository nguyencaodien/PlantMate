package com.example.ungdungchiasecaycanh.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import java.util.List;

@Dao
public interface PlantDao {
    @Insert long insert(PlantEntity plant);
    @Update int update(PlantEntity plant);

    @Query("SELECT * FROM plants WHERE ownerUid = :ownerUid ORDER BY createdAt DESC")
    LiveData<List<PlantEntity>> observeForOwner(String ownerUid);

    @Query("SELECT * FROM plants WHERE id = :plantId AND ownerUid = :ownerUid LIMIT 1")
    LiveData<PlantEntity> observeByIdForOwner(long plantId, String ownerUid);

    @Query("SELECT * FROM plants WHERE id = :plantId AND ownerUid = :ownerUid LIMIT 1")
    PlantEntity getByIdForOwner(long plantId, String ownerUid);

    @Query("UPDATE plants SET nextWateringAt = :nextWateringAt, updatedAt = :updatedAt WHERE id = :plantId AND ownerUid = :ownerUid")
    int updateWateringScheduleForOwner(long plantId, String ownerUid, Long nextWateringAt, long updatedAt);

    /** Removes the owner's plant; its care logs and photos go with it (CASCADE), Doctor history keeps a null plantId. */
    @Query("DELETE FROM plants WHERE id = :plantId AND ownerUid = :ownerUid")
    int deleteForOwner(long plantId, String ownerUid);

    @Query("SELECT COUNT(*) FROM plants WHERE imageUri = :imageUri AND id != :exceptPlantId")
    int countOtherPlantsUsingImage(String imageUri, long exceptPlantId);

    @Query("SELECT id FROM plants WHERE ownerUid = :ownerUid")
    List<Long> getIdsForOwner(String ownerUid);

    @Query("SELECT imageUri FROM plants WHERE ownerUid = :ownerUid AND imageUri != ''")
    List<String> getImageUrisForOwner(String ownerUid);

    /** Account deletion: every plant of the account (care logs and photos follow by CASCADE). */
    @Query("DELETE FROM plants WHERE ownerUid = :ownerUid")
    int deleteAllForOwner(String ownerUid);

    @Query("SELECT * FROM plants WHERE ownerUid = :ownerUid AND reminderEnabled = 1 ORDER BY nextWateringAt ASC")
    List<PlantEntity> getReminderPlantsForOwner(String ownerUid);
}
