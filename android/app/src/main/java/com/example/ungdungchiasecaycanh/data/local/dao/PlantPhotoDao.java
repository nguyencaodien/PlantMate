package com.example.ungdungchiasecaycanh.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantPhotoEntity;

import java.util.List;

@Dao
public interface PlantPhotoDao {
    @Insert long insert(PlantPhotoEntity photo);

    /** A plant's photos, newest first, only when the plant belongs to {@code ownerUid}. */
    @Query("SELECT plant_photos.* FROM plant_photos INNER JOIN plants ON plants.id = plant_photos.plantId "
            + "WHERE plant_photos.plantId = :plantId AND plants.ownerUid = :ownerUid ORDER BY plant_photos.createdAt DESC")
    LiveData<List<PlantPhotoEntity>> observeForPlant(long plantId, String ownerUid);

    @Query("SELECT plant_photos.imageUri FROM plant_photos INNER JOIN plants ON plants.id = plant_photos.plantId "
            + "WHERE plant_photos.plantId = :plantId AND plants.ownerUid = :ownerUid")
    List<String> getImageUrisForPlant(long plantId, String ownerUid);

    @Query("SELECT plant_photos.imageUri FROM plant_photos INNER JOIN plants ON plants.id = plant_photos.plantId "
            + "WHERE plants.ownerUid = :ownerUid")
    List<String> getImageUrisForOwner(String ownerUid);

    @Query("SELECT COUNT(*) FROM plant_photos WHERE imageUri = :imageUri AND plantId != :exceptPlantId")
    int countOtherPlantsUsingImage(String imageUri, long exceptPlantId);
}
