package com.example.ungdungchiasecaycanh.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.ungdungchiasecaycanh.ui.JourneyTestSupport.LOAD_MS;
import static com.example.ungdungchiasecaycanh.ui.JourneyTestSupport.first;
import static com.example.ungdungchiasecaycanh.ui.JourneyTestSupport.revealAndWaitFor;
import static com.example.ungdungchiasecaycanh.ui.JourneyTestSupport.textOf;
import static com.example.ungdungchiasecaycanh.ui.JourneyTestSupport.waitFor;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ungdungchiasecaycanh.MainActivity;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * The most important journeys, end to end on a signed-in device against the real project. Every
 * journey only reads and navigates: nothing is posted, sent, saved or deleted. Run with the account
 * already signed in; without a session the tests are skipped.
 */
@RunWith(AndroidJUnit4.class)
public class MainJourneysTest {
    private SharedPreferences prefs;
    private boolean onboardingWasDone;

    @Before
    public void signedInWithoutFirstRunSteps() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assumeTrue("Sign in on the device first", user != null);
        Context context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences(SettingsRepository.namespaceForUid(user.getUid()), Context.MODE_PRIVATE);
        onboardingWasDone = prefs.getBoolean(SettingsRepository.KEY_ONBOARDING_DONE, false);
        prefs.edit().putBoolean(SettingsRepository.KEY_ONBOARDING_DONE, true).commit();
    }

    @After
    public void restoreFirstRunSteps() {
        if (prefs != null && !onboardingWasDone) prefs.edit().remove(SettingsRepository.KEY_ONBOARDING_DONE).commit();
    }

    /** Journey 1: exactly five tabs in the fixed order, each opening its own screen. */
    @Test
    public void fiveTabsInOrderEachOpenTheirScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                Menu menu = ((BottomNavigationView) activity.findViewById(R.id.bottom_navigation)).getMenu();
                assertEquals(5, menu.size());
                int[] order = {R.id.nav_home, R.id.nav_garden, R.id.nav_doctor, R.id.nav_community, R.id.nav_profile};
                for (int i = 0; i < order.length; i++) assertEquals(order[i], menu.getItem(i).getItemId());
            });
            waitFor(withId(R.id.home_listing_recycler), LOAD_MS);
            onView(withId(R.id.nav_garden)).perform(click());
            waitFor(withId(R.id.gardenHero), LOAD_MS);
            onView(withId(R.id.nav_doctor)).perform(click());
            waitFor(withId(R.id.doctorScanFrame), LOAD_MS);
            onView(withId(R.id.nav_community)).perform(click());
            waitFor(withId(R.id.communityScroll), LOAD_MS);
            onView(withId(R.id.nav_profile)).perform(click());
            waitFor(withId(R.id.profile_content), LOAD_MS);
            onView(withId(R.id.nav_home)).perform(click());
            waitFor(withId(R.id.home_listing_recycler), LOAD_MS);
        }
    }

    /** Journey 2: a Home give-away opens its own listing (listing ID handed over); Back returns Home. */
    @Test
    public void homeListingOpensItsDetailAndBackReturnsHome() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            waitFor(first(withId(R.id.hero_name)), LOAD_MS);
            String name = textOf(first(withId(R.id.hero_name)));
            onView(first(withId(R.id.hero_name))).perform(click());
            waitFor(allOf(withId(R.id.detail_nickname), withText(name)), LOAD_MS);
            pressBack();
            waitFor(withId(R.id.home_listing_recycler), LOAD_MS);
        }
    }

    /** Journey 3: a Garden plant opens its detail (plant ID handed over); Back returns to the Garden. */
    @Test
    public void gardenPlantOpensItsDetail() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_garden)).perform(click());
            waitFor(withId(R.id.gardenHero), LOAD_MS);
            // The plant grid sits under the week and today's tasks, and lists plants only.
            revealAndWaitFor(withId(R.id.gardenShelf),
                    first(allOf(withId(R.id.ringName), isDescendantOfA(withId(R.id.gardenShelf)))), LOAD_MS);
            String name = textOf(nthRingName(0));
            assumeTrue("Needs at least one plant in the Garden", name != null && !name.isEmpty());
            onView(nthRingName(0)).perform(click());
            waitFor(allOf(withId(R.id.plantDetailNickname), withText(name)), LOAD_MS);
            pressBack();
            // Back keeps the Garden where it was scrolled: the plant grid, not the header.
            revealAndWaitFor(withId(R.id.gardenShelf), withId(R.id.gardenShelf), LOAD_MS);
        }
    }

    /** Journey 4: "Give or trade this plant" opens the listing form filled from that plant. */
    @Test
    public void gardenPlantPrefillsAGiveAwayListing() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_garden)).perform(click());
            revealAndWaitFor(withId(R.id.gardenShelf),
                    first(allOf(withId(R.id.ringName), isDescendantOfA(withId(R.id.gardenShelf)))), LOAD_MS);
            String name = textOf(nthRingName(0));
            onView(nthRingName(0)).perform(click());
            waitFor(withId(R.id.plantDetailNickname), LOAD_MS);
            onView(withId(R.id.plantGiveAway)).perform(JourneyTestSupport.tap());
            waitFor(allOf(withId(R.id.listing_nickname_input), withText(name)), LOAD_MS);
            // Leave without saving a draft or publishing.
            pressBack();
            waitFor(withId(R.id.plantDetailNickname), LOAD_MS);
        }
    }

    /** Journey 5: a Community category tab filters the feed without an error. */
    @Test
    public void communityCategoryFilterLoads() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_community)).perform(click());
            waitFor(withId(R.id.communityList), LOAD_MS);
            onView(allOf(withText(R.string.post_category_question), isDescendantOfA(withId(R.id.communityTabs))))
                    .perform(click());
            waitFor(first(allOf(withId(R.id.postCategory), withText(R.string.post_category_question))), LOAD_MS);
            onView(withId(R.id.communityError)).check(matches(org.hamcrest.Matchers.not(isDisplayed())));
        }
    }

    /** Journey 6: Profile opens Friends; Back returns to Profile, no tab is duplicated. */
    @Test
    public void profileOpensFriendsAndBackReturns() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.nav_profile)).perform(click());
            waitFor(withId(R.id.profile_content), LOAD_MS);
            onView(withId(R.id.profile_friends)).perform(JourneyTestSupport.tap());
            waitFor(withId(R.id.friendsTabFriends), LOAD_MS);
            pressBack();
            waitFor(withId(R.id.profile_content), LOAD_MS);
            onView(withId(R.id.friendsTabFriends)).check(doesNotExist());
        }
    }

    /** The ring name at an adapter position of the Garden shelf (0 is the add tile). */
    private static org.hamcrest.Matcher<android.view.View> nthRingName(int position) {
        return new org.hamcrest.TypeSafeMatcher<android.view.View>() {
            @Override protected boolean matchesSafely(android.view.View view) {
                if (view.getId() != R.id.ringName) return false;
                android.view.View row = view;
                while (row.getParent() instanceof android.view.View
                        && ((android.view.View) row.getParent()).getId() != R.id.gardenShelf) {
                    row = (android.view.View) row.getParent();
                }
                if (!(row.getParent() instanceof androidx.recyclerview.widget.RecyclerView)) return false;
                return ((androidx.recyclerview.widget.RecyclerView) row.getParent()).getChildAdapterPosition(row) == position;
            }

            @Override public void describeTo(org.hamcrest.Description description) {
                description.appendText("Garden shelf ring name at position " + position);
            }
        };
    }
}
