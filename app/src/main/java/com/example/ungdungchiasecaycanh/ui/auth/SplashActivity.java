package com.example.ungdungchiasecaycanh.ui.auth;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.MainActivity;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.AuthRepository;
import com.example.ungdungchiasecaycanh.util.Motion;

/**
 * The app's launcher: the logo intro plays while the saved session is checked, then Home (signed in), Complete
 * profile (signed in without a profile) or Sign in opens. A cold start is the only time it runs: returning
 * from the background resumes the open screen, and signing in goes straight to Home.
 */
public class SplashActivity extends AppCompatActivity {
    /** The full intro; with reduced motion the still final frame shows this long. */
    static final long SHOW_MS = 1_600, STILL_MS = 400;
    /** A slow profile check does not hold the launch longer than this. */
    static final long MAX_WAIT_MS = 4_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Class<?> next;
    private boolean shownEnough, routed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        // Deep green in both themes: light system bar icons.
        WindowInsetsControllerCompat bars = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(false);
        bars.setAppearanceLightNavigationBars(false);

        boolean motion = Motion.enabled(this);
        if (motion) playIntro();
        handler.postDelayed(() -> { shownEnough = true; route(); }, motion ? SHOW_MS : STILL_MS);

        if (!new AuthRepository().hasAuthenticatedUser()) {
            next = SignInActivity.class;
            return;
        }
        AuthViewModel auth = new ViewModelProvider(this).get(AuthViewModel.class);
        auth.getAuthState().observe(this, state -> {
            if (state.status == AuthViewModel.Status.NEEDS_PROFILE) next = CompleteProfileActivity.class;
            else if (state.status == AuthViewModel.Status.SUCCESS) next = MainActivity.class;
            route();
        });
        auth.checkExistingSession();
        handler.postDelayed(() -> {
            if (next == null) next = MainActivity.class;
            route();
        }, MAX_WAIT_MS);
    }

    /** The ring draws, the logo blooms with its glow, the name closes in from wide spacing, the line rises. */
    private void playIntro() {
        Drawable ring = ((ImageView) findViewById(R.id.splash_ring)).getDrawable();
        if (ring instanceof Animatable) ((Animatable) ring).start();

        View logo = findViewById(R.id.splash_logo);
        logo.setAlpha(0f);
        logo.setScaleX(0.7f);
        logo.setScaleY(0.7f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(250).setDuration(800)
                .setInterpolator(new OvershootInterpolator(1.4f)).start();

        View glow = findViewById(R.id.splash_glow);
        glow.setAlpha(0f);
        glow.setScaleX(0.6f);
        glow.setScaleY(0.6f);
        glow.animate().alpha(0.8f).scaleX(1f).scaleY(1f).setStartDelay(300).setDuration(1400)
                .setInterpolator(Motion.EASE_OUT).start();

        TextView name = findViewById(R.id.splash_name);
        float spacing = name.getLetterSpacing();
        name.setAlpha(0f);
        ValueAnimator track = ValueAnimator.ofFloat(0.25f, spacing);
        track.setStartDelay(800);
        track.setDuration(700);
        track.setInterpolator(Motion.EASE_OUT);
        track.addUpdateListener(a -> {
            name.setLetterSpacing((float) a.getAnimatedValue());
            name.setAlpha(a.getAnimatedFraction());
        });
        track.start();

        Motion.rise(findViewById(R.id.splash_tagline), 1000);
    }

    /** Opens the next screen once the intro has been seen and the session is known. */
    private void route() {
        if (routed || !shownEnough || next == null || isFinishing()) return;
        routed = true;
        startActivity(new Intent(this, next));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
