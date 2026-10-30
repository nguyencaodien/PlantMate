package com.example.ungdungchiasecaycanh.ui.settings;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.ungdungchiasecaycanh.R;
import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Opens Settings screens without a session; never confirms or submits account deletion. */
@RunWith(AndroidJUnit4.class)
public class SettingsScreensTest {
    private int originalNightMode;

    @Before
    public void requireSignedOutDevice() {
        originalNightMode = AppCompatDelegate.getDefaultNightMode();
        assumeTrue("Run on a signed-out test device", FirebaseAuth.getInstance().getCurrentUser() == null);
    }

    @After
    public void restoreNightMode() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> AppCompatDelegate.setDefaultNightMode(originalNightMode));
    }

    @Test
    public void aboutOpensInLightTheme() {
        checkAbout(AppCompatDelegate.MODE_NIGHT_NO);
    }

    @Test
    public void aboutOpensInDarkTheme() {
        checkAbout(AppCompatDelegate.MODE_NIGHT_YES);
    }

    private void checkAbout(int mode) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                () -> AppCompatDelegate.setDefaultNightMode(mode));
        try (ActivityScenario<AboutActivity> scenario = ActivityScenario.launch(AboutActivity.class)) {
            onView(withId(R.id.aboutRoot)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> assertFalse(
                    ((TextView) activity.findViewById(R.id.aboutVersion)).getText().toString().trim().isEmpty()));
            onView(withId(R.id.aboutBack)).perform(click());
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void settingsClosesWithoutAuthenticatedSession() {
        try (ActivityScenario<SettingsActivity> scenario = ActivityScenario.launch(SettingsActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void deleteAccountOpensWithoutConfirmingOrDeleting() {
        try (ActivityScenario<DeleteAccountActivity> scenario = ActivityScenario.launch(DeleteAccountActivity.class)) {
            onView(withId(R.id.deleteAccountRoot)).check(matches(isDisplayed()));
            scenario.onActivity(activity -> {
                assertFalse(((CompoundButton) activity.findViewById(R.id.deleteAccountConfirm)).isChecked());
                assertFalse(activity.findViewById(R.id.deleteAccountButton).isEnabled());
            });
            onView(withId(R.id.deleteAccountBack)).perform(click());
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }
}
