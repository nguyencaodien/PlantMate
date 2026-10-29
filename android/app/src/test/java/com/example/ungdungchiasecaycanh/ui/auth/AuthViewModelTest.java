package com.example.ungdungchiasecaycanh.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;

public class AuthViewModelTest {

    @Test
    public void registrationRejectsBlankDisplayName() {
        AuthViewModel.RegistrationValidation result =
                AuthViewModel.validateRegistration("  ", "person@example.com", "password", "Hà Nội");

        assertEquals(R.string.auth_display_name_required, result.displayNameError);
    }

    @Test
    public void registrationRejectsDisplayNameLongerThanTheRulesAllow() {
        String sixty = new String(new char[AuthViewModel.DISPLAY_NAME_MAX]).replace('\0', 'a');

        assertEquals(0, AuthViewModel.validateRegistration(
                sixty, "person@example.com", "password", "Hà Nội").displayNameError);
        assertEquals(R.string.auth_display_name_too_long, AuthViewModel.validateRegistration(
                sixty + "a", "person@example.com", "password", "Hà Nội").displayNameError);
    }

    @Test
    public void registrationRejectsEmptyEmail() {
        AuthViewModel.RegistrationValidation result =
                AuthViewModel.validateRegistration("Maya", "", "password", "Hà Nội");

        assertEquals(R.string.auth_email_required, result.emailError);
    }

    @Test
    public void registrationRejectsInvalidEmail() {
        AuthViewModel.RegistrationValidation result =
                AuthViewModel.validateRegistration("Maya", "invalid", "password", "Hà Nội");

        assertEquals(R.string.auth_email_invalid, result.emailError);
    }

    @Test
    public void registrationRejectsEmptyPassword() {
        AuthViewModel.RegistrationValidation result =
                AuthViewModel.validateRegistration("Maya", "person@example.com", "", "Hà Nội");

        assertEquals(R.string.auth_password_required, result.passwordError);
    }

    @Test
    public void registrationRejectsShortPassword() {
        AuthViewModel.RegistrationValidation result =
                AuthViewModel.validateRegistration("Maya", "person@example.com", "1234567", "Hà Nội");

        assertEquals(R.string.auth_password_min_length, result.passwordError);
    }

    @Test
    public void registrationAcceptsEightCharacterPassword() {
        AuthViewModel.RegistrationValidation result =
                AuthViewModel.validateRegistration("Maya", "person@example.com", "12345678", "Hà Nội");

        assertTrue(result.isValid());
    }

    @Test
    public void registrationRejectsBlankLocation() {
        AuthViewModel.RegistrationValidation result =
                AuthViewModel.validateRegistration("Maya", "person@example.com", "password", "  ");

        assertEquals(R.string.auth_location_required, result.locationError);
    }

    @Test
    public void registrationRejectsLongLocation() {
        AuthViewModel.RegistrationValidation result =
                AuthViewModel.validateRegistration("Maya", "person@example.com", "password",
                        new String(new char[101]).replace('\0', 'a'));

        assertEquals(R.string.auth_location_too_long, result.locationError);
    }
}
