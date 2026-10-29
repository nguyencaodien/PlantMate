package com.example.ungdungchiasecaycanh.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.AccountRepository;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;

/** Confirm, then delete the account; the state survives rotation while deletion runs. */
public class DeleteAccountViewModel extends AndroidViewModel {
    public enum Phase { READY, WORKING, DONE }

    public static final class State {
        public final Phase phase;
        /** String resource of the last error, or 0. */
        public final int error;
        State(Phase phase, int error) { this.phase = phase; this.error = error; }
    }

    private final AccountRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Phase.READY, 0));

    public DeleteAccountViewModel(@NonNull Application application) {
        super(application);
        repository = new AccountRepository(application);
    }

    public LiveData<State> getState() { return state; }
    public AccountRepository.Method method() { return repository.confirmationMethod(); }
    public boolean signedInRecently() { return repository.signedInRecently(); }
    public String email() { return repository.email(); }

    public void deleteWithPassword(String password) {
        if (password == null || password.isEmpty()) { fail(R.string.delete_account_password_required); return; }
        start();
        run(repository.confirmWithPassword(password));
    }

    public void deleteWithGoogle(String idToken) {
        start();
        run(repository.confirmWithGoogle(idToken));
    }

    /** Accounts without in-app re-authentication (Facebook): only right after signing in. */
    public void deleteAfterRecentSignIn() {
        if (!repository.signedInRecently()) { fail(R.string.delete_account_sign_in_again); return; }
        start();
        run(com.google.android.gms.tasks.Tasks.forResult(null));
    }

    public void googleCancelled(int message) {
        if (message != 0) fail(message);
    }

    private void run(Task<Void> confirmed) {
        confirmed.continueWithTask(task -> {
            if (!task.isSuccessful()) throw new Confirmation(task.getException());
            return repository.deleteAccount();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) state.setValue(new State(Phase.DONE, 0));
            else fail(messageFor(task.getException()));
        });
    }

    static int messageFor(Exception error) {
        boolean confirming = error instanceof Confirmation;
        Throwable cause = confirming && error.getCause() != null ? error.getCause() : error;
        if (cause instanceof FirebaseAuthInvalidCredentialsException) return R.string.delete_account_wrong_password;
        if (cause instanceof FirebaseAuthRecentLoginRequiredException) return R.string.delete_account_sign_in_again;
        if (cause instanceof FirebaseNetworkException) return R.string.delete_account_offline;
        return confirming ? R.string.delete_account_confirm_failed : R.string.delete_account_failed;
    }

    private void start() { state.setValue(new State(Phase.WORKING, 0)); }

    private void fail(int message) { state.setValue(new State(Phase.READY, message)); }

    /** The identity check failed: nothing was deleted. */
    static final class Confirmation extends Exception {
        Confirmation(Exception cause) { super(cause); }
    }
}
