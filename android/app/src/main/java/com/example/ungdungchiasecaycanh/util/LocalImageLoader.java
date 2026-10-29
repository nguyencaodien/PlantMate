package com.example.ungdungchiasecaycanh.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Loads a local photo (content:// or file://, e.g. a Garden plant) off the main thread,
 * downsampled to the view width, with a small memory cache. The view keeps its placeholder
 * when the photo cannot be read.
 */
public final class LocalImageLoader {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
    };

    private LocalImageLoader() { }

    /** Returns true when a load was started or served from cache; false for an empty uri. */
    public static boolean load(ImageView target, String uri, int targetWidthPx, Runnable onLoaded) {
        target.setTag(uri);
        if (uri == null || uri.isEmpty()) return false;
        String key = uri + "#" + targetWidthPx;
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            target.setImageBitmap(cached);
            if (onLoaded != null) onLoaded.run();
            return true;
        }
        Context app = target.getContext().getApplicationContext();
        IO.execute(() -> {
            Bitmap bitmap = decode(app, Uri.parse(uri), Math.max(64, targetWidthPx));
            if (bitmap != null) CACHE.put(key, bitmap);
            MAIN.post(() -> {
                if (bitmap == null || !uri.equals(target.getTag())) return;
                target.setImageBitmap(bitmap);
                if (onLoaded != null) onLoaded.run();
            });
        });
        return true;
    }

    /** Longest side 4/3 of the view width: full width for photos up to 3:4 portrait. */
    private static Bitmap decode(Context context, Uri uri, int width) {
        try {
            return Bitmaps.decode(context.getContentResolver(), uri, width * 4 / 3);
        } catch (Exception | OutOfMemoryError error) {
            return null;
        }
    }
}
