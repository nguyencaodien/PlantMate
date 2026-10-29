package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/** How a member confirms account deletion, by sign-in method. */
public class AccountDeletionTest {
    @Test public void googleIsPreferredForLinkedAccounts() {
        assertEquals(AccountRepository.Method.GOOGLE,
                AccountRepository.confirmationMethod(Arrays.asList("firebase", "password", "google.com")));
    }

    @Test public void emailAccountsConfirmWithTheirPassword() {
        assertEquals(AccountRepository.Method.PASSWORD,
                AccountRepository.confirmationMethod(Arrays.asList("firebase", "password")));
    }

    @Test public void otherProvidersNeedAFreshSignIn() {
        assertEquals(AccountRepository.Method.RECENT_SIGN_IN,
                AccountRepository.confirmationMethod(Collections.singletonList("facebook.com")));
    }
}
