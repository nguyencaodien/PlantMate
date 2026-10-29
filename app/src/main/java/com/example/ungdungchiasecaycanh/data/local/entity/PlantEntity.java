package com.example.ungdungchiasecaycanh.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "plants")
public class PlantEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @NonNull private String ownerUid;
    @NonNull private String nickname;
    @NonNull private String species;
    @NonNull private String imageUri;
    private boolean reminderEnabled;
    private int wateringIntervalDays;
    private Long nextWateringAt;
    private long createdAt;
    private long updatedAt;
    /** Daily plan: reminder times in minutes after midnight, e.g. "420,1020"; "" = every N days. */
    @ColumnInfo(defaultValue = "") @NonNull private String wateringTimes;
    /** Where the plant lives ("Balcony", "Living room"); "" = not set. Groups the Garden areas. */
    @ColumnInfo(defaultValue = "") @NonNull private String room;
    /** {@link com.example.ungdungchiasecaycanh.model.LightLevel} name, or "" when not set. */
    @ColumnInfo(defaultValue = "") @NonNull private String light;

    public PlantEntity(long id, @NonNull String ownerUid, @NonNull String nickname,
                       @NonNull String species, @NonNull String imageUri,
                       boolean reminderEnabled, int wateringIntervalDays,
                       Long nextWateringAt, long createdAt, long updatedAt,
                       @NonNull String wateringTimes, @NonNull String room, @NonNull String light) {
        this.id = id;
        this.ownerUid = ownerUid;
        this.nickname = nickname;
        this.species = species;
        this.imageUri = imageUri;
        this.reminderEnabled = reminderEnabled;
        this.wateringIntervalDays = wateringIntervalDays;
        this.nextWateringAt = nextWateringAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.wateringTimes = wateringTimes;
        this.room = room;
        this.light = light;
    }

    /** A plant without an area or light (older plants, tests). */
    @Ignore
    public PlantEntity(long id, @NonNull String ownerUid, @NonNull String nickname,
                       @NonNull String species, @NonNull String imageUri,
                       boolean reminderEnabled, int wateringIntervalDays,
                       Long nextWateringAt, long createdAt, long updatedAt,
                       @NonNull String wateringTimes) {
        this(id, ownerUid, nickname, species, imageUri, reminderEnabled, wateringIntervalDays,
                nextWateringAt, createdAt, updatedAt, wateringTimes, "", "");
    }

    /** An every-N-days plant (no daily reminder times). */
    @Ignore
    public PlantEntity(long id, @NonNull String ownerUid, @NonNull String nickname,
                       @NonNull String species, @NonNull String imageUri,
                       boolean reminderEnabled, int wateringIntervalDays,
                       Long nextWateringAt, long createdAt, long updatedAt) {
        this(id, ownerUid, nickname, species, imageUri, reminderEnabled, wateringIntervalDays,
                nextWateringAt, createdAt, updatedAt, "");
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    @NonNull public String getOwnerUid() { return ownerUid; }
    @NonNull public String getNickname() { return nickname; }
    @NonNull public String getSpecies() { return species; }
    @NonNull public String getImageUri() { return imageUri; }
    public boolean isReminderEnabled() { return reminderEnabled; }
    public int getWateringIntervalDays() { return wateringIntervalDays; }
    public Long getNextWateringAt() { return nextWateringAt; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    @NonNull public String getWateringTimes() { return wateringTimes; }
    @NonNull public String getRoom() { return room; }
    @NonNull public String getLight() { return light; }
}
