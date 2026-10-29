package com.example.ungdungchiasecaycanh.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.PathInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;


import com.example.ungdungchiasecaycanh.R;

/**
 * Motion tokens from the Vivid Green design (pmRise, pmFromLeft, pmFromRight, pmSheet, pmPop,
 * pmZoom, pmBeat, pmFloat, pmGlow). Everything is skipped when the system turns animations off,
 * so views always end in their resting state.
 */
public final class Motion {

    /** cubic-bezier(0.16, 1, 0.3, 1), the design's entrance curve. */
    public static final PathInterpolator EASE_OUT = new PathInterpolator(0.16f, 1f, 0.3f, 1f);
    /** CSS ease-in-out, used by the idle loops. */
    public static final PathInterpolator EASE_IN_OUT = new PathInterpolator(0.42f, 0f, 0.58f, 1f);

    private Motion() {
    }

    public static boolean enabled(Context context) {
        float scale = Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f);
        return scale > 0f;
    }

    /** pmRise: fade in while rising 16dp. */
    public static void rise(View view, long delayMs) {
        enter(view, 0f, 16f, 1f, 520, delayMs);
    }

    /** pmFromLeft: fade in from 22dp on the left. */
    public static void fromLeft(View view, long delayMs) {
        enter(view, -22f, 0f, 1f, 520, delayMs);
    }

    /** pmFromRight: fade in from 22dp on the right. */
    public static void fromRight(View view, long delayMs) {
        enter(view, 22f, 0f, 1f, 520, delayMs);
    }

    /** pmSheet: a sheet sliding up 56dp. */
    public static void sheet(View view, long durationMs, long delayMs) {
        enter(view, 0f, 56f, 1f, durationMs, delayMs);
    }

    /** pmPop: fade in while growing from 60%. */
    public static void pop(View view, long durationMs, long delayMs) {
        enter(view, 0f, 0f, 0.6f, durationMs, delayMs);
    }

    private static void enter(View view, float dxDp, float dyDp, float fromScale,
                              long durationMs, long delayMs) {
        if (view == null || !enabled(view.getContext())) return;
        float density = view.getResources().getDisplayMetrics().density;
        view.setAlpha(0f);
        view.setTranslationX(dxDp * density);
        view.setTranslationY(dyDp * density);
        view.setScaleX(fromScale);
        view.setScaleY(fromScale);
        view.animate()
                .alpha(1f).translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(durationMs)
                .setStartDelay(delayMs)
                .setInterpolator(EASE_OUT)
                .start();
    }

    /** pmZoom: a photo settling from 108% to 100%. */
    public static void zoom(View view) {
        if (view == null || !enabled(view.getContext())) return;
        view.setScaleX(1.08f);
        view.setScaleY(1.08f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(1200).setInterpolator(EASE_OUT).start();
    }

    /** pmBeat: a quick 130% pulse, used when a heart is toggled. */
    public static void beat(View view) {
        if (view == null || !enabled(view.getContext())) return;
        ObjectAnimator beat = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.3f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.3f, 1f));
        beat.setDuration(420);
        beat.setInterpolator(EASE_OUT);
        beat.start();
    }

    /** pmFloat: speech bubbles bob 4dp and rock ±2° every 3 s. */
    public static void floatLoop(View view) {
        if (view == null || !enabled(view.getContext())) return;
        float density = view.getResources().getDisplayMetrics().density;
        ObjectAnimator bob = ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, -4f * density),
                PropertyValuesHolder.ofFloat(View.ROTATION, -2f, 2f));
        loop(view, bob, 1500);
    }

    /**
     * pmGlow: the lime glow in a forest hero fades in once to about 90% opacity.
     * The hero background must be a layer-list whose glow layer has the id {@code hero_glow}.
     */
    public static void glow(View hero) {
        if (hero == null) return;
        Drawable background = hero.getBackground();
        if (!(background instanceof LayerDrawable)) return;
        Drawable glow = ((LayerDrawable) background.mutate()).findDrawableByLayerId(R.id.hero_glow);
        if (glow == null) return;
        if (!enabled(hero.getContext())) {
            glow.setAlpha(255);
            return;
        }
        // One gentle fade-in instead of a 60 fps loop: animating the layer's alpha forced the whole
        // hero (gradient, text, Mầm) to redraw every frame.
        ValueAnimator fadeIn = ValueAnimator.ofInt(90, 230);
        fadeIn.addUpdateListener(a -> glow.setAlpha((int) a.getAnimatedValue()));
        fadeIn.setDuration(1600);
        fadeIn.setInterpolator(EASE_OUT);
        fadeIn.start();
    }

    /** pmSwapA/B: content slides in 24dp from the right when its selection changes. */
    public static void swap(View view) {
        enter(view, 24f, 0f, 1f, 480, 0);
    }

    /** pmTyping: one of Mầm's typing dots bouncing 5dp every second. */
    public static void typingDot(View dot, long delayMs) {
        if (dot == null || dot.getTag() != null || !enabled(dot.getContext())) return;
        dot.setTag("typing");
        float lift = -5f * dot.getResources().getDisplayMetrics().density;
        ObjectAnimator bounce = ObjectAnimator.ofPropertyValuesHolder(dot,
                PropertyValuesHolder.ofKeyframe(View.TRANSLATION_Y,
                        Keyframe.ofFloat(0f, 0f), Keyframe.ofFloat(0.35f, lift),
                        Keyframe.ofFloat(0.7f, 0f), Keyframe.ofFloat(1f, 0f)),
                PropertyValuesHolder.ofKeyframe(View.ALPHA,
                        Keyframe.ofFloat(0f, 0.4f), Keyframe.ofFloat(0.35f, 1f),
                        Keyframe.ofFloat(0.7f, 0.4f), Keyframe.ofFloat(1f, 0.4f)));
        bounce.setDuration(1000);
        bounce.setStartDelay(delayMs);
        bounce.setInterpolator(EASE_IN_OUT);
        bounce.setRepeatCount(ValueAnimator.INFINITE);
        dot.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) { if (!bounce.isStarted()) bounce.start(); }
            @Override public void onViewDetachedFromWindow(View v) { bounce.cancel(); }
        });
        if (dot.isAttachedToWindow()) bounce.start();
    }

    /**
     * Staggered pmRise for list rows: the first row starts after {@code firstDelayMs}, each next
     * row {@code stepMs} later. Call once, when the first real rows arrive.
     */
    public static void staggerRows(ViewGroup list, long firstDelayMs, long stepMs) {
        if (list == null || !enabled(list.getContext())) return;
        TranslateAnimation rise = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.ABSOLUTE,
                16f * list.getResources().getDisplayMetrics().density, Animation.ABSOLUTE, 0f);
        AlphaAnimation fade = new AlphaAnimation(0f, 1f);
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(rise);
        set.addAnimation(fade);
        set.setDuration(520);
        set.setStartOffset(firstDelayMs);
        set.setInterpolator(EASE_OUT);
        LayoutAnimationController controller = new LayoutAnimationController(set, stepMs / 520f);
        list.setLayoutAnimation(controller);
        list.scheduleLayoutAnimation();
    }

    /** How long an animated vector plays, then rests, before playing again (while on screen). */
    static final long VECTOR_PLAY_MS = 5_000;
    static final long VECTOR_REST_MS = 12_000;

    /**
     * Plays an animated vector (Mầm, the sign-in banner) in bursts: about 5 s of motion, then a
     * 12 s rest, repeating only while the view is attached and visible. A vector with dozens of
     * paths is re-rasterized every frame while it runs, so a never-ending loop kept the screen
     * redrawing even when nothing changed.
     */
    public static void start(ImageView image) {
        if (image == null || !enabled(image.getContext())) return;
        Drawable drawable = image.getDrawable();
        if (!(drawable instanceof Animatable)) return;
        Animatable vector = (Animatable) drawable;
        Runnable[] cycle = new Runnable[1];
        cycle[0] = () -> {
            if (!image.isAttachedToWindow()) {
                vector.stop();
                return;
            }
            if (vector.isRunning()) {
                vector.stop();
                image.postDelayed(cycle[0], VECTOR_REST_MS);
            } else {
                if (image.isShown()) vector.start();
                image.postDelayed(cycle[0], vector.isRunning() ? VECTOR_PLAY_MS : VECTOR_REST_MS);
            }
        };
        vector.start();
        image.postDelayed(cycle[0], VECTOR_PLAY_MS);
    }

    /**
     * Plays a small back-and-forth motion in bursts: two full cycles (ending back at rest), then
     * a pause, repeating only while the owner is attached and visible. Endless loops kept the
     * screen redrawing at 60 fps even when nothing else changed.
     */
    private static void loop(View owner, ValueAnimator animator, long halfPeriodMs) {
        animator.setDuration(halfPeriodMs);
        animator.setInterpolator(EASE_IN_OUT);
        animator.setRepeatCount(3);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        Runnable replay = () -> {
            if (owner.isAttachedToWindow() && owner.isShown() && !animator.isStarted()) animator.start();
        };
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationStart(Animator a) { cancelled = false; }
            @Override public void onAnimationCancel(Animator a) { cancelled = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (!cancelled && owner.isAttachedToWindow()) owner.postDelayed(replay, VECTOR_REST_MS);
            }
        });
        owner.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                if (!animator.isStarted()) animator.start();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                v.removeCallbacks(replay);
                animator.cancel();
            }
        });
        if (owner.isAttachedToWindow()) animator.start();
    }
}
