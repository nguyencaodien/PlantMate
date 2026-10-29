package com.example.ungdungchiasecaycanh.ui.community;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseCommunityDataSource;
import com.example.ungdungchiasecaycanh.data.repository.CommunityRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

public class CommunityViewModel extends ViewModel {
    public enum Sort { NEWEST, MOST_LIKED }

    /** Numbers for the two stat cards, computed from the loaded (unblocked) posts. */
    public static final class Stats {
        public final int thisWeek, lastWeek;
        /** Posts per day for the last 7 days, oldest first. */
        public final int[] daily;
        /** Most-liked post of the last 7 days (comments break ties), or null. */
        public final CommunityPostItem trending;

        Stats(int thisWeek, int lastWeek, int[] daily, CommunityPostItem trending) {
            this.thisWeek = thisWeek;
            this.lastWeek = lastWeek;
            this.daily = daily;
            this.trending = trending;
        }
    }

    public static final class State {
        public final boolean loading;
        public final List<CommunityPostItem> posts;
        public final String error;
        public final boolean hasSourcePosts;
        public final int likeErrorVersion;
        public final Stats stats;
        public final Sort sort;
        /** Older posts exist beyond the loaded pages; {@code loadingMore}: the next page is on its way. */
        public final boolean hasMore, loadingMore;

        State(boolean loading, List<CommunityPostItem> posts, String error, boolean hasSourcePosts, int likeErrorVersion) {
            this(loading, posts, error, hasSourcePosts, likeErrorVersion, null, Sort.NEWEST, false, false);
        }

        State(boolean loading, List<CommunityPostItem> posts, String error, boolean hasSourcePosts, int likeErrorVersion,
              Stats stats, Sort sort, boolean hasMore, boolean loadingMore) {
            this.stats = stats;
            this.sort = sort;
            this.hasMore = hasMore;
            this.loadingMore = loadingMore;
            this.loading = loading;
            this.posts = posts;
            this.error = error;
            this.hasSourcePosts = hasSourcePosts;
            this.likeErrorVersion = likeErrorVersion;
        }
    }

    private final CommunityRepository repository = new CommunityRepository();
    private final UserRepository users = new UserRepository();
    private final SafetyRepository safety = new SafetyRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final ListingRepository listingRepository = new ListingRepository();
    private final MutableLiveData<Map<String, Listing>> listings = new MutableLiveData<>(new HashMap<>());
    private final Map<String, Listing> listingCache = new HashMap<>();
    private final Set<String> listingRequested = new HashSet<>();
    private List<CommunityPostItem> source = Collections.emptyList();
    /** Listings shared in posts, by ID, for the attached-listing panel. */
    public LiveData<Map<String, Listing>> getListings() { return listings; }

    /** Fetches each attached listing once; one that cannot be read keeps the plain "shared listing" row. */
    public void loadListings(List<CommunityPostItem> posts) {
        if (posts == null) return;
        for (CommunityPostItem item : posts) {
            String id = item.post.getListingId();
            if (id == null || id.trim().isEmpty() || !listingRequested.add(id)) continue;
            listingRepository.getListing(id).addOnSuccessListener(listing -> {
                if (listing == null) return;
                listingCache.put(id, listing);
                listings.setValue(new HashMap<>(listingCache));
            });
        }
    }

    private PostCategory category;
    private String query = "";
    private Sort sort = Sort.NEWEST;
    private int likeErrors;
    private Set<String> blockedUids = Collections.emptySet();
    private int loadGeneration;
    private boolean ready;
    /** Paging: where the next page starts, whether there is one, and whether it is loading. */
    private Object cursor;
    private boolean hasMore, loadingMore;

    public LiveData<State> getState() {
        return state;
    }

    private final MutableLiveData<User> me = new MutableLiveData<>();

    /** The signed-in member's profile, for the avatar in the top bar (null until loaded). */
    public LiveData<User> getMe() {
        if (me.getValue() == null) {
            users.getCurrentProfile().addOnSuccessListener(value -> { if (value != null) me.setValue(value); });
        }
        return me;
    }

    /** First page of the selected category; older posts arrive through {@link #loadMore()}. */
    public void load() {
        int generation = ++loadGeneration;
        ready = false;
        source = Collections.emptyList();
        cursor = null;
        hasMore = false;
        loadingMore = false;
        state.setValue(new State(true, Collections.emptyList(), null, false, likeErrors));
        String uid = safety.getCurrentUserId();
        if (uid == null) {
            fail(generation);
            return;
        }
        Task<FirebaseCommunityDataSource.Page> pageTask = repository.getPostsPage(category, null);
        Task<Set<String>> blocksTask = safety.getBlockedUids();
        Tasks.whenAllComplete(pageTask, blocksTask).addOnCompleteListener(done -> {
            if (generation != loadGeneration) return;
            if (!uid.equals(safety.getCurrentUserId()) || !pageTask.isSuccessful()
                    || !blocksTask.isSuccessful()) {
                fail(generation);
                return;
            }
            blockedUids = new HashSet<>(blocksTask.getResult());
            receive(pageTask.getResult(), generation, false);
        });
    }

    public void reload() {
        load();
    }

    /** Next page, when the reader nears the end of the feed. A failure leaves it to the next scroll. */
    public void loadMore() {
        if (!ready || loadingMore || !hasMore) return;
        int generation = loadGeneration;
        loadingMore = true;
        publish();
        repository.getPostsPage(category, cursor).addOnCompleteListener(task -> {
            if (generation != loadGeneration) return;
            if (!task.isSuccessful() || task.getResult() == null) {
                loadingMore = false;
                publish();
                return;
            }
            receive(task.getResult(), generation, true);
        });
    }

    private void receive(FirebaseCommunityDataSource.Page page, int generation, boolean append) {
        enrich(SafetyRepository.filterByBlockedAuthors(page.posts, blockedUids, Post::getAuthorId),
                generation, append, page);
    }

    private void enrich(List<Post> posts, int generation, boolean append, FirebaseCommunityDataSource.Page page) {
        if (posts == null || posts.isEmpty()) {
            accept(Collections.emptyList(), append, page);
            return;
        }
        List<Task<?>> tasks = new ArrayList<>();
        List<Task<User>> authorTasks = new ArrayList<>();
        List<Task<Boolean>> likedTasks = new ArrayList<>();
        List<Task<Integer>> countTasks = new ArrayList<>();
        List<Task<Integer>> commentTasks = new ArrayList<>();
        Map<String, Task<User>> authors = new HashMap<>();
        for (Post post : posts) {
            Task<Integer> comments = repository.getCommentCount(post.getPostId());
            commentTasks.add(comments);
            tasks.add(comments);
            Task<User> a = authors.get(post.getAuthorId());
            if (a == null) {
                a = users.getProfile(post.getAuthorId());
                authors.put(post.getAuthorId(), a);
            }
            Task<Boolean> l = repository.isPostLiked(post.getPostId());
            Task<Integer> c = repository.getLikeCount(post.getPostId());
            authorTasks.add(a);
            likedTasks.add(l);
            countTasks.add(c);
            tasks.add(a);
            tasks.add(l);
            tasks.add(c);
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            if (generation != loadGeneration) return;
            List<CommunityPostItem> result = new ArrayList<>();
            for (int i = 0; i < posts.size(); i++) {
                if (!likedTasks.get(i).isSuccessful() || !countTasks.get(i).isSuccessful()) {
                    if (append) {
                        loadingMore = false;
                        publish();
                    } else {
                        fail(generation);
                    }
                    return;
                }
                String name = "";
                String avatar = authorTasks.get(i).isSuccessful() && authorTasks.get(i).getResult() != null
                        ? authorTasks.get(i).getResult().getAvatarUrl() : "";
                if (authorTasks.get(i).isSuccessful() && authorTasks.get(i).getResult() != null && !authorTasks.get(i).getResult().getDisplayName().trim().isEmpty())
                    name = authorTasks.get(i).getResult().getDisplayName();
                boolean liked = Boolean.TRUE.equals(likedTasks.get(i).getResult());
                int count = countTasks.get(i).getResult() == null ? 0 : countTasks.get(i).getResult();
                String area = authorTasks.get(i).isSuccessful() && authorTasks.get(i).getResult() != null
                        ? authorTasks.get(i).getResult().getLocationText() : "";
                // A missing comment count only hides the number; it never fails the feed.
                int comments = commentTasks.get(i).isSuccessful() && commentTasks.get(i).getResult() != null
                        ? commentTasks.get(i).getResult() : 0;
                result.add(new CommunityPostItem(posts.get(i), name, liked, count, false, avatar, area, comments));
            }
            accept(result, append, page);
        });
    }

    private void accept(List<CommunityPostItem> items, boolean append, FirebaseCommunityDataSource.Page page) {
        source = append ? appendNew(source, items) : items;
        cursor = page.cursor;
        hasMore = page.hasMore;
        loadingMore = false;
        ready = true;
        publish();
    }

    /** {@code older} after {@code current}, skipping posts already shown (a post can shift across pages). */
    static List<CommunityPostItem> appendNew(List<CommunityPostItem> current, List<CommunityPostItem> older) {
        List<CommunityPostItem> out = new ArrayList<>(current);
        for (CommunityPostItem item : older)
            if (indexOf(out, item.post.getPostId()) < 0) out.add(item);
        return out;
    }

    private void fail(int generation) {
        if (generation == loadGeneration)
            state.postValue(new State(false, Collections.emptyList(), "Unable to load community posts.", false, likeErrors));
    }

    /** Categories are filtered by the query, so changing one starts again from its first page. */
    public void setCategory(PostCategory value) {
        if (value == category) return;
        category = value;
        load();
    }

    public void setSort(Sort value) {
        sort = value == null ? Sort.NEWEST : value;
        publish();
    }

    public void setSearch(String value) {
        query = value == null ? "" : value.trim();
        publish();
    }

    public void toggleLike(String postId) {
        int index = indexOf(source, postId);
        if (index < 0 || source.get(index).likeInProgress) return;
        int generation = loadGeneration;
        CommunityPostItem old = source.get(index);
        replace(index, old.withLike(old.liked, old.likeCount, true));
        publish();
        Task<Void> task = old.liked ? repository.unlikePost(postId) : repository.likePost(postId);
        task.addOnSuccessListener(v -> {
            if (!old.liked) new com.example.ungdungchiasecaycanh.data.repository.NotificationRepository()
                    .notifyLike(postId, old.post.getAuthorId());
            if (generation != loadGeneration) return;
            int current = indexOf(source, postId);
            if (current >= 0) {
                replace(current, afterLikeOperation(old, true));
                publish();
            }
        }).addOnFailureListener(e -> {
            if (generation != loadGeneration) return;
            int current = indexOf(source, postId);
            if (current >= 0) replace(current, afterLikeOperation(old, false));
            likeErrors++;
            publish();
        });
    }

    private void replace(int i, CommunityPostItem item) {
        List<CommunityPostItem> copy = new ArrayList<>(source);
        copy.set(i, item);
        source = copy;
    }

    private static int indexOf(List<CommunityPostItem> items, String id) {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).post.getPostId().equals(id)) return i;
        return -1;
    }

    private void publish() {
        if (ready)
            state.postValue(new State(false, sorted(filter(source, category, query), sort), null, !source.isEmpty(),
                    likeErrors, stats(source, System.currentTimeMillis()), sort, hasMore, loadingMore));
    }

    static List<CommunityPostItem> filter(List<CommunityPostItem> input, PostCategory category, String query) {
        List<CommunityPostItem> out = new ArrayList<>();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (input == null) return out;
        for (CommunityPostItem item : input)
            if ((category == null || item.post.getCategory() == category) && (q.isEmpty() || item.post.getText().toLowerCase(Locale.ROOT).contains(q) || item.authorName.toLowerCase(Locale.ROOT).contains(q)))
                out.add(item);
        return out;
    }

    static List<CommunityPostItem> sorted(List<CommunityPostItem> items, Sort sort) {
        List<CommunityPostItem> out = new ArrayList<>(items);
        if (sort == Sort.MOST_LIKED) {
            // Stable sort: equal like counts keep the newest-first order from Firestore.
            out.sort((a, b) -> Integer.compare(b.likeCount, a.likeCount));
        }
        return out;
    }

    static final long DAY_MS = 24L * 60 * 60 * 1000;

    static Stats stats(List<CommunityPostItem> items, long now) {
        int thisWeek = 0, lastWeek = 0;
        int[] daily = new int[7];
        CommunityPostItem trending = null;
        if (items != null) for (CommunityPostItem item : items) {
            if (item.post.getCreatedAt() == null) continue;
            long age = now - item.post.getCreatedAt().toDate().getTime();
            if (age < 0) age = 0;
            if (age < 7 * DAY_MS) {
                thisWeek++;
                daily[6 - (int) (age / DAY_MS)]++;
                if (trending == null || item.likeCount > trending.likeCount
                        || (item.likeCount == trending.likeCount && item.commentCount > trending.commentCount)) {
                    trending = item;
                }
            } else if (age < 14 * DAY_MS) {
                lastWeek++;
            }
        }
        return new Stats(thisWeek, lastWeek, daily, trending);
    }

    static CommunityPostItem afterLikeOperation(CommunityPostItem previous, boolean success) {
        if (!success) return previous.withLike(previous.liked, previous.likeCount, false);
        return previous.withLike(!previous.liked, Math.max(0, previous.likeCount + (previous.liked ? -1 : 1)), false);
    }
}
