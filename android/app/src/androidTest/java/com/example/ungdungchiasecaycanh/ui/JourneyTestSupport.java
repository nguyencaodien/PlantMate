package com.example.ungdungchiasecaycanh.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.graphics.Rect;
import android.view.View;
import android.widget.TextView;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.ViewMatchers;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** Waiting for Firestore-backed screens and picking one view among recycled rows. */
final class JourneyTestSupport {
    static final long LOAD_MS = 15_000;

    private JourneyTestSupport() { }

    /** Polls until a view matching {@code matcher} is displayed (data arrives from the network). */
    static void waitFor(Matcher<View> matcher, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                return;
            } catch (Throwable notYet) {
                if (System.currentTimeMillis() > end) {
                    if (notYet instanceof RuntimeException) throw (RuntimeException) notYet;
                    if (notYet instanceof Error) throw (Error) notYet;
                    throw new AssertionError(notYet);
                }
                try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    /**
     * Scrolls {@code section} into view (it may sit below the fold of a scrolling screen) until a view
     * matching {@code target} is displayed.
     */
    static void revealAndWaitFor(Matcher<View> section, Matcher<View> target, long timeoutMs) {
        long end = System.currentTimeMillis() + timeoutMs;
        while (true) {
            try {
                onView(section).perform(reveal());
                onView(target).check(matches(isDisplayed()));
                return;
            } catch (Throwable notYet) {
                if (System.currentTimeMillis() > end) {
                    if (notYet instanceof RuntimeException) throw (RuntimeException) notYet;
                    if (notYet instanceof Error) throw (Error) notYet;
                    throw new AssertionError(notYet);
                }
                try { Thread.sleep(250); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }
    }

    /** Asks the enclosing scroll view to bring the top of the view on screen. */
    private static ViewAction reveal() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() { return ViewMatchers.isAssignableFrom(View.class); }
            @Override public String getDescription() { return "reveal"; }
            @Override public void perform(UiController controller, View view) {
                view.requestRectangleOnScreen(new Rect(0, 0, view.getWidth(), Math.min(view.getHeight(), 600)), true);
                controller.loopMainThreadUntilIdle();
            }
        };
    }

    /** The first displayed view that matches, for lists where every row has the same ID. */
    static Matcher<View> first(Matcher<View> matcher) {
        return new TypeSafeMatcher<View>() {
            private View found;

            @Override protected boolean matchesSafely(View view) {
                // A retry sees a new hierarchy: forget a row that has since been recycled away.
                if (found != null && (!found.isAttachedToWindow() || !found.isShown())) found = null;
                if (found == null && matcher.matches(view) && view.isShown()) found = view;
                return view == found;
            }

            @Override public void describeTo(Description description) {
                description.appendText("first of ");
                matcher.describeTo(description);
            }
        };
    }

    /**
     * Activates the view the way TalkBack does (performClick), for buttons that a late-loading card
     * can push around or a fixed bottom bar can cover while a coordinate tap is in flight.
     */
    static ViewAction tap() {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() {
                return ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE);
            }
            @Override public String getDescription() { return "performClick"; }
            @Override public void perform(UiController controller, View view) {
                view.performClick();
                controller.loopMainThreadUntilIdle();
            }
        };
    }

    /** Reads the text of the matched TextView. */
    static String textOf(Matcher<View> matcher) {
        String[] out = new String[1];
        onView(matcher).perform(new ViewAction() {
            @Override public Matcher<View> getConstraints() { return ViewMatchers.isAssignableFrom(TextView.class); }
            @Override public String getDescription() { return "read text"; }
            @Override public void perform(UiController controller, View view) {
                out[0] = ((TextView) view).getText().toString();
            }
        });
        return out[0];
    }
}
