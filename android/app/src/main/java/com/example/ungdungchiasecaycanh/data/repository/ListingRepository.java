package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseListingDataSource;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.util.ImageRefs;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import com.google.firebase.Timestamp;

public class ListingRepository {
    private final FirebaseAuthDataSource authDataSource = new FirebaseAuthDataSource();
    private final FirebaseListingDataSource listingDataSource = new FirebaseListingDataSource();

    public Task<String> publishListing(String nickname, String species, String description,
                                       ListingType type, long price, String lookingFor,
                                       String locationText) {
        return publishListing(nickname, species, description, type, price, lookingFor,
                Collections.emptyList(), locationText);
    }

    /** Publishes with already-hosted https image URLs (0..5); local URIs are rejected. */
    public Task<String> publishListing(String nickname, String species, String description,
                                       ListingType type, long price, String lookingFor,
                                       List<String> imageUrls, String locationText) {
        return publishListing(nickname, species, description, type, price, lookingFor, imageUrls,
                locationText, ListingDetails.NONE);
    }

    /** As above, with the optional condition / height / light / pick-up details. */
    public Task<String> publishListing(String nickname, String species, String description,
                                       ListingType type, long price, String lookingFor,
                                       List<String> imageUrls, String locationText,
                                       ListingDetails details) {
        String ownerId = authDataSource.getCurrentUserId();
        if (ownerId == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        try {
            Listing listing = normalizeForCreate(ownerId, nickname, species, description,
                    type, price, lookingFor, imageUrls, locationText, details);
            return listingDataSource.createListing(listing);
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
    }

    public Task<Listing> getListing(String listingId) {
        if (listingId == null || listingId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Listing ID is required"));
        }
        return listingDataSource.getListing(listingId);
    }

    /** An active listing not touched for this long asks its owner "is the plant still there?". */
    public static final long CHECK_AFTER_MS = 14L * 24 * 60 * 60 * 1000;
    /** ...and after this long it leaves Home and search until the owner confirms it. */
    public static final long HIDE_AFTER_MS = 30L * 24 * 60 * 60 * 1000;

    /** Time since the owner last created or updated the listing (0 when unknown). */
    public static long idleMs(Listing listing, long now) {
        Timestamp at = listing.getUpdatedAt() != null ? listing.getUpdatedAt() : listing.getCreatedAt();
        return at == null ? 0 : Math.max(0, now - at.toDate().getTime());
    }

    public static boolean needsCheck(Listing listing, long now) {
        return listing.getStatus() == ListingStatus.ACTIVE && idleMs(listing, now) >= CHECK_AFTER_MS;
    }

    public static boolean hiddenAsStale(Listing listing, long now) {
        return listing.getStatus() == ListingStatus.ACTIVE && idleMs(listing, now) >= HIDE_AFTER_MS;
    }

    static List<Listing> withoutStale(List<Listing> listings, long now) {
        List<Listing> fresh = new ArrayList<>();
        if (listings != null) for (Listing l : listings) if (!hiddenAsStale(l, now)) fresh.add(l);
        return fresh;
    }

    /** Active listings others can see: those not confirmed for 30 days are left out. */
    public Task<List<Listing>> getActiveListings() {
        return listingDataSource.getActiveListings().continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return withoutStale(task.getResult(), System.currentTimeMillis());
        });
    }

    /** A page of active listings for Home, without the ones hidden as stale. */
    public Task<FirebaseListingDataSource.Page> getActiveListingsPage(Object cursor, int size) {
        return listingDataSource.getActiveListingsPage(cursor, size).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            FirebaseListingDataSource.Page page = task.getResult();
            return new FirebaseListingDataSource.Page(withoutStale(page.listings, System.currentTimeMillis()),
                    page.cursor, page.hasMore);
        });
    }

    /** A sold plant shows on Home with its "Sold" pill for this long after the sale, then leaves. */
    public static final long SOLD_SHOWN_MS = 3L * 24 * 60 * 60 * 1000;
    static final int SALE_UPDATES_LIMIT = 30;

    /** Reserved sale listings, and ones sold in the last {@link #SOLD_SHOWN_MS}. */
    static List<Listing> recentSaleUpdates(List<Listing> listings, long now) {
        List<Listing> out = new ArrayList<>();
        if (listings != null) for (Listing l : listings) {
            if (l.getStatus() == ListingStatus.RESERVED
                    || (l.getStatus() == ListingStatus.REHOMED && idleMs(l, now) < SOLD_SHOWN_MS)) out.add(l);
        }
        return out;
    }

    /** Sale listings that are reserved or recently sold, for the status pills on Home. */
    public Task<List<Listing>> getSaleUpdates() {
        return listingDataSource.getSaleUpdates(SALE_UPDATES_LIMIT).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return recentSaleUpdates(task.getResult(), System.currentTimeMillis());
        });
    }

    /** Server counts of active Give Away, Trade and Sell listings, in that order. */
    public Task<int[]> countActiveByType() {
        Task<Integer> give = listingDataSource.countActive(ListingType.GIVE_AWAY);
        Task<Integer> trade = listingDataSource.countActive(ListingType.TRADE);
        Task<Integer> sell = listingDataSource.countActive(ListingType.SELL);
        return Tasks.whenAllSuccess(give, trade, sell).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return new int[]{give.getResult(), trade.getResult(), sell.getResult()};
        });
    }

    /** "Still available": refreshes the listing's update time so it stays (or comes back) on Home. */
    public Task<Void> confirmStillAvailable(String listingId) {
        return updateListingStatus(listingId, ListingStatus.ACTIVE);
    }

    /** Every listing owned by {@code ownerUid} (any status); callers decide what to show. */
    public Task<List<Listing>> getListingsForOwner(String ownerUid) {
        if (authDataSource.getCurrentUserId() == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        if (ownerUid == null || ownerUid.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Owner is required"));
        }
        return listingDataSource.getMyListings(ownerUid);
    }

    public Task<List<Listing>> getMyListings() {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        return listingDataSource.getMyListings(uid);
    }

    public Task<Void> updateListingStatus(String listingId, ListingStatus newStatus) {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        if (listingId == null || listingId.trim().isEmpty() || newStatus == null) {
            return Tasks.forException(new IllegalArgumentException("Listing and status are required"));
        }
        return listingDataSource.updateListingStatus(listingId.trim(), newStatus);
    }

    public Task<Void> saveListing(String listingId) {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        if (listingId == null || listingId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Listing ID is required"));
        }
        return listingDataSource.saveListing(uid, listingId.trim());
    }

    public Task<Void> unsaveListing(String listingId) {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        if (listingId == null || listingId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Listing ID is required"));
        }
        return listingDataSource.unsaveListing(uid, listingId.trim());
    }

    public Task<Boolean> isListingSaved(String listingId) {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        if (listingId == null || listingId.trim().isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("Listing ID is required"));
        }
        return listingDataSource.isListingSaved(uid, listingId.trim());
    }

    public Task<Set<String>> getSavedListingIds() {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        return listingDataSource.getSavedListingIds(uid);
    }

    public Task<List<Listing>> getSavedListings() {
        String uid = authDataSource.getCurrentUserId();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("Authentication required"));
        }
        return listingDataSource.getSavedListings(uid);
    }

    static Listing normalizeForCreate(String ownerId, String nickname, String species,
                                      String description, ListingType type, long price,
                                      String lookingFor, List<String> imageUrls,
                                      String locationText) {
        return normalizeForCreate(ownerId, nickname, species, description, type, price, lookingFor,
                imageUrls, locationText, ListingDetails.NONE);
    }

    static Listing normalizeForCreate(String ownerId, String nickname, String species,
                                      String description, ListingType type, long price,
                                      String lookingFor, List<String> imageUrls,
                                      String locationText, ListingDetails details) {
        if (ownerId == null || ownerId.isEmpty() || type == null) {
            throw new IllegalArgumentException("Owner and listing type are required");
        }
        String name = requiredText(nickname, 60, "nickname");
        String plantSpecies = optionalText(species, 100, "species");
        String text = requiredText(description, 1000, "description");
        String location = requiredText(locationText, 100, "location");
        if (imageUrls == null || imageUrls.size() > 5) {
            throw new IllegalArgumentException("Zero to five images are allowed");
        }
        List<String> images = new ArrayList<>(imageUrls.size());
        for (String url : imageUrls) {
            images.add(remoteImageUrl(url));
        }

        long normalizedPrice = 0;
        String normalizedLookingFor = "";
        if (type == ListingType.TRADE) {
            normalizedLookingFor = requiredText(lookingFor, 200, "lookingFor");
        } else if (type == ListingType.SELL) {
            if (price <= 0) {
                throw new IllegalArgumentException("Sell price must be positive");
            }
            normalizedPrice = price;
        }
        return new Listing(null, ownerId, name, plantSpecies, text, type,
                normalizedPrice, normalizedLookingFor, images, location,
                ListingStatus.ACTIVE, null, null, details);
    }

    private static String requiredText(String value, int maxLength, String field) {
        String result = optionalText(value, maxLength, field);
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return result;
    }

    private static String optionalText(String value, int maxLength, String field) {
        String result = value == null ? "" : value.trim();
        if (result.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return result;
    }

    private static String remoteImageUrl(String value) {
        String result = requiredText(value, Integer.MAX_VALUE, "image URL");
        if (ImageRefs.isUploaded(result)) return result;
        try {
            URI uri = new URI(result);
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null && !uri.getHost().isEmpty()) {
                return result;
            }
        } catch (URISyntaxException ignored) {
            // Normalized below as the same safe validation failure.
        }
        throw new IllegalArgumentException("image URL must be a remote HTTP(S) location");
    }
}
