package com.example.ungdungchiasecaycanh.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** One photo on a plant's "photos over time" strip; removed with its plant. */
@Entity(tableName = "plant_photos",
        foreignKeys = @ForeignKey(entity = PlantEntity.class, parentColumns = "id",
                childColumns = "plantId", onDelete = ForeignKey.CASCADE),
        indices = @Index("plantId"))
public class PlantPhotoEntity {
    @PrimaryKey(autoGenerate = true) private long id;
    private long plantId;
    /** App-private file URI written by the photo cropper. */
    @NonNull private String imageUri;
    private long createdAt;

    public PlantPhotoEntity(long id, long plantId, @NonNull String imageUri, long createdAt) {
        this.id = id;
        this.plantId = plantId;
        this.imageUri = imageUri;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getPlantId() { return plantId; }
    @NonNull public String getImageUri() { return imageUri; }
    public long getCreatedAt() { return createdAt; }
}
