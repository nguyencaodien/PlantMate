package com.example.ungdungchiasecaycanh.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.IOException;
import java.nio.ByteBuffer;

/** Quick on-device check that a photo is bright and sharp enough for Plant Doctor. */
public final class PhotoQuality {
    public enum Result { OK, TOO_DARK, BLURRY }

    // ponytail: fixed thresholds for phone photos scaled to 256 px; raise or lower these two knobs
    // if good photos get flagged (it is only a warning, the member can analyse anyway).
    static final double MIN_BRIGHTNESS = 40;   // mean luma, 0..255
    static final double MIN_SHARPNESS = 40;    // variance of the Laplacian
    private static final int SIDE = 256;

    private PhotoQuality() {
    }

    /** Call off the main thread. An unreadable photo counts as OK: the analyzer reports it. */
    public static Result check(ContentResolver resolver, Uri uri) {
        try {
            Bitmap small = Bitmaps.decode(resolver, uri, SIDE);
            int w = small.getWidth(), h = small.getHeight();
            int[] pixels = new int[w * h];
            small.getPixels(pixels, 0, w, 0, 0, w, h);
            int[] luma = new int[pixels.length];
            for (int i = 0; i < pixels.length; i++) {
                int p = pixels[i];
                luma[i] = (299 * ((p >> 16) & 0xFF) + 587 * ((p >> 8) & 0xFF) + 114 * (p & 0xFF)) / 1000;
            }
            return assess(luma, w, h);
        } catch (IOException | RuntimeException error) {
            return Result.OK;
        }
    }

    /** Side of the centre square sampled from a camera frame. */
    static final int FRAME_SIDE = 128;

    /**
     * Same check on a live camera frame: the Y (luma) plane, sampled as a {@link #FRAME_SIDE} square from
     * the centre, where the framing window is. Call off the main thread.
     */
    public static Result checkLumaPlane(ByteBuffer plane, int rowStride, int pixelStride, int width, int height) {
        int square = Math.min(width, height);
        int step = Math.max(1, square / FRAME_SIDE);
        int side = Math.min(FRAME_SIDE, square / step);
        int x0 = (width - side * step) / 2, y0 = (height - side * step) / 2;
        int[] luma = new int[side * side];
        for (int row = 0; row < side; row++) {
            int base = (y0 + row * step) * rowStride + x0 * pixelStride;
            for (int col = 0; col < side; col++) {
                luma[row * side + col] = plane.get(base + col * step * pixelStride) & 0xFF;
            }
        }
        return assess(luma, side, side);
    }

    /** Dark: low mean brightness. Blurry: few edges (low variance of the Laplacian). */
    static Result assess(int[] luma, int width, int height) {
        double sum = 0;
        for (int value : luma) sum += value;
        if (sum / luma.length < MIN_BRIGHTNESS) return Result.TOO_DARK;
        if (width < 3 || height < 3) return Result.OK;
        double mean = 0, squares = 0;
        int count = 0;
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int i = y * width + x;
                int laplacian = luma[i - width] + luma[i + width] + luma[i - 1] + luma[i + 1] - 4 * luma[i];
                mean += laplacian;
                squares += (double) laplacian * laplacian;
                count++;
            }
        }
        mean /= count;
        return squares / count - mean * mean < MIN_SHARPNESS ? Result.BLURRY : Result.OK;
    }
}
