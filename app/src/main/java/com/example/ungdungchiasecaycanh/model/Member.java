package com.example.ungdungchiasecaycanh.model;

/** A member as the admin sees it: the public profile, its UID, whether it is suspended and whether it is an admin. */
public final class Member {
    private final String uid;
    private final User profile;
    private final boolean suspended;
    private final boolean admin;

    public Member(String uid, User profile, boolean suspended) {
        this(uid, profile, suspended, false);
    }

    public Member(String uid, User profile, boolean suspended, boolean admin) {
        this.uid = uid;
        this.profile = profile;
        this.suspended = suspended;
        this.admin = admin;
    }

    public String getUid() { return uid; }
    public User getProfile() { return profile; }
    public boolean isSuspended() { return suspended; }
    public boolean isAdmin() { return admin; }

    public String getDisplayName() {
        String name = profile == null ? null : profile.getDisplayName();
        return name == null ? "" : name;
    }
}
