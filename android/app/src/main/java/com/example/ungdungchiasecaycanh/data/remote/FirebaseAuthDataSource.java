package com.example.ungdungchiasecaycanh.data.remote;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class FirebaseAuthDataSource {

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthDataSource() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public boolean hasAuthenticatedUser() {
        return firebaseAuth.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        return firebaseAuth.getCurrentUser() == null
                ? null : firebaseAuth.getCurrentUser().getUid();
    }

    public Task<AuthResult> signIn(String email, String password) {
        return firebaseAuth.signInWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> register(String email, String password) {
        return firebaseAuth.createUserWithEmailAndPassword(email, password);
    }

    /** Signs in to Firebase with a Google ID token from Credential Manager. */
    public Task<AuthResult> signInWithGoogle(String idToken) {
        return firebaseAuth.signInWithCredential(
                GoogleAuthProvider.getCredential(idToken, null));
    }

    /** Signs in to Firebase with a Facebook Login access token. */
    public Task<AuthResult> signInWithFacebook(String accessToken) {
        return firebaseAuth.signInWithCredential(FacebookAuthProvider.getCredential(accessToken));
    }

    /** Name on the signed-in account, or "" (prefills a Google member's profile). */
    public String getCurrentUserDisplayName() {
        return firebaseAuth.getCurrentUser() == null || firebaseAuth.getCurrentUser().getDisplayName() == null
                ? "" : firebaseAuth.getCurrentUser().getDisplayName();
    }

    /**
     * True when the account has an email Firebase has not confirmed yet (email/password sign-ups,
     * and Facebook accounts). Google accounts are verified by Google and never need this.
     */
    public boolean needsEmailVerification() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null && !user.isEmailVerified() && user.getEmail() != null && !user.getEmail().isEmpty();
    }

    /** Firebase has confirmed the account's email; selling requires it. */
    public boolean isEmailVerified() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    /** Reloads the account and fetches a new ID token, so Firestore rules see a just-confirmed email. */
    public Task<Void> refreshToken() {
        if (firebaseAuth.getCurrentUser() == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        return firebaseAuth.getCurrentUser().reload()
                .onSuccessTask(ignored -> firebaseAuth.getCurrentUser().getIdToken(true))
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return null;
                });
    }

    /** Email of the signed-in account, or "". */
    public String getCurrentUserEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user == null || user.getEmail() == null ? "" : user.getEmail();
    }

    public Task<Void> sendEmailVerification() {
        if (firebaseAuth.getCurrentUser() == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        return firebaseAuth.getCurrentUser().sendEmailVerification();
    }

    /** Refreshes the account (e.g. after the member tapped the link in the verification email). */
    public Task<Void> reloadCurrentUser() {
        if (firebaseAuth.getCurrentUser() == null) return Tasks.forResult(null);
        return firebaseAuth.getCurrentUser().reload();
    }

    public Task<Void> sendPasswordReset(String email) {
        return firebaseAuth.sendPasswordResetEmail(email);
    }

    /** Sign-in methods of the current user ("password", "google.com", "facebook.com"). */
    public java.util.List<String> getProviderIds() {
        java.util.List<String> ids = new java.util.ArrayList<>();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) for (com.google.firebase.auth.UserInfo info : user.getProviderData()) ids.add(info.getProviderId());
        return ids;
    }

    /** When the current user last signed in (0 when unknown); deleting an account needs a recent one. */
    public long getLastSignInMillis() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user == null || user.getMetadata() == null ? 0 : user.getMetadata().getLastSignInTimestamp();
    }

    public Task<Void> reauthenticateWithPassword(String email, String password) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        return user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(email, password));
    }

    public Task<Void> reauthenticateWithGoogle(String idToken) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        return user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null));
    }

    public Task<Void> deleteCurrentUser() {
        if (firebaseAuth.getCurrentUser() == null) {
            return Tasks.forResult(null);
        }
        return firebaseAuth.getCurrentUser().delete();
    }

    public void signOut() {
        firebaseAuth.signOut();
    }
}
