package com.example.ungdungchiasecaycanh;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.fragment.app.Fragment;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.ungdungchiasecaycanh.data.repository.AuthRepository;
import com.example.ungdungchiasecaycanh.ui.auth.SignInActivity;
import com.example.ungdungchiasecaycanh.ui.community.CommunityFragment;
import com.example.ungdungchiasecaycanh.ui.doctor.DoctorFragment;
import com.example.ungdungchiasecaycanh.ui.garden.GardenFragment;
import com.example.ungdungchiasecaycanh.ui.home.HomeFragment;
import com.example.ungdungchiasecaycanh.ui.profile.ProfileFragment;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.ui.auth.OnboardingActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG_HOME = "home";
    private static final String TAG_GARDEN = "garden";
    private static final String TAG_DOCTOR = "doctor";
    private static final String TAG_COMMUNITY = "community";
    private static final String TAG_PROFILE = "profile";

    /** Optional extra: "garden" or "doctor" selects that primary tab (used by Mầm's answers). */
    public static final String EXTRA_SELECT_TAB = "select_tab";
    public static final String TAB_GARDEN = "garden";
    public static final String TAB_HOME = "home";
    public static final String TAB_DOCTOR = "doctor";
    /** A second Back on Home within this window leaves the app. */
    private static final long EXIT_WINDOW_MS = 2000;

    private long lastHomeBackAt;

    private String currentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The tabs are for signed-in members only: without a session, go to sign in instead.
        if (!new AuthRepository().hasAuthenticatedUser()) {
            startActivity(new Intent(this, SignInActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        setContentView(R.layout.activity_main);
        boolean onboarding = savedInstanceState == null && !new SettingsRepository(this).isOnboardingDone();
        if (onboarding) startActivity(new Intent(this, OnboardingActivity.class));

        View root = findViewById(R.id.main);
        View statusScrim = findViewById(R.id.status_scrim);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, 0, bars.right, bars.bottom);
            statusScrim.getLayoutParams().height = bars.top;
            statusScrim.requestLayout();
            return insets;
        });
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(
                        SecondaryChrome.useDarkStatusIcons(this));

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        // The root already pads for the system bars; the bar must not pad again.
        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> insets);
        bottomNavigation.setOnItemSelectedListener(item -> {
            String tag = tagForMenuItem(item.getItemId());
            if (tag == null || tag.equals(currentTag)) {
                return tag != null;
            }
            showFragment(tag);
            View itemView = bottomNavigation.findViewById(item.getItemId());
            if (itemView != null) Motion.pop(itemView, 360, 0);
            return true;
        });
        // The raised round button opens the Doctor tab straight into its camera.
        findViewById(R.id.nav_doctor_button).setOnClickListener(v -> {
            bottomNavigation.setSelectedItemId(R.id.nav_doctor);
            getSupportFragmentManager().setFragmentResult(DoctorFragment.REQUEST_OPEN_CAMERA, Bundle.EMPTY);
        });

        // Back from another tab returns Home; on Home the first Back only warns, the second leaves.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (!TAG_HOME.equals(currentTag)) {
                    bottomNavigation.setSelectedItemId(R.id.nav_home);
                    return;
                }
                long now = SystemClock.elapsedRealtime();
                if (now - lastHomeBackAt <= EXIT_WINDOW_MS) {
                    finish();
                    return;
                }
                lastHomeBackAt = now;
                Toast.makeText(MainActivity.this, R.string.main_back_again_to_exit, Toast.LENGTH_SHORT).show();
            }
        });

        if (savedInstanceState == null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            showFragment(TAG_HOME);
        } else {
            restoreCurrentTag();
        }
        selectTabFrom(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        selectTabFrom(intent);
    }

    private void selectTabFrom(Intent intent) {
        String tab = intent == null ? null : intent.getStringExtra(EXTRA_SELECT_TAB);
        if (tab == null) return;
        intent.removeExtra(EXTRA_SELECT_TAB);
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (TAB_HOME.equals(tab)) nav.setSelectedItemId(R.id.nav_home);
        else if (TAB_GARDEN.equals(tab)) nav.setSelectedItemId(R.id.nav_garden);
        else if (TAB_DOCTOR.equals(tab)) nav.setSelectedItemId(R.id.nav_doctor);
    }

    private void restoreCurrentTag() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_HOME) != null) {
            currentTag = TAG_HOME;
        } else if (getSupportFragmentManager().findFragmentByTag(TAG_GARDEN) != null) {
            currentTag = TAG_GARDEN;
        } else if (getSupportFragmentManager().findFragmentByTag(TAG_DOCTOR) != null) {
            currentTag = TAG_DOCTOR;
        } else if (getSupportFragmentManager().findFragmentByTag(TAG_COMMUNITY) != null) {
            currentTag = TAG_COMMUNITY;
        } else if (getSupportFragmentManager().findFragmentByTag(TAG_PROFILE) != null) {
            currentTag = TAG_PROFILE;
        }
        updateStatusScrim();
    }

    private String tagForMenuItem(int itemId) {
        if (itemId == R.id.nav_home) {
            return TAG_HOME;
        } else if (itemId == R.id.nav_garden) {
            return TAG_GARDEN;
        } else if (itemId == R.id.nav_doctor) {
            return TAG_DOCTOR;
        } else if (itemId == R.id.nav_community) {
            return TAG_COMMUNITY;
        } else if (itemId == R.id.nav_profile) {
            return TAG_PROFILE;
        }
        return null;
    }

    private void showFragment(String tag) {
        Fragment fragment;
        if (TAG_HOME.equals(tag)) {
            fragment = new HomeFragment();
        } else if (TAG_GARDEN.equals(tag)) {
            fragment = new GardenFragment();
        } else if (TAG_DOCTOR.equals(tag)) {
            fragment = new DoctorFragment();
        } else if (TAG_COMMUNITY.equals(tag)) {
            fragment = new CommunityFragment();
        } else {
            fragment = new ProfileFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit();
        currentTag = tag;
        updateStatusScrim();
    }

    /** The status-bar scrim continues the tab's top: every tab opens on the page colour (canvas B). */
    private void updateStatusScrim() {
        findViewById(R.id.status_scrim).setBackgroundColor(ContextCompat.getColor(this, R.color.color_background));
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(SecondaryChrome.useDarkStatusIcons(this));
    }
}
