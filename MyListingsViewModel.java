package com.example.ungdungchiasecaycanh.ui.listing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyListingsViewModel extends ViewModel {
    public enum Filter { ALL, ACTIVE, RESERVED, REHOMED, UNAVAILABLE }
    public enum LoadStatus { LOADING, CONTENT, EMPTY, ERROR }

    public static final class UiState {
        public final LoadStatus status;
        public final Filter filter;
        public final List<Listing> listings;
        public final boolean noOwnedListings;
        public final String updatingListingId;
        public final int errorVersion;

        private UiState(LoadStatus status, Filter filter, List<Listing> listings,
                        boolean noOwnedListings, String updatingListingId, int errorVersion) {
            this.status = status;
            this.filter = filter;
            this.listings = listings;
            this.noOwnedListings = noOwnedListings;
            this.updatingListingId = updatingListingId;
            this.errorVersion = errorVersion;
        }
    }

    private final ListingRepository repository = new ListingRepository();
    private final MutableLiveData<UiState> state = new MutableLiveData<>();
    private List<Listing> ownedListings = new ArrayList<>();
    private Filter selectedFilter = Filter.ALL;
    private String updatingListingId;
    private int errorVersion;
    private boolean loaded;

    public LiveData<UiState> getState() { return state; }

    public void loadMyListings() {
        state.setValue(new UiState(LoadStatus.LOADING, selectedFilter,
                Collections.emptyList(), false, updatingListingId, errorVersion));
        repository.getMyListings().addOnSuccessListener(listings -> {
            ownedListings = new ArrayList<>(listings);
            loaded = true;
            publish();
        }).addOnFailureListener(error -> {
            state.postValue(new UiState(LoadStatus.ERROR, selectedFilter,
                    Collections.emptyList(), false, updatingListingId, errorVersion));
        });
    }

    public void retry() { loadMyListings(); }

    public void setFilter(Filter filter) {
        if (filter == null) return;
        selectedFilter = filter;
        if (loaded) publish();
    }

    public void updateStatus(Listing listing, ListingStatus target) {
        if (listing == null || target == null || updatingListingId != null
                || !canTransition(listing.getStatus(), target)) return;
        updatingListingId = listing.getListingId();
        publish();
        repository.updateListingStatus(listing.getListingId(), target)
                .addOnSuccessListener(ignored -> {
                    List<Listing> updated = new ArrayList<>(ownedListings.size());
                    for (Listing current : ownedListings) {
                        updated.add(current.getListingId().equals(listing.getListingId())
                                ? withStatus(current, target) : current);
                    }
                    ownedListings = updated;
                    updatingListingId = null;
                    publish();
                })
                .addOnFailureListener(error -> {
                    updatingListingId = null;
                    errorVersion++;
                    publish();
                });
    }

    private void publish() {
        List<Listing> filtered = filterListings(ownedListings, selectedFilter);
        boolean noOwned = ownedListings.isEmpty();
        state.postValue(new UiState(filtered.isEmpty() ? LoadStatus.EMPTY : LoadStatus.CONTENT,
                selectedFilter, filtered, noOwned, updatingListingId, errorVersion));
    }

    static Listing withStatus(Listing listing, ListingStatus status) {
        return new Listing(listing.getListingId(), listing.getOwnerId(), listing.getNickname(),
                listing.getSpecies(), listing.getDescription(), listing.getType(), listing.getPrice(),
                listing.getLookingFor(), listing.getImageUrls(), listing.getLocationText(), status,
                listing.getCreatedAt(), listing.getUpdatedAt());
    }

    public static List<Listing> filterListings(List<Listing> source, Filter filter) {
        List<Listing> result = new ArrayList<>();
        if (source == null) return result;
        for (Listing listing : source) {
            if (filter == Filter.ALL || matches(listing, filter)) result.add(listing);
        }
        return result;
    }

    private static boolean matches(Listing listing, Filter filter) {
        if (listing == null) return false;
        switch (filter) {
            case ACTIVE: return listing.getStatus() == ListingStatus.ACTIVE;
            case RESERVED: return listing.getStatus() == ListingStatus.RESERVED;
            case REHOMED: return listing.getStatus() == ListingStatus.REHOMED;
            case UNAVAILABLE: return listing.getStatus() == ListingStatus.UNAVAILABLE;
            default: return true;
        }
    }

    public static boolean canTransition(ListingStatus current, ListingStatus target) {
        if (current == null || target == null) return false;
        if (current == target) return true;
        switch (current) {
            case ACTIVE:
                return target == ListingStatus.RESERVED || target == ListingStatus.REHOMED
                        || target == ListingStatus.UNAVAILABLE;
            case RESERVED:
                return target == ListingStatus.ACTIVE || target == ListingStatus.REHOMED
                        || target == ListingStatus.UNAVAILABLE;
            case UNAVAILABLE:
                return target == ListingStatus.ACTIVE;
            case REHOMED:
            default:
                return false;
        }
    }
}
