package com.example.ungdungchiasecaycanh.ui.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;

public class CompleteProfileViewModelTest {
    @Test public void nameAndAreaAreRequiredAndLimited() {
        assertEquals(R.string.auth_display_name_required, CompleteProfileViewModel.validateName("  "));
        String sixtyOne = new String(new char[AuthViewModel.DISPLAY_NAME_MAX + 1]).replace('\0', 'a');
        assertEquals(R.string.auth_display_name_too_long, CompleteProfileViewModel.validateName(sixtyOne));
        assertEquals(0, CompleteProfileViewModel.validateName("Nguyễn Minh Anh"));
        assertEquals(R.string.auth_location_required, CompleteProfileViewModel.validateLocation(null));
        assertEquals(R.string.auth_location_too_long,
                CompleteProfileViewModel.validateLocation(new String(new char[101]).replace('\0', 'a')));
        assertEquals(0, CompleteProfileViewModel.validateLocation("Quận 7, TP.HCM"));
    }

    @Test public void onlyAMissingProfileAsksForCompletion() {
        assertTrue(AuthViewModel.isMissingProfile(new IllegalStateException("Profile does not exist")));
        assertFalse(AuthViewModel.isMissingProfile(new IllegalStateException("Profile load failed")));
        assertFalse(AuthViewModel.isMissingProfile(new RuntimeException("network")));
    }
}
