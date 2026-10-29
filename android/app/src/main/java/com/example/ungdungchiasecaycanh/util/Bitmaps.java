package com.example.ungdungchiasecaycanh.util;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;

import java.io.IOException;

/** Decodes a local photo (content:// or file://) upright and no larger than needed. */
public final class Bitmaps {
    private Bitmaps() {
    }

    /**
     * Longest side at most {@code maxSide} px, with the camera's EXIF rotation applied. A software
     * bitmap, so it can be cropped or read pixel by pixel. Call off the main thread.
     */
    public static Bitmap decode(ContentResolver resolver, Uri uri, int maxSide) throws IOException {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri), (decoder, info, source) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            int w = info.getSize().getWidth(), h = info.getSize().getHeight();
            float scale = maxSide / (float) Math.max(w, h);
            if (scale < 1f) decoder.setTargetSize(Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)));
        });
    }
}
