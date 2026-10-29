package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

public class User {
    private String displayName;
    private String locationText;
    private String avatarUrl;
    private String bio;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    /** Published by the member's app once Firebase confirmed the email; rules tie it to the token. */
    private boolean emailVerified;
    /** Demo checks (2026-10-17): a phone number confirmed with an on-screen code, and the VNeID demo step. */
    private boolean phoneVerified, vneidVerified;

    public User() {
    }

    public User(String displayName, String locationText, String avatarUrl, String bio) {
        this.displayName = displayName;
        this.locationText = locationText;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
    }

    public User(String displayName, String locationText, String avatarUrl, String bio,
                Timestamp createdAt, Timestamp updatedAt) {
        this(displayName, locationText, avatarUrl, bio);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public User(String displayName, String locationText, String avatarUrl, String bio,
                Timestamp createdAt, Timestamp updatedAt, boolean emailVerified) {
        this(displayName, locationText, avatarUrl, bio, createdAt, updatedAt);
        this.emailVerified = emailVerified;
    }

    public User(String displayName, String locationText, String avatarUrl, String bio,
                Timestamp createdAt, Timestamp updatedAt, boolean emailVerified,
                boolean phoneVerified, boolean vneidVerified) {
        this(displayName, locationText, avatarUrl, bio, createdAt, updatedAt, emailVerified);
        this.phoneVerified = phoneVerified;
        this.vneidVerified = vneidVerified;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public boolean isVneidVerified() {
        return vneidVerified;
    }

    /** Selling needs all three checks (the rules enforce the same on the listing). */
    public boolean canSell() {
        return emailVerified && phoneVerified && vneidVerified;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLocationText() {
        return locationText == null ? "" : locationText;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }
}
