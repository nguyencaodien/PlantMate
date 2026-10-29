package com.example.ungdungchiasecaycanh.ui.auth;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;

public class PasswordResetViewModelTest {

    @Test
    public void emptyEmailIsRequired() {
        assertEquals(R.string.auth_email_required, PasswordResetViewModel.validateEmail("   "));
    }

    @Test
    public void nullEmailIsRequired() {
        assertEquals(R.string.auth_email_required, PasswordResetViewModel.validateEmail(null));
    }

    @Test
    public void malformedEmailIsInvalid() {
        assertEquals(R.string.auth_email_invalid, PasswordResetViewModel.validateEmail("plant-lover"));
    }

    @Test
    public void validEmailWithSpacesIsAccepted() {
        assertEquals(0, PasswordResetViewModel.validateEmail("  person@example.com "));
    }
}
