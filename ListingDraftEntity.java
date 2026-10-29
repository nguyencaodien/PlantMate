package com.example.ungdungchiasecaycanh.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * The member's unpublished listing ("Save draft"); one per account, keyed by the account. Lists are
 * stored as text: photo URIs one per line, condition and pick-up times as comma-separated enum names.
 */
@Entity(tableName = "listing_drafts")
public class ListingDraftEntity {
    @PrimaryKey @NonNull private String ownerUid;
    @NonNull private String type;
    @NonNull private String nickname;
    @NonNull private String species;
    @NonNull private String lookingFor;
    private long price;
    @NonNull private String description;
    @NonNull private String locationText;
    @NonNull private String imageUris;
    @NonNull private String conditions;
    private int heightCm;
    @NonNull private String light;
    @NonNull private String pickupTimes;
    private long updatedAt;
    /** Version 10: kind of plant, pot size, age (enum names, "" when not given) and "negotiable". */
    @ColumnInfo(defaultValue = "") @NonNull private String category;
    @ColumnInfo(defaultValue = "0") private int potCm;
    @ColumnInfo(defaultValue = "") @NonNull private String age;
    @ColumnInfo(defaultValue = "0") private boolean negotiable;

    public ListingDraftEntity(@NonNull String ownerUid, @NonNull String type, @NonNull String nickname,
                              @NonNull String species, @NonNull String lookingFor, long price,
                              @NonNull String description, @NonNull String locationText,
                              @NonNull String imageUris, @NonNull String conditions, int heightCm,
                              @NonNull String light, @NonNull String pickupTimes, long updatedAt,
                              @NonNull String category, int potCm, @NonNull String age, boolean negotiable) {
        this.ownerUid = ownerUid;
        this.type = type;
        this.nickname = nickname;
        this.species = species;
        this.lookingFor = lookingFor;
        this.price = price;
        this.description = description;
        this.locationText = locationText;
        this.imageUris = imageUris;
        this.conditions = conditions;
        this.heightCm = heightCm;
        this.light = light;
        this.pickupTimes = pickupTimes;
        this.updatedAt = updatedAt;
        this.category = category;
        this.potCm = potCm;
        this.age = age;
        this.negotiable = negotiable;
    }

    @NonNull public String getOwnerUid() { return ownerUid; }
    @NonNull public String getType() { return type; }
    @NonNull public String getNickname() { return nickname; }
    @NonNull public String getSpecies() { return species; }
    @NonNull public String getLookingFor() { return lookingFor; }
    public long getPrice() { return price; }
    @NonNull public String getDescription() { return description; }
    @NonNull public String getLocationText() { return locationText; }
    @NonNull public String getImageUris() { return imageUris; }
    @NonNull public String getConditions() { return conditions; }
    public int getHeightCm() { return heightCm; }
    @NonNull public String getLight() { return light; }
    @NonNull public String getPickupTimes() { return pickupTimes; }
    public long getUpdatedAt() { return updatedAt; }
    @NonNull public String getCategory() { return category; }
    public int getPotCm() { return potCm; }
    @NonNull public String getAge() { return age; }
    public boolean isNegotiable() { return negotiable; }
}
