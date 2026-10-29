package com.example.ungdungchiasecaycanh.ui.auth;

import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.example.ungdungchiasecaycanh.ui.auth.AuthFormTestSupport.errorOf;
import static com.example.ungdungchiasecaycanh.ui.auth.AuthFormTestSupport.reveal;
import static com.example.ungdungchiasecaycanh.ui.auth.AuthFormTestSupport.text;
import static org.junit.Assert.assertEquals;

/**
 * Forgot-password validation on the device. No case submits a valid address, so no reset email
 * is sent.
 */
@RunWith(AndroidJUnit4.class)
public class ForgotPasswordValidationTest {

    @Test
    public void emptyEmailShowsRequiredAndKeepsTheForm() {
        try (ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class)) {
            onView(withId(R.id.forgotEmail)).perform(reveal(), replaceText(""));
            closeSoftKeyboard();
            onView(withId(R.id.forgotSend)).perform(reveal(), click());

            assertEquals(text(scenario, R.string.auth_email_required), errorOf(scenario, R.id.forgotEmailLayout));
            assertEquals(View.VISIBLE, visibility(scenario, R.id.forgotForm));
            assertEquals(View.GONE, visibility(scenario, R.id.forgotSent));
        }
    }

    @Test
    public void malformedEmailShowsInvalid() {
        try (ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(ForgotPasswordActivity.class)) {
            onView(withId(R.id.forgotEmail)).perform(reveal(), replaceText("lan@"));
            closeSoftKeyboard();
            onView(withId(R.id.forgotSend)).perform(reveal(), click());

            assertEquals(text(scenario, R.string.auth_email_invalid), errorOf(scenario, R.id.forgotEmailLayout));
            assertEquals(View.GONE, visibility(scenario, R.id.forgotSent));
        }
    }

    @Test
    public void emailTypedOnSignInIsPrefilledTrimmed() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), ForgotPasswordActivity.class)
                .putExtra(ForgotPasswordActivity.EXTRA_EMAIL, "  lan@example.com ");
        try (ActivityScenario<ForgotPasswordActivity> scenario = ActivityScenario.launch(intent)) {
            AtomicReference<String> typed = new AtomicReference<>();
            scenario.onActivity(activity -> typed.set(
                    ((TextView) activity.findViewById(R.id.forgotEmail)).getText().toString()));
            assertEquals("lan@example.com", typed.get());
        }
    }

    private static int visibility(ActivityScenario<ForgotPasswordActivity> scenario, int id) {
        AtomicInteger value = new AtomicInteger();
        scenario.onActivity(activity -> value.set(activity.findViewById(id).getVisibility()));
        return value.get();
    }
}
