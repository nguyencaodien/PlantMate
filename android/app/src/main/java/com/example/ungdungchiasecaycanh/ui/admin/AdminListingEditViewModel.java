package com.example.ungdungchiasecaycanh.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.AdminRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin editor for one listing, reloaded by its listingId. */
public class AdminListingEditViewModel extends ViewModel {

    public static final class Data {
        public final Listing listing;
        public final String ownerName;
        Data(Listing listing, String ownerName) { this.listing = listing; this.ownerName = ownerName; }
    }

    private final AdminRepository repository = new AdminRepository();
    private final ListingRepository listings = new ListingRepository();
    private final MutableLiveData<AdminEditState<Data>> state = new MutableLiveData<>(AdminEditState.loading());
    private String listingId;

    public LiveData<AdminEditState<Data>> getState() { return state; }

    public void load(String listingId) {
        this.listingId = listingId;
        state.setValue(AdminEditState.loading());
        listings.getListing(listingId)
                .addOnSuccessListener(listing -> repository.getMember(listing.getOwnerId())
                        .addOnSuccessListener(owner -> state.postValue(current().withStatus(
                                AdminEditState.Status.CONTENT, new Data(listing, owner.getDisplayName()))))
                        .addOnFailureListener(e -> state.postValue(current().withStatus(
                                AdminEditState.Status.CONTENT, new Data(listing, "")))))
                .addOnFailureListener(e -> state.postValue(current().withStatus(
                        e instanceof IllegalStateException ? AdminEditState.Status.NOT_FOUND : AdminEditState.Status.ERROR, null)));
    }

    public void save(String nickname, String species, String description, ListingType type, long price,
                     String lookingFor, String location, ListingStatus status, List<String> imageUrls,
                     String note) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("imageUrls", new ArrayList<>(imageUrls));
        fields.put("nickname", nickname);
        fields.put("species", species);
        fields.put("description", description);
        fields.put("type", type.name());
        fields.put("price", type == ListingType.SELL ? price : 0L);
        fields.put("lookingFor", type == ListingType.TRADE ? lookingFor : "");
        fields.put("locationText", location);
        fields.put("status", status.name());
        state.setValue(current().busy());
        repository.updateListing(listingId, fields, note)
                .addOnSuccessListener(ignored -> {
                    state.postValue(current().saved(null));
                    load(listingId);
                })
                .addOnFailureListener(e -> state.postValue(current().failed()));
    }

    public void delete(String note) {
        state.setValue(current().busy());
        repository.deleteListing(listingId, note)
                .addOnSuccessListener(ignored -> state.postValue(current().deleted()))
                .addOnFailureListener(e -> state.postValue(current().failed()));
    }

    private AdminEditState<Data> current() {
        AdminEditState<Data> value = state.getValue();
        return value == null ? AdminEditState.loading() : value;
    }
}
