package com.example.ungdungchiasecaycanh.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.ChatRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseListingDataSource;

public class HomeViewModel extends ViewModel {
    public enum Filter {
        ALL, GIVE_AWAY, TRADE, SELL
    }

    public static final class HomeUiState {
        public final boolean loading;
        public final List<Listing> listings;
        /** Chosen listing types; empty means every type ("All"). */
        public final Set<Filter> types;
        /** The plain "All" view: no type, search or sheet filter, so Home shows its sections. */
        public final boolean preview;
        /** Listings matching the current filters (shown under the chips while filtering). */
        public final int matchCount;
        public final String error;
        /** Visible (non-blocked) listing counts per type, independent of the selected filter. */
        public final int giveAwayCount, tradeCount, sellCount, newThisWeek;
        /** Newest visible Give Away listing, preferring one with a photo; null when none. */
        public final Listing featured;
        /** Newest visible listing of any type, preferring one with a photo; null when none. */
        public final Listing latest;
        /** First visible listing with a photo for each type (tile artwork), or null. */
        public final Listing giveAwayCover, tradeCover, sellCover;
        /** Newest visible Give Away listings (max {@link #NEWEST_GIVE_AWAYS}) for the Home carousel. */
        public final List<Listing> newestGiveAways;
        /** Newest visible Trade listings for the Home exchange strip. */
        public final List<Listing> newestTrades;
        /** Sale rows under the grid ("All" preview only; empty otherwise). */
        public final List<Listing> sales;
        /** True when the preview hides listings that "See all" would show. */
        public final boolean hasMore;
        /** Listing IDs the member saved (filled hearts). */
        public final Set<String> savedIds;
        /** Signed-in member, so cards can hide the heart on their own listings. */
        public final String uid;
        /** Older listings exist beyond the loaded pages, and whether the next page is loading. */
        public final boolean morePages, loadingMore;

        private HomeUiState(boolean loading, List<Listing> listings,
                            Set<Filter> types, String error) {
            this(loading, listings, types, true, 0, error, Collections.emptyList(), 0L,
                    Collections.emptyList(), false, Collections.emptySet(), null, null, false, false);
        }

        private HomeUiState(boolean loading, List<Listing> listings, Set<Filter> types, boolean preview,
                            int matchCount, String error,
                            List<Listing> visible, long now, List<Listing> sales, boolean hasMore,
                            Set<String> savedIds, String uid, int[] serverCounts, boolean morePages,
                            boolean loadingMore) {
            this.loading = loading;
            this.morePages = morePages;
            this.loadingMore = loadingMore;
            this.listings = listings;
            this.sales = sales;
            this.hasMore = hasMore;
            this.savedIds = savedIds;
            this.uid = uid;
            this.types = Collections.unmodifiableSet(types.isEmpty()
                    ? EnumSet.noneOf(Filter.class) : EnumSet.copyOf(types));
            this.preview = preview;
            this.matchCount = matchCount;
            this.error = error;
            // Every page loaded: exact counts here (blocked and stale left out). Otherwise the server's.
            this.giveAwayCount = serverCounts != null ? serverCounts[0] : count(visible, ListingType.GIVE_AWAY);
            this.tradeCount = serverCounts != null ? serverCounts[1] : count(visible, ListingType.TRADE);
            this.sellCount = serverCounts != null ? serverCounts[2] : count(visible, ListingType.SELL);
            this.newThisWeek = countNewSince(visible, now - 7L * 24 * 60 * 60 * 1000);
            this.featured = featured(visible);
            this.latest = latest(visible);
            this.giveAwayCover = firstWithPhoto(visible, ListingType.GIVE_AWAY);
            this.tradeCover = firstWithPhoto(visible, ListingType.TRADE);
            this.sellCover = firstWithPhoto(visible, ListingType.SELL);
            this.newestGiveAways = newest(visible, ListingType.GIVE_AWAY, NEWEST_GIVE_AWAYS);
            this.newestTrades = newest(visible, ListingType.TRADE, NEWEST_GIVE_AWAYS);
        }
    }

    /** Greeting and inbox numbers for the hero header; both come from the signed-in account. */
    public static final class HeaderState {
        public final String firstName;
        public final int conversationCount;
        /** Full display name (for avatar initials), the profile avatar ref and area; empty when unknown. */
        public final String displayName, avatarRef, area;

        HeaderState(String firstName, int conversationCount) {
            this(firstName, conversationCount, "", "", "");
        }

        HeaderState(String firstName, int conversationCount, String displayName, String avatarRef, String area) {
            this.firstName = firstName;
            this.conversationCount = conversationCount;
            this.displayName = displayName == null ? "" : displayName;
            this.avatarRef = avatarRef == null ? "" : avatarRef;
            this.area = area == null ? "" : area.trim();
        }
    }

    private final ListingRepository repository;
    private final SafetyRepository safetyRepository = new SafetyRepository();
    private final MutableLiveData<HomeUiState> state = new MutableLiveData<>();
    private final MutableLiveData<HeaderState> header = new MutableLiveData<>(new HeaderState("", 0));
    private final UserRepository userRepository = new UserRepository();
    private final ChatRepository chatRepository = new ChatRepository();
    private String searchQuery = "";
    private List<Listing> loadedListings = Collections.emptyList();
    /** Reserved and recently sold sale listings (best effort), shown in the grid with their status pill. */
    private List<Listing> saleUpdates = Collections.emptyList();
    /** Chosen listing types (never {@link Filter#ALL}); empty means every type. */
    private final Set<Filter> selectedTypes = EnumSet.noneOf(Filter.class);
    private boolean loaded;
    private Set<String> blockedUids = Collections.emptySet();
    private int loadGeneration;
    private long loadedAt;
    private Set<String> savedIds = Collections.emptySet();
    /** "See all" pressed while on "All": show every listing instead of the preview. */
    private boolean showAll;
    /** Paging: Home reads {@link #PAGE_SIZE} listings at a time, up to {@link #MAX_LOADED}. */
    static final int PAGE_SIZE = 50;
    static final int MAX_LOADED = 300;
    /** A filtered or searched list shorter than this loads older pages by itself. */
    static final int FILL_TARGET = 20;
    private Object cursor;
    private boolean morePages, loadingMore;
    /**
     * Pages read so far: the 300-listing budget counts documents read, not the ones left after stale
     * listings are dropped (BUG-007).
     */
    private int pagesRead;
    /** When the last next-page read failed (0: none). Home stops loading by itself after a failure. */
    private long pageFailedAt;
    /** After a failed page, scrolling to the end tries again at most this often (BUG-003). */
    static final long PAGE_RETRY_MS = 5_000;
    private int[] serverCounts;
    private final MutableLiveData<Boolean> saveFailed = new MutableLiveData<>(false);
    /** Public profiles of listing owners shown on Home (names on cards, "Givers near you"). */
    private final MutableLiveData<Map<String, User>> owners = new MutableLiveData<>(Collections.emptyMap());
    private final Map<String, User> ownerCache = new HashMap<>();
    private final Set<String> ownerRequested = new HashSet<>();
    /** At most this many owner profiles are read per Home session. */
    static final int MAX_OWNER_PROFILES = 60;

    public HomeViewModel() {
        repository = new ListingRepository();
    }

    public LiveData<HomeUiState> getState() {
        return state;
    }

    public LiveData<HeaderState> getHeader() {
        return header;
    }

    public LiveData<Map<String, User>> getOwners() {
        return owners;
    }

    /** Reads the profiles of these listings' owners once each; a failed read leaves the card without a name. */
    public void loadOwners(List<Listing> listings) {
        if (listings == null) return;
        for (Listing l : listings) {
            String uid = l.getOwnerId();
            if (uid == null || uid.isEmpty() || ownerRequested.contains(uid)) continue;
            if (ownerRequested.size() >= MAX_OWNER_PROFILES) return;
            ownerRequested.add(uid);
            userRepository.getProfile(uid).addOnSuccessListener(user -> {
                if (user == null) return;
                ownerCache.put(uid, user);
                owners.setValue(new HashMap<>(ownerCache));
            });
        }
    }

    /** Loads the greeting name and conversation count; failures keep the neutral header. */
    public void loadHeader() {
        Task<User> profile = userRepository.getCurrentProfile();
        Task<List<Conversation>> chats = chatRepository.getConversationsForCurrentUser();
        Tasks.whenAllComplete(profile, chats).addOnCompleteListener(done -> {
            String name = profile.isSuccessful() && profile.getResult() != null
                    ? firstName(profile.getResult().getDisplayName()) : "";
            int count = chats.isSuccessful() && chats.getResult() != null ? chats.getResult().size() : 0;
            User user = profile.isSuccessful() ? profile.getResult() : null;
            if (user != null) userRepository.syncEmailVerified(user);
            String area = user == null || user.getLocationText() == null ? "" : user.getLocationText();
            mainHandler.post(() -> {
                profileArea = area;
                if (loaded && refine.sort != Sort.NEWEST) publishContent();
            });
            header.postValue(new HeaderState(name, count,
                    user == null ? "" : user.getDisplayName(), user == null ? "" : user.getAvatarUrl(),
                    user == null ? "" : user.getLocationText()));
        });
    }

    public void setSearch(String query) {
        searchQuery = query == null ? "" : query.trim();
        if (loaded) publishContent();
    }

    public void load() {
        if (loaded || (state.getValue() != null && state.getValue().loading)) {
            return;
        }
        state.setValue(new HomeUiState(true, Collections.emptyList(), selectedTypes, null));
        fetch(false);
    }

    public void retry() {
        loaded = false;
        load();
    }

    /**
     * Returning to Home: reloads quietly (current cards stay on screen) and at most once per
     * {@code maxAgeMs}, instead of clearing the list behind a spinner on every visit.
     */
    public void refreshIfStale(long maxAgeMs) {
        if (!loaded) {
            if (state.getValue() == null || !state.getValue().loading) retry();
            return;
        }
        if (System.currentTimeMillis() - loadedAt < maxAgeMs) return;
        fetch(true);
    }

    /** {@code quiet}: keep the current content and ignore failures (the old list stays valid). */
    private void fetch(boolean quiet) {
        int generation = ++loadGeneration;
        String uid = safetyRepository.getCurrentUserId();
        if (uid == null) { if (!quiet) showLoadError(generation); return; }
        Task<FirebaseListingDataSource.Page> listingTask = repository.getActiveListingsPage(null, PAGE_SIZE);
        Task<Set<String>> blockedTask = safetyRepository.getBlockedUids();
        Task<Set<String>> savedTask = repository.getSavedListingIds();
        repository.getSaleUpdates().addOnSuccessListener(updates -> {
            if (generation != loadGeneration) return;
            saleUpdates = updates;
            if (loaded) publishContent();
        });
        Tasks.whenAllComplete(listingTask, blockedTask, savedTask).addOnCompleteListener(done -> {
            if (generation != loadGeneration) return;
            if (!uid.equals(safetyRepository.getCurrentUserId())
                    || !listingTask.isSuccessful() || !blockedTask.isSuccessful()) {
                if (!quiet) showLoadError(generation);
                return;
            }
            FirebaseListingDataSource.Page page = listingTask.getResult();
            loadedListings = new ArrayList<>(page.listings);
            cursor = page.cursor;
            pagesRead = 1;
            pageFailedAt = 0;
            morePages = hasBudget(page.hasMore, pagesRead);
            loadingMore = false;
            serverCounts = null;
            if (morePages) loadServerCounts(generation);
            blockedUids = new HashSet<>(blockedTask.getResult());
            if (savedTask.isSuccessful() && savedTask.getResult() != null) savedIds = new HashSet<>(savedTask.getResult());
            loaded = true;
            loadedAt = System.currentTimeMillis();
            publishContent();
        });
    }

    /** Next page, when the grid nears its end or a filter shows too few listings. */
    public void loadMore() {
        if (!loaded || loadingMore || !morePages) return;
        if (pageFailedAt != 0 && System.currentTimeMillis() - pageFailedAt < PAGE_RETRY_MS) return;
        int generation = loadGeneration;
        loadingMore = true;
        publishContent();
        repository.getActiveListingsPage(cursor, PAGE_SIZE).addOnCompleteListener(task -> {
            if (generation != loadGeneration) return;
            loadingMore = false;
            if (task.isSuccessful() && task.getResult() != null) {
                loadedListings = appendNew(loadedListings, task.getResult().listings);
                cursor = task.getResult().cursor;
                pagesRead++;
                pageFailedAt = 0;
                morePages = hasBudget(task.getResult().hasMore, pagesRead);
                if (!morePages) serverCounts = null; // everything is here: count on the device
            } else {
                pageFailedAt = System.currentTimeMillis();
            }
            publishContent();
        });
    }

    /** The grid was scrolled near its end; the "All" preview shows four cards and needs no more. */
    public void onGridEndReached() {
        if (!isPreview()) loadMore();
    }

    /** {@code older} after {@code current}, without listings already loaded. */
    static List<Listing> appendNew(List<Listing> current, List<Listing> older) {
        Set<String> ids = new HashSet<>();
        for (Listing l : current) ids.add(l.getListingId());
        List<Listing> out = new ArrayList<>(current);
        for (Listing l : older) if (ids.add(l.getListingId())) out.add(l);
        return out;
    }

    /** Best effort: without them the chips count the loaded pages. */
    private void loadServerCounts(int generation) {
        repository.countActiveByType().addOnSuccessListener(counts -> {
            if (generation != loadGeneration || !morePages) return;
            serverCounts = counts;
            if (loaded) publishContent();
        });
    }

    // ---------------- advanced filters (filter sheet) ----------------

    /** "Tin mới quanh bạn" order (sort menu on Home; not a filter, so the Home sections stay). */
    public enum Sort { NEWEST, MATCH, NEAR }

    /** Everything the filter sheet controls besides the type; defaults mean "no filter". */
    public static final class Refine {
        /** Pick-up areas to keep (any of them); empty means every area. */
        public final Set<String> areas;
        public final Sort sort;
        public final boolean withPhotos;

        public Refine(Set<String> areas, Sort sort, boolean withPhotos) {
            Set<String> kept = new LinkedHashSet<>();
            if (areas != null) for (String a : areas) if (a != null && !a.trim().isEmpty()) kept.add(a.trim());
            this.areas = Collections.unmodifiableSet(kept);
            this.sort = sort == null ? Sort.NEWEST : sort;
            this.withPhotos = withPhotos;
        }

        static Refine none() { return new Refine(Collections.emptySet(), Sort.NEWEST, false); }

        /** Number of non-default choices, shown as a badge on the filter button. */
        public int activeCount() {
            return (areas.isEmpty() ? 0 : 1) + (withPhotos ? 1 : 0);
        }
    }

    private Refine refine = Refine.none();
    private final MutableLiveData<Refine> refineState = new MutableLiveData<>(refine);

    public LiveData<Refine> getRefine() { return refineState; }
    public Set<Filter> getSelectedTypes() {
        return selectedTypes.isEmpty() ? EnumSet.noneOf(Filter.class) : EnumSet.copyOf(selectedTypes);
    }

    /** How many listings a filter-sheet draft would show (live count on the apply button). */
    public int countFor(Set<Filter> types, Refine draft) {
        if (!loaded) return 0;
        return refineListings(searchListings(filterListings(listed(), types), searchQuery), draft, profileArea, tradesFirst).size();
    }

    /** Changes only the order; areas and photos stay. */
    public void setSort(Sort sort) {
        setRefine(new Refine(refine.areas, sort, refine.withPhotos));
    }

    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private String profileArea = "";
    private boolean tradesFirst;

    /** What "Best match" and "Near you" look at: the profile area and what the member came for. */
    public void setPreferences(boolean tradesFirst) {
        if (this.tradesFirst == tradesFirst) return;
        this.tradesFirst = tradesFirst;
        if (loaded && refine.sort != Sort.NEWEST) publishContent();
    }

    public void setRefine(Refine value) {
        refine = value == null ? Refine.none() : value;
        refineState.setValue(refine);
        if (loaded) publishContent();
    }

    /** Distinct pick-up areas among the visible listings, most common first (max 12). */
    public List<String> availableAreas() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> display = new HashMap<>();
        for (Listing l : SafetyRepository.filterByBlockedAuthors(loadedListings, blockedUids, Listing::getOwnerId)) {
            String area = l.getLocationText() == null ? "" : l.getLocationText().trim();
            if (area.isEmpty()) continue;
            String key = normalize(area);
            counts.put(key, counts.containsKey(key) ? counts.get(key) + 1 : 1);
            if (!display.containsKey(key)) display.put(key, area);
        }
        List<String> keys = new ArrayList<>(counts.keySet());
        Collections.sort(keys, (a, b) -> Integer.compare(counts.get(b), counts.get(a)));
        List<String> areas = new ArrayList<>();
        for (String key : keys) {
            if (areas.size() == 12) break;
            areas.add(display.get(key));
        }
        return areas;
    }

    static List<Listing> refineListings(List<Listing> listings, Refine refine) {
        return refineListings(listings, refine, "", false);
    }

    /**
     * Keeps the chosen areas / photos, then orders: newest first; "Near you" puts the profile's area
     * first; "Best match" scores same area (3), the kind the member came for (2) and a photo (1).
     * Ties stay newest first.
     */
    static List<Listing> refineListings(List<Listing> listings, Refine refine, String profileArea, boolean tradesFirst) {
        List<Listing> out = new ArrayList<>();
        Set<String> areas = new HashSet<>();
        for (String a : refine.areas) areas.add(normalize(a));
        for (Listing l : listings) {
            if (!areas.isEmpty() && !areas.contains(normalize(l.getLocationText()))) continue;
            if (refine.withPhotos && (l.getCoverImageUrl() == null || l.getCoverImageUrl().trim().isEmpty())) continue;
            out.add(l);
        }
        Comparator<Listing> newest = (a, b) -> Long.compare(millis(b), millis(a));
        String home = district(profileArea);
        if (refine.sort == Sort.NEAR) {
            Collections.sort(out, Comparator.<Listing>comparingInt(l -> sameArea(l, home) ? 0 : 1).thenComparing(newest));
        } else if (refine.sort == Sort.MATCH) {
            Collections.sort(out, Comparator.<Listing>comparingInt(l -> -matchScore(l, home, tradesFirst)).thenComparing(newest));
        } else {
            Collections.sort(out, newest);
        }
        return out;
    }

    private static boolean sameArea(Listing l, String home) {
        return !home.isEmpty() && home.equals(district(l.getLocationText()));
    }

    /** "Tan Phu, TP.HCM" and "Tân Phú, HCM" are the same place: compare the part before the comma. */
    static String district(String area) {
        String key = normalize(area);
        int comma = key.indexOf(',');
        return (comma < 0 ? key : key.substring(0, comma)).trim();
    }

    static int matchScore(Listing l, String home, boolean tradesFirst) {
        ListingType wanted = tradesFirst ? ListingType.TRADE : ListingType.GIVE_AWAY;
        boolean photo = l.getCoverImageUrl() != null && !l.getCoverImageUrl().trim().isEmpty();
        return (sameArea(l, district(home)) ? 3 : 0) + (l.getType() == wanted ? 2 : 0) + (photo ? 1 : 0);
    }

    /** Case-, accent- and spacing-insensitive key for comparing areas ("Quận 7" = "quan 7"). */
    public static String normalize(String text) {
        if (text == null) return "";
        String lower = text.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
    }

    private static long millis(Listing l) {
        return l.getCreatedAt() == null ? 0 : l.getCreatedAt().toDate().getTime();
    }

    /**
     * A type chip: Give Away, Trade and Sell toggle on and off together; "All" (or every type at once)
     * clears the types and the sheet filters, back to the plain Home with its sections.
     */
    public void toggleType(Filter type) {
        if (type == null || type == Filter.ALL) {
            clearFilters();
            return;
        }
        if (!selectedTypes.remove(type)) selectedTypes.add(type);
        if (selectedTypes.size() == Filter.values().length - 1) selectedTypes.clear();
        showAll = false;
        if (loaded) publishContent();
    }

    /** Types chosen in the filter sheet (several allowed); {@link Filter#ALL} or an empty set means all. */
    public void setTypes(Set<Filter> types) {
        selectedTypes.clear();
        if (types != null) for (Filter t : types) if (t != Filter.ALL) selectedTypes.add(t);
        if (selectedTypes.size() == Filter.values().length - 1) selectedTypes.clear();
        showAll = false;
        if (loaded) publishContent();
    }

    /** "All": no type and no sheet filter. */
    public void clearFilters() {
        selectedTypes.clear();
        showAll = false;
        refine = Refine.none();
        refineState.setValue(refine);
        if (loaded) publishContent();
    }

    private boolean isPreview() {
        return !showAll && selectedTypes.isEmpty() && searchQuery.isEmpty() && refine.activeCount() == 0;
    }

    /** "See all" on the preview: the grid lists every match. */
    public void seeAll() {
        showAll = true;
        if (loaded) publishContent();
    }

    public LiveData<Boolean> getSaveFailed() { return saveFailed; }

    public void clearSaveFailed() { saveFailed.setValue(false); }

    /** Saves or unsaves a listing; the heart changes at once and reverts if the write fails. */
    public void toggleSaved(String listingId) {
        if (listingId == null || listingId.isEmpty()) return;
        boolean save = !savedIds.contains(listingId);
        setSaved(listingId, save);
        (save ? repository.saveListing(listingId) : repository.unsaveListing(listingId))
                .addOnFailureListener(e -> {
                    setSaved(listingId, !save);
                    saveFailed.setValue(true);
                });
    }

    private void setSaved(String listingId, boolean saved) {
        Set<String> next = new HashSet<>(savedIds);
        if (saved) next.add(listingId); else next.remove(listingId);
        savedIds = next;
        if (loaded) publishContent();
    }

    private void publishContent() {
        List<Listing> visible = SafetyRepository.filterByBlockedAuthors(
                loadedListings, blockedUids, Listing::getOwnerId);
        List<Listing> shown = refineListings(searchListings(filterListings(listed(), selectedTypes), searchQuery), refine,
                profileArea, tradesFirst);
        boolean preview = isPreview();
        List<Listing> grid = preview ? previewGrid(shown) : shown;
        List<Listing> sales = preview ? previewSales(shown) : Collections.<Listing>emptyList();
        state.postValue(new HomeUiState(false, grid, selectedTypes,
                preview && searchQuery.isEmpty(), shown.size(), null, visible, System.currentTimeMillis(),
                sales, grid.size() + sales.size() < shown.size(), savedIds, safetyRepository.getCurrentUserId(),
                morePages ? serverCounts : null, morePages, loadingMore));
        // A filter or search that matches only a few loaded listings looks further back by itself.
        boolean narrowed = !selectedTypes.isEmpty() || !searchQuery.isEmpty() || refine.activeCount() > 0;
        // Also when every listing read so far was dropped as stale (BUG-004). Never right after a failed
        // page: that would ask for the same page again and again (BUG-003).
        if (pageFailedAt == 0 && ((narrowed && shown.size() < FILL_TARGET) || shown.isEmpty())) loadMore();
    }

    /**
     * What the grid, sale row and filter count work on: loaded active listings plus reserved and recently
     * sold sales, without blocked members. The hero and the other sections stay active only.
     */
    private List<Listing> listed() {
        return SafetyRepository.filterByBlockedAuthors(
                withSaleUpdates(loadedListings, saleUpdates), blockedUids, Listing::getOwnerId);
    }

    /** Sale listings that can lead the hero slider: available ones only. */
    public static List<Listing> activeOnly(List<Listing> listings) {
        List<Listing> out = new ArrayList<>();
        if (listings != null) for (Listing l : listings) if (l.getStatus() == ListingStatus.ACTIVE) out.add(l);
        return out;
    }

    /** More pages are worth reading while the server has them and the 300-document budget allows. */
    static boolean hasBudget(boolean serverHasMore, int pagesRead) {
        return serverHasMore && pagesRead * PAGE_SIZE < MAX_LOADED;
    }

    static final int PREVIEW_GRID = 4;
    static final int PREVIEW_SALES = 3;

    /** Home "All" preview: the first {@link #PREVIEW_GRID} give-away and trade cards (selling stays secondary). */
    static List<Listing> previewGrid(List<Listing> listings) {
        List<Listing> out = new ArrayList<>();
        for (Listing l : listings) {
            if (out.size() == PREVIEW_GRID) break;
            if (l != null && l.getType() != ListingType.SELL) out.add(l);
        }
        return out;
    }

    /** Home "All" preview: up to {@link #PREVIEW_SALES} sale rows under the grid, available ones first. */
    static List<Listing> previewSales(List<Listing> listings) {
        List<Listing> sales = new ArrayList<>();
        for (Listing l : listings) if (l != null && l.getType() == ListingType.SELL) sales.add(l);
        Collections.sort(sales, (a, b) -> Integer.compare(saleRank(a), saleRank(b)));
        return new ArrayList<>(sales.subList(0, Math.min(PREVIEW_SALES, sales.size())));
    }

    /** Sale order on Home: available, then reserved, then sold. */
    public static int saleRank(Listing listing) {
        ListingStatus status = listing.getStatus();
        return status == ListingStatus.ACTIVE ? 0 : status == ListingStatus.RESERVED ? 1 : 2;
    }

    /** Loaded active listings with the sale updates; an update replaces an older copy of the same listing. */
    static List<Listing> withSaleUpdates(List<Listing> loaded, List<Listing> updates) {
        if (updates == null || updates.isEmpty()) return loaded;
        Set<String> ids = new HashSet<>();
        for (Listing l : updates) ids.add(l.getListingId());
        List<Listing> out = new ArrayList<>();
        for (Listing l : loaded) if (!ids.contains(l.getListingId())) out.add(l);
        out.addAll(updates);
        return out;
    }

    private void showLoadError(int generation) {
        if (generation == loadGeneration) state.postValue(new HomeUiState(
                false, Collections.emptyList(), selectedTypes, "Unable to load listings."));
    }

    /** Vietnamese names put the given name last ("Ho Trong Phuc" greets as "Phuc"). */
    static String firstName(String displayName) {
        String name = displayName == null ? "" : displayName.trim();
        if (name.isEmpty()) return "";
        String[] parts = name.split("\\s+");
        return parts[parts.length - 1];
    }

    static List<Listing> searchListings(List<Listing> listings, String query) {
        if (listings == null) return Collections.emptyList();
        // Accent-insensitive: "thao" finds "Thảo", "duong xi" finds "Dương xỉ".
        String q = normalize(query);
        if (q.isEmpty()) return new ArrayList<>(listings);
        List<Listing> result = new ArrayList<>();
        for (Listing listing : listings) {
            if (listing == null) continue;
            String haystack = normalize(listing.getNickname() + " " + listing.getSpecies() + " "
                    + listing.getLocationText() + " " + listing.getLookingFor());
            if (haystack.contains(q)) result.add(listing);
        }
        return result;
    }

    static int count(List<Listing> listings, ListingType type) {
        int n = 0;
        if (listings != null) for (Listing l : listings) if (l != null && l.getType() == type) n++;
        return n;
    }

    static int countNewSince(List<Listing> listings, long sinceMillis) {
        int n = 0;
        if (listings != null) for (Listing l : listings) {
            if (l != null && l.getCreatedAt() != null
                    && l.getCreatedAt().toDate().getTime() >= sinceMillis) n++;
        }
        return n;
    }

    static Listing featured(List<Listing> listings) {
        Listing withPhoto = null, any = null;
        if (listings != null) for (Listing l : listings) {
            if (l == null || l.getType() != ListingType.GIVE_AWAY) continue;
            if (any == null || newer(l, any)) any = l;
            if (l.getCoverImageUrl() != null && (withPhoto == null || newer(l, withPhoto))) withPhoto = l;
        }
        return withPhoto != null ? withPhoto : any;
    }

    static final int NEWEST_GIVE_AWAYS = 8;

    /** Up to {@code max} listings of {@code type}, newest first. */
    static Listing latest(List<Listing> listings) {
        Listing withPhoto = null, any = null;
        if (listings != null) for (Listing l : listings) {
            if (l == null) continue;
            if (any == null || newer(l, any)) any = l;
            if (l.getCoverImageUrl() != null && (withPhoto == null || newer(l, withPhoto))) withPhoto = l;
        }
        return withPhoto != null ? withPhoto : any;
    }

    static List<Listing> newest(List<Listing> listings, ListingType type, int max) {
        List<Listing> out = new ArrayList<>();
        if (listings != null) for (Listing l : listings) if (l != null && l.getType() == type) out.add(l);
        Collections.sort(out, (a, b) -> Long.compare(millis(b), millis(a)));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    static Listing firstWithPhoto(List<Listing> listings, ListingType type) {
        if (listings != null) for (Listing l : listings) {
            if (l != null && l.getType() == type && l.getCoverImageUrl() != null) return l;
        }
        return null;
    }

    private static boolean newer(Listing a, Listing b) {
        if (a.getCreatedAt() == null) return false;
        if (b.getCreatedAt() == null) return true;
        return a.getCreatedAt().compareTo(b.getCreatedAt()) > 0;
    }

    static List<Listing> filterListings(List<Listing> listings, Filter filter) {
        return filterListings(listings, filter == null || filter == Filter.ALL
                ? EnumSet.noneOf(Filter.class) : EnumSet.of(filter));
    }

    /** Listings of any of {@code types}; an empty set (or one holding {@link Filter#ALL}) keeps every type. */
    static List<Listing> filterListings(List<Listing> listings, Set<Filter> types) {
        if (listings == null || listings.isEmpty()) {
            return Collections.emptyList();
        }
        if (types == null || types.isEmpty() || types.contains(Filter.ALL)) {
            return new ArrayList<>(listings);
        }
        Set<ListingType> requested = EnumSet.noneOf(ListingType.class);
        for (Filter f : types) {
            requested.add(f == Filter.GIVE_AWAY ? ListingType.GIVE_AWAY
                    : f == Filter.TRADE ? ListingType.TRADE : ListingType.SELL);
        }
        List<Listing> result = new ArrayList<>();
        for (Listing listing : listings) {
            if (listing != null && requested.contains(listing.getType())) {
                result.add(listing);
            }
        }
        return result;
    }
}
