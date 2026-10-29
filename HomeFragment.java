package com.example.ungdungchiasecaycanh.ui.home;

import com.example.ungdungchiasecaycanh.util.LocalImageLoader;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.garden.GardenOverview;
import com.example.ungdungchiasecaycanh.ui.garden.GardenViewModel;
import com.example.ungdungchiasecaycanh.ui.inbox.InboxActivity;
import com.example.ungdungchiasecaycanh.ui.listing.CreateListingActivity;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailActivity;
import com.example.ungdungchiasecaycanh.ui.listing.SavedListingsActivity;
import com.example.ungdungchiasecaycanh.ui.profile.MemberProfileActivity;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.PhotoTone;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * Home (canvas B): area chip, "Featured near you" hero slider, search, garden reminder, "New near you"
 * type chips over the two-column grid, then "Needs a home soon", "Givers near you", "Good prices for sale"
 * and the ink "Post" panel.
 */
public class HomeFragment extends Fragment {
    private HomeViewModel viewModel;
    private com.example.ungdungchiasecaycanh.ui.notifications.BellViewModel bellViewModel;
    private GardenViewModel gardenViewModel;
    private RecyclerView listingsView;
    private View header;
    private View footer;
    private View loadingView;
    private View emptyView;
    private View errorView;
    private TextView errorMessage;
    private ListingAdapter adapter;
    private HomeHeroAdapter heroAdapter;
    private RecyclerView heroView;
    /** The four type chips, in order: all, give away, trade, sell. */
    private static final int[] SEGMENTS = {R.id.home_filter_all, R.id.home_filter_give_away,
            R.id.home_filter_trade, R.id.home_filter_sell};
    private String profileArea = "";
    private String searchQuery = "";
    private boolean firstResume = true;
    private boolean createFlowActive;
    private boolean resultReloadIssued;
    /** Returning to Home reloads listings quietly, at most this often. */
    private static final long REFRESH_AFTER_MS = 30_000;
    /** Hero: each slide shows this long before the next one slides in. */
    private static final int HERO_MAX = 8;
    /** Width of the scaled-down copy whose colours are checked before a cover is featured. */
    private static final int HERO_TONE_PX = 72;
    /** Cover URL → suits the slider (not mostly red); filled as small copies are checked. */
    private final Map<String, Boolean> heroTone = new HashMap<>();
    private final Set<String> heroToneAsked = new HashSet<>();
    private static final int URGENT_MAX = 3;
    private static final int GIVERS_MAX = 6;
    private static final int SALES_MAX = 6;
    private ActivityResultLauncher<Intent> createListingLauncher;
    private HomeViewModel.HomeUiState lastState;
    /** A chip or the filter sheet changed the list: once it is drawn, bring the chips back to the top. */
    private boolean keepChipsInView;


    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createListingLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    createFlowActive = false;
                    if (result.getResultCode() == Activity.RESULT_OK && viewModel != null) {
                        resultReloadIssued = true;
                        viewModel.retry();
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        listingsView = view.findViewById(R.id.home_listing_recycler);
        adapter = new ListingAdapter(this::openListing, id -> viewModel.toggleSaved(id));
        GridLayoutManager grid = new GridLayoutManager(requireContext(), 2);
        // Header and footer span both columns; listing cards take one.
        grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override public int getSpanSize(int position) {
                return position == 0 || position > adapter.getItemCount() ? 2 : 1;
            }
        });
        listingsView.setLayoutManager(grid);
        listingsView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                // Within a few rows of the end: fetch older listings (the full list only, not the preview).
                if (dy > 0 && grid.findLastVisibleItemPosition() >= grid.getItemCount() - 6) viewModel.onGridEndReached();
            }
        });
        // Inflated against the grid (after its layout manager exists) for RecyclerView layout params.
        header = LayoutInflater.from(requireContext()).inflate(R.layout.item_home_header, listingsView, false);
        loadingView = header.findViewById(R.id.home_loading);
        emptyView = header.findViewById(R.id.home_empty_state);
        errorView = header.findViewById(R.id.home_error_state);
        errorMessage = header.findViewById(R.id.home_error_message);
        footer = LayoutInflater.from(requireContext()).inflate(R.layout.item_home_sales, listingsView, false);
        footer.setVisibility(View.GONE);
        listingsView.setAdapter(new ConcatAdapter(new HomeHeaderAdapter(header), adapter, new HomeHeaderAdapter(footer)));
        listingsView.addItemDecoration(new GridGutter((int) (14 * getResources().getDisplayMetrics().density)));

        setUpHero();
        gardenViewModel = new ViewModelProvider(this).get(GardenViewModel.class);
        gardenViewModel.getPlants().observe(getViewLifecycleOwner(), plants ->
                updateGardenWidget(plants, gardenViewModel.getCare().getValue()));
        gardenViewModel.getCare().observe(getViewLifecycleOwner(), care ->
                updateGardenWidget(gardenViewModel.getPlants().getValue(), care));
        View.OnClickListener openGarden = v -> selectTab(R.id.nav_garden);
        header.findViewById(R.id.home_garden_widget).setOnClickListener(openGarden);

        Motion.fromLeft(header.findViewById(R.id.home_brand_row), 0);
        Motion.rise(header.findViewById(R.id.home_headline_block), 60);


        // Give Away, Trade and Sell combine; "All" clears every filter.
        for (int id : SEGMENTS) header.findViewById(id).setOnClickListener(v -> {
            keepChipsInView = true;
            viewModel.toggleType(filterForId(v.getId()));
        });
        bindSegments(viewModel.getSelectedTypes());

        View.OnClickListener createListener = ignored -> {
            createFlowActive = true;
            createListingLauncher.launch(new Intent(requireContext(), CreateListingActivity.class));
        };
        // Empty community: invite the first share. No match for a search or filter: clear them instead (BUG-023).
        header.findViewById(R.id.home_empty_action).setOnClickListener(v -> {
            HomeViewModel.HomeUiState s = viewModel.getState().getValue();
            if (s == null || s.giveAwayCount + s.tradeCount + s.sellCount == 0) { createListener.onClick(v); return; }
            ((EditText) header.findViewById(R.id.home_search)).setText("");
            viewModel.clearFilters();
        });
        footer.findViewById(R.id.home_cta_action).setOnClickListener(createListener);
        // The post button sits beside the title instead of floating over the listings (BUG-009).
        header.findViewById(R.id.home_create_button).setOnClickListener(createListener);
        header.findViewById(R.id.home_retry).setOnClickListener(ignored -> viewModel.retry());
        header.findViewById(R.id.home_saved_button).setOnClickListener(ignored ->
                startActivity(new Intent(requireContext(), SavedListingsActivity.class)));
        header.findViewById(R.id.home_inbox_button).setOnClickListener(ignored ->
                startActivity(new Intent(requireContext(), InboxActivity.class)));
        header.findViewById(R.id.home_bell_button).setOnClickListener(ignored -> startActivity(
                new Intent(requireContext(), com.example.ungdungchiasecaycanh.ui.notifications.NotificationsActivity.class)));
        bellViewModel = new ViewModelProvider(this).get(com.example.ungdungchiasecaycanh.ui.notifications.BellViewModel.class);
        bellViewModel.getChats().observe(getViewLifecycleOwner(), n ->
                com.example.ungdungchiasecaycanh.ui.notifications.BellViewModel.bindBadge(
                        header.findViewById(R.id.home_inbox_badge), header.findViewById(R.id.home_inbox_button), n,
                        R.string.home_inbox_description, R.plurals.inbox_unread_chats));
        bellViewModel.getCount().observe(getViewLifecycleOwner(), n -> {
            TextView badge = header.findViewById(R.id.home_bell_badge);
            int value = n == null ? 0 : n;
            badge.setVisibility(value > 0 ? View.VISIBLE : View.GONE);
            badge.setText(value > 9 ? "9+" : String.valueOf(value));
            header.findViewById(R.id.home_bell_button).setContentDescription(value > 0
                    ? getResources().getQuantityString(R.plurals.notifications_bell_unread, value, value)
                    : getString(R.string.notifications_title));
        });
        footer.findViewById(R.id.home_see_all).setOnClickListener(v -> viewModel.seeAll());
        footer.findViewById(R.id.home_sales_see_all).setOnClickListener(v -> {
            keepChipsInView = true;
            viewModel.setTypes(EnumSet.of(HomeViewModel.Filter.SELL));
        });

        View.OnClickListener openFilters = ignored -> {
            if (getChildFragmentManager().findFragmentByTag(HomeFiltersBottomSheet.TAG) == null) {
                new HomeFiltersBottomSheet().show(getChildFragmentManager(), HomeFiltersBottomSheet.TAG);
            }
        };
        header.findViewById(R.id.home_search_filters).setOnClickListener(openFilters);
        // "Newest ▾" only picks the order; filters live on the button beside search.
        header.findViewById(R.id.home_sort).setOnClickListener(this::showSortMenu);
        viewModel.setPreferences(SettingsRepository.tradesFirst(new SettingsRepository(requireContext()).getIntents()));
        getChildFragmentManager().setFragmentResultListener(HomeFiltersBottomSheet.TAG, getViewLifecycleOwner(),
                (key, result) -> keepChipsInView = true);
        viewModel.getRefine().observe(getViewLifecycleOwner(), refine -> {
            TextView badge = header.findViewById(R.id.home_filter_badge);
            int active = refine.activeCount();
            badge.setVisibility(active > 0 ? View.VISIBLE : View.GONE);
            badge.setText(String.valueOf(active));
            header.findViewById(R.id.home_search_filters).setContentDescription(active > 0
                    ? getString(R.string.home_filters_active_desc, active)
                    : getString(R.string.home_filters_description));
            bindAreaChip();
            // The label names the order actually applied (BUG-014).
            ((TextView) header.findViewById(R.id.home_sort)).setText(sortLabel(refine.sort));
        });
        viewModel.getSaveFailed().observe(getViewLifecycleOwner(), failed -> {
            if (!Boolean.TRUE.equals(failed)) return;
            viewModel.clearSaveFailed();
            Snackbar.make(view, R.string.home_save_failed, Snackbar.LENGTH_SHORT).show();
        });

        ((EditText) header.findViewById(R.id.home_search)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                viewModel.setSearch(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });

        bindAreaChip();
        viewModel.getHeader().observe(getViewLifecycleOwner(), state -> {
            profileArea = state.area;
            bindAreaChip();
        });
        viewModel.getOwners().observe(getViewLifecycleOwner(), owners -> {
            adapter.setOwners(owners);
            heroAdapter.setOwners(owners);
            if (lastState != null) bindGivers(lastState, owners);
        });
        viewModel.getState().observe(getViewLifecycleOwner(), this::render);
        viewModel.load();
        viewModel.loadHeader();
    }

    // ---------------------------------------------------------------- hero slider

    /** "Featured near you" (mockup C1): a sideways strip of photo cards that drifts by itself after a while. */
    @android.annotation.SuppressLint("ClickableViewAccessibility") // the listener only watches; clicks still reach the cards
    private void setUpHero() {
        heroView = header.findViewById(R.id.home_hero_recycler);
        heroView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        heroAdapter = new HomeHeroAdapter(this::openListing);
        heroView.setAdapter(heroAdapter);
        // Touching the strip stops the drift; it starts again a while after the finger lifts.
        heroView.setOnTouchListener((v, e) -> {
            int a = e.getActionMasked();
            if (a == android.view.MotionEvent.ACTION_DOWN) stopHeroDrift();
            else if (a == android.view.MotionEvent.ACTION_UP || a == android.view.MotionEvent.ACTION_CANCEL) scheduleHeroDrift();
            return false;
        });
    }

    /** After this long on Home (or after the last touch) the featured cards start drifting right to left. */
    private static final long HERO_DRIFT_DELAY_MS = 6_000;
    /** Drift speed: slow enough to read a card as it passes. */
    private static final float HERO_DRIFT_DP_PER_S = 28f;
    private boolean heroDrifting;
    private long heroLastFrame;
    private float heroCarry;
    private final Runnable heroStartDrift = () -> {
        if (!isAdded() || !heroAdapter.loops()) return;
        heroDrifting = true;
        heroLastFrame = 0;
        heroView.postOnAnimation(this::heroDriftFrame);
    };

    /** (Re)starts the wait before the drift; with reduced motion the strip never moves by itself. */
    private void scheduleHeroDrift() {
        stopHeroDrift();
        if (heroView != null && heroAdapter.loops() && Motion.enabled(requireContext()))
            heroView.postDelayed(heroStartDrift, HERO_DRIFT_DELAY_MS);
    }

    private void stopHeroDrift() {
        heroDrifting = false;
        if (heroView != null) heroView.removeCallbacks(heroStartDrift);
    }

    /** One frame: move by the time elapsed; after one full round jump back by a round (the cards repeat). */
    private void heroDriftFrame() {
        if (!heroDrifting || !isAdded()) return;
        long now = android.os.SystemClock.uptimeMillis();
        if (heroLastFrame != 0) {
            float density = getResources().getDisplayMetrics().density;
            heroCarry += HERO_DRIFT_DP_PER_S * density * (now - heroLastFrame) / 1000f;
            int step = (int) heroCarry;
            heroCarry -= step;
            if (step > 0) heroView.scrollBy(step, 0);
            int round = Math.round(heroAdapter.cardCount() * (160 + 10) * density); // card width + gap
            if (heroView.computeHorizontalScrollOffset() >= round) heroView.scrollBy(-round, 0);
        }
        heroLastFrame = now;
        heroView.postOnAnimation(this::heroDriftFrame);
    }

    /** Hero mix: give-aways lead, then a swap and a sale; members who came to swap see swaps first. */
    private List<Listing> heroItems(HomeViewModel.HomeUiState state) {
        List<Listing> give = state.newestGiveAways == null ? Collections.emptyList() : state.newestGiveAways;
        List<Listing> trade = state.newestTrades == null ? Collections.emptyList() : state.newestTrades;
        List<Listing> sale = HomeViewModel.activeOnly(state.sales); // reserved and sold sales stay out of the hero
        boolean tradesFirst = SettingsRepository.tradesFirst(new SettingsRepository(requireContext()).getIntents());
        List<List<Listing>> order = new ArrayList<>();
        if (tradesFirst) { order.add(trade); order.add(give); } else { order.add(give); order.add(trade); }
        order.add(sale);
        int rounds = Math.max(give.size(), Math.max(trade.size(), sale.size()));
        Map<String, Listing> out = new LinkedHashMap<>();
        // Round robin over the three kinds keeps the slides varied; covers that are mostly red stay out.
        for (int round = 0; out.size() < HERO_MAX && round < rounds; round++) {
            for (List<Listing> kind : order) {
                if (round >= kind.size() || out.size() >= HERO_MAX) continue;
                Listing l = kind.get(round);
                if (Boolean.TRUE.equals(heroToneOk(l))) out.put(l.getListingId(), l);
            }
        }
        return new ArrayList<>(out.values());
    }

    /**
     * Whether a listing's cover suits the slider: true when it has a photo that is not mostly red, false
     * otherwise, null while its small copy is still being checked (the slider redraws once it is).
     */
    private Boolean heroToneOk(Listing listing) {
        String cover = listing.getCoverImageUrl();
        if (cover == null || cover.trim().isEmpty()) return false;
        Boolean known = heroTone.get(cover);
        if (known != null || !heroToneAsked.add(cover)) return known;
        // Same width as the slide loads it: web photos share one cache entry per URL, whatever the width.
        int slideWidth = getResources().getDisplayMetrics().widthPixels;
        RemoteImageLoader.loadBitmap(requireContext(), cover, slideWidth, bitmap -> {
            if (bitmap == null) { heroTone.put(cover, false); scheduleHeroRedraw(); return; }
            int h = Math.max(1, Math.round(HERO_TONE_PX * bitmap.getHeight() / (float) bitmap.getWidth()));
            Bitmap small = Bitmap.createScaledBitmap(bitmap, HERO_TONE_PX, h, true);
            int[] pixels = new int[small.getWidth() * small.getHeight()];
            small.getPixels(pixels, 0, small.getWidth(), 0, 0, small.getWidth(), small.getHeight());
            if (small != bitmap) small.recycle();
            heroTone.put(cover, !PhotoTone.mostlyRed(pixels));
            scheduleHeroRedraw();
        });
        return null;
    }

    /** One redraw for a burst of checked covers. */
    private void scheduleHeroRedraw() {
        if (!isAdded() || heroView == null) return;
        heroView.removeCallbacks(heroRedraw);
        heroView.postDelayed(heroRedraw, 120);
    }

    private final Runnable heroRedraw = () -> { if (isAdded() && lastState != null) render(lastState); };

    // ---------------------------------------------------------------- header, chips, grid

    /** The area line names the member's own area (it is a label, not a filter). */
    private void bindAreaChip() {
        TextView chip = header.findViewById(R.id.home_area_chip);
        chip.setText(profileArea.isEmpty() ? getString(R.string.home_filters_any_area) : profileArea);
    }

    /** "All" is lit when no type is chosen; otherwise every chosen type is. */
    private void bindSegments(Set<HomeViewModel.Filter> types) {
        for (int segment : SEGMENTS) {
            HomeViewModel.Filter type = filterForId(segment);
            header.findViewById(segment).setSelected(type == HomeViewModel.Filter.ALL
                    ? types.isEmpty() : types.contains(type));
        }
    }

    /** Scrolls so the chips sit at the top with the grid right below. */
    private void scrollToGrid() {
        View title = header.findViewById(R.id.home_fresh_title);
        GridLayoutManager grid = (GridLayoutManager) listingsView.getLayoutManager();
        if (grid != null) grid.scrollToPositionWithOffset(0, -title.getTop() + (int) (8 * getResources().getDisplayMetrics().density));
    }

    private void selectTab(int itemId) {
        BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
        if (nav != null) nav.setSelectedItemId(itemId);
    }

    private void openListing(String listingId) {
        if (listingId == null || listingId.trim().isEmpty()) return;
        startActivity(new Intent(requireContext(), ListingDetailActivity.class)
                .putExtra(ListingDetailActivity.EXTRA_LISTING_ID, listingId));
    }

    private HomeViewModel.Filter filterForId(int id) {
        if (id == R.id.home_filter_give_away) return HomeViewModel.Filter.GIVE_AWAY;
        if (id == R.id.home_filter_trade) return HomeViewModel.Filter.TRADE;
        if (id == R.id.home_filter_sell) return HomeViewModel.Filter.SELL;
        return HomeViewModel.Filter.ALL;
    }

    @Override public void onResume() {
        super.onResume();
        if (bellViewModel != null) bellViewModel.refresh();
        if (heroAdapter != null && heroAdapter.getItemCount() > 0) scheduleHeroDrift();
        if (firstResume) firstResume = false;
        else if (resultReloadIssued) resultReloadIssued = false;
        else if (!createFlowActive && viewModel != null) {
            viewModel.refreshIfStale(REFRESH_AFTER_MS);
            viewModel.loadHeader();
        }
    }

    @Override public void onPause() {
        super.onPause();
        stopHeroDrift();
    }

    /** "Vườn hôm nay" (mockup E2): today's watering line, done / due as a bar, and up to three plant photos. */
    private static int sortLabel(HomeViewModel.Sort sort) {
        return sort == HomeViewModel.Sort.MATCH ? R.string.home_sort_match
                : sort == HomeViewModel.Sort.NEAR ? R.string.home_sort_near : R.string.home_sort_newest;
    }

    /** Small menu under "Newest ▾": Newest, Best match, Near you, each with a one-line reason. */
    private void showSortMenu(View anchor) {
        HomeViewModel.Refine refine = viewModel.getRefine().getValue();
        HomeViewModel.Sort current = refine == null ? HomeViewModel.Sort.NEWEST : refine.sort;
        LinearLayout menu = new LinearLayout(requireContext());
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackgroundResource(R.drawable.bg_sort_menu);
        int pad = Math.round(6 * getResources().getDisplayMetrics().density);
        menu.setPadding(pad, pad, pad, pad);
        android.widget.PopupWindow popup = new android.widget.PopupWindow(menu,
                Math.round(270 * getResources().getDisplayMetrics().density), ViewGroup.LayoutParams.WRAP_CONTENT, true);
        int[][] options = {
                {HomeViewModel.Sort.NEWEST.ordinal(), R.string.home_sort_newest, R.string.home_sort_newest_hint},
                {HomeViewModel.Sort.MATCH.ordinal(), R.string.home_sort_match, R.string.home_sort_match_hint},
                {HomeViewModel.Sort.NEAR.ordinal(), R.string.home_sort_near, R.string.home_sort_near_hint}};
        for (int[] o : options) {
            HomeViewModel.Sort sort = HomeViewModel.Sort.values()[o[0]];
            View row = getLayoutInflater().inflate(R.layout.item_sort_option, menu, false);
            ((TextView) row.findViewById(R.id.sortTitle)).setText(o[1]);
            ((TextView) row.findViewById(R.id.sortHint)).setText(o[2]);
            boolean on = sort == current;
            row.findViewById(R.id.sortCheck).setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            row.setSelected(on);
            row.setOnClickListener(v -> {
                popup.dismiss();
                keepChipsInView = true;
                viewModel.setSort(sort);
            });
            menu.addView(row);
        }
        popup.setElevation(12 * getResources().getDisplayMetrics().density);
        popup.setOutsideTouchable(true);
        popup.showAsDropDown(anchor, 0, pad, android.view.Gravity.END);
    }

    private void updateGardenWidget(List<PlantEntity> plants, List<CareLogEntity> care) {
        if (!isAdded()) return;
        TextView status = header.findViewById(R.id.home_garden_status);
        LinearProgressIndicator progress = header.findViewById(R.id.home_garden_progress);
        LinearLayout thumbs = header.findViewById(R.id.home_garden_thumbs);
        thumbs.removeAllViews();
        if (plants == null || plants.isEmpty()) {
            status.setText(R.string.home_garden_empty);
            progress.setVisibility(View.GONE);
            return;
        }
        GardenOverview o = GardenOverview.of(plants, care, System.currentTimeMillis());
        status.setText(o.pending > 0
                ? getResources().getQuantityString(R.plurals.home_garden_due, o.pending, o.pending, plants.size())
                : getString(R.string.home_garden_clear, plants.size()));
        // Nothing due counts as a full bar: the garden is looked after for today.
        int planned = o.done + o.pending;
        progress.setVisibility(View.VISIBLE);
        progress.setProgressCompat(planned == 0 ? 100 : Math.round(100f * o.done / planned), true);
        float dp = getResources().getDisplayMetrics().density;
        int size = Math.round(30 * dp);
        for (PlantEntity plant : plants) {
            if (thumbs.getChildCount() == 3) break;
            if (plant.getImageUri().trim().isEmpty()) continue;
            ShapeableImageView photo = new ShapeableImageView(requireContext());
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.setShapeAppearanceModel(ShapeAppearanceModel.builder().setAllCornerSizes(size / 2f).build());
            photo.setStrokeColorResource(R.color.color_primary_tint);
            photo.setStrokeWidth(2 * dp);
            photo.setPadding((int) dp, (int) dp, (int) dp, (int) dp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            if (thumbs.getChildCount() > 0) lp.setMarginStart(Math.round(-9 * dp));
            thumbs.addView(photo, lp);
            LocalImageLoader.load(photo, plant.getImageUri(), size, null);
        }
    }

    private void render(HomeViewModel.HomeUiState state) {
        lastState = state;
        boolean loading = state.loading;
        boolean error = !loading && state.error != null;
        boolean empty = !loading && !error && state.listings.isEmpty() && state.sales.isEmpty();
        loadingView.setVisibility(loading ? View.VISIBLE : View.GONE);
        errorView.setVisibility(error ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (error) errorMessage.setText(R.string.home_load_error);
        adapter.submitList(loading || error ? null : state.listings, state.savedIds, state.uid);
        if (loading || error) {
            footer.setVisibility(View.GONE);
            return;
        }

        int total = state.giveAwayCount + state.tradeCount + state.sellCount;
        ((TextView) header.findViewById(R.id.home_empty_title)).setText(total > 0
                ? R.string.home_search_no_results : R.string.home_empty_title);
        ((TextView) header.findViewById(R.id.home_empty_supporting)).setText(total > 0
                ? R.string.home_search_no_results_supporting : R.string.home_empty_supporting_text);
        ((TextView) header.findViewById(R.id.home_empty_action)).setText(total > 0
                ? R.string.home_search_clear : R.string.home_empty_action);
        bindSegments(state.types);
        // While filtering, one line says how many plants matched; the plain "All" view shows none.
        TextView result = header.findViewById(R.id.home_filter_result);
        result.setVisibility(state.preview || state.matchCount == 0 ? View.GONE : View.VISIBLE);
        result.setText(getResources().getQuantityString(R.plurals.home_filter_result, state.matchCount, state.matchCount));

        // The featured slider, the garden reminder and the footer sections belong to the "All" preview.
        boolean preview = state.preview;
        List<Listing> hero = preview ? heroItems(state) : Collections.emptyList();
        int heroVisibility = hero.isEmpty() ? View.GONE : View.VISIBLE;
        header.findViewById(R.id.home_headline_block).setVisibility(heroVisibility);
        heroView.setVisibility(heroVisibility);
        boolean firstHero = heroAdapter.getItemCount() == 0 && !hero.isEmpty();
        heroAdapter.submitList(hero);
        if (firstHero) scheduleHeroDrift();
        else if (hero.isEmpty()) stopHeroDrift();
        header.findViewById(R.id.home_garden_widget).setVisibility(preview ? View.VISIBLE : View.GONE);

        footer.setVisibility(View.VISIBLE);
        footer.findViewById(R.id.home_see_all).setVisibility(state.hasMore ? View.VISIBLE : View.GONE);
        bindUrgent(preview ? state.listings : Collections.emptyList());
        bindGivers(state, viewModel.getOwners().getValue());
        bindSales(preview ? state.sales : Collections.emptyList());
        bindCta(state);

        List<Listing> shown = new ArrayList<>(state.listings);
        shown.addAll(hero);
        viewModel.loadOwners(shown);
        // Hiding the slider shortens the header; scroll after the new layout so the chips stay on screen.
        if (keepChipsInView) {
            keepChipsInView = false;
            listingsView.post(this::scrollToGrid);
        }
    }

    // ---------------------------------------------------------------- footer sections

    /** Listings that leave Home within a week unless their owner confirms them (30-day rule). */
    private void bindUrgent(List<Listing> listings) {
        long now = System.currentTimeMillis();
        long day = DateUtils.DAY_IN_MILLIS;
        List<Listing> urgent = new ArrayList<>();
        for (Listing l : listings) {
            long left = ListingRepository.HIDE_AFTER_MS - ListingRepository.idleMs(l, now);
            if (left > 0 && left <= 7 * day) urgent.add(l);
        }
        Collections.sort(urgent, (a, b) -> Long.compare(ListingRepository.idleMs(b, now), ListingRepository.idleMs(a, now)));
        ViewGroup rows = footer.findViewById(R.id.home_urgent_rows);
        rows.removeAllViews();
        footer.findViewById(R.id.home_urgent_section).setVisibility(urgent.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int photoPx = (int) (56 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < Math.min(URGENT_MAX, urgent.size()); i++) {
            Listing l = urgent.get(i);
            View row = inflater.inflate(R.layout.item_home_urgent_row, rows, false);
            if (i > 0) row.setBackgroundResource(R.drawable.bg_top_hairline_panel);
            RemoteImageLoader.load(row.findViewById(R.id.urgent_photo), l.getCoverImageUrl(), photoPx);
            ((TextView) row.findViewById(R.id.urgent_name)).setText(l.getNickname());
            String area = l.getLocationText() == null ? "" : l.getLocationText().trim();
            ((TextView) row.findViewById(R.id.urgent_meta)).setText(getString(R.string.home_urgent_meta, area));
            int days = (int) Math.max(1, Math.ceil((ListingRepository.HIDE_AFTER_MS - ListingRepository.idleMs(l, now)) / (double) day));
            ((TextView) row.findViewById(R.id.urgent_days)).setText(
                    getResources().getQuantityString(R.plurals.home_urgent_days, days, days));
            row.setOnClickListener(v -> openListing(l.getListingId()));
            rows.addView(row);
        }
    }

    /** Members giving the most plants among the listings on Home (their profiles must be loaded). */
    private void bindGivers(HomeViewModel.HomeUiState state, @Nullable Map<String, User> owners) {
        ViewGroup row = footer.findViewById(R.id.home_givers_row);
        row.removeAllViews();
        boolean preview = state.preview;
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (preview && owners != null) {
            for (Listing l : state.listings) {
                if (l.getType() != ListingType.GIVE_AWAY || l.getOwnerId() == null) continue;
                if (l.getOwnerId().equals(state.uid) || !owners.containsKey(l.getOwnerId())) continue;
                Integer c = counts.get(l.getOwnerId());
                counts.put(l.getOwnerId(), c == null ? 1 : c + 1);
            }
        }
        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(counts.entrySet());
        Collections.sort(ranked, (a, b) -> b.getValue() - a.getValue());
        footer.findViewById(R.id.home_givers_section).setVisibility(ranked.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < Math.min(GIVERS_MAX, ranked.size()); i++) {
            String uid = ranked.get(i).getKey();
            int count = ranked.get(i).getValue();
            User user = owners.get(uid);
            View card = inflater.inflate(R.layout.item_home_giver, row, false);
            AvatarBinder.bind(card.findViewById(R.id.giver_avatar), user.getDisplayName(), user.getAvatarUrl(),
                    R.drawable.bg_avatar_neutral);
            ((TextView) card.findViewById(R.id.giver_name)).setText(user.getDisplayName());
            ((TextView) card.findViewById(R.id.giver_count)).setText(
                    getResources().getQuantityString(R.plurals.home_giver_count, count, count));
            card.findViewById(R.id.giver_view).setOnClickListener(v -> startActivity(
                    new Intent(requireContext(), MemberProfileActivity.class)
                            .putExtra(MemberProfileActivity.EXTRA_USER_ID, uid)));
            row.addView(card);
        }
    }

    /** "Good prices for sale": available first, then reserved, then sold; cheapest first within each. */
    private void bindSales(List<Listing> sales) {
        List<Listing> sorted = new ArrayList<>(sales);
        Collections.sort(sorted, (a, b) -> a.getStatus() != b.getStatus()
                ? Integer.compare(HomeViewModel.saleRank(a), HomeViewModel.saleRank(b))
                : Long.compare(a.getPrice(), b.getPrice()));
        ViewGroup rows = footer.findViewById(R.id.home_sales_rows);
        rows.removeAllViews();
        footer.findViewById(R.id.home_sales_section).setVisibility(sorted.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int photoPx = (int) (200 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < Math.min(SALES_MAX, sorted.size()); i++) {
            Listing l = sorted.get(i);
            View card = inflater.inflate(R.layout.item_home_sale_card, rows, false);
            RemoteImageLoader.load(card.findViewById(R.id.sale_card_photo), l.getCoverImageUrl(), photoPx);
            ((TextView) card.findViewById(R.id.sale_card_price)).setText(
                    getString(R.string.listing_type_sell_detail, ListingAdapter.formatVnd(l.getPrice())));
            ((TextView) card.findViewById(R.id.sale_card_name)).setText(l.getNickname());
            String area = l.getLocationText() == null ? "" : l.getLocationText().trim();
            ((TextView) card.findViewById(R.id.sale_card_meta)).setText(area);
            ListingAdapter.bindSaleStatus(card.findViewById(R.id.sale_card_status), card.findViewById(R.id.sale_card_photo), l);
            card.setOnClickListener(v -> openListing(l.getListingId()));
            rows.addView(card);
        }
    }

    /** The "Post" panel shows a neighbour's plant photo beside the invitation. */
    private void bindCta(HomeViewModel.HomeUiState state) {
        ImageView photo = footer.findViewById(R.id.home_cta_photo);
        String url = null;
        for (Listing l : state.listings) {
            if (l.getCoverImageUrl() != null && !l.getCoverImageUrl().isEmpty()) {
                url = l.getCoverImageUrl();
                break;
            }
        }
        photo.setVisibility(url == null ? View.GONE : View.VISIBLE);
        if (url != null) RemoteImageLoader.load(photo, url, (int) (112 * getResources().getDisplayMetrics().density));
    }

    /** Outer gutter for grid cards; with their 6dp margins the page edge is 20dp and the gap 12dp. */
    private static final class GridGutter extends RecyclerView.ItemDecoration {
        private final int gutter;

        GridGutter(int gutter) { this.gutter = gutter; }

        @Override public void getItemOffsets(@NonNull Rect out, @NonNull View view,
                                             @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            if (((GridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanSize() != 1) return;
            int span = ((GridLayoutManager.LayoutParams) view.getLayoutParams()).getSpanIndex();
            if (span == 0) out.left = gutter; else out.right = gutter;
        }
    }
}
