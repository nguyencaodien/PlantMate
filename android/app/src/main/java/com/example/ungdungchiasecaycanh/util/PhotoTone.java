package com.example.ungdungchiasecaycanh.util;

/**
 * How much of a photo is strong red. Home's featured slider leaves out covers dominated by red
 * (poinsettias, red anthuriums) so the slides stay with the green of the brand.
 */
public final class PhotoTone {
    /** A cover with more strong-red pixels than this share is not featured. */
    public static final double MAX_RED_SHARE = 0.12;

    private PhotoTone() { }

    /** Share (0–1) of pixels that are saturated, not dark, and red to magenta-red. */
    public static double redShare(int[] argb) {
        if (argb == null || argb.length == 0) return 0;
        int red = 0;
        for (int pixel : argb) if (isStrongRed(pixel)) red++;
        return red / (double) argb.length;
    }

    public static boolean mostlyRed(int[] argb) {
        return redShare(argb) > MAX_RED_SHARE;
    }

    /** Hue within 18° of red, saturation at least 45 %, brightness at least 30 % (plain HSV). */
    static boolean isStrongRed(int pixel) {
        int r = (pixel >> 16) & 0xFF, g = (pixel >> 8) & 0xFF, b = pixel & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        if (max != r || max == 0) return false;
        double saturation = (max - min) / (double) max;
        double value = max / 255.0;
        if (saturation < 0.45 || value < 0.30) return false;
        // Red is the largest channel, so the hue is 60° × (g − b) / (max − min), in (−60°, 60°).
        double hue = 60.0 * (g - b) / (max - min);
        return hue <= 18 && hue >= -25;
    }
}
