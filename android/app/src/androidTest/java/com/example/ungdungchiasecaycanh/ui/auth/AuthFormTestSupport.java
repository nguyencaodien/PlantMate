package com.example.ungdungchiasecaycanh.ui.auth;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import com.google.android.material.textfield.TextInputLayout;

import org.hamcrest.Matcher;

import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

/** Helpers shared by the auth form tests. The forms sit in a NestedScrollView. */
final class AuthFormTestSupport {
    private AuthFormTestSupport() {
    }

    /** Scrolls the nearest scrolling parent until the view is on screen. */
    static ViewAction reveal() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @Override public String getDescription() {
                return "scroll the view on screen";
            }

            @Override public void perform(UiController uiController, View view) {
                view.requestRectangleOnScreen(new Rect(0, 0, view.getWidth(), view.getHeight()), true);
                uiController.loopMainThreadUntilIdle();
            }
        };
    }

    /** The error shown under a text field, or null when there is none. */
    static <A extends Activity> String errorOf(ActivityScenario<A> scenario, int layoutId) {
        AtomicReference<String> error = new AtomicReference<>();
        scenario.onActivity(activity -> {
            CharSequence value = ((TextInputLayout) activity.findViewById(layoutId)).getError();
            error.set(value == null ? null : value.toString());
        });
        return error.get();
    }

    /** A string as the screen shows it (the app may run in Vietnamese or English). */
    static <A extends Activity> String text(ActivityScenario<A> scenario, int stringId) {
        AtomicReference<String> value = new AtomicReference<>();
        scenario.onActivity(activity -> value.set(activity.getString(stringId)));
        return value.get();
    }
}
