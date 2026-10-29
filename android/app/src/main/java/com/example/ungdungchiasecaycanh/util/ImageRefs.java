package com.example.ungdungchiasecaycanh.util;

/**
 * Image references stored in listing.imageUrls and post.imageUrl: either a public https URL or
 * {@code pmimg://{imageId}}, a member-uploaded photo kept in Firestore at images/{imageId}.
 */
public final class ImageRefs {
    public static final String SCHEME = "pmimg://";

    private ImageRefs() { }

    public static boolean isUploaded(String ref) {
        return idOf(ref) != null;
    }

    /** The images/{id} document id, or null when {@code ref} is not a valid uploaded-image ref. */
    public static String idOf(String ref) {
        if (ref == null) return null;
        String r = ref.trim();
        if (!r.startsWith(SCHEME)) return null;
        String id = r.substring(SCHEME.length());
        return id.matches("[A-Za-z0-9]{10,40}") ? id : null;
    }

    public static String refFor(String imageId) {
        return SCHEME + imageId;
    }

    /** Accepted by the Firestore rules: an http(s) URL or an uploaded-image ref. */
    public static boolean isValid(String ref) {
        return ref != null && (ref.trim().matches("(?i)https?://\\S+") || isUploaded(ref));
    }
}
