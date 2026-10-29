package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseUserDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.example.ungdungchiasecaycanh.model.User;

public class UserRepository {

    private final FirebaseUserDataSource userDataSource;
    private final FirebaseAuthDataSource authDataSource;

    public UserRepository() {
        userDataSource = new FirebaseUserDataSource();
        authDataSource = new FirebaseAuthDataSource();
    }

    public Task<Void> createInitialProfile(String uid, String displayName, String locationText) {
        return userDataSource.createInitialProfile(uid, displayName, locationText);
    }

    public Task<User> getProfile(String uid) {
        return userDataSource.getProfile(uid);
    }

    public Task<User> getCurrentProfile() {
        String uid = currentUid();
        return uid == null ? Tasks.forException(new IllegalStateException("Authentication required"))
                : userDataSource.getProfile(uid);
    }

    public Task<Void> updateCurrentUserProfile(String displayName, String locationText, String bio) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        try {
            ProfileInput input = normalizeProfileInput(displayName, locationText, bio);
            return userDataSource.updateProfile(uid, input.displayName, input.locationText, input.bio);
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
    }

    /** Same as {@link #updateCurrentUserProfile(String, String, String)} plus the avatar ref. */
    public Task<Void> updateCurrentUserProfile(String displayName, String locationText, String bio,
                                               String avatarUrl) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        try {
            ProfileInput input = normalizeProfileInput(displayName, locationText, bio);
            return userDataSource.updateProfile(uid, input.displayName, input.locationText, input.bio,
                    avatarUrl == null ? "" : avatarUrl.trim());
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
    }

    /**
     * Publishes the "email verified" badge once Firebase has confirmed the member's address. The
     * rules accept only what the sign-in token says, so a fresh token is fetched first.
     */
    public Task<Void> syncEmailVerified(User currentProfile) {
        String uid = currentUid();
        if (uid == null || currentProfile == null || currentProfile.isEmailVerified()
                || !authDataSource.isEmailVerified()) return Tasks.forResult(null);
        return authDataSource.refreshToken().onSuccessTask(ignored -> userDataSource.markEmailVerified(uid));
    }

    /** The phone demo check passed (only "verified" is stored, never the number). */
    public Task<Void> markPhoneVerified() {
        String uid = currentUid();
        return uid == null ? Tasks.forException(new IllegalStateException("Authentication required"))
                : userDataSource.markVerified(uid, "phoneVerified");
    }

    /** The VNeID demo step finished. */
    public Task<Void> markVneidVerified() {
        String uid = currentUid();
        return uid == null ? Tasks.forException(new IllegalStateException("Authentication required"))
                : userDataSource.markVerified(uid, "vneidVerified");
    }

    /**
     * The signed-in member's profile after reloading the account, so an email link opened meanwhile is
     * published first (the sell checks need it).
     */
    public Task<User> refreshCurrentProfile() {
        return authDataSource.reloadCurrentUser()
                .continueWithTask(ignored -> getCurrentProfile())
                .onSuccessTask(user -> syncEmailVerified(user).continueWithTask(ignored -> getCurrentProfile()));
    }

    private String currentUid() {
        String uid = authDataSource.getCurrentUserId();
        return uid == null || uid.trim().isEmpty() ? null : uid;
    }

    public static ProfileInput normalizeProfileInput(String displayName, String locationText, String bio) {
        String name = displayName == null ? "" : displayName.trim();
        String location = locationText == null ? "" : locationText.trim();
        String normalizedBio = bio == null ? "" : bio.trim();
        if (name.isEmpty()) throw new IllegalArgumentException("Display name is required");
        if (location.isEmpty()) throw new IllegalArgumentException("Location is required");
        if (location.length() > 100) throw new IllegalArgumentException("Location is too long");
        if (normalizedBio.length() > 500) throw new IllegalArgumentException("Bio is too long");
        return new ProfileInput(name, location, normalizedBio);
    }

    public static final class ProfileInput {
        public final String displayName;
        public final String locationText;
        public final String bio;
        ProfileInput(String displayName, String locationText, String bio) {
            this.displayName = displayName; this.locationText = locationText; this.bio = bio;
        }
    }
}
