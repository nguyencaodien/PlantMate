package com.example.ungdungchiasecaycanh.ui.listing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import java.util.Locale;

public class MyListingsActivity extends AppCompatActivity {
    /** Optional {@link MyListingsViewModel.Filter} name to open with. */
    public static final String EXTRA_FILTER = "extra_my_listings_filter";
    private MyListingsViewModel viewModel;
    private MyListingAdapter adapter;
    private View loading, content, empty, error;
    private TextView emptyTitle, emptySupporting, filteredEmpty;
    private int shownErrorVersion;

    private boolean rowsAnimated;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_listings);
        SecondaryChrome.apply(this, findViewById(R.id.my_listings_root), findViewById(R.id.my_listings_hero));
        Motion.start(findViewById(R.id.my_listings_mam));
        Motion.fromRight(findViewById(R.id.my_listings_filter_scroll), 200);
        loading = findViewById(R.id.my_listings_loading);
        content = findViewById(R.id.my_listings_recycler);
        empty = findViewById(R.id.my_listings_empty);
        filteredEmpty = findViewById(R.id.my_listings_filtered_empty);
        error = findViewById(R.id.my_listings_error);
        emptyTitle = findViewById(R.id.my_listings_empty_title);
        emptySupporting = findViewById(R.id.my_listings_empty_supporting);

        RecyclerView recycler = (RecyclerView) content;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyListingAdapter(new MyListingAdapter.Listener() {
            @Override public void onListingClick(String listingId) { openDetail(listingId); }
            @Override public void onManageStatus(Listing listing) { showStatusChoices(listing); }
        });
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(MyListingsViewModel.class);
        viewModel.getState().observe(this, this::render);
        findViewById(R.id.my_listings_back).setOnClickListener(view -> finish());
        findViewById(R.id.my_listings_retry).setOnClickListener(view -> viewModel.retry());
        findViewById(R.id.my_listings_share_plant).setOnClickListener(view ->
                startActivity(new Intent(this, CreateListingActivity.class)));
        setupFilters();
        // Opened from a Profile tab: show the same set of listings that tab counted (BUG-018).
        String filter = getIntent().getStringExtra(EXTRA_FILTER);
        if (savedInstanceState == null && filter != null) viewModel.setFilter(MyListingsViewModel.Filter.valueOf(filter));
        viewModel.loadMyListings();
    }

    private void setupFilters() {
        int[] ids = {R.id.my_filter_all, R.id.my_filter_active, R.id.my_filter_reserved,
                R.id.my_filter_rehomed, R.id.my_filter_unavailable};
        MyListingsViewModel.Filter[] filters = MyListingsViewModel.Filter.values();
        for (int i = 0; i < ids.length; i++) {
            final MyListingsViewModel.Filter filter = filters[i];
            findViewById(ids[i]).setOnClickListener(view -> viewModel.setFilter(filter));
        }
    }

    private void render(MyListingsViewModel.UiState state) {
        loading.setVisibility(state.status == MyListingsViewModel.LoadStatus.LOADING
                ? View.VISIBLE : View.GONE);
        error.setVisibility(state.status == MyListingsViewModel.LoadStatus.ERROR
                ? View.VISIBLE : View.GONE);
        boolean hasItems = state.status == MyListingsViewModel.LoadStatus.CONTENT;
        content.setVisibility(hasItems ? View.VISIBLE : View.GONE);
        boolean noOwned = state.status == MyListingsViewModel.LoadStatus.EMPTY
                && state.noOwnedListings && state.filter == MyListingsViewModel.Filter.ALL;
        empty.setVisibility(noOwned ? View.VISIBLE : View.GONE);
        boolean noFilterMatches = state.status == MyListingsViewModel.LoadStatus.EMPTY && !noOwned;
        filteredEmpty.setVisibility(noFilterMatches ? View.VISIBLE : View.GONE);
        if (noOwned) {
            emptyTitle.setText(R.string.my_listings_empty_title);
            emptySupporting.setText(R.string.my_listings_empty_supporting);
        }
        if (noFilterMatches) {
            filteredEmpty.setText(getString(R.string.my_listings_filtered_empty,
                    filterLabel(state.filter)));
        }
        adapter.submitList(state.listings, state.updatingListingId);
        if (!rowsAnimated && !state.listings.isEmpty()) {
            rowsAnimated = true;
            Motion.staggerRows(findViewById(R.id.my_listings_recycler), 220, 60);
        }
        selectFilterChip(state.filter);
        if (state.errorVersion > shownErrorVersion) {
            shownErrorVersion = state.errorVersion;
            Snackbar.make(findViewById(R.id.my_listings_root), R.string.my_listings_update_error,
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void selectFilterChip(MyListingsViewModel.Filter filter) {
        int selected = new int[]{R.id.my_filter_all, R.id.my_filter_active, R.id.my_filter_reserved,
                R.id.my_filter_rehomed, R.id.my_filter_unavailable}[filter.ordinal()];
        ((Chip) findViewById(selected)).setChecked(true);
    }

    private void openDetail(String listingId) {
        Intent intent = new Intent(this, ListingDetailActivity.class);
        intent.putExtra(ListingDetailActivity.EXTRA_LISTING_ID, listingId);
        startActivity(intent);
    }

    private void showStatusChoices(Listing listing) {
        List<ListingStatus> legal = legalTargets(listing.getStatus());
        if (legal.isEmpty()) return;
        String[] labels = new String[legal.size()];
        for (int i = 0; i < legal.size(); i++) labels[i] = statusLabel(legal.get(i));
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.my_listing_manage_status)
                .setItems(labels, (dialog, which) -> confirmStatusChange(listing, legal.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmStatusChange(Listing listing, ListingStatus target) {
        if (target == ListingStatus.REHOMED) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.my_listing_rehomed_title)
                    .setMessage(R.string.my_listing_rehomed_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.my_listing_confirm_rehomed,
                            (dialog, which) -> viewModel.updateStatus(listing, target))
                    .show();
        } else {
            viewModel.updateStatus(listing, target);
        }
    }

    private String statusLabel(ListingStatus status) {
        switch (status) {
            case ACTIVE: return getString(R.string.listing_status_active);
            case RESERVED: return getString(R.string.listing_status_reserved);
            case REHOMED: return getString(R.string.listing_status_rehomed);
            case UNAVAILABLE: return getString(R.string.listing_status_unavailable);
            default: return "";
        }
    }

    private static List<ListingStatus> legalTargets(ListingStatus current) {
        List<ListingStatus> result = new ArrayList<>();
        for (ListingStatus target : ListingStatus.values()) {
            if (MyListingsViewModel.canTransition(current, target)) result.add(target);
        }
        return result;
    }

    private String filterLabel(MyListingsViewModel.Filter filter) {
        switch (filter) {
            case ACTIVE: return getString(R.string.listing_status_active).toLowerCase(Locale.getDefault());
            case RESERVED: return getString(R.string.listing_status_reserved).toLowerCase(Locale.getDefault());
            case REHOMED: return getString(R.string.listing_status_rehomed).toLowerCase(Locale.getDefault());
            case UNAVAILABLE: return getString(R.string.listing_status_unavailable).toLowerCase(Locale.getDefault());
            default: return getString(R.string.my_filter_all).toLowerCase(Locale.getDefault());
        }
    }
}
