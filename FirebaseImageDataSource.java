package com.example.ungdungchiasecaycanh.data.remote;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;


import com.example.ungdungchiasecaycanh.util.ImageRefs;
import com.example.ungdungchiasecaycanh.util.Bitmaps;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Member photos stored in Firestore (the project has no Cloud Storage): images/{id} holds a
 * JPEG as base64 plus its owner, size and creation time. Photos are downscaled and compressed on
 * the device so each document stays well under Firestore's 1 MB limit. imageThumbs/{id} holds a
 * small copy (longest side {@value #THUMB_SIDE_PX} px) for cards and grids.
 */
public class FirebaseImageDataSource {
    static final String COLLECTION = "images";
    static final String THUMBS = "imageThumbs";
    /** Rules let only the owner, their friends and admins read a photo with this audience. */
    static final String AUDIENCE_FRIENDS = "friends";
    static final int MAX_SIDE_PX = 1080;
    public static final int THUMB_SIDE_PX = 320;
    /** Rules allow 700 000 characters; stay safely below. */
    static final int MAX_BASE64_CHARS = 650_000;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Result of {@link #encode}: base64 JPEG and its pixel size. */
    public static final class Encoded {
        public final String base64;
        public final int width, height;
        /** Small copy for cards; may be empty. */
        public final String thumbBase64;
        Encoded(String base64, int width, int height, String thumbBase64) {
            this.base64 = base64; this.width = width; this.height = height; this.thumbBase64 = thumbBase64;
        }
    }

    /** Reads, orients, downscales and compresses a local photo. Call off the main thread. */
    public static Encoded encode(Context context, Uri uri) throws Exception {
        Bitmap bitmap = Bitmaps.decode(context.getContentResolver(), uri, MAX_SIDE_PX);
        int quality = 82;
        String base64;
        while (true) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            if (base64.length() <= MAX_BASE64_CHARS || quality <= 40) break;
            quality -= 12;
        }
        if (base64.length() > MAX_BASE64_CHARS) {
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 3 / 4, bitmap.getHeight() * 3 / 4, true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out);
            base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        }
        return new Encoded(base64, bitmap.getWidth(), bitmap.getHeight(), thumbnail(bitmap));
    }

    private static String thumbnail(Bitmap bitmap) {
        float scale = Math.min(1f, THUMB_SIDE_PX / (float) Math.max(bitmap.getWidth(), bitmap.getHeight()));
        Bitmap small = Bitmap.createScaledBitmap(bitmap, Math.max(1, Math.round(bitmap.getWidth() * scale)),
                Math.max(1, Math.round(bitmap.getHeight() * scale)), true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        small.compress(Bitmap.CompressFormat.JPEG, 75, out);
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }


    /**
     * Writes an encoded photo owned by {@code ownerId}; resolves to its {@code pmimg://} ref.
     * {@code friendsOnly} (story photos) lets only the owner, their friends and admins read it.
     */
    public Task<String> save(String ownerId, Encoded encoded, boolean friendsOnly) {
        DocumentReference doc = db.collection(COLLECTION).document();
        Map<String, Object> data = new HashMap<>();
        data.put("ownerId", ownerId);
        data.put("data", encoded.base64);
        data.put("width", encoded.width);
        data.put("height", encoded.height);
        data.put("createdAt", FieldValue.serverTimestamp());
        if (friendsOnly) data.put("audience", AUDIENCE_FRIENDS);
        return doc.set(data).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            String ref = ImageRefs.refFor(doc.getId());
            if (encoded.thumbBase64 == null || encoded.thumbBase64.isEmpty()) return Tasks.forResult(ref);
            // Best effort: without the small copy, cards fall back to the full photo.
            Map<String, Object> thumb = new HashMap<>();
            thumb.put("ownerId", ownerId);
            thumb.put("data", encoded.thumbBase64);
            thumb.put("createdAt", FieldValue.serverTimestamp());
            if (friendsOnly) thumb.put("audience", AUDIENCE_FRIENDS);
            return db.collection(THUMBS).document(doc.getId()).set(thumb).continueWith(ignored -> ref);
        });
    }

    /** JPEG bytes of an uploaded photo (blocking; call off the main thread). */
    public static byte[] loadBlocking(String imageId) throws Exception {
        return loadBlocking(COLLECTION, imageId);
    }

    /** The small copy, or the full photo for photos uploaded before thumbnails existed. */
    public static byte[] loadThumbBlocking(String imageId) throws Exception {
        byte[] thumb = null;
        try {
            thumb = loadBlocking(THUMBS, imageId);
        } catch (Exception ignored) {
            // No thumbnail readable: use the full photo below.
        }
        return thumb != null ? thumb : loadBlocking(imageId);
    }

    private static byte[] loadBlocking(String collection, String imageId) throws Exception {
        DocumentSnapshot snapshot = Tasks.await(
                FirebaseFirestore.getInstance().collection(collection).document(imageId).get(),
                20, TimeUnit.SECONDS);
        String base64 = snapshot.getString("data");
        return base64 == null ? null : Base64.decode(base64, Base64.DEFAULT);
    }
}
