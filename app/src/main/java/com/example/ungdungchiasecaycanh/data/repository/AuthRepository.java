package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.notification.NotificationHelper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class AuthRepository {

    private final FirebaseAuthDataSource authDataSource;

    public AuthRepository() {
        authDataSource = new FirebaseAuthDataSource();
    }

    public boolean hasAuthenticatedUser() {
        return authDataSource.hasAuthenticatedUser();
    }

    public String getCurrentUserId() {
        return authDataSource.getCurrentUserId();
    }

    public Task<AuthResult> signIn(String email, String password) {
        return authDataSource.signIn(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return authDataSource.register(email, password);
    }

    public Task<AuthResult> createAccount(String email, String password) {
        return authDataSource.register(email, password);
    }

    public Task<AuthResult> signInWithGoogle(String idToken) {
        return authDataSource.signInWithGoogle(idToken);
    }

    public Task<AuthResult> signInWithFacebook(String accessToken) {
        return authDataSource.signInWithFacebook(accessToken);
    }

    public String getCurrentUserDisplayName() {
        return authDataSource.getCurrentUserDisplayName();
    }

    public boolean needsEmailVerification() {
        return authDataSource.needsEmailVerification();
    }

    public boolean isEmailVerified() {
        return authDataSource.isEmailVerified();
    }

    public Task<Void> refreshToken() {
        return authDataSource.refreshToken();
    }

    public String getCurrentUserEmail() {
        return authDataSource.getCurrentUserEmail();
    }

    public Task<Void> sendEmailVerification() {
        return authDataSource.sendEmailVerification();
    }

    public Task<Void> reloadCurrentUser() {
        return authDataSource.reloadCurrentUser();
    }

    /**
     * Sends a password-reset email. An unknown address also completes successfully, so the
     * app never reveals whether an account exists for an email.
     */
    public Task<Void> sendPasswordReset(String email) {
        return authDataSource.sendPasswordReset(email).continueWithTask(task -> {
            if (task.isSuccessful() || task.getException()
                    instanceof FirebaseAuthInvalidUserException) {
                return Tasks.forResult(null);
            }
            return Tasks.forException(task.getException());
        });
    }

    public Task<Void> deleteCurrentUser() {
        return authDataSource.deleteCurrentUser().addOnSuccessListener(ignored -> clearNotifications());
    }

    /** Signs out and removes the member's reminders still on screen, so the next account never sees them. */
    public void signOut() {
        clearNotifications();
        authDataSource.signOut();
    }

    private static void clearNotifications() {
        NotificationHelper.clearAll(FirebaseApp.getInstance().getApplicationContext());
    }
}
