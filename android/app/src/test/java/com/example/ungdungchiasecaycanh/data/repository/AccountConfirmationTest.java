package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** The way a member proves their identity follows the sign-in providers on the account. */
public class AccountConfirmationTest {
    @Test public void aPasswordAccountConfirmsWithItsPassword() {
        assertEquals(AccountRepository.Method.PASSWORD,
                AccountRepository.confirmationMethod(Collections.singletonList("password")));
    }

    @Test public void aGoogleAccountConfirmsWithGoogle() {
        assertEquals(AccountRepository.Method.GOOGLE,
                AccountRepository.confirmationMethod(Collections.singletonList("google.com")));
    }

    @Test public void aLinkedAccountPrefersGoogle() {
        assertEquals(AccountRepository.Method.GOOGLE,
                AccountRepository.confirmationMethod(Arrays.asList("password", "google.com")));
    }

    @Test public void otherProvidersNeedARecentSignIn() {
        assertEquals(AccountRepository.Method.RECENT_SIGN_IN,
                AccountRepository.confirmationMethod(Collections.singletonList("facebook.com")));
        assertEquals(AccountRepository.Method.RECENT_SIGN_IN,
                AccountRepository.confirmationMethod(Collections.emptyList()));
    }

    @Test public void theRecentWindowEndsBeforeFirebaseRefuses() {
        assertTrue(AccountRepository.RECENT_SIGN_IN_MS < 5 * 60 * 1000L);
    }
}
