package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Listing {
    private final String listingId;
    private final String ownerId;
    private final String nickname;
    private final String species;
    private final String description;
    private final ListingType type;
    private final long price;
    private final String lookingFor;
    private final List<String> imageUrls;
    private final String locationText;
    private final ListingStatus status;
    private final Timestamp createdAt;
    private final Timestamp updatedAt;
    private final ListingDetails details;

    public Listing(String listingId, String ownerId, String nickname, String species,
                   String description, ListingType type, long price, String lookingFor,
                   List<String> imageUrls, String locationText, ListingStatus status,
                   Timestamp createdAt, Timestamp updatedAt) {
        this(listingId, ownerId, nickname, species, description, type, price, lookingFor,
                imageUrls, locationText, status, createdAt, updatedAt, ListingDetails.NONE);
    }

    public Listing(String listingId, String ownerId, String nickname, String species,
                   String description, ListingType type, long price, String lookingFor,
                   List<String> imageUrls, String locationText, ListingStatus status,
                   Timestamp createdAt, Timestamp updatedAt, ListingDetails details) {
        this.listingId = listingId;
        this.ownerId = ownerId;
        this.nickname = nickname;
        this.species = species;
        this.description = description;
        this.type = type;
        this.price = price;
        this.lookingFor = lookingFor;
        this.imageUrls = imageUrls == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(imageUrls));
        this.locationText = locationText;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.details = details == null ? ListingDetails.NONE : details;
    }

    public String getListingId() { return listingId; }
    public String getOwnerId() { return ownerId; }
    public String getNickname() { return nickname; }
    public String getSpecies() { return species; }
    public String getDescription() { return description; }
    public ListingType getType() { return type; }
    public long getPrice() { return price; }
    public String getLookingFor() { return lookingFor; }
    public List<String> getImageUrls() { return imageUrls; }
    public String getCoverImageUrl() {
        return imageUrls.isEmpty() ? null : imageUrls.get(0);
    }
    public String getLocationText() { return locationText; }
    public ListingStatus getStatus() { return status; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    /** Condition, height, light and pick-up times; {@link ListingDetails#NONE} when not given. */
    public ListingDetails getDetails() { return details; }
}
