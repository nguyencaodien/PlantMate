package com.example.ungdungchiasecaycanh.data.repository;

import android.content.Context;

import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseAccountDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Account deletion: confirm the member is who they say (recent sign-in), delete their shared data,
 * then everything on this device, then the Firebase Auth account. Each step is safe to repeat.
 */
public class AccountRepository {
    public enum Method { PASSWORD, GOOGLE, RECENT_SIGN_IN }

    /** Firebase asks for a sign-in within about five minutes before an account can be deleted. */
    static final long RECENT_SIGN_IN_MS = 4L * 60 * 1000;

    private static final ExecutorService LOCAL = Executors.newSingleThreadExecutor();

    private final Context context;
    private final FirebaseAuthDataSource auth = new FirebaseAuthDataSource();
    private final FirebaseAccountDataSource remote = new FirebaseAccountDataSource();

    public AccountRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** How the member proves it is them before deletion. */
    public Method confirmationMethod() {
        return confirmationMethod(auth.getProviderIds());
    }

    static Method confirmationMethod(List<String> providers) {
        // Google first: one tap, while a linked account may never have typed its password.
        if (providers.contains("google.com")) return Method.GOOGLE;
        if (providers.contains("password")) return Method.PASSWORD;
        // Facebook (or unknown): no in-app re-authentication; a fresh sign-in is enough.
        return Method.RECENT_SIGN_IN;
    }

    public boolean signedInRecently() {
        return System.currentTimeMillis() - auth.getLastSignInMillis() < RECENT_SIGN_IN_MS;
    }

    public String email() {
        String email = auth.getCurrentUserEmail();
        return email == null ? "" : email;
    }

    public Task<Void> confirmWithPassword(String password) {
        String email = email();
        if (email.isEmpty() || password == null || password.isEmpty())
            return Tasks.forException(new IllegalArgumentException("Password required"));
        return auth.reauthenticateWithPassword(email, password);
    }

    public Task<Void> confirmWithGoogle(String idToken) {
        return auth.reauthenticateWithGoogle(idToken);
    }

    /** Runs after a successful confirmation. The account and its data are gone when it succeeds. */
    public Task<Void> deleteAccount() {
        String uid = auth.getCurrentUserId();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        GardenRepository garden = new GardenRepository(context);
        return remote.deleteMemberData(uid)
                .continueWithTask(deleted -> {
                    if (!deleted.isSuccessful()) throw deleted.getException();
                    return Tasks.call(LOCAL, () -> {
                        garden.deleteAllForOwnerBlocking(uid);
                        context.deleteSharedPreferences(SettingsRepository.namespaceForUid(uid));
                        return null;
                    });
                })
                .continueWithTask(local -> {
                    if (!local.isSuccessful()) throw local.getException();
                    return auth.deleteCurrentUser();
                });
    }
}
