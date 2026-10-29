package com.example.ungdungchiasecaycanh.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "care_logs",
        foreignKeys = @ForeignKey(entity = PlantEntity.class, parentColumns = "id",
                childColumns = "plantId", onDelete = ForeignKey.CASCADE),
        indices = @Index("plantId"))
public class CareLogEntity {
    @PrimaryKey(autoGenerate = true) private long id;
    private long plantId;
    @NonNull private CareType type;
    @NonNull private String note;
    private long createdAt;

    public CareLogEntity(long id, long plantId, @NonNull CareType type,
                         @NonNull String note, long createdAt) {
        this.id = id;
        this.plantId = plantId;
        this.type = type;
        this.note = note;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getPlantId() { return plantId; }
    @NonNull public CareType getType() { return type; }
    @NonNull public String getNote() { return note; }
    public long getCreatedAt() { return createdAt; }
}
