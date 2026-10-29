package com.example.ungdungchiasecaycanh.ui.auth;

import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.AuthRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

import java.util.function.Supplier;
import java.util.regex.Pattern;

public class AuthViewModel extends ViewModel {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    public enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        /** Signed in with Google but no PlantMate profile yet: ask for the area. */
        NEEDS_PROFILE,
        ERROR
    }

    public enum Operation {
        SIGN_IN,
        REGISTER,
        GOOGLE,
        FACEBOOK
    }

    public static final class AuthUiState {
        public final Status status;
        public final Operation operation;
        @StringRes
        public final int displayNameError;
        @StringRes
        public final int emailError;
        @StringRes
        public final int passwordError;
        @StringRes
        public final int locationError;
        @StringRes
        public final int message;

        private AuthUiState(Status status, Operation operation, @StringRes int displayNameError,
                            @StringRes int emailError,
                            @StringRes int passwordError, @StringRes int locationError,
                            @StringRes int message) {
            this.status = status;
            this.operation = operation;
            this.displayNameError = displayNameError;
            this.emailError = emailError;
            this.passwordError = passwordError;
            this.locationError = locationError;
            this.message = message;
        }
    }

    public static final class RegistrationValidation {
        @StringRes
        public final int emailError;
        @StringRes
        public final int displayNameError;
        @StringRes
        public final int passwordError;
        @StringRes
        public final int locationError;

        private RegistrationValidation(@StringRes int displayNameError,
                                       @StringRes int emailError,
                                       @StringRes int passwordError,
                                       @StringRes int locationError) {
            this.displayNameError = displayNameError;
            this.emailError = emailError;
            this.passwordError = passwordError;
            this.locationError = locationError;
        }

        public boolean isValid() {
            return displayNameError == 0 && emailError == 0
                    && passwordError == 0 && locationError == 0;
        }
    }

    private final AuthRepository repository;
    private final UserRepository userRepository;
    private final MutableLiveData<AuthUiState> authState = new MutableLiveData<>(idleState());

    public AuthViewModel() {
        repository = new AuthRepository();
        userRepository = new UserRepository();
    }

    public LiveData<AuthUiState> getAuthState() {
        return authState;
    }

    public void checkExistingSession() {
        if (!repository.hasAuthenticatedUser()) return;
        // A Google sign-in may have stopped before the profile was created.
        userRepository.getProfile(repository.getCurrentUserId())
                .addOnSuccessListener(profile -> authState.postValue(successState(Operation.SIGN_IN)))
                .addOnFailureListener(error -> authState.postValue(isMissingProfile(error)
                        ? new AuthUiState(Status.NEEDS_PROFILE, Operation.GOOGLE, 0, 0, 0, 0, 0)
                        : successState(Operation.SIGN_IN)));
    }

    /** Google flow step 2: Firebase sign-in with the ID token, then check for a profile. */
    public void signInWithGoogle(String idToken) {
        signInWithProvider(Operation.GOOGLE, idToken, () -> repository.signInWithGoogle(idToken));
    }

    /** Facebook flow step 2: Firebase sign-in with the access token, then check for a profile. */
    public void signInWithFacebook(String accessToken) {
        signInWithProvider(Operation.FACEBOOK, accessToken, () -> repository.signInWithFacebook(accessToken));
    }

    private void signInWithProvider(Operation operation, String token, Supplier<Task<AuthResult>> signIn) {
        if (authState.getValue() != null && authState.getValue().status == Status.LOADING) return;
        if (token == null || token.isEmpty()) {
            authState.setValue(errorState(operation));
            return;
        }
        authState.setValue(new AuthUiState(Status.LOADING, operation, 0, 0, 0, 0, 0));
        signIn.get()
                .addOnSuccessListener(result -> userRepository.getProfile(repository.getCurrentUserId())
                        .addOnSuccessListener(profile -> authState.postValue(successState(operation)))
                        .addOnFailureListener(error -> {
                            if (isMissingProfile(error)) {
                                authState.postValue(new AuthUiState(Status.NEEDS_PROFILE, operation, 0, 0, 0, 0, 0));
                            } else {
                                repository.signOut();
                                authState.postValue(errorState(operation));
                            }
                        }))
                .addOnFailureListener(error -> authState.postValue(
                        error instanceof FirebaseAuthUserCollisionException
                                // The email already belongs to a PlantMate account with another sign-in.
                                ? new AuthUiState(Status.ERROR, operation, 0, 0, 0, 0, R.string.auth_account_exists_other_method)
                                : errorState(operation)));
    }

    /** Google sign-in was cancelled or failed before reaching Firebase. */
    public void googleFailed(@StringRes int message) {
        authState.setValue(message == 0 ? idleState()
                : new AuthUiState(Status.ERROR, Operation.GOOGLE, 0, 0, 0, 0, message));
    }

    /** Facebook Login was cancelled (0: no message) or failed before reaching Firebase. */
    public void facebookFailed(@StringRes int message) {
        authState.setValue(message == 0 ? idleState()
                : new AuthUiState(Status.ERROR, Operation.FACEBOOK, 0, 0, 0, 0, message));
    }

    /** After the NEEDS_PROFILE hand-off was consumed, so a recreated Activity does not repeat it. */
    public void profileRequestHandled() {
        authState.setValue(idleState());
    }

    static boolean isMissingProfile(Exception error) {
        return error instanceof IllegalStateException && error.getMessage() != null
                && error.getMessage().contains("does not exist");
    }

    public void signIn(String email, String password) {
        if (authState.getValue() != null && authState.getValue().status == Status.LOADING) {
            return;
        }

        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedPassword = password == null ? "" : password;
        int emailError = 0;
        int passwordError = 0;

        if (normalizedEmail.isEmpty()) {
            emailError = R.string.auth_email_required;
        } else if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            emailError = R.string.auth_email_invalid;
        }
        if (normalizedPassword.isEmpty()) {
            passwordError = R.string.auth_password_required;
        }

        if (emailError != 0 || passwordError != 0) {
            authState.setValue(new AuthUiState(Status.ERROR, Operation.SIGN_IN,
                    0, emailError, passwordError, 0, 0));
            return;
        }

        authState.setValue(new AuthUiState(Status.LOADING, Operation.SIGN_IN, 0, 0, 0, 0, 0));
        repository.signIn(normalizedEmail, normalizedPassword)
                .addOnSuccessListener(result -> authState.postValue(successState(Operation.SIGN_IN)))
                .addOnFailureListener(error -> authState.postValue(errorState(Operation.SIGN_IN)));
    }

    public void register(String displayName, String email, String password, String locationText) {
        if (authState.getValue() != null && authState.getValue().status == Status.LOADING) {
            return;
        }

        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedPassword = password == null ? "" : password;
        String normalizedLocation = locationText == null ? "" : locationText.trim();
        RegistrationValidation validation = validateRegistration(
                normalizedDisplayName, normalizedEmail, normalizedPassword, normalizedLocation);

        if (!validation.isValid()) {
            authState.setValue(new AuthUiState(Status.ERROR, Operation.REGISTER,
                    validation.displayNameError, validation.emailError,
                    validation.passwordError, validation.locationError, 0));
            return;
        }

        authState.setValue(new AuthUiState(Status.LOADING, Operation.REGISTER, 0, 0, 0, 0, 0));
        repository.createAccount(normalizedEmail, normalizedPassword)
                .addOnSuccessListener(result -> {
                    if (result.getUser() == null) {
                        authState.postValue(errorState(Operation.REGISTER));
                        return;
                    }
                    userRepository.createInitialProfile(result.getUser().getUid(),
                                    normalizedDisplayName, normalizedLocation)
                            .addOnSuccessListener(ignored -> {
                                repository.sendEmailVerification();
                                authState.postValue(successState(Operation.REGISTER));
                            })
                            .addOnFailureListener(error -> rollbackAfterProfileFailure());
                })
                .addOnFailureListener(error -> authState.postValue(errorState(Operation.REGISTER)));
    }

    private void rollbackAfterProfileFailure() {
        repository.deleteCurrentUser().addOnCompleteListener(ignored -> {
            repository.signOut();
            authState.postValue(profileSetupErrorState());
        });
    }

    /** Longest display name; firestore.rules rejects longer ones on users/{uid}. */
    public static final int DISPLAY_NAME_MAX = 60;

    public static RegistrationValidation validateRegistration(
            String displayName, String email, String password, String locationText) {
        String normalizedDisplayName = displayName == null ? "" : displayName.trim();
        String normalizedEmail = email == null ? "" : email.trim();
        String normalizedPassword = password == null ? "" : password;
        String normalizedLocation = locationText == null ? "" : locationText.trim();
        int displayNameError = normalizedDisplayName.isEmpty() ? R.string.auth_display_name_required
                : normalizedDisplayName.length() > DISPLAY_NAME_MAX ? R.string.auth_display_name_too_long : 0;
        int emailError = 0;
        int passwordError = 0;
        int locationError = 0;

        if (normalizedEmail.isEmpty()) {
            emailError = R.string.auth_email_required;
        } else if (!EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            emailError = R.string.auth_email_invalid;
        }
        if (normalizedPassword.isEmpty()) {
            passwordError = R.string.auth_password_required;
        } else if (normalizedPassword.length() < 8) {
            passwordError = R.string.auth_password_min_length;
        }
        if (normalizedLocation.isEmpty()) {
            locationError = R.string.auth_location_required;
        } else if (normalizedLocation.length() > 100) {
            locationError = R.string.auth_location_too_long;
        }
        return new RegistrationValidation(displayNameError, emailError, passwordError, locationError);
    }

    private static AuthUiState idleState() {
        return new AuthUiState(Status.IDLE, Operation.SIGN_IN, 0, 0, 0, 0, 0);
    }

    private static AuthUiState successState(Operation operation) {
        return new AuthUiState(Status.SUCCESS, operation, 0, 0, 0, 0, 0);
    }

    private static AuthUiState errorState(Operation operation) {
        int message = operation == Operation.REGISTER ? R.string.auth_register_error
                : operation == Operation.GOOGLE ? R.string.auth_google_error
                : operation == Operation.FACEBOOK ? R.string.auth_facebook_error
                : R.string.auth_sign_in_error;
        return new AuthUiState(Status.ERROR, operation, 0, 0, 0, 0, message);
    }

    private static AuthUiState profileSetupErrorState() {
        return new AuthUiState(Status.ERROR, Operation.REGISTER, 0, 0, 0, 0,
                R.string.auth_profile_setup_error);
    }
}
