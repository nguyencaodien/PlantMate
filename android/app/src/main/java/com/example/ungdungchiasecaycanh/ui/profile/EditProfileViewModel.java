package com.example.ungdungchiasecaycanh.ui.profile;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.ui.auth.AuthViewModel;
import com.example.ungdungchiasecaycanh.data.repository.ImageRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.User;

import android.app.Application;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class EditProfileViewModel extends AndroidViewModel {
    public enum Status { IDLE, LOADING, CONTENT, SAVING, ERROR, SUCCESS }

    public static final class State {
        public final Status status;
        public final User user;
        State(Status status, User user) { this.status = status; this.user = user; }
    }

    public static final class Validation {
        @StringRes public final int displayNameError;
        @StringRes public final int locationError;
        @StringRes public final int bioError;
        Validation(int displayNameError, int locationError, int bioError) {
            this.displayNameError = displayNameError;
            this.locationError = locationError;
            this.bioError = bioError;
        }
        public boolean isValid() { return displayNameError == 0 && locationError == 0 && bioError == 0; }
    }

    private final UserRepository repository = new UserRepository();
    private final ImageRepository images;
    /**
     * Avatar being edited: a stored ref ("", preset:key, pmimg://id) or a local photo
     * (content:// URI) that is uploaded only when the profile is saved. Null = not loaded yet.
     */
    private final MutableLiveData<String> avatar = new MutableLiveData<>(null);

    public EditProfileViewModel(@NonNull Application application) {
        super(application);
        images = new ImageRepository(application);
    }

    public LiveData<String> getAvatar() { return avatar; }

    /** Picked a preset ("preset:key"), a local photo (content URI) or "" for initials. */
    public void setAvatar(String value) {
        State current = state.getValue();
        if (current != null && current.status == Status.SAVING) return;
        avatar.setValue(value == null ? "" : value);
    }

    static boolean isLocalPhoto(String value) {
        return value != null && value.startsWith("content://");
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.IDLE, null));
    private User loadedUser;

    public LiveData<State> getState() { return state; }

    public void load() {
        State current = state.getValue();
        if (current != null && (current.status == Status.LOADING || current.status == Status.CONTENT
                || current.status == Status.SAVING)) return;
        state.setValue(new State(Status.LOADING, null));
        repository.getCurrentProfile()
                .addOnSuccessListener(user -> {
                    loadedUser = user;
                    if (avatar.getValue() == null) avatar.postValue(user.getAvatarUrl() == null ? "" : user.getAvatarUrl());
                    state.postValue(new State(Status.CONTENT, user));
                })
                .addOnFailureListener(error -> state.postValue(new State(Status.ERROR, null)));
    }

    public Validation validate(String displayName, String locationText, String bio) {
        String name = displayName == null ? "" : displayName.trim();
        String location = locationText == null ? "" : locationText.trim();
        String normalizedBio = bio == null ? "" : bio.trim();
        int nameError = name.isEmpty() ? R.string.edit_profile_name_required
                : name.length() > AuthViewModel.DISPLAY_NAME_MAX ? R.string.auth_display_name_too_long : 0;
        int locationError = location.isEmpty() ? R.string.edit_profile_location_required
                : location.length() > 100 ? R.string.edit_profile_location_too_long : 0;
        int bioError = normalizedBio.length() > 500 ? R.string.edit_profile_bio_too_long : 0;
        return new Validation(nameError, locationError, bioError);
    }

    public Validation save(String displayName, String locationText, String bio) {
        State current = state.getValue();
        Validation validation = validate(displayName, locationText, bio);
        if (!validation.isValid() || (current != null && current.status == Status.SAVING)) return validation;
        UserRepository.ProfileInput input = UserRepository.normalizeProfileInput(displayName, locationText, bio);
        state.setValue(new State(Status.SAVING, loadedUser));
        String chosen = avatar.getValue() == null ? (loadedUser == null ? "" : loadedUser.getAvatarUrl()) : avatar.getValue();
        Task<String> avatarRef = isLocalPhoto(chosen)
                ? images.upload(chosen) : Tasks.forResult(chosen == null ? "" : chosen);
        avatarRef.continueWithTask(uploaded -> {
                    if (!uploaded.isSuccessful()) throw uploaded.getException();
                    return repository.updateCurrentUserProfile(input.displayName, input.locationText, input.bio,
                            uploaded.getResult());
                })
                .addOnSuccessListener(ignored -> state.postValue(new State(Status.SUCCESS, loadedUser)))
                .addOnFailureListener(error -> state.postValue(new State(Status.ERROR, loadedUser)));
        return validation;
    }

    public void retryLoad() { state.setValue(new State(Status.IDLE, null)); load(); }
}
