package com.example.ungdungchiasecaycanh.ui.listing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;

public class SavedListingsActivity extends AppCompatActivity {
    private SavedListingsViewModel viewModel;
    private SavedListingAdapter adapter;
    private View loading;
    private View content;
    private View empty;
    private View error;

    private boolean rowsAnimated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_listings);
        SecondaryChrome.apply(this, findViewById(R.id.saved_listings_root), findViewById(R.id.saved_listings_hero));
        Motion.start(findViewById(R.id.saved_listings_mam));

        loading = findViewById(R.id.saved_listings_loading);
        content = findViewById(R.id.saved_listings_recycler);
        empty = findViewById(R.id.saved_listings_empty);
        error = findViewById(R.id.saved_listings_error);
        RecyclerView recyclerView = (RecyclerView) content;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SavedListingAdapter(this::openListing);
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SavedListingsViewModel.class);
        viewModel.getState().observe(this, this::render);
        findViewById(R.id.saved_listings_back).setOnClickListener(view -> finish());
        findViewById(R.id.saved_listings_retry).setOnClickListener(view -> viewModel.retry());
        findViewById(R.id.saved_listings_browse).setOnClickListener(view -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.loadSavedListings();
    }

    private void render(SavedListingsViewModel.UiState state) {
        loading.setVisibility(state.status == SavedListingsViewModel.Status.LOADING
                ? View.VISIBLE : View.GONE);
        content.setVisibility(state.status == SavedListingsViewModel.Status.CONTENT
                ? View.VISIBLE : View.GONE);
        empty.setVisibility(state.status == SavedListingsViewModel.Status.EMPTY
                ? View.VISIBLE : View.GONE);
        error.setVisibility(state.status == SavedListingsViewModel.Status.ERROR
                ? View.VISIBLE : View.GONE);
        adapter.submitList(state.listings);
        if (!rowsAnimated && !state.listings.isEmpty()) {
            rowsAnimated = true;
            Motion.staggerRows(findViewById(R.id.saved_listings_recycler), 220, 60);
        }
    }

    private void openListing(String listingId) {
        Intent intent = new Intent(this, ListingDetailActivity.class);
        intent.putExtra(ListingDetailActivity.EXTRA_LISTING_ID, listingId);
        startActivity(intent);
    }
}
