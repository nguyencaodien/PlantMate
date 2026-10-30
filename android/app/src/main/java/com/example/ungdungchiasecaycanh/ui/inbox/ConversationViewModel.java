package com.example.ungdungchiasecaycanh.ui.inbox;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.ChatRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.data.repository.ReviewRepository;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.MessageType;
import com.example.ungdungchiasecaycanh.model.Message;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Locale;

import android.content.Context;
import android.net.Uri;

public class ConversationViewModel extends ViewModel {
    public enum Status { LOADING, CONTENT, NOT_FOUND, ERROR }

    public static final class UiState {
        public final Status status;
        public final Conversation conversation;
        public final Listing listing;
        public final User peer;
        public final List<Message> messages;
        public final String currentUid;
        public final boolean sending;
        public final int sentVersion;
        public final int sendErrorVersion;
        public final int summaryWarningVersion;
        public final int refreshErrorVersion;
        public final ReviewRepository.Eligibility reviewEligibility;
        public final boolean reviewSubmitting;
        public final int reviewSuccessVersion;
        public final int reviewErrorVersion;
        public final String peerUid;
        public final boolean ownBlockKnown, ownBlocked;
        public final int mediaSentVersion, mediaErrorVersion;
        /** Listings of the plant cards in the conversation, by ID. */
        public final Map<String, Listing> cardListings;

        private UiState(Status status, Conversation conversation, Listing listing, User peer,
                        List<Message> messages, String currentUid, boolean sending,
                        int sentVersion, int sendErrorVersion, int summaryWarningVersion,
                        int refreshErrorVersion,
                        ReviewRepository.Eligibility reviewEligibility,
                        boolean reviewSubmitting, int reviewSuccessVersion,
                        int reviewErrorVersion, String peerUid, boolean ownBlockKnown,
                        boolean ownBlocked, int mediaSentVersion, int mediaErrorVersion,
                        Map<String, Listing> cardListings) {
            this.cardListings = Collections.unmodifiableMap(new HashMap<>(cardListings));
            this.status = status;
            this.conversation = conversation;
            this.listing = listing;
            this.peer = peer;
            this.messages = new ArrayList<>(messages);
            this.currentUid = currentUid;
            this.sending = sending;
            this.sentVersion = sentVersion;
            this.sendErrorVersion = sendErrorVersion;
            this.summaryWarningVersion = summaryWarningVersion;
            this.refreshErrorVersion = refreshErrorVersion;
            this.reviewEligibility = reviewEligibility;
            this.reviewSubmitting = reviewSubmitting;
            this.reviewSuccessVersion = reviewSuccessVersion;
            this.reviewErrorVersion = reviewErrorVersion;
            this.peerUid = peerUid;
            this.ownBlockKnown = ownBlockKnown;
            this.ownBlocked = ownBlocked;
            this.mediaSentVersion = mediaSentVersion;
            this.mediaErrorVersion = mediaErrorVersion;
        }
    }

    private final ChatRepository chatRepository = new ChatRepository();
    private final ListingRepository listingRepository = new ListingRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final SafetyRepository safetyRepository = new SafetyRepository();
    private final MutableLiveData<UiState> state = new MutableLiveData<>();
    private String conversationId;
    private String currentUid;
    private Conversation conversation;
    private Listing listing;
    private User peer;
    private List<Message> messages = Collections.emptyList();
    private boolean sending;
    private int sentVersion, sendErrorVersion, summaryWarningVersion, refreshErrorVersion;
    private ReviewRepository.Eligibility reviewEligibility;
    private boolean reviewSubmitting;
    private int reviewSuccessVersion, reviewErrorVersion;
    private int loadGeneration;
    private boolean ownBlockKnown, ownBlocked;
    private int mediaSentVersion, mediaErrorVersion;
    /** The listing this chat was opened from ("Ask the owner"): its card goes before the next message. */
    private String askListingId;
    private final Map<String, Listing> cardListings = new HashMap<>();
    private final Set<String> cardRequested = new HashSet<>();
    /** Live message updates while the screen is visible. */
    private ChatRepository.Subscription messagesSubscription;
    private boolean liveWanted;

    public LiveData<UiState> getState() { return state; }

    /** Called from onStart/onStop: listen for new messages only while the chat is on screen. */
    public void setLive(boolean live) {
        liveWanted = live;
        if (live) startListening();
        else stopListening();
    }

    private void startListening() {
        if (!liveWanted || messagesSubscription != null || conversation == null || currentUid == null) return;
        int generation = loadGeneration;
        String uid = currentUid;
        messagesSubscription = chatRepository.listenMessages(conversationId, values -> {
            if (generation != loadGeneration || !uid.equals(chatRepository.getCurrentUserId())) return;
            messages = values;
            loadCardListings(generation);
            publish(Status.CONTENT);
        }, error -> {
            // Keep the last messages on screen; the refresh button still works.
            if (generation == loadGeneration) stopListening();
        });
    }

    private void stopListening() {
        if (messagesSubscription != null) messagesSubscription.cancel();
        messagesSubscription = null;
    }

    @Override protected void onCleared() {
        stopListening();
        super.onCleared();
    }

    /** Opened from a listing's "Ask the owner": a plant card for it precedes the member's next message. */
    public void loadConversation(String id, String askListing) {
        // Kept across a rotation; the "latest card" check stops a second card for the same plant.
        if (askListing != null && !askListing.trim().isEmpty()) askListingId = askListing.trim();
        loadConversation(id);
    }

    public void loadConversation(String id) {
        stopListening();
        cardListings.clear();
        cardRequested.clear();
        conversationId = id;
        int generation = ++loadGeneration;
        conversation = null;
        listing = null;
        peer = null;
        messages = Collections.emptyList();
        sending = false;
        reviewSubmitting = false;
        reviewEligibility = null;
        ownBlockKnown = false;
        ownBlocked = false;
        currentUid = chatRepository.getCurrentUserId();
        if (id == null || id.trim().isEmpty()) {
            publish(Status.NOT_FOUND);
            return;
        }
        if (currentUid == null) {
            publish(Status.ERROR);
            return;
        }
        conversationId = id.trim();
        publish(Status.LOADING);
        chatRepository.getConversation(conversationId)
                .addOnSuccessListener(value -> loadContext(generation, value))
                .addOnFailureListener(error -> {
                    if (generation == loadGeneration) {
                        publish(isMissing(error) ? Status.NOT_FOUND : Status.ERROR);
                    }
                });
    }

    public void retry() { loadConversation(conversationId); }

    public void refreshBlockState() {
        String target = peerId();
        if (target == null) return;
        int generation = loadGeneration;
        ownBlockKnown = false;
        publish(Status.CONTENT);
        safetyRepository.isBlockedByCurrentUser(target).addOnSuccessListener(value -> {
            if (generation != loadGeneration) return;
            ownBlockKnown = true;
            ownBlocked = value;
            publish(Status.CONTENT);
        }).addOnFailureListener(error -> {
            if (generation != loadGeneration) return;
            ownBlockKnown = false;
            refreshErrorVersion++;
            publish(Status.CONTENT);
        });
    }

    public void refreshMessages() {
        if (conversation == null || currentUid == null) return;
        String uid = currentUid;
        int generation = loadGeneration;
        chatRepository.getMessages(conversationId)
                .addOnSuccessListener(values -> {
                    if (generation == loadGeneration && uid.equals(chatRepository.getCurrentUserId())) {
                        messages = values;
                        loadCardListings(generation);
                        publish(Status.CONTENT);
                    }
                })
                .addOnFailureListener(error -> {
                    if (generation == loadGeneration) {
                        refreshErrorVersion++;
                        publish(Status.CONTENT);
                    }
                });
    }

    public void sendTextMessage(String text) {
        if (!canSend()) return;
        try {
            ChatRepository.normalizeMessageText(text);
        } catch (IllegalArgumentException error) {
            sendErrorVersion++;
            publish(Status.CONTENT);
            return;
        }
        sending = true;
        int generation = loadGeneration;
        publish(Status.CONTENT);
        withCard(() -> chatRepository.sendTextMessage(conversationId, text))
                .addOnSuccessListener(summaryUpdated -> {
                    if (generation != loadGeneration) return;
                    sending = false;
                    sentVersion++;
                    if (!summaryUpdated) summaryWarningVersion++;
                    publish(Status.CONTENT);
                    refreshMessages();
                })
                .addOnFailureListener(error -> {
                    if (generation != loadGeneration) return;
                    sending = false;
                    sendErrorVersion++;
                    publish(Status.CONTENT);
                });
    }

    /** Sends a photo from the picker; {@code summary} is the localized inbox preview ("Photo"). */
    public void sendImage(Context context, Uri uri, String summary) {
        if (uri == null || !canSend()) return;
        sendMedia(withCard(() -> chatRepository.sendImageMessage(context, conversationId, uri.toString(), summary)), null);
    }

    /** Sends a recorded voice note (0.5–60 s); the temporary file is removed afterwards. */
    public void sendVoice(File audio, long durationMs, String summary) {
        if (audio == null) return;
        if (!canSend()) { audio.delete(); return; }
        sendMedia(withCard(() -> chatRepository.sendVoiceMessage(conversationId, audio, durationMs, summary)), audio);
    }

    /**
     * Sends the plant card first when this chat was opened from a listing that is not already the latest
     * card here (a second plant of the same member gets its own card), then the message itself.
     */
    private Task<Boolean> withCard(java.util.function.Supplier<Task<Boolean>> send) {
        String card = askListingId;
        if (card == null || card.equals(latestCardListingId(messages))) return send.get();
        return chatRepository.sendListingMessage(conversationId, card).continueWithTask(sent -> {
            if (!sent.isSuccessful()) throw sent.getException() == null
                    ? new IllegalStateException("Plant card not sent") : sent.getException();
            askListingId = null;
            return send.get();
        });
    }

    /** The listing of the newest plant card, or null when the conversation has none. */
    static String latestCardListingId(List<Message> values) {
        if (values == null) return null;
        for (int i = values.size() - 1; i >= 0; i--) {
            Message m = values.get(i);
            if (m != null && m.getType() == MessageType.LISTING) return m.getListingId();
        }
        return null;
    }

    /** Reads each card's listing once; a missing one keeps the "listing unavailable" card. */
    private void loadCardListings(int generation) {
        for (Message m : messages) {
            String id = m.getType() == MessageType.LISTING ? m.getListingId() : "";
            if (id.isEmpty() || !cardRequested.add(id)) continue;
            if (listing != null && id.equals(listing.getListingId())) {
                cardListings.put(id, listing);
                continue;
            }
            listingRepository.getListing(id).addOnSuccessListener(value -> {
                if (generation != loadGeneration || value == null) return;
                cardListings.put(id, value);
                publish(Status.CONTENT);
            });
        }
    }

    private boolean canSend() {
        return canSend(conversation != null && currentUid != null, sending, ownBlockKnown, ownBlocked);
    }

    /** Nothing is sent before the chat is loaded, during a send, or until the block state is known and clear. */
    static boolean canSend(boolean loaded, boolean sending, boolean blockKnown, boolean blocked) {
        return loaded && !sending && blockKnown && !blocked;
    }

    private void sendMedia(Task<Boolean> task, File cleanup) {
        sending = true;
        int generation = loadGeneration;
        publish(Status.CONTENT);
        task.addOnCompleteListener(done -> {
            if (cleanup != null) cleanup.delete();
            if (generation != loadGeneration) return;
            sending = false;
            if (done.isSuccessful()) {
                if (!Boolean.TRUE.equals(done.getResult())) summaryWarningVersion++;
                mediaSentVersion++;
                publish(Status.CONTENT);
                refreshMessages();
            } else {
                mediaErrorVersion++;
                publish(Status.CONTENT);
            }
        });
    }

    public void submitReview(int rating, String comment) {
        if (conversation == null || listing == null || currentUid == null
                || reviewEligibility != ReviewRepository.Eligibility.ELIGIBLE
                || reviewSubmitting) return;
        String revieweeId = peerId();
        if (revieweeId == null) return;
        reviewSubmitting = true;
        int generation = loadGeneration;
        publish(Status.CONTENT);
        reviewRepository.submitReview(listing.getListingId(), revieweeId, rating, comment)
                .addOnSuccessListener(ignored -> {
                    if (generation != loadGeneration) return;
                    reviewSubmitting = false;
                    reviewEligibility = ReviewRepository.Eligibility.ALREADY_REVIEWED;
                    reviewSuccessVersion++;
                    publish(Status.CONTENT);
                })
                .addOnFailureListener(error -> {
                    if (generation != loadGeneration) return;
                    reviewSubmitting = false;
                    reviewErrorVersion++;
                    publish(Status.CONTENT);
                });
    }

    private void loadContext(int generation, Conversation value) {
        if (generation != loadGeneration) return;
        conversation = value;
        String uid = currentUid;
        String peerId = uid.equals(value.getOwnerId())
                ? value.getRequesterId() : value.getOwnerId();
        Task<User> peerTask = userRepository.getProfile(peerId);
        Task<Listing> listingTask = value.getListingId().isEmpty() ? Tasks.forResult(null)
                : listingRepository.getListing(value.getListingId());
        Task<List<Message>> messagesTask = chatRepository.getMessages(value.getConversationId());
        Tasks.whenAllComplete(peerTask, listingTask, messagesTask)
                .addOnCompleteListener(done -> {
                    if (generation != loadGeneration || !uid.equals(chatRepository.getCurrentUserId())) return;
                    if (!messagesTask.isSuccessful()) {
                        publish(Status.ERROR);
                        return;
                    }
                    peer = peerTask.isSuccessful() ? peerTask.getResult() : null;
                    listing = listingTask.isSuccessful() ? listingTask.getResult() : null;
                    messages = messagesTask.getResult();
                    loadCardListings(generation);
                    publish(Status.CONTENT);
                    startListening();
                    refreshBlockState();
                    checkReviewEligibility(generation);
                });
    }

    private void checkReviewEligibility(int generation) {
        String revieweeId = peerId();
        if (listing == null || revieweeId == null) return;
        reviewRepository.checkEligibility(listing.getListingId(), revieweeId)
                .addOnSuccessListener(value -> {
                    if (generation != loadGeneration) return;
                    reviewEligibility = value;
                    publish(Status.CONTENT);
                })
                .addOnFailureListener(error -> {
                    if (generation != loadGeneration) return;
                    reviewEligibility = null;
                    publish(Status.CONTENT);
                });
    }

    private String peerId() {
        if (conversation == null || currentUid == null) return null;
        return currentUid.equals(conversation.getOwnerId())
                ? conversation.getRequesterId() : conversation.getOwnerId();
    }

    private void publish(Status status) {
        state.setValue(new UiState(status, conversation, listing, peer, messages, currentUid,
                sending, sentVersion, sendErrorVersion, summaryWarningVersion,
                refreshErrorVersion, reviewEligibility, reviewSubmitting,
                reviewSuccessVersion, reviewErrorVersion, peerId(), ownBlockKnown, ownBlocked,
                mediaSentVersion, mediaErrorVersion, cardListings));
    }

    private static boolean isMissing(Exception error) {
        Throwable cause = error;
        while (cause != null) {
            if (cause.getMessage() != null
                    && cause.getMessage().toLowerCase(Locale.ROOT).contains("does not exist")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
