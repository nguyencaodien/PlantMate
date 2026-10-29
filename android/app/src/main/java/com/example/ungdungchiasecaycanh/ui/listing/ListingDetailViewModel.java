package com.example.ungdungchiasecaycanh.ui.listing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.Review;
import com.example.ungdungchiasecaycanh.data.repository.ReviewRepository;
import com.example.ungdungchiasecaycanh.data.repository.ChatRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class ListingDetailViewModel extends ViewModel {
    public enum Status { LOADING, CONTENT, NOT_FOUND, ERROR, HIDDEN }

    public static final class UiState {
        public final Status status;
        public final Listing listing;
        public final User owner;
        public final boolean saved;
        public final boolean savedStateKnown;
        public final boolean saveInProgress;
        public final int saveErrorVersion;
        public final boolean contactEligible;
        public final boolean contactInProgress;
        public final int contactErrorVersion;

        private UiState(Status status, Listing listing, User owner, boolean saved,
                        boolean savedStateKnown, boolean saveInProgress,
                        int saveErrorVersion, boolean contactEligible,
                        boolean contactInProgress, int contactErrorVersion) {
            this.status = status;
            this.listing = listing;
            this.owner = owner;
            this.saved = saved;
            this.savedStateKnown = savedStateKnown;
            this.saveInProgress = saveInProgress;
            this.saveErrorVersion = saveErrorVersion;
            this.contactEligible = contactEligible;
            this.contactInProgress = contactInProgress;
            this.contactErrorVersion = contactErrorVersion;
        }
    }

    static final class SavedState {
        final boolean saved;
        final boolean inProgress;

        SavedState(boolean saved, boolean inProgress) {
            this.saved = saved;
            this.inProgress = inProgress;
        }
    }

    private final ListingRepository listingRepository = new ListingRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ChatRepository chatRepository = new ChatRepository();
    private final SafetyRepository safetyRepository = new SafetyRepository();
    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final MutableLiveData<UiState> state = new MutableLiveData<>();
    private String listingId;
    private Listing currentListing;
    private User currentOwner;
    private boolean saved;
    private boolean savedStateKnown;
    private boolean saveInProgress;
    private int saveErrorVersion;
    private boolean contactInProgress;
    private int contactErrorVersion;
    private String openedConversationId;
    private int loadGeneration;
    private boolean blockChecked;
    private String hiddenOwnerUid;

    public LiveData<UiState> getState() {
        return state;
    }

    public void loadListing(String listingId) {
        this.listingId = listingId;
        if (listingId == null || listingId.trim().isEmpty()) {
            state.setValue(nonContentState(Status.NOT_FOUND));
            return;
        }
        int generation = ++loadGeneration;
        currentListing = null;
        hiddenOwnerUid = null;
        blockChecked = false;
        currentOwner = null;
        saved = false;
        savedStateKnown = false;
        saveInProgress = false;
        contactInProgress = false;
        openedConversationId = null;
        state.setValue(nonContentState(Status.LOADING));
        listingRepository.getListing(listingId.trim())
                .addOnSuccessListener(listing -> onListingLoaded(generation, listing))
                .addOnFailureListener(error -> {
                    if (generation == loadGeneration) {
                        state.postValue(nonContentState(
                                isMissing(error) ? Status.NOT_FOUND : Status.ERROR));
                    }
                });
    }

    public void retry() {
        loadListing(listingId);
    }

    public void openConversation() {
        if (!canContactCurrentListing() || contactInProgress) return;
        contactInProgress = true;
        int generation = loadGeneration;
        publishContent();
        chatRepository.openOrCreateConversation(currentListing.getListingId())
                .addOnSuccessListener(id -> {
                    if (generation != loadGeneration) return;
                    contactInProgress = false;
                    openedConversationId = id;
                    publishContent();
                })
                .addOnFailureListener(error -> {
                    if (generation != loadGeneration) return;
                    contactInProgress = false;
                    contactErrorVersion++;
                    publishContent();
                });
    }

    public String consumeOpenedConversationId() {
        String id = openedConversationId;
        openedConversationId = null;
        return id;
    }

    private boolean canContactCurrentListing() {
        String uid = chatRepository.getCurrentUserId();
        return currentListing != null && uid != null
                && !uid.equals(currentListing.getOwnerId())
                && currentListing.getStatus() == ListingStatus.ACTIVE && blockChecked;
    }

    public void toggleSaved() {
        if (currentListing == null || !savedStateKnown || saveInProgress) {
            return;
        }
        boolean previous = saved;
        boolean target = !previous;
        SavedState pending = beginSavedAction(previous);
        saved = pending.saved;
        saveInProgress = pending.inProgress;
        publishContent();
        Task<Void> task = target
                ? listingRepository.saveListing(currentListing.getListingId())
                : listingRepository.unsaveListing(currentListing.getListingId());
        task.addOnSuccessListener(ignored -> completeSavedAction(previous, target, true))
                .addOnFailureListener(error -> completeSavedAction(previous, target, false));
    }

    private void onListingLoaded(int generation, Listing listing) {
        if (generation != loadGeneration) return;
        safetyRepository.isBlockedByCurrentUser(listing.getOwnerId())
                .addOnSuccessListener(blocked -> {
                    if (generation != loadGeneration) return;
                    if (blocked) { hiddenOwnerUid=listing.getOwnerId();state.postValue(nonContentState(Status.HIDDEN)); }
                    else showListing(generation, listing);
                }).addOnFailureListener(error -> {
                    if (generation == loadGeneration) state.postValue(nonContentState(Status.ERROR));
                });
    }

    private void showListing(int generation, Listing listing) {
        blockChecked = true;
        currentListing = listing;
        saveInProgress = true;
        publishContent();
        loadSimilar(generation, listing);
        loadOwnerStats(generation, listing);
        userRepository.getProfile(listing.getOwnerId())
                .addOnSuccessListener(owner -> {
                    if (generation == loadGeneration) {
                        currentOwner = owner;
                        publishContent();
                    }
                })
                .addOnFailureListener(error -> {
                    if (generation == loadGeneration) publishContent();
                });
        listingRepository.isListingSaved(listing.getListingId())
                .addOnSuccessListener(isSaved -> {
                    if (generation == loadGeneration) {
                        saved = isSaved;
                        savedStateKnown = true;
                        saveInProgress = false;
                        publishContent();
                    }
                })
                .addOnFailureListener(error -> {
                    if (generation == loadGeneration) {
                        savedStateKnown = false;
                        saveInProgress = false;
                        saveErrorVersion++;
                        publishContent();
                    }
                });
    }

    static final int MAX_SIMILAR = 6;
    private final MutableLiveData<List<Listing>> similar = new MutableLiveData<>(Collections.emptyList());

    /** Other open listings to show under this one ("similar plants nearby"); empty on failure. */
    public LiveData<List<Listing>> getSimilar() { return similar; }

    private void loadSimilar(int generation, Listing listing) {
        Task<List<Listing>> active = listingRepository.getActiveListings();
        Task<Set<String>> blocked = safetyRepository.getBlockedUids();
        Tasks.whenAllComplete(active, blocked).addOnCompleteListener(done -> {
            if (generation != loadGeneration || !active.isSuccessful()) return;
            similar.postValue(similarTo(active.getResult(), listing,
                    blocked.isSuccessful() ? blocked.getResult() : Collections.emptySet()));
        });
    }

    /** What the owner has given, swapped and still lists, and how receivers rated them. */
    public static final class OwnerStats {
        public final int given, swapped;
        public final int ratingCount;
        /** Average stars, 0 when nobody rated the owner yet. */
        public final float ratingAverage;
        /** The owner's other open listings, newest first (at most {@link #MAX_SIMILAR}, for the carousel). */
        public final List<Listing> others;
        /** All of the owner's open listings, this one included when it is open (BUG-015: not the carousel size). */
        public final int open;

        OwnerStats(int given, int swapped, int ratingCount, float ratingAverage, List<Listing> others, int open) {
            this.given = given;
            this.swapped = swapped;
            this.ratingCount = ratingCount;
            this.ratingAverage = ratingAverage;
            this.others = others;
            this.open = open;
        }
    }

    private final MutableLiveData<OwnerStats> ownerStats = new MutableLiveData<>();

    public LiveData<OwnerStats> getOwnerStats() { return ownerStats; }

    private void loadOwnerStats(int generation, Listing listing) {
        Task<List<Listing>> theirs = listingRepository.getListingsForOwner(listing.getOwnerId());
        Task<List<Review>> reviews = reviewRepository.getReviewsForUser(listing.getOwnerId());
        Tasks.whenAllComplete(theirs, reviews).addOnCompleteListener(done -> {
            if (generation != loadGeneration) return;
            List<Listing> all = theirs.isSuccessful() && theirs.getResult() != null ? theirs.getResult() : Collections.emptyList();
            List<Review> rated = reviews.isSuccessful() && reviews.getResult() != null ? reviews.getResult() : Collections.emptyList();
            ownerStats.postValue(ownerStatsOf(all, rated, listing.getListingId()));
        });
    }

    static OwnerStats ownerStatsOf(List<Listing> all, List<Review> reviews, String excludeId) {
        int given = 0, swapped = 0, open = 0;
        List<Listing> others = new ArrayList<>();
        for (Listing l : all) {
            if (l.getStatus() == ListingStatus.ACTIVE) open++;
            if (l.getStatus() == ListingStatus.REHOMED) {
                if (l.getType() == ListingType.GIVE_AWAY) given++;
                else if (l.getType() == ListingType.TRADE) swapped++;
            } else if (l.getStatus() == ListingStatus.ACTIVE && !l.getListingId().equals(excludeId)) {
                others.add(l);
            }
        }
        Collections.sort(others, (a, b) -> Long.compare(millis(b), millis(a)));
        int sum = 0;
        for (Review r : reviews) sum += r.getRating();
        float average = reviews.isEmpty() ? 0f : sum / (float) reviews.size();
        return new OwnerStats(given, swapped, reviews.size(), average,
                others.size() > MAX_SIMILAR ? new ArrayList<>(others.subList(0, MAX_SIMILAR)) : others, open);
    }

    /** Same type first, then the same area, then newest; never this listing or blocked members. */
    static List<Listing> similarTo(List<Listing> all, Listing target, Set<String> blocked) {
        List<Listing> out = new ArrayList<>();
        if (all == null || target == null) return out;
        String area = target.getLocationText() == null ? "" : target.getLocationText().trim();
        for (Listing l : all) {
            if (l == null || l.getListingId().equals(target.getListingId())) continue;
            if (l.getStatus() != ListingStatus.ACTIVE || blocked.contains(l.getOwnerId())) continue;
            out.add(l);
        }
        Collections.sort(out, (a, b) -> {
            int type = Boolean.compare(b.getType() == target.getType(), a.getType() == target.getType());
            if (type != 0) return type;
            int near = Boolean.compare(area.equalsIgnoreCase(String.valueOf(b.getLocationText()).trim()),
                    area.equalsIgnoreCase(String.valueOf(a.getLocationText()).trim()));
            if (near != 0) return near;
            return Long.compare(millis(b), millis(a));
        });
        return out.size() > MAX_SIMILAR ? new ArrayList<>(out.subList(0, MAX_SIMILAR)) : out;
    }

    private static long millis(Listing l) {
        return l.getCreatedAt() == null ? 0 : l.getCreatedAt().toDate().getTime();
    }

    public void hideBlockedListing() {
        if (currentListing != null) hiddenOwnerUid=currentListing.getOwnerId();
        loadGeneration++;
        blockChecked = false;
        currentListing = null;
        state.setValue(nonContentState(Status.HIDDEN));
    }

    public String getHiddenOwnerUid() { return hiddenOwnerUid; }
    public boolean isOwnedByCurrentUser(Listing listing) {
        String uid=chatRepository.getCurrentUserId();
        return listing!=null&&uid!=null&&uid.equals(listing.getOwnerId());
    }

    private synchronized void publishContent() {
        state.postValue(new UiState(Status.CONTENT, currentListing, currentOwner, saved,
                savedStateKnown, saveInProgress, saveErrorVersion,
                canContactCurrentListing(), contactInProgress, contactErrorVersion));
    }

    private void completeSavedAction(boolean previous, boolean target, boolean success) {
        SavedState result = finishSavedAction(previous, target, success);
        saved = result.saved;
        saveInProgress = result.inProgress;
        if (!success) saveErrorVersion++;
        publishContent();
    }

    static SavedState beginSavedAction(boolean saved) {
        return new SavedState(saved, true);
    }

    static SavedState finishSavedAction(boolean previous, boolean target, boolean success) {
        return new SavedState(success ? target : previous, false);
    }

    private UiState nonContentState(Status status) {
        return new UiState(status, null, null, false, false, false, saveErrorVersion,
                false, false, contactErrorVersion);
    }

    private static boolean isMissing(Exception error) {
        return error != null && error.getMessage() != null
                && error.getMessage().toLowerCase(Locale.ROOT).contains("does not exist");
    }

    public static String typeValue(Listing listing) {
        if (listing == null || listing.getType() == ListingType.GIVE_AWAY) {
            return "Give Away";
        }
        if (listing.getType() == ListingType.TRADE) {
            return "Looking for: " + listing.getLookingFor();
        }
        return ListingAdapter.formatVnd(listing.getPrice()) + " ₫";
    }

    /** Localized type line for the UI (Give Away / Looking for … / price). */
    public static String typeLabel(Context context, Listing listing) {
        if (listing == null || listing.getType() == ListingType.GIVE_AWAY) {
            return context.getString(R.string.listing_type_give_away_detail);
        }
        if (listing.getType() == ListingType.TRADE) {
            return context.getString(R.string.listing_type_trade_detail, listing.getLookingFor());
        }
        return context.getString(R.string.listing_type_sell_detail, ListingAdapter.formatVnd(listing.getPrice()));
    }

    /** Localized status label for the UI. */
    public static String statusLabel(Context context, ListingStatus status) {
        int res = status == ListingStatus.RESERVED ? R.string.listing_status_reserved
                : status == ListingStatus.REHOMED ? R.string.listing_status_rehomed
                : status == ListingStatus.UNAVAILABLE ? R.string.listing_status_unavailable
                : R.string.listing_status_active;
        return context.getString(res);
    }
}
