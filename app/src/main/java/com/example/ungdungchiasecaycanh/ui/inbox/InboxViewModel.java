package com.example.ungdungchiasecaycanh.ui.inbox;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.ChatRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.home.HomeViewModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InboxViewModel extends ViewModel {
    public enum Status { LOADING, CONTENT, EMPTY, ERROR }

    public static final class UiState {
        public final Status status;
        public final List<InboxConversationItem> items;
        public final boolean noConversations;

        private UiState(Status status, List<InboxConversationItem> items,
                        boolean noConversations) {
            this.status = status;
            this.items = items;
            this.noConversations = noConversations;
        }
    }

    private final ChatRepository chatRepository = new ChatRepository();
    private final UserRepository userRepository = new UserRepository();
    private final ListingRepository listingRepository = new ListingRepository();
    private final MutableLiveData<UiState> state = new MutableLiveData<>();
    private List<InboxConversationItem> loadedItems = Collections.emptyList();
    private String query = "";
    private int loadGeneration;
    private ChatRepository.Subscription inboxSubscription;
    private boolean liveWanted;
    private final Map<String, User> peerCache = new HashMap<>();
    private final Map<String, Listing> listingCache = new HashMap<>();

    public LiveData<UiState> getState() { return state; }

    /** Called from onStart/onStop: the list updates live while the inbox is visible. */
    public void setLive(boolean live) {
        liveWanted = live;
        if (!live) {
            stopListening();
            return;
        }
        if (inboxSubscription != null) return;
        String uid = chatRepository.getCurrentUserId();
        if (uid == null) return;
        if (state.getValue() == null) {
            state.setValue(new UiState(Status.LOADING, Collections.emptyList(), false));
        }
        inboxSubscription = chatRepository.listenInbox(conversations -> {
            if (!uid.equals(chatRepository.getCurrentUserId())) return;
            enrich(++loadGeneration, uid, conversations);
        }, error -> {
            // Fall back to a one-time load when live updates are not available.
            stopListening();
            loadInbox();
        });
    }

    private void stopListening() {
        if (inboxSubscription != null) inboxSubscription.cancel();
        inboxSubscription = null;
    }

    @Override protected void onCleared() {
        stopListening();
        super.onCleared();
    }

    public void loadInbox() {
        int generation = ++loadGeneration;
        loadedItems = Collections.emptyList();
        state.setValue(new UiState(Status.LOADING, Collections.emptyList(), false));
        String uid = chatRepository.getCurrentUserId();
        if (uid == null) {
            state.setValue(new UiState(Status.ERROR, Collections.emptyList(), false));
            return;
        }
        chatRepository.getConversationsForCurrentUser()
                .addOnSuccessListener(conversations -> enrich(generation, uid, conversations))
                .addOnFailureListener(error -> {
                    if (generation == loadGeneration) {
                        state.setValue(new UiState(Status.ERROR, Collections.emptyList(), false));
                    }
                });
    }

    public void retry() { loadInbox(); }

    public void setSearchQuery(String searchQuery) {
        query = searchQuery == null ? "" : searchQuery;
        UiState current = state.getValue();
        if (current != null && (current.status == Status.CONTENT || current.status == Status.EMPTY)) {
            publish();
        }
    }

    /**
     * One inbox row per person: the pair conversation when there is one (new messages go there), otherwise
     * their most recent per-listing one, newest first. Older per-listing chats with the same person stay
     * in Firestore but no longer get a row of their own.
     */
    public static List<Conversation> onePerPeer(List<Conversation> conversations, String uid) {
        Map<String, Conversation> chosen = new java.util.LinkedHashMap<>();
        for (Conversation c : conversations) {
            String peer = uid.equals(c.getOwnerId()) ? c.getRequesterId() : c.getOwnerId();
            Conversation current = chosen.get(peer);
            String pairId = ChatRepository.pairConversationId(uid, peer);
            boolean pair = pairId.equals(c.getConversationId());
            boolean currentPair = current != null && pairId.equals(current.getConversationId());
            if (current == null || (pair && !currentPair)
                    || (!currentPair && seconds(c) > seconds(current))) chosen.put(peer, c);
        }
        List<Conversation> out = new ArrayList<>(chosen.values());
        Collections.sort(out, (a, b) -> Long.compare(seconds(b), seconds(a)));
        return out;
    }

    private static long seconds(Conversation c) {
        return c.getUpdatedAt() == null ? Long.MIN_VALUE : c.getUpdatedAt().getSeconds();
    }

    private void enrich(int generation, String uid, List<Conversation> all) {
        if (generation != loadGeneration) return;
        List<Conversation> conversations = onePerPeer(all, uid);
        if (conversations.isEmpty()) {
            loadedItems = Collections.emptyList();
            publish();
            return;
        }
        List<Task<User>> peers = new ArrayList<>();
        List<Task<Listing>> listings = new ArrayList<>();
        List<Task<?>> dependencies = new ArrayList<>();
        for (Conversation conversation : conversations) {
            String peerId = uid.equals(conversation.getOwnerId())
                    ? conversation.getRequesterId() : conversation.getOwnerId();
            User knownPeer = peerCache.get(peerId);
            Listing knownListing = listingCache.get(conversation.getListingId());
            Task<User> peer = knownPeer != null ? Tasks.forResult(knownPeer) : userRepository.getProfile(peerId);
            Task<Listing> listing = knownListing != null ? Tasks.forResult(knownListing)
                    : conversation.getListingId().isEmpty() ? Tasks.forResult(null)
                    : listingRepository.getListing(conversation.getListingId());
            peers.add(peer);
            listings.add(listing);
            dependencies.add(peer);
            dependencies.add(listing);
        }
        Tasks.whenAllComplete(dependencies).addOnCompleteListener(done -> {
            if (generation != loadGeneration || !uid.equals(chatRepository.getCurrentUserId())) return;
            List<InboxConversationItem> items = new ArrayList<>();
            for (int i = 0; i < conversations.size(); i++) {
                Conversation conversation = conversations.get(i);
                if (peers.get(i).isSuccessful() && peers.get(i).getResult() != null) {
                    peerCache.put(uid.equals(conversation.getOwnerId())
                            ? conversation.getRequesterId() : conversation.getOwnerId(), peers.get(i).getResult());
                }
                if (listings.get(i).isSuccessful() && listings.get(i).getResult() != null) {
                    listingCache.put(conversation.getListingId(), listings.get(i).getResult());
                }
                String peerName = peers.get(i).isSuccessful() && peers.get(i).getResult() != null
                        ? peers.get(i).getResult().getDisplayName() : null;
                String listingName = listings.get(i).isSuccessful()
                        && listings.get(i).getResult() != null
                        ? listings.get(i).getResult().getNickname() : null;
                items.add(new InboxConversationItem(conversation.getConversationId(),
                        peerName, listingName, conversation.getLastMessage(),
                        conversation.getUpdatedAt(), conversation.getListingId().isEmpty()));
            }
            loadedItems = items;
            publish();
        });
    }

    private void publish() {
        List<InboxConversationItem> filtered = filterItems(loadedItems, query);
        state.setValue(new UiState(filtered.isEmpty() ? Status.EMPTY : Status.CONTENT,
                filtered, loadedItems.isEmpty()));
    }

    public static List<InboxConversationItem> filterItems(List<InboxConversationItem> source,
                                                           String searchQuery) {
        List<InboxConversationItem> result = new ArrayList<>();
        if (source == null) return result;
        String needle = HomeViewModel.normalize(searchQuery); // accent-insensitive, like Home search
        for (InboxConversationItem item : source) {
            if (needle.isEmpty() || contains(item.peerDisplayName, needle)
                    || contains(item.listingNickname, needle)) {
                result.add(item);
            }
        }
        return result;
    }

    private static boolean contains(String value, String needle) {
        return value != null && HomeViewModel.normalize(value).contains(needle);
    }
}
