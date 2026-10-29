package com.example.ungdungchiasecaycanh.ui.profile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VerifyViewModelTest {

    @Test public void acceptsVietnameseMobileNumbers() {
        assertTrue(VerifyViewModel.isPhone("0912345678"));
        assertTrue(VerifyViewModel.isPhone("+84 912 345 678"));
        assertTrue(VerifyViewModel.isPhone("038.123.4567"));
    }

    @Test public void rejectsOtherInput() {
        assertFalse(VerifyViewModel.isPhone(null));
        assertFalse(VerifyViewModel.isPhone(""));
        assertFalse(VerifyViewModel.isPhone("091234567"));
        assertFalse(VerifyViewModel.isPhone("0212345678"));
        assertFalse(VerifyViewModel.isPhone("09123456789"));
    }
}
