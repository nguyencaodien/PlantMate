package com.example.ungdungchiasecaycanh.ui.admin;

import androidx.annotation.StringRes;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.util.ImageRefs;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Client-side mirror of the Firestore limits for admin edits (the rules remain the authority). */
public final class AdminValidation {

    public static final String NICKNAME = "nickname";
    public static final String SPECIES = "species";
    public static final String DESCRIPTION = "description";
    public static final String PRICE = "price";
    public static final String LOOKING_FOR = "lookingFor";
    public static final String LOCATION = "locationText";
    public static final String TEXT = "text";
    public static final String DISPLAY_NAME = "displayName";
    public static final String BIO = "bio";
    public static final String NOTE = "note";
    public static final String IMAGE = "image";
    public static final int MAX_LISTING_IMAGES = 5;

    private AdminValidation() {
    }

    /** Field → error message for a listing edit; empty when the edit is valid. */
    public static Map<String, Integer> listing(String nickname, String species, String description,
                                                ListingType type, String priceText, String lookingFor,
                                                String location) {
        Map<String, Integer> errors = new HashMap<>();
        String n = trim(nickname), s = trim(species), d = trim(description), l = trim(location), f = trim(lookingFor);
        if (n.isEmpty()) errors.put(NICKNAME, R.string.admin_error_required);
        else if (n.length() > 60) errors.put(NICKNAME, R.string.admin_error_too_long_60);
        if (s.length() > 100) errors.put(SPECIES, R.string.admin_error_too_long_100);
        if (d.isEmpty()) errors.put(DESCRIPTION, R.string.admin_error_required);
        else if (d.length() > 1000) errors.put(DESCRIPTION, R.string.admin_error_too_long_1000);
        if (l.isEmpty()) errors.put(LOCATION, R.string.admin_error_required);
        else if (l.length() > 100) errors.put(LOCATION, R.string.admin_error_too_long_100);
        if (type == ListingType.SELL && parsePrice(priceText) <= 0) errors.put(PRICE, R.string.admin_error_price);
        if (type == ListingType.TRADE) {
            if (f.isEmpty()) errors.put(LOOKING_FOR, R.string.admin_error_required);
            else if (f.length() > 200) errors.put(LOOKING_FOR, R.string.admin_error_too_long_200);
        }
        return errors;
    }

    @StringRes
    public static int postText(String text) {
        String t = trim(text);
        if (t.isEmpty()) return R.string.admin_error_required;
        if (t.length() > 2000) return R.string.admin_error_too_long_2000;
        return 0;
    }

    public static Map<String, Integer> member(String displayName, String location, String bio) {
        Map<String, Integer> errors = new HashMap<>();
        if (trim(displayName).isEmpty()) errors.put(DISPLAY_NAME, R.string.admin_error_required);
        String l = trim(location);
        if (l.isEmpty()) errors.put(LOCATION, R.string.admin_error_required);
        else if (l.length() > 100) errors.put(LOCATION, R.string.admin_error_too_long_100);
        if (trim(bio).length() > 500) errors.put(BIO, R.string.admin_error_too_long_500);
        return errors;
    }

    /** One remote image URL: empty (no image) or an http(s) link up to 2048 characters. */
    @StringRes
    public static int imageUrl(String url) {
        String u = trim(url);
        if (u.isEmpty()) return 0;
        if (u.length() > 2048) return R.string.admin_error_image_too_long;
        if (!ImageRefs.isValid(u)) return R.string.admin_error_image_url;
        return 0;
    }

    /** Listing images, one URL per line: at most five valid http(s) links. */
    public static List<String> parseImageUrls(String text) {
        List<String> urls = new ArrayList<>();
        if (text == null) return urls;
        for (String line : text.split("\r?\n")) {
            String u = line.trim();
            if (!u.isEmpty()) urls.add(u);
        }
        return urls;
    }

    @StringRes
    public static int imageUrls(String text) {
        List<String> urls = parseImageUrls(text);
        if (urls.size() > MAX_LISTING_IMAGES) return R.string.admin_error_too_many_images;
        for (String u : urls) {
            int error = imageUrl(u);
            if (error != 0) return error;
        }
        return 0;
    }

    @StringRes
    public static int note(String note) {
        return trim(note).length() > 500 ? R.string.admin_error_too_long_500 : 0;
    }

    /** Price in VND, or -1 when it is not a positive whole number. */
    public static long parsePrice(String text) {
        String digits = trim(text).replace(".", "").replace(",", "").replace(" ", "");
        if (digits.isEmpty() || !digits.matches("\\d{1,12}")) return -1;
        return Long.parseLong(digits);
    }

    /** Case- and accent-insensitive search used by the admin lists. */
    public static boolean matches(String query, String... fields) {
        String q = fold(query);
        if (q.isEmpty()) return true;
        for (String field : fields) if (fold(field).contains(q)) return true;
        return false;
    }

    static String fold(String value) {
        if (value == null) return "";
        String lower = value.toLowerCase(Locale.ROOT).trim();
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "").replace('đ', 'd');
    }

    static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
