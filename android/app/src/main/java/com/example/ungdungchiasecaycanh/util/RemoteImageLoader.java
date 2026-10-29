package com.example.ungdungchiasecaycanh.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseChatMediaDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseImageDataSource;

/**
 * Minimal https image loader for Listing.imageUrls (no third-party library).
 * <ul>
 *   <li>Memory cache (LRU, 1/8 of the heap) for instant rebinds.</li>
 *   <li>Disk cache in {@code cacheDir/images}: each image is stored already downscaled
 *       (longest side ≤ 1080 px, JPEG), so a cold start does not download it again.</li>
 *   <li>Requests for the same URL share one download.</li>
 * </ul>
 * Network and decoding run on a small background pool; binding happens on the main thread
 * and is skipped when a recycled view has since been bound to a different URL.
 */
public final class RemoteImageLoader {
    private static final int MAX_BYTES = 6 * 1024 * 1024;
    private static final int DISK_MAX_SIDE_PX = 1080;
    private static final long DISK_MAX_BYTES = 40L * 1024 * 1024;
    /** Uploaded photos shown at most this wide use the small copy (imageThumbs). */
    static final int THUMB_MAX_TARGET_PX = 2 * FirebaseImageDataSource.THUMB_SIDE_PX;
    private static final String THUMB_SUFFIX = "#thumb";

    /** Android-only state, created on first use so pure helpers stay JVM-testable. */
    private static final class Holder {
        static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
        static final Handler MAIN = new Handler(Looper.getMainLooper());
        static final LruCache<String, Bitmap> CACHE =
                new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 8)) {
                    @Override protected int sizeOf(String key, Bitmap value) {
                        return value.getByteCount();
                    }
                };
        /** URL → callbacks waiting for the same download. Main thread only. */
        static final Map<String, List<Consumer<Bitmap>>> IN_FLIGHT = new HashMap<>();
        static volatile File diskDir;
    }

    private RemoteImageLoader() { }

    /** Returns true for refs this loader will fetch: http(s) URLs and uploaded {@code pmimg://} photos. */
    public static boolean isRemote(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("https://") || lower.startsWith("http://") || isStored(url);
    }

    /**
     * Shows {@code target} with the image at {@code url}. {@code target} stays hidden while
     * loading and on failure, so the placeholder underneath remains the honest fallback.
     */
    public static void load(ImageView target, String url, int targetWidthPx) {
        if (url != null && url.equals(target.getTag()) && target.getDrawable() != null
                && target.getVisibility() == View.VISIBLE) {
            return; // already showing this image: no flicker on rebind
        }
        target.setTag(url);
        target.setImageDrawable(null);
        target.setVisibility(View.GONE);
        if (!isRemote(url)) return;
        // The bitmap is handed to waiting views directly: reading it back from the LRU could
        // miss when several large images finish together and evict each other.
        loadBitmap(target.getContext(), url, targetWidthPx, bitmap -> {
            if (bitmap != null && url.equals(target.getTag())) show(target, bitmap);
        });
    }

    /** Loads a bitmap (memory, disk, then network/Firestore) and delivers it on the main thread. */
    public static void loadBitmap(Context context, String url, int targetWidthPx, Consumer<Bitmap> callback) {
        if (!isRemote(url)) { callback.accept(null); return; }
        int width = Math.max(1, targetWidthPx);
        String key = cacheKey(url, width);
        Bitmap cached = Holder.CACHE.get(key);
        if (cached != null) { callback.accept(cached); return; }
        ensureDiskDir(context);
        List<Consumer<Bitmap>> waiting = Holder.IN_FLIGHT.get(key);
        if (waiting != null) { waiting.add(callback); return; }
        waiting = new ArrayList<>();
        waiting.add(callback);
        Holder.IN_FLIGHT.put(key, waiting);
        Holder.EXECUTOR.execute(() -> {
            Bitmap loaded = fromDisk(key, width);
            if (loaded == null) loaded = isStored(url) ? fetchUploaded(url, key, width) : fetch(url, width);
            final Bitmap bitmap = loaded;
            if (bitmap != null) Holder.CACHE.put(key, bitmap);
            Holder.MAIN.post(() -> {
                List<Consumer<Bitmap>> callbacks = Holder.IN_FLIGHT.remove(key);
                if (callbacks != null) for (Consumer<Bitmap> c : callbacks) c.accept(bitmap);
            });
        });
    }

    /**
     * Cache identity of an image at a display width: small views of uploaded photos use the
     * thumbnail, a separate image from the full photo.
     */
    static String cacheKey(String url, int targetWidthPx) {
        return ImageRefs.isUploaded(url) && targetWidthPx <= THUMB_MAX_TARGET_PX ? url.trim() + THUMB_SUFFIX : url;
    }

    private static void show(ImageView target, Bitmap bitmap) {
        target.setImageBitmap(bitmap);
        target.setVisibility(View.VISIBLE);
    }

    private static void ensureDiskDir(Context context) {
        if (Holder.diskDir != null) return;
        File dir = new File(context.getApplicationContext().getCacheDir(), "images");
        if (dir.exists() || dir.mkdirs()) Holder.diskDir = dir;
    }

    private static File diskFile(String url) {
        File dir = Holder.diskDir;
        return dir == null ? null : new File(dir, key(url) + ".jpg");
    }

    private static Bitmap fromDisk(String url, int targetWidthPx) {
        File file = diskFile(url);
        if (file == null || !file.isFile()) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        if (bounds.outWidth <= 0) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(bounds.outWidth, targetWidthPx);
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath(), options);
        if (bitmap != null) file.setLastModified(System.currentTimeMillis());
        return bitmap;
    }

    /** Member photo stored in Firestore (images/{id}); cached on disk like any other image. */
    /** Photos kept in Firestore: public post/listing images or private chat photos. */
    static boolean isStored(String ref) {
        return ImageRefs.isUploaded(ref)
                || FirebaseChatMediaDataSource.isChatMedia(ref);
    }

    private static Bitmap fetchUploaded(String ref, String key, int targetWidthPx) {
        try {
            byte[] bytes = FirebaseChatMediaDataSource.isChatMedia(ref)
                    ? FirebaseChatMediaDataSource.loadBlocking(ref)
                    : key.endsWith(THUMB_SUFFIX)
                    ? FirebaseImageDataSource.loadThumbBlocking(ImageRefs.idOf(ref))
                    : FirebaseImageDataSource.loadBlocking(ImageRefs.idOf(ref));
            return bytes == null ? null : decodeAndStore(key, bytes, targetWidthPx);
        } catch (Exception | OutOfMemoryError ignored) {
            return null;
        }
    }

    private static Bitmap fetch(String url, int targetWidthPx) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(15_000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "PlantMate-Android/1.0");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return null;
            byte[] bytes = readLimited(connection.getInputStream());
            if (bytes == null) return null;
            return decodeAndStore(url, bytes, targetWidthPx);
        } catch (Exception | OutOfMemoryError ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    /** Stores a downscaled copy on disk, then decodes for the requested width. */
    private static Bitmap decodeAndStore(String url, byte[] bytes, int targetWidthPx) {
        {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
            if (bounds.outWidth <= 0) return null;
            // Store a downscaled copy on disk, then decode for the requested width.
            BitmapFactory.Options diskOptions = new BitmapFactory.Options();
            diskOptions.inSampleSize = sampleSize(Math.max(bounds.outWidth, bounds.outHeight), DISK_MAX_SIDE_PX);
            Bitmap diskCopy = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, diskOptions);
            if (diskCopy == null) return null;
            writeDisk(url, diskCopy);
            int sample = sampleSize(diskCopy.getWidth(), targetWidthPx);
            if (sample <= 1) return diskCopy;
            Bitmap scaled = Bitmap.createScaledBitmap(diskCopy, Math.max(1, diskCopy.getWidth() / sample),
                    Math.max(1, diskCopy.getHeight() / sample), true);
            if (scaled != diskCopy) diskCopy.recycle();
            return scaled;
        }
    }

    private static void writeDisk(String url, Bitmap bitmap) {
        File file = diskFile(url);
        if (file == null) return;
        File temp = new File(file.getPath() + ".tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
        } catch (Exception ignored) {
            temp.delete();
            return;
        }
        if (!temp.renameTo(file)) temp.delete();
        trimDisk(file.getParentFile());
    }

    /** Keeps the disk cache under its size limit by deleting the least recently used files. */
    private static void trimDisk(File dir) {
        File[] files = dir == null ? null : dir.listFiles();
        if (files == null) return;
        long total = 0;
        for (File f : files) total += f.length();
        if (total <= DISK_MAX_BYTES) return;
        Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (File f : files) {
            if (total <= DISK_MAX_BYTES * 3 / 4) break;
            total -= f.length();
            f.delete();
        }
    }

    static String key(String url) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(url.getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) hex.append(String.format(Locale.ROOT, "%02x", b));
            return hex.toString();
        } catch (Exception error) {
            return Integer.toHexString(url.hashCode());
        }
    }

    static int sampleSize(int sourceWidth, int targetWidth) {
        int sample = 1;
        while (sourceWidth > 0 && targetWidth > 0 && sourceWidth / (sample * 2) >= targetWidth) {
            sample *= 2;
        }
        return sample;
    }

    private static byte[] readLimited(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BYTES) return null;
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }
}
