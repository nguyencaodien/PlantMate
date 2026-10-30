package com.example.ungdungchiasecaycanh.ui.settings;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Each failure tells the member what to do next, wherever in the deletion it happened. */
@RunWith(AndroidJUnit4.class)
public class DeleteAccountErrorsTest {
    private static DeleteAccountViewModel.Confirmation confirming(Exception cause) {
        return new DeleteAccountViewModel.Confirmation(cause);
    }

    @Test public void aWrongPasswordIsReported() {
        assertEquals(R.string.delete_account_wrong_password, DeleteAccountViewModel.messageFor(
                confirming(new FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "Wrong password"))));
    }

    @Test public void anOldSignInAsksToSignInAgain() {
        assertEquals(R.string.delete_account_sign_in_again, DeleteAccountViewModel.messageFor(
                new FirebaseAuthRecentLoginRequiredException("ERROR_REQUIRES_RECENT_LOGIN", "Sign in again")));
    }

    @Test public void beingOfflineIsReportedBeforeAndAfterTheCheck() {
        assertEquals(R.string.delete_account_offline, DeleteAccountViewModel.messageFor(
                confirming(new FirebaseNetworkException("Offline"))));
        assertEquals(R.string.delete_account_offline, DeleteAccountViewModel.messageFor(
                new FirebaseNetworkException("Offline")));
    }
}
