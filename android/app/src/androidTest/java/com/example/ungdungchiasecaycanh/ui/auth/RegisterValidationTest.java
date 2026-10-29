package com.example.ungdungchiasecaycanh.ui.auth;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.example.ungdungchiasecaycanh.ui.auth.AuthFormTestSupport.errorOf;
import static com.example.ungdungchiasecaycanh.ui.auth.AuthFormTestSupport.reveal;
import static com.example.ungdungchiasecaycanh.ui.auth.AuthFormTestSupport.text;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Register form validation on the device. Every case stops at validation, so no account is
 * created and nothing is sent to Firebase.
 */
@RunWith(AndroidJUnit4.class)
public class RegisterValidationTest {

    @Test
    public void emptyFormShowsEveryRequiredFieldError() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            onView(withId(R.id.button_create_account)).perform(reveal(), click());

            assertEquals(text(scenario, R.string.auth_display_name_required),
                    errorOf(scenario, R.id.register_display_name_input_layout));
            assertEquals(text(scenario, R.string.auth_email_required),
                    errorOf(scenario, R.id.register_email_input_layout));
            assertEquals(text(scenario, R.string.auth_password_required),
                    errorOf(scenario, R.id.register_password_input_layout));
            assertEquals(text(scenario, R.string.auth_location_required),
                    errorOf(scenario, R.id.register_location_input_layout));
        }
    }

    @Test
    public void invalidEmailAndShortPasswordShowSpecificErrors() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            onView(withId(R.id.register_display_name_input)).perform(reveal(), replaceText("Lan"));
            onView(withId(R.id.register_email_input)).perform(reveal(), replaceText("lan.example.com"));
            onView(withId(R.id.register_password_input)).perform(reveal(), replaceText("short"));
            onView(withId(R.id.register_location_input)).perform(reveal(), replaceText("Quận 3"));
            closeSoftKeyboard();
            onView(withId(R.id.button_create_account)).perform(reveal(), click());

            assertNull(errorOf(scenario, R.id.register_display_name_input_layout));
            assertEquals(text(scenario, R.string.auth_email_invalid),
                    errorOf(scenario, R.id.register_email_input_layout));
            assertEquals(text(scenario, R.string.auth_password_min_length),
                    errorOf(scenario, R.id.register_password_input_layout));
            assertNull(errorOf(scenario, R.id.register_location_input_layout));
        }
    }

    @Test
    public void fixingAFieldClearsItsError() {
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(RegisterActivity.class)) {
            onView(withId(R.id.button_create_account)).perform(reveal(), click());
            onView(withId(R.id.register_display_name_input)).perform(reveal(), replaceText("Lan"));
            closeSoftKeyboard();
            onView(withId(R.id.button_create_account)).perform(reveal(), click());

            assertNull(errorOf(scenario, R.id.register_display_name_input_layout));
            assertEquals(text(scenario, R.string.auth_email_required),
                    errorOf(scenario, R.id.register_email_input_layout));
        }
    }
}
