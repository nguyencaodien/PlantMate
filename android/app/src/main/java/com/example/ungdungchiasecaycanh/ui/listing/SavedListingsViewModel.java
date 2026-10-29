package com.example.ungdungchiasecaycanh.ui.listing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class SavedListingsViewModel extends ViewModel {
    public enum Status { LOADING, CONTENT, EMPTY, ERROR }

    public static final class UiState {
        public final Status status;
        public final List<Listing> listings;

        private UiState(Status status, List<Listing> listings) {
            this.status = status;
            this.listings = listings;
        }
    }

    private final ListingRepository repository = new ListingRepository();
    private final SafetyRepository safety = new SafetyRepository();
    private final MutableLiveData<UiState> state = new MutableLiveData<>();
    private int loadGeneration;

    public LiveData<UiState> getState() {
        return state;
    }

    public void loadSavedListings() {
        int generation = ++loadGeneration;
        state.setValue(new UiState(Status.LOADING, Collections.emptyList()));
        String uid = safety.getCurrentUserId();
        if (uid == null) { showError(generation); return; }
        Task<List<Listing>> listingTask = repository.getSavedListings();
        Task<Set<String>> blockedTask = safety.getBlockedUids();
        Tasks.whenAllComplete(listingTask, blockedTask).addOnCompleteListener(done -> {
            if (generation != loadGeneration) return;
            if (!uid.equals(safety.getCurrentUserId()) || !listingTask.isSuccessful()
                    || !blockedTask.isSuccessful()) { showError(generation); return; }
            List<Listing> visible = SafetyRepository.filterByBlockedAuthors(
                    listingTask.getResult(), blockedTask.getResult(), Listing::getOwnerId);
            state.postValue(new UiState(visible.isEmpty() ? Status.EMPTY : Status.CONTENT, visible));
        });
    }

    private void showError(int generation) {
        if (generation == loadGeneration)
            state.postValue(new UiState(Status.ERROR, Collections.emptyList()));
    }

    public void retry() {
        loadSavedListings();
    }
}
