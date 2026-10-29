package com.example.ungdungchiasecaycanh.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.example.ungdungchiasecaycanh.R;

/**
 * Shows a member avatar on the round TextView used across the app. users/{uid}.avatarUrl is:
 * <ul>
 *   <li>"" - the initials on the view's own background (default);</li>
 *   <li>{@code preset:<key>} - a built-in plant illustration ({@link #PRESETS});</li>
 *   <li>{@code pmimg://<id>} or an https URL - a photo, shown as a circle.</li>
 * </ul>
 */
public final class AvatarBinder {
    public static final String PRESET_PREFIX = "preset:";

    /** One built-in avatar: key, background colour, icon, icon tint (0 keeps the icon's colours). */
    public static final class Preset {
        public final String key;
        final int background, icon, tint;
        final float inset, zoom, shiftY;
        Preset(String key, int background, int icon, int tint, float inset) {
            this(key, background, icon, tint, inset, 1f, 0f);
        }
        /** {@code zoom} enlarges the fitted icon, {@code shiftY} moves it by a fraction of the size. */
        Preset(String key, int background, int icon, int tint, float inset, float zoom, float shiftY) {
            this.key = key; this.background = background; this.icon = icon; this.tint = tint; this.inset = inset;
            this.zoom = zoom; this.shiftY = shiftY;
        }
        public String ref() { return PRESET_PREFIX + key; }
    }

    public static final Preset[] PRESETS = {
        // Keys are stored on profiles; only the colours follow the current palette.
        new Preset("mam", 0xFFE4F1E8, R.drawable.ic_mam, 0, 0.04f, 1.55f, -0.05f),
        new Preset("mam_lime", 0xFFCDEBD7, R.drawable.ic_mam, 0, 0.04f, 1.55f, -0.05f),
        new Preset("leaf", 0xFF1E6B46, R.drawable.ic_leaf, 0xFFFFFFFF, 0.26f),
        new Preset("leaf_mint", 0xFFE4F1E8, R.drawable.ic_leaf, 0xFF1E6B46, 0.26f),
        new Preset("sprout", 0xFFCDEBD7, R.drawable.ic_botanical_placeholder, 0, 0.22f),
        new Preset("drop", 0xFFF4F4F4, R.drawable.ic_water_drop, 0xFF1E6B46, 0.27f),
        new Preset("sun", 0xFF111111, R.drawable.ic_sparkle, 0xFF8ED3A6, 0.26f),
        new Preset("pot", 0xFFF4F4F4, R.drawable.ic_symptom_slow, 0xFF111111, 0.26f),
    };

    private AvatarBinder() { }

    @Nullable
    public static Preset preset(@Nullable String ref) {
        if (ref == null || !ref.startsWith(PRESET_PREFIX)) return null;
        String key = ref.substring(PRESET_PREFIX.length());
        for (Preset p : PRESETS) if (p.key.equals(key)) return p;
        return null;
    }

    /** True when {@code ref} is a photo (uploaded or remote), not a preset or initials. */
    public static boolean isPhoto(@Nullable String ref) {
        return ref != null && !ref.startsWith(PRESET_PREFIX) && RemoteImageLoader.isRemote(ref);
    }

    /** Round drawable for a preset, sized by the view it is set on. */
    public static Drawable presetDrawable(Context context, Preset preset) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(preset.background);
        Drawable icon = ContextCompat.getDrawable(context, preset.icon).mutate();
        if (preset.tint != 0) icon.setTint(preset.tint);
        return new InsetLayer(new Drawable[]{circle, icon}, preset.inset, preset.zoom, preset.shiftY);
    }

    /**
     * Binds the avatar. {@code initialsBackground} is restored when the member has no avatar, so a
     * recycled row never keeps someone else's picture.
     */
    public static void bind(TextView view, String name, @Nullable String avatarRef, int initialsBackground) {
        bind(view, name, avatarRef, initialsBackground, 0);
    }

    /** As {@link #bind(TextView, String, String, int)}, with a {@code ringColor} halo around pictures. */
    public static void bind(TextView view, String name, @Nullable String avatarRef, int initialsBackground, int ringColor) {
        String ref = avatarRef == null ? "" : avatarRef.trim();
        view.setTag(R.id.avatar_ref_tag, ref);
        Preset preset = preset(ref);
        if (preset != null) {
            view.setText("");
            view.setBackground(ringed(view, presetDrawable(view.getContext(), preset), ringColor));
            return;
        }
        view.setText(SecondaryChrome.initials(name));
        view.setBackgroundResource(initialsBackground);
        if (!isPhoto(ref)) return;
        int size = Math.max(96, view.getLayoutParams() == null ? 96 : view.getLayoutParams().width);
        RemoteImageLoader.loadBitmap(view.getContext(), ref, size, bitmap -> {
            if (bitmap == null || !ref.equals(view.getTag(R.id.avatar_ref_tag))) return;
            view.setText("");
            view.setBackground(ringed(view, circle(view.getContext(), bitmap), ringColor));
        });
    }

    private static Drawable ringed(TextView view, Drawable picture, int ringColor) {
        if (ringColor == 0) return picture;
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(ringColor);
        LayerDrawable layers = new LayerDrawable(new Drawable[]{ring, picture});
        int inset = Math.round(3 * view.getResources().getDisplayMetrics().density);
        layers.setLayerInset(1, inset, inset, inset, inset);
        return layers;
    }

    static RoundedBitmapDrawable circle(Context context, Bitmap bitmap) {
        int side = Math.min(bitmap.getWidth(), bitmap.getHeight());
        Bitmap square = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - side) / 2,
                (bitmap.getHeight() - side) / 2, side, side);
        RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), square);
        drawable.setCircular(true);
        drawable.setAntiAlias(true);
        return drawable;
    }

    /** Layer drawable whose icon layer keeps a proportional inset at any size. */
    private static final class InsetLayer extends LayerDrawable {
        private final float inset, zoom, shiftY;
        private final Path clip = new Path();
        InsetLayer(Drawable[] layers, float inset, float zoom, float shiftY) {
            super(layers); this.inset = inset; this.zoom = zoom; this.shiftY = shiftY;
        }

        @Override public void draw(Canvas canvas) {
            // Keep a zoomed icon inside the circle.
            int save = canvas.save();
            canvas.clipPath(clip);
            super.draw(canvas);
            canvas.restoreToCount(save);
        }

        @Override protected void onBoundsChange(Rect bounds) {
            // Fit the icon inside the padded circle without stretching (Mầm is taller than wide).
            int pad = Math.round(Math.min(bounds.width(), bounds.height()) * inset);
            int boxW = bounds.width() - 2 * pad, boxH = bounds.height() - 2 * pad;
            Drawable icon = getDrawable(1);
            int iw = Math.max(1, icon.getIntrinsicWidth()), ih = Math.max(1, icon.getIntrinsicHeight());
            float scale = Math.min(boxW / (float) iw, boxH / (float) ih) * zoom;
            int w = Math.round(iw * scale), h = Math.round(ih * scale);
            int left = pad + (boxW - w) / 2;
            int top = pad + (boxH - h) / 2 + Math.round(bounds.height() * shiftY);
            clip.reset();
            clip.addOval(new RectF(bounds), Path.Direction.CW);
            setLayerInset(1, left, top, bounds.width() - left - w, bounds.height() - top - h);
            super.onBoundsChange(bounds);
        }
    }
}
