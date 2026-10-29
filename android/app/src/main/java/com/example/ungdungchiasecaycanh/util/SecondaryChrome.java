package com.example.ungdungchiasecaycanh.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.ungdungchiasecaycanh.R;

import java.util.Locale;

/**
 * Shared chrome for secondary screens in the Vivid Green design: the forest hero continues under
 * the status bar (light icons), content keeps clear of the system bars and keyboard, and the hero
 * gets its glow and entrance.
 */
public final class SecondaryChrome {

    private SecondaryChrome() {
    }

    /**
     * Edge to edge for screens that draw their own ground (the lime wash of the auth screens):
     * keeps the layout's background, pads for system bars and the keyboard, dark status icons.
     */
    public static void applyPlain(Activity activity, View root) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
        root.setFitsSystemWindows(false);
        int start = root.getPaddingLeft(), top = root.getPaddingTop();
        int end = root.getPaddingRight(), bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(start + bars.left, top + bars.top, end + bars.right,
                    bottom + Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        new WindowInsetsControllerCompat(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(useDarkStatusIcons(activity));
    }

    public static void apply(Activity activity, View root, @Nullable View hero) {
        root.setFitsSystemWindows(false);
        // Current style: the lime-wash page ground runs under the status bar.
        root.setBackgroundResource(R.drawable.bg_composer);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        new WindowInsetsControllerCompat(activity.getWindow(), activity.getWindow().getDecorView())
                .setAppearanceLightStatusBars(useDarkStatusIcons(activity));
        if (hero != null) {
            Motion.glow(hero);
            if (hero instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) hero;
                for (int i = 0; i < group.getChildCount(); i++) {
                    Motion.rise(group.getChildAt(i), 60 + i * 40L);
                }
            }
        }
    }


    /** Dark status-bar icons on the light (Bright Green) theme, light icons in dark mode. */
    public static boolean useDarkStatusIcons(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES;
    }

    /** Up to two initials for the lime avatar discs. */
    public static String initials(@Nullable String name) {
        // First and last word: "Lê Thảo Vy" → "LV", matching the other screens.
        if (name == null || name.trim().isEmpty()) return "";
        String[] parts = name.trim().split("\\s+");
        String first = parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        if (parts.length == 1) return first;
        return first + parts[parts.length - 1].substring(0, 1).toUpperCase(Locale.ROOT);
    }
}
