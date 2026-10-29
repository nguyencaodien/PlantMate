package com.example.ungdungchiasecaycanh.ui.auth;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.AuthRepository;

import java.util.regex.Pattern;

/**
 * Sends a Firebase password-reset email. The result never reveals whether an account exists
 * for the address: an unknown address is shown the same "check your inbox" state.
 */
public class PasswordResetViewModel extends ViewModel {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public enum Status { IDLE, SENDING, SENT, ERROR }

    public static final class State {
        public final Status status;
        @StringRes public final int emailError;
        public final String email;

        State(Status status, @StringRes int emailError, String email) {
            this.status = status;
            this.emailError = emailError;
            this.email = email;
        }
    }

    private final AuthRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.IDLE, 0, ""));

    public PasswordResetViewModel() {
        this.repository = new AuthRepository();
    }

    public LiveData<State> getState() {
        return state;
    }

    /** Returns 0 when the address can be used, otherwise the error message. */
    @StringRes
    public static int validateEmail(String email) {
        String normalized = email == null ? "" : email.trim();
        if (normalized.isEmpty()) return R.string.auth_email_required;
        if (!EMAIL_PATTERN.matcher(normalized).matches()) return R.string.auth_email_invalid;
        return 0;
    }

    public void send(String email) {
        State current = state.getValue();
        if (current != null && current.status == Status.SENDING) return;
        String normalized = email == null ? "" : email.trim();
        int error = validateEmail(normalized);
        if (error != 0) {
            state.setValue(new State(Status.IDLE, error, normalized));
            return;
        }
        state.setValue(new State(Status.SENDING, 0, normalized));
        repository.sendPasswordReset(normalized)
                .addOnSuccessListener(ignored -> state.postValue(new State(Status.SENT, 0, normalized)))
                // The repository already treats an unknown address as sent (no account enumeration).
                .addOnFailureListener(failure -> state.postValue(new State(Status.ERROR, 0, normalized)));
    }

    /** Back to the form, keeping the address, so the member can send again. */
    public void reset() {
        State current = state.getValue();
        state.setValue(new State(Status.IDLE, 0, current == null ? "" : current.email));
    }
}
