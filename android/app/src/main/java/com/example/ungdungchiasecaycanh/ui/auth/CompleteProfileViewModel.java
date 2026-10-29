package com.example.ungdungchiasecaycanh.ui.auth;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.AuthRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;

/** Creates the users/{uid} profile for a member who signed in with Google for the first time. */
public class CompleteProfileViewModel extends ViewModel {
    public enum Status { EDITING, SAVING, DONE, ERROR }

    public static final class State {
        public final Status status;
        @StringRes public final int nameError;
        @StringRes public final int locationError;
        State(Status status, int nameError, int locationError) {
            this.status = status; this.nameError = nameError; this.locationError = locationError;
        }
    }

    private final AuthRepository auth = new AuthRepository();
    private final UserRepository users = new UserRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.EDITING, 0, 0));

    public LiveData<State> getState() { return state; }

    public boolean isSignedIn() { return auth.hasAuthenticatedUser(); }

    /** Name from the Google account, used to prefill the form. */
    public String suggestedName() { return auth.getCurrentUserDisplayName(); }

    public void save(String displayName, String location) {
        State current = state.getValue();
        if (current != null && current.status == Status.SAVING) return;
        String name = displayName == null ? "" : displayName.trim();
        String area = location == null ? "" : location.trim();
        int nameError = validateName(name);
        int locationError = validateLocation(area);
        if (nameError != 0 || locationError != 0) {
            state.setValue(new State(Status.EDITING, nameError, locationError));
            return;
        }
        String uid = auth.getCurrentUserId();
        if (uid == null) {
            state.setValue(new State(Status.ERROR, 0, 0));
            return;
        }
        state.setValue(new State(Status.SAVING, 0, 0));
        users.createInitialProfile(uid, name, area)
                .addOnSuccessListener(ignored -> state.postValue(new State(Status.DONE, 0, 0)))
                .addOnFailureListener(error -> state.postValue(new State(Status.ERROR, 0, 0)));
    }

    /** Leaving without a profile signs the Google account out again. */
    public void cancel() {
        auth.signOut();
    }

    @StringRes
    static int validateName(String name) {
        if (name == null || name.trim().isEmpty()) return R.string.auth_display_name_required;
        return name.trim().length() > AuthViewModel.DISPLAY_NAME_MAX ? R.string.auth_display_name_too_long : 0;
    }

    @StringRes
    static int validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) return R.string.auth_location_required;
        return location.trim().length() > 100 ? R.string.auth_location_too_long : 0;
    }
}
