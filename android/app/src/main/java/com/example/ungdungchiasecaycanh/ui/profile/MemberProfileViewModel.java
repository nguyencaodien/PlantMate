package com.example.ungdungchiasecaycanh.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.CommunityRepository;
import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.ReviewRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.Review;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.community.CommunityPostItem;
import com.example.ungdungchiasecaycanh.ui.review.ReviewListItem;
import com.example.ungdungchiasecaycanh.ui.review.ReviewSummary;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Public profile of another member: who they are, what they share, their posts and reviews. */
public class MemberProfileViewModel extends ViewModel {
    public enum Status { LOADING, CONTENT, NOT_FOUND, ERROR }

    public static final class Data {
        public final String uid;
        public final User user;
        /** Listings other members can still ask for (Active or Reserved), newest first. */
        public final List<Listing> listings;
        public final int rehomedCount;
        public final List<CommunityPostItem> posts;
        public final List<ReviewListItem> reviews;
        public final ReviewSummary summary;
        public final Reputation reputation;

        Data(String uid, User user, List<Listing> listings, int rehomedCount, List<CommunityPostItem> posts,
             List<ReviewListItem> reviews, ReviewSummary summary, Reputation reputation) {
            this.uid = uid; this.user = user; this.listings = listings; this.rehomedCount = rehomedCount;
            this.posts = posts; this.reviews = reviews; this.summary = summary; this.reputation = reputation;
        }
    }

    public static final class State {
        public final Status status;
        public final Data data;
        State(Status status, Data data) { this.status = status; this.data = data; }
    }

    private final UserRepository users = new UserRepository();
    private final ListingRepository listings = new ListingRepository();
    private final CommunityRepository community = new CommunityRepository();
    private final ReviewRepository reviews = new ReviewRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.LOADING, null));
    private final FriendRepository friends = new FriendRepository();
    /** Friendship with this member; null while unknown (the button stays hidden). */
    private final MutableLiveData<FriendRepository.Relation> relation = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> friendBusy = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> friendErrors = new MutableLiveData<>(0);
    private String uid;

    public LiveData<State> getState() { return state; }
    public LiveData<FriendRepository.Relation> getRelation() { return relation; }
    public LiveData<Boolean> getFriendBusy() { return friendBusy; }
    public LiveData<Integer> getFriendErrors() { return friendErrors; }

    private final com.example.ungdungchiasecaycanh.data.repository.ChatRepository chats =
            new com.example.ungdungchiasecaycanh.data.repository.ChatRepository();
    private final MutableLiveData<String> chatToOpen = new MutableLiveData<>();
    private final MutableLiveData<Boolean> chatBusy = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> chatErrors = new MutableLiveData<>(0);

    /** Conversation ID to open once (cleared with {@link #chatOpened}). */
    public LiveData<String> getChatToOpen() { return chatToOpen; }
    public LiveData<Boolean> getChatBusy() { return chatBusy; }
    public LiveData<Integer> getChatErrors() { return chatErrors; }

    /** Opens (or creates) the chat with this member; refused across a block or when offline. */
    public void message() {
        String member = uid;
        if (member == null || Boolean.TRUE.equals(chatBusy.getValue())) return;
        chatBusy.setValue(true);
        chats.openDirectConversation(member).addOnCompleteListener(task -> {
            chatBusy.setValue(false);
            if (!member.equals(uid)) return;
            if (task.isSuccessful()) chatToOpen.setValue(task.getResult());
            else {
                Integer n = chatErrors.getValue();
                chatErrors.setValue(n == null ? 1 : n + 1);
            }
        });
    }

    public void chatOpened() { chatToOpen.setValue(null); }

    public void loadRelation() {
        String member = uid;
        if (member == null) return;
        friends.getRelation(member).addOnCompleteListener(task -> {
            if (member.equals(uid)) relation.setValue(task.isSuccessful() ? task.getResult() : null);
        });
    }

    /** The friend button: add (or accept theirs), cancel our request, or unfriend. */
    public void friendAction() {
        FriendRepository.Relation now = relation.getValue();
        String member = uid;
        if (now == null || member == null || Boolean.TRUE.equals(friendBusy.getValue())) return;
        Task<FriendRepository.Relation> action;
        switch (now) {
            case NONE: case INCOMING: action = friends.addFriend(member); break;
            case OUTGOING: action = friends.cancelRequest(member); break;
            case FRIENDS: action = friends.unfriend(member); break;
            default: return;
        }
        friendBusy.setValue(true);
        action.addOnCompleteListener(task -> {
            friendBusy.setValue(false);
            if (!member.equals(uid)) return;
            if (task.isSuccessful()) relation.setValue(task.getResult());
            else {
                Integer n = friendErrors.getValue();
                friendErrors.setValue(n == null ? 1 : n + 1);
                loadRelation();
            }
        });
    }

    public void load(String memberUid) {
        if (memberUid == null || memberUid.trim().isEmpty()) {
            state.setValue(new State(Status.NOT_FOUND, null));
            return;
        }
        if (memberUid.equals(uid) && state.getValue() != null && state.getValue().data != null) return;
        uid = memberUid;
        state.setValue(new State(Status.LOADING, null));
        relation.setValue(null);
        loadRelation();
        Task<User> profile = users.getProfile(memberUid);
        Task<List<Listing>> owned = listings.getListingsForOwner(memberUid);
        Task<List<Post>> allPosts = community.getPostsByAuthor(memberUid);
        Task<List<Review>> received = reviews.getReviewsForUser(memberUid);
        Tasks.whenAllComplete(profile, owned, allPosts, received).addOnCompleteListener(done -> {
            if (!profile.isSuccessful() || profile.getResult() == null) {
                state.setValue(new State(profile.isSuccessful() ? Status.NOT_FOUND : Status.ERROR, null));
                return;
            }
            List<Listing> visible = new ArrayList<>();
            int rehomed = 0;
            if (owned.isSuccessful() && owned.getResult() != null) {
                for (Listing l : owned.getResult()) {
                    if (l.getStatus() == ListingStatus.REHOMED) rehomed++;
                    if (l.getStatus() == ListingStatus.ACTIVE || l.getStatus() == ListingStatus.RESERVED) visible.add(l);
                }
            }
            Collections.sort(visible, (a, b) -> Long.compare(millis(b.getCreatedAt()), millis(a.getCreatedAt())));
            List<Post> theirPosts = allPosts.isSuccessful() && allPosts.getResult() != null
                    ? allPosts.getResult() : new ArrayList<>();
            List<Review> reviewList = received.isSuccessful() && received.getResult() != null
                    ? received.getResult() : new ArrayList<>();
            finish(memberUid, profile.getResult(), visible, rehomed, theirPosts, reviewList);
        });
    }

    public void retry() {
        String member = uid;
        uid = null;
        load(member);
    }

    /** Second round: like counts for their posts and names of the reviewers. */
    private void finish(String memberUid, User user, List<Listing> visible, int rehomed,
                        List<Post> posts, List<Review> reviewList) {
        List<Task<Integer>> likeTasks = new ArrayList<>();
        for (Post p : posts) likeTasks.add(community.getLikeCount(p.getPostId()));
        Map<String, Task<User>> reviewerTasks = new HashMap<>();
        for (Review r : reviewList) {
            if (!reviewerTasks.containsKey(r.getReviewerId())) reviewerTasks.put(r.getReviewerId(), users.getProfile(r.getReviewerId()));
        }
        List<Task<?>> all = new ArrayList<>(likeTasks);
        all.addAll(reviewerTasks.values());
        Tasks.whenAllComplete(all).addOnCompleteListener(done -> {
            String name = user.getDisplayName() == null ? "" : user.getDisplayName();
            List<CommunityPostItem> items = new ArrayList<>();
            for (int i = 0; i < posts.size(); i++) {
                Task<Integer> likes = likeTasks.get(i);
                int count = likes.isSuccessful() && likes.getResult() != null ? likes.getResult() : 0;
                items.add(new CommunityPostItem(posts.get(i), name, false, count, false, user.getAvatarUrl()));
            }
            List<Review> sorted = new ArrayList<>(reviewList);
            Collections.sort(sorted, (a, b) -> Long.compare(millis(b.getCreatedAt()), millis(a.getCreatedAt())));
            List<ReviewListItem> reviewItems = new ArrayList<>();
            for (Review r : sorted) {
                Task<User> reviewer = reviewerTasks.get(r.getReviewerId());
                String reviewerName = reviewer != null && reviewer.isSuccessful() && reviewer.getResult() != null
                        ? reviewer.getResult().getDisplayName() : "";
                reviewItems.add(new ReviewListItem(r, reviewerName));
            }
            ReviewSummary summary = ReviewSummary.from(reviewList);
            long days = user.getCreatedAt() == null ? 0
                    : (System.currentTimeMillis() - millis(user.getCreatedAt())) / 86_400_000L;
            Reputation reputation = Reputation.of(summary.averageRating, summary.reviewCount, rehomed, days,
                    Reputation.verifiedPoints(user));
            state.setValue(new State(Status.CONTENT, new Data(memberUid, user, visible, rehomed, items,
                    reviewItems, summary, reputation)));
        });
    }

    private static long millis(Timestamp t) {
        return t == null ? 0 : t.toDate().getTime();
    }
}
