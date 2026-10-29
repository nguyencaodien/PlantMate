package com.example.ungdungchiasecaycanh.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.AuthRepository;
import com.example.ungdungchiasecaycanh.data.repository.ChatRepository;
import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.ReviewRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.Review;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.review.ReviewSummary;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ProfileViewModel extends ViewModel {

    public enum Status {
        IDLE, LOADING, LOADED, ERROR, SIGNED_OUT
    }

    public static final class ProfileUiState {
        public final Status status;
        public final User user;

        private ProfileUiState(Status status, User user) {
            this.status = status;
            this.user = user;
        }
    }

    /** Counts shown on the profile; each is -1 until its source loads (then shown as a dash on failure). */
    public static final class Stats {
        public final int shared, active, rehomed, saved, chats;
        public final ReviewSummary reviews;
        /** The newest review received, or null. */
        public final Review latest;

        Stats(int shared, int active, int rehomed, int saved, int chats, ReviewSummary reviews, Review latest) {
            this.shared = shared; this.active = active; this.rehomed = rehomed;
            this.saved = saved; this.chats = chats; this.reviews = reviews; this.latest = latest;
        }
    }

    /** The three Profile tabs. */
    public enum Tab { MINE, GIVEN, SAVED }

    /** Listings behind the tabs and the newest conversation; a null list means it failed to load. */
    public static final class Lists {
        public final List<Listing> mine, given, saved;
        /** Every listing of the member, whatever its status (the "Listings" total). */
        public final List<Listing> all;
        public final int chatCount;
        public final String chatPreview, chatPeerName, chatPeerAvatar;
        /** Conversations could not be loaded (and none were loaded before): the inbox row says so. */
        public final boolean chatsFailed;

        Lists(List<Listing> all, List<Listing> mine, List<Listing> given, List<Listing> saved, int chatCount,
              String chatPreview, String chatPeerName, String chatPeerAvatar, boolean chatsFailed) {
            this.all = all; this.mine = mine; this.given = given; this.saved = saved; this.chatCount = chatCount;
            this.chatPreview = chatPreview; this.chatPeerName = chatPeerName; this.chatPeerAvatar = chatPeerAvatar;
            this.chatsFailed = chatsFailed;
        }
    }

    private final MutableLiveData<Lists> lists = new MutableLiveData<>();
    private final MutableLiveData<Integer> actionErrors = new MutableLiveData<>(0);
    /** Selected tab, kept across rotation. */
    Tab tab = Tab.MINE;

    public LiveData<Lists> getLists() { return lists; }
    public LiveData<Integer> getActionErrors() { return actionErrors; }

    private final AuthRepository authRepository = new AuthRepository();
    private final ListingRepository listingRepository = new ListingRepository();
    private final ChatRepository chatRepository = new ChatRepository();
    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final MutableLiveData<Stats> stats = new MutableLiveData<>();
    private final MutableLiveData<User> latestReviewer = new MutableLiveData<>();
    private final UserRepository userRepository = new UserRepository();
    private final FriendRepository friendRepository = new FriendRepository();
    private final MutableLiveData<Integer> friendRequests = new MutableLiveData<>(0);
    private final MutableLiveData<ProfileUiState> state =
            new MutableLiveData<>(new ProfileUiState(Status.IDLE, null));

    public LiveData<ProfileUiState> getState() {
        return state;
    }

    /** Email/password accounts that still need to confirm their address (a gentle reminder). */
    public enum Verification { HIDDEN, NEEDED, SENDING, SENT, FAILED, STILL_UNVERIFIED }

    private final MutableLiveData<Verification> verification = new MutableLiveData<>(Verification.HIDDEN);

    public LiveData<Verification> getVerification() {
        return verification;
    }

    /** Firebase has confirmed this account's email (Google accounts always). */
    public boolean isEmailVerified() {
        return authRepository.isEmailVerified();
    }

    private void syncBadge() {
        ProfileUiState current = state.getValue();
        if (current != null && current.user != null) userRepository.syncEmailVerified(current.user);
    }

    public String getCurrentUserEmail() {
        return authRepository.getCurrentUserEmail();
    }

    /** Reloads the account so a link opened in the mail app is picked up, then shows or hides the card. */
    public void checkVerification() {
        authRepository.reloadCurrentUser().addOnCompleteListener(done -> {
            if (!authRepository.needsEmailVerification()) {
                verification.postValue(Verification.HIDDEN);
                syncBadge();
                return;
            }
            // Keep "sent" / "not yet" feedback; only a hidden card becomes the plain reminder.
            Verification current = verification.getValue();
            if (current == null || current == Verification.HIDDEN) verification.postValue(Verification.NEEDED);
        });
    }

    /** "I've verified": reload and say so if the link has not been opened yet. */
    public void confirmVerified() {
        authRepository.reloadCurrentUser().addOnCompleteListener(done -> {
            boolean waiting = authRepository.needsEmailVerification();
            verification.postValue(waiting ? Verification.STILL_UNVERIFIED : Verification.HIDDEN);
            if (!waiting) syncBadge();
        });
    }

    public void resendVerification() {
        if (verification.getValue() == Verification.SENDING) return;
        verification.setValue(Verification.SENDING);
        authRepository.sendEmailVerification()
                .addOnSuccessListener(ignored -> verification.postValue(Verification.SENT))
                .addOnFailureListener(error -> verification.postValue(Verification.FAILED));
    }

    public LiveData<Stats> getStats() {
        return stats;
    }

    /** Friend requests waiting for this member (the "Friends" row badge); 0 when unknown. */
    public LiveData<Integer> getFriendRequests() { return friendRequests; }

    /** Author of {@link Stats#latest}, once their profile loads. */
    public LiveData<User> getLatestReviewer() { return latestReviewer; }

    public void loadFriendRequests() {
        String uid = authRepository.getCurrentUserId();
        if (uid == null) { friendRequests.setValue(0); return; }
        friendRepository.getIncomingCount().addOnCompleteListener(task -> {
            if (uid.equals(authRepository.getCurrentUserId()))
                friendRequests.setValue(task.isSuccessful() ? task.getResult() : 0);
        });
    }

    /** Loads the profile numbers from the account's real listings, saves, chats and reviews. */
    public void loadStats() {
        String uid = authRepository.getCurrentUserId();
        if (uid == null) return;
        Task<List<Listing>> mine = listingRepository.getMyListings();
        Task<List<Listing>> saved = listingRepository.getSavedListings();
        Task<List<Conversation>> chats = chatRepository.getConversationsForCurrentUser();
        Task<List<Review>> reviews = reviewRepository.getReviewsForUser(uid);
        Tasks.whenAllComplete(mine, saved, chats, reviews).addOnCompleteListener(done -> {
            List<Listing> own = mine.isSuccessful() ? mine.getResult() : null;
            postLists(uid, own, saved.isSuccessful() ? saved.getResult() : null,
                    chats.isSuccessful() ? chats.getResult() : null);
            stats.postValue(new Stats(
                    own == null ? -1 : own.size(),
                    own == null ? -1 : countStatus(own, ListingStatus.ACTIVE),
                    own == null ? -1 : countStatus(own, ListingStatus.REHOMED),
                    saved.isSuccessful() && saved.getResult() != null ? saved.getResult().size() : -1,
                    chats.isSuccessful() && chats.getResult() != null ? chats.getResult().size() : -1,
                    reviews.isSuccessful() ? ReviewSummary.from(reviews.getResult()) : ReviewSummary.from(null),
                    reviews.isSuccessful() ? newest(reviews.getResult()) : null));
            Review newest = reviews.isSuccessful() ? newest(reviews.getResult()) : null;
            if (newest != null && newest.getReviewerId() != null) {
                userRepository.getProfile(newest.getReviewerId()).addOnSuccessListener(user -> {
                    if (uid.equals(authRepository.getCurrentUserId())) latestReviewer.setValue(user);
                });
            }
        });
    }

    /**
     * Publishes the tabs and inbox row. A part that failed to refresh keeps what was shown before (offline
     * after a good load); with nothing shown before it stays null and the screen shows its error state.
     */
    private void postLists(String uid, List<Listing> own, List<Listing> saved, List<Conversation> chats) {
        Lists previous = lists.getValue();
        List<Listing> open = own != null ? byStatus(own, ListingStatus.ACTIVE) : previous == null ? null : previous.mine;
        List<Listing> given = own != null ? byStatus(own, ListingStatus.REHOMED) : previous == null ? null : previous.given;
        List<Listing> all = own != null ? own : previous == null ? null : previous.all;
        List<Listing> keptSaved = saved != null ? saved : previous == null ? null : previous.saved;
        if (chats == null) {
            boolean hadChats = previous != null && !previous.chatsFailed;
            lists.postValue(hadChats
                    ? new Lists(all, open, given, keptSaved, previous.chatCount, previous.chatPreview,
                            previous.chatPeerName, previous.chatPeerAvatar, false)
                    : new Lists(all, open, given, keptSaved, 0, "", "", "", true));
            return;
        }
        Conversation latest = latest(chats);
        int count = uid == null ? chats.size() // one per person, as the inbox shows them
                : com.example.ungdungchiasecaycanh.ui.inbox.InboxViewModel.onePerPeer(chats, uid).size();
        lists.postValue(new Lists(all, open, given, keptSaved, count, latest == null ? "" : latest.getLastMessage(),
                "", "", false));
        String peer = peerOf(latest, uid);
        if (peer == null) return;
        userRepository.getProfile(peer).addOnSuccessListener(user -> {
            if (user == null || !uid.equals(authRepository.getCurrentUserId())) return;
            lists.postValue(new Lists(all, open, given, keptSaved, count, latest.getLastMessage(),
                    user.getDisplayName(), user.getAvatarUrl(), false));
        });
    }

    /** Newest review by creation time; one without a time counts as oldest. */
    static Review newest(List<Review> reviews) {
        Review best = null;
        if (reviews == null) return null;
        for (Review r : reviews) {
            if (best == null || seconds(r) > seconds(best)) best = r;
        }
        return best;
    }

    private static long seconds(Review r) {
        return r.getCreatedAt() == null ? Long.MIN_VALUE : r.getCreatedAt().getSeconds();
    }

    /**
     * The member's listings with one status: "Open" counts only ACTIVE ones, matching the public profile and
     * the "Open" filter of My listings (BUG-019: hidden and reserved ones are not "open").
     */
    static List<Listing> byStatus(List<Listing> listings, ListingStatus status) {
        List<Listing> out = new ArrayList<>();
        for (Listing l : listings) if (l != null && l.getStatus() == status) out.add(l);
        return out;
    }

    /** Rehomed listings of one type (the card's "gave away N, traded N"). */
    static int countRehomed(List<Listing> listings, ListingType type) {
        int n = 0;
        if (listings != null) for (Listing l : listings) {
            if (l != null && l.getStatus() == ListingStatus.REHOMED && l.getType() == type) n++;
        }
        return n;
    }

    static Conversation latest(List<Conversation> chats) {
        Conversation best = null;
        if (chats != null) for (Conversation c : chats) {
            if (c == null) continue;
            if (best == null || millis(c) > millis(best)) best = c;
        }
        return best;
    }

    private static long millis(Conversation c) {
        Timestamp t = c.getUpdatedAt() != null ? c.getUpdatedAt() : c.getCreatedAt();
        return t == null ? 0 : t.toDate().getTime();
    }

    static String peerOf(Conversation c, String uid) {
        if (c == null || uid == null || c.getParticipantIds() == null) return null;
        for (String id : c.getParticipantIds()) if (id != null && !id.equals(uid)) return id;
        return null;
    }

    /** Marks one of the member's listings with a new status, then reloads the tabs. */
    public void updateStatus(Listing listing, ListingStatus target) {
        listingRepository.updateListingStatus(listing.getListingId(), target)
                .addOnSuccessListener(ignored -> loadStats())
                .addOnFailureListener(error -> failAction());
    }

    /** The owner's active listing that has gone longest without an update past 14 days, or null. */
    static Listing listingToCheck(List<Listing> mine, long now) {
        Listing oldest = null;
        if (mine != null) for (Listing l : mine) {
            if (!ListingRepository.needsCheck(l, now)) continue;
            if (oldest == null || updated(l) < updated(oldest)) oldest = l;
        }
        return oldest;
    }

    private static long updated(Listing l) {
        Timestamp t = l.getUpdatedAt() != null ? l.getUpdatedAt() : l.getCreatedAt();
        return t == null ? Long.MAX_VALUE : t.toDate().getTime();
    }

    public void confirmStillAvailable(Listing listing) {
        listingRepository.confirmStillAvailable(listing.getListingId())
                .addOnSuccessListener(ignored -> loadStats())
                .addOnFailureListener(error -> failAction());
    }

    public void unsave(String listingId) {
        listingRepository.unsaveListing(listingId)
                .addOnSuccessListener(ignored -> loadStats())
                .addOnFailureListener(error -> failAction());
    }

    private void failAction() {
        Integer n = actionErrors.getValue();
        actionErrors.setValue(n == null ? 1 : n + 1);
    }

    static int countStatus(List<Listing> listings, ListingStatus status) {
        int n = 0;
        if (listings != null) for (Listing l : listings) if (l != null && l.getStatus() == status) n++;
        return n;
    }

    public String getCurrentUserId() {
        return authRepository.getCurrentUserId();
    }

    public void loadProfile() {
        ProfileUiState current = state.getValue();
        if (current != null && (current.status == Status.LOADING || current.status == Status.LOADED)) {
            return;
        }
        String uid = authRepository.getCurrentUserId();
        if (uid == null) {
            state.setValue(new ProfileUiState(Status.SIGNED_OUT, null));
            return;
        }
        state.setValue(new ProfileUiState(Status.LOADING, null));
        userRepository.getProfile(uid)
                .addOnSuccessListener(user -> {
                    state.postValue(new ProfileUiState(Status.LOADED, user));
                    userRepository.syncEmailVerified(user);
                })
                .addOnFailureListener(error -> state.postValue(new ProfileUiState(Status.ERROR, null)));
    }

    /** Fetches the profile again without a loading state (after a verification screen closes). */
    public void refreshProfile() {
        ProfileUiState current = state.getValue();
        if (current == null || current.status != Status.LOADED) return;
        userRepository.getCurrentProfile().addOnSuccessListener(user -> {
            if (user != null) state.postValue(new ProfileUiState(Status.LOADED, user));
        });
    }

    public void retry() {
        state.setValue(new ProfileUiState(Status.IDLE, null));
        loadProfile();
    }

    public void signOut() {
        authRepository.signOut();
        state.setValue(new ProfileUiState(Status.SIGNED_OUT, null));
    }
}
