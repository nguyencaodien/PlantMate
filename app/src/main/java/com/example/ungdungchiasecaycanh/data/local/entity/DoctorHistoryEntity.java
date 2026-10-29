package com.example.ungdungchiasecaycanh.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "doctor_history",
        foreignKeys = @ForeignKey(entity = PlantEntity.class, parentColumns = "id",
                childColumns = "plantId", onDelete = ForeignKey.SET_NULL),
        indices = @Index("plantId"))
public class DoctorHistoryEntity {
    @PrimaryKey(autoGenerate = true) private long id;
    @NonNull private String ownerUid;
    private Long plantId;
    @NonNull private String imageUri;
    @NonNull private String diagnosis;
    private Double confidence;
    @NonNull private String guidance;
    private long createdAt;
    /** Identification from the photo (added in database v3; null for older rows). */
    private String plantName;
    private String scientificName;
    private Boolean healthy;
    /** The member's answer to "was this right?" (added in v5; null until answered). */
    private Boolean feedback;

    public DoctorHistoryEntity(long id, @NonNull String ownerUid, Long plantId,
                               @NonNull String imageUri, @NonNull String diagnosis,
                               Double confidence, @NonNull String guidance, long createdAt,
                               String plantName, String scientificName, Boolean healthy) {
        this(id, ownerUid, plantId, imageUri, diagnosis, confidence, guidance, createdAt);
        this.plantName = plantName;
        this.scientificName = scientificName;
        this.healthy = healthy;
    }

    @Ignore
    public DoctorHistoryEntity(long id, @NonNull String ownerUid, Long plantId,
                               @NonNull String imageUri, @NonNull String diagnosis,
                               Double confidence, @NonNull String guidance, long createdAt) {
        this.id = id;
        this.ownerUid = ownerUid;
        this.plantId = plantId;
        this.imageUri = imageUri;
        this.diagnosis = diagnosis;
        this.confidence = confidence;
        this.guidance = guidance;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    @NonNull public String getOwnerUid() { return ownerUid; }
    public Long getPlantId() { return plantId; }
    @NonNull public String getImageUri() { return imageUri; }
    @NonNull public String getDiagnosis() { return diagnosis; }
    public Double getConfidence() { return confidence; }
    @NonNull public String getGuidance() { return guidance; }
    public long getCreatedAt() { return createdAt; }
    public String getPlantName() { return plantName; }
    public String getScientificName() { return scientificName; }
    public Boolean getHealthy() { return healthy; }
    public Boolean getFeedback() { return feedback; }
    public void setFeedback(Boolean feedback) { this.feedback = feedback; }
}
