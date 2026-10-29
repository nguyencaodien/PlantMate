package com.example.ungdungchiasecaycanh.data.remote;

import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.google.firebase.firestore.AggregateSource;

public class FirebaseListingDataSource {
    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    public Task<String> createListing(Listing listing) {
        DocumentReference document = firestore.collection("listings").document();
        Map<String, Object> fields = listingCreateFields(listing);
        return document.set(fields).continueWith(task -> {
            if (!task.isSuccessful()) {
                throw failure(task.getException());
            }
            return document.getId();
        });
    }

    static Map<String, Object> listingCreateFields(Listing listing) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("ownerId", listing.getOwnerId());
        fields.put("nickname", listing.getNickname());
        fields.put("species", listing.getSpecies());
        fields.put("description", listing.getDescription());
        fields.put("type", listing.getType().name());
        fields.put("price", listing.getPrice());
        fields.put("lookingFor", listing.getLookingFor());
        fields.put("imageUrls", listing.getImageUrls());
        fields.put("locationText", listing.getLocationText());
        fields.put("status", ListingStatus.ACTIVE.name());
        fields.put("createdAt", FieldValue.serverTimestamp());
        fields.put("updatedAt", FieldValue.serverTimestamp());
        // Optional details are written only when given, so plain listings keep the canonical fields.
        ListingDetails details = listing.getDetails();
        if (!details.getCondition().isEmpty()) fields.put("condition", ListingDetails.names(details.getCondition()));
        if (details.getHeightCm() > 0) fields.put("heightCm", (long) details.getHeightCm());
        if (details.getLight() != null) fields.put("light", details.getLight().name());
        if (!details.getPickupTimes().isEmpty()) fields.put("pickupTimes", ListingDetails.names(details.getPickupTimes()));
        if (details.getCategory() != null) fields.put("category", details.getCategory().name());
        if (details.getPotCm() > 0) fields.put("potCm", (long) details.getPotCm());
        if (details.getAge() != null) fields.put("age", details.getAge().name());
        if (details.isNegotiable()) fields.put("negotiable", true);
        return fields;
    }

    static ListingDetails mapDetails(DocumentSnapshot document) {
        Object condition = document.get("condition");
        Object pickup = document.get("pickupTimes");
        Long height = document.getLong("heightCm");
        Long pot = document.getLong("potCm");
        return new ListingDetails(
                ListingDetails.parse(ListingDetails.Condition.class, condition instanceof List<?> ? (List<?>) condition : null),
                height == null ? 0 : height.intValue(),
                LightLevel.fromName(document.getString("light")),
                ListingDetails.parse(ListingDetails.PickupTime.class, pickup instanceof List<?> ? (List<?>) pickup : null),
                ListingDetails.parseOne(ListingDetails.Category.class, document.get("category")),
                pot == null ? 0 : pot.intValue(),
                ListingDetails.parseOne(ListingDetails.Age.class, document.get("age")),
                Boolean.TRUE.equals(document.getBoolean("negotiable")));
    }

    public Task<Listing> getListing(String listingId) {
        return firestore.collection("listings").document(listingId).get().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw failure(task.getException());
            }
            DocumentSnapshot document = task.getResult();
            if (document == null || !document.exists()) {
                throw new IllegalStateException("Listing does not exist");
            }
            return map(document);
        });
    }

    /**
     * Newest active listings (Home filters and searches these on the device). The cap bounds
     * reads as the community grows; it uses the (status, createdAt) index in firestore.indexes.json.
     */
    public static final int HOME_LIMIT = 300;

    public Task<List<Listing>> getActiveListings() {
        return firestore.collection("listings").whereEqualTo("status", "ACTIVE")
                .orderBy("createdAt", Query.Direction.DESCENDING).limit(HOME_LIMIT)
                .get().continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw failure(task.getException());
                    }
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        listings.add(map(document));
                    }
                    return listings;
                });
    }

    /** One page of the newest active listings; {@code cursor} is the previous page's (null first). */
    public static final class Page {
        public final List<Listing> listings;
        /** Opaque: pass back for the next page. */
        public final Object cursor;
        public final boolean hasMore;

        public Page(List<Listing> listings, Object cursor, boolean hasMore) {
            this.listings = listings; this.cursor = cursor; this.hasMore = hasMore;
        }
    }

    public Task<Page> getActiveListingsPage(Object cursor, int size) {
        Query query = firestore.collection("listings").whereEqualTo("status", "ACTIVE")
                .orderBy("createdAt", Query.Direction.DESCENDING);
        if (cursor instanceof DocumentSnapshot) query = query.startAfter((DocumentSnapshot) cursor);
        return query.limit(size).get().continueWith(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            List<DocumentSnapshot> documents = task.getResult().getDocuments();
            List<Listing> listings = new ArrayList<>();
            for (DocumentSnapshot document : documents) listings.add(map(document));
            Object next = documents.isEmpty() ? cursor : documents.get(documents.size() - 1);
            return new Page(listings, next, documents.size() == size);
        });
    }

    /**
     * Sale listings that are reserved or sold, most recently changed first, for the status pills on Home.
     * Uses the (type, status, updatedAt) index in firestore.indexes.json.
     */
    public Task<List<Listing>> getSaleUpdates(int limit) {
        return firestore.collection("listings").whereEqualTo("type", ListingType.SELL.name())
                .whereIn("status", Arrays.asList(ListingStatus.RESERVED.name(), ListingStatus.REHOMED.name()))
                .orderBy("updatedAt", Query.Direction.DESCENDING).limit(limit)
                .get().continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) listings.add(map(document));
                    return listings;
                });
    }

    /** Active listings of one type, counted on the server (one read per 1,000 listings). */
    public Task<Integer> countActive(ListingType type) {
        return OfflineCounts.count(firestore.collection("listings").whereEqualTo("status", "ACTIVE")
                .whereEqualTo("type", type.name()), "active:" + type.name())
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    return task.getResult().intValue();
                });
    }

    public Task<List<Listing>> getMyListings(String uid) {
        return firestore.collection("listings").whereEqualTo("ownerId", uid).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    List<Listing> listings = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        listings.add(map(document));
                    }
                    Collections.sort(listings, Comparator.comparing(
                            Listing::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
                    return listings;
                });
    }

    public Task<Void> updateListingStatus(String listingId, ListingStatus status) {
        return firestore.collection("listings").document(listingId)
                .update(statusUpdateFields(status));
    }

    static Map<String, Object> statusUpdateFields(ListingStatus status) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", status.name());
        fields.put("updatedAt", FieldValue.serverTimestamp());
        return fields;
    }

    public Task<Void> saveListing(String uid, String listingId) {
        return firestore.collection("users").document(uid)
                .collection("savedListings").document(listingId)
                .set(savedReferenceFields());
    }

    public Task<Void> unsaveListing(String uid, String listingId) {
        return firestore.collection("users").document(uid)
                .collection("savedListings").document(listingId).delete();
    }

    public Task<Boolean> isListingSaved(String uid, String listingId) {
        return firestore.collection("users").document(uid)
                .collection("savedListings").document(listingId).get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw failure(task.getException());
                    }
                    DocumentSnapshot document = task.getResult();
                    return document != null && document.exists();
                });
    }

    /** IDs of the listings the member saved (references only; no listing reads). */
    public Task<Set<String>> getSavedListingIds(String uid) {
        return firestore.collection("users").document(uid).collection("savedListings").get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) throw failure(task.getException());
                    Set<String> ids = new HashSet<>();
                    for (QueryDocumentSnapshot reference : task.getResult()) ids.add(reference.getId());
                    return ids;
                });
    }

    public Task<List<Listing>> getSavedListings(String uid) {
        return firestore.collection("users").document(uid).collection("savedListings")
                .orderBy("savedAt", Query.Direction.DESCENDING).get()
                .continueWithTask(referenceTask -> {
                    if (!referenceTask.isSuccessful()) {
                        throw failure(referenceTask.getException());
                    }
                    List<String> listingIds = new ArrayList<>();
                    List<Task<Listing>> reads = new ArrayList<>();
                    for (QueryDocumentSnapshot reference : referenceTask.getResult()) {
                        listingIds.add(reference.getId());
                        reads.add(getListing(reference.getId()));
                    }
                    if (reads.isEmpty()) {
                        return Tasks.forResult(new ArrayList<>());
                    }
                    List<Task<?>> allReads = new ArrayList<>(reads);
                    return Tasks.whenAllComplete(allReads).continueWith(completed -> {
                        Map<String, Listing> listingsById = new HashMap<>();
                        for (int index = 0; index < reads.size(); index++) {
                            Task<Listing> read = reads.get(index);
                            if (read.isSuccessful()) {
                                listingsById.put(listingIds.get(index), read.getResult());
                            } else if (!isMissingListing(read.getException())) {
                                throw failure(read.getException());
                            }
                        }
                        return selectExistingListings(listingIds, listingsById);
                    });
                });
    }

    static Map<String, Object> savedReferenceFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put("savedAt", FieldValue.serverTimestamp());
        return fields;
    }

    static List<Listing> selectExistingListings(List<String> orderedIds,
                                                 Map<String, Listing> listingsById) {
        List<Listing> result = new ArrayList<>();
        for (String listingId : orderedIds) {
            Listing listing = listingsById.get(listingId);
            if (listing != null) result.add(listing);
        }
        return result;
    }

    static Listing map(DocumentSnapshot document) {
        try {
            Object rawImages = document.get("imageUrls");
            if (!(rawImages instanceof List<?>)) {
                throw new IllegalArgumentException("Invalid image list");
            }
            List<String> images = new ArrayList<>();
            for (Object image : (List<?>) rawImages) {
                if (!(image instanceof String)) {
                    throw new IllegalArgumentException("Invalid image URL");
                }
                images.add((String) image);
            }
            Long price = document.getLong("price");
            if (price == null) {
                throw new IllegalArgumentException("Missing price");
            }
            if (document.getTimestamp("createdAt") == null
                    || document.getTimestamp("updatedAt") == null) {
                throw new IllegalArgumentException("Missing timestamps");
            }
            return new Listing(document.getId(), requiredString(document, "ownerId"),
                    requiredString(document, "nickname"), requiredString(document, "species"),
                    requiredString(document, "description"),
                    ListingType.valueOf(requiredString(document, "type")), price,
                    requiredString(document, "lookingFor"), images,
                    requiredString(document, "locationText"),
                    ListingStatus.valueOf(requiredString(document, "status")),
                    document.getTimestamp("createdAt"), document.getTimestamp("updatedAt"),
                    mapDetails(document));
        } catch (IllegalArgumentException | NullPointerException error) {
            throw new IllegalStateException("Invalid listing data: " + document.getId(), error);
        }
    }

    private static String requiredString(DocumentSnapshot document, String field) {
        String value = document.getString(field);
        if (value == null) {
            throw new IllegalArgumentException("Missing " + field);
        }
        return value;
    }

    private static Exception failure(Exception cause) {
        return cause == null ? new IllegalStateException("Listing request failed") : cause;
    }

    private static boolean isMissingListing(Exception error) {
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().toLowerCase(Locale.ROOT)
                    .contains("does not exist")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
