package com.example.ungdungchiasecaycanh.ui.listing;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.AndroidViewModel;
import android.app.Application;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;

import com.example.ungdungchiasecaycanh.data.repository.AuthRepository;
import com.example.ungdungchiasecaycanh.data.repository.ImageRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class ListingPreviewViewModel extends AndroidViewModel {
    public enum Status { IDLE, PUBLISHING, SUCCESS, ERROR }

    public static final class State {
        public final Status status;
        public final String listingId;
        /** ERROR because a SELL listing needs a verified email first. */
        public final boolean needsVerification;

        State(Status status, String listingId) {
            this(status, listingId, false);
        }

        State(Status status, String listingId, boolean needsVerification) {
            this.status = status;
            this.listingId = listingId;
            this.needsVerification = needsVerification;
        }
    }

    private final ListingRepository repository = new ListingRepository();
    private final ImageRepository images;
    private final AuthRepository auth = new AuthRepository();
    private final MutableLiveData<State> state =
            new MutableLiveData<>(new State(Status.IDLE, null));

    public ListingPreviewViewModel(@NonNull Application application) {
        super(application);
        images = new ImageRepository(application);
    }

    public LiveData<State> getState() {
        return state;
    }

    public synchronized void publish(String nickname, String species, String description,
                                     ListingType type, long price, String lookingFor,
                                     List<String> localImageUris, String locationText,
                                     ListingDetails details) {
        State current = state.getValue();
        if (current != null && !canStart(current.status)) {
            return;
        }
        state.setValue(new State(Status.PUBLISHING, null));
        boolean selling = type == ListingType.SELL;
        // Selling needs a verified email and Firestore checks the sign-in token, so refresh it first.
        Task<Void> ready = selling ? auth.refreshToken() : Tasks.forResult(null);
        ready.continueWithTask(checked -> {
            if (!checked.isSuccessful()) throw checked.getException();
            if (selling && !auth.isEmailVerified()) throw new IllegalStateException("Email not verified");
            // Photos are uploaded first (compressed, stored in Firestore), then the listing references them.
            return images.uploadAll(localImageUris == null ? new ArrayList<>() : localImageUris);
        }).continueWithTask(uploaded -> {
            if (!uploaded.isSuccessful()) throw uploaded.getException();
            return repository.publishListing(nickname, species, description, type, price,
                    lookingFor, uploaded.getResult(), locationText, details);
        }).addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && !task.getResult().trim().isEmpty()) {
                        state.setValue(new State(Status.SUCCESS, task.getResult()));
                    } else {
                        state.setValue(new State(Status.ERROR, null, selling && !auth.isEmailVerified()));
                    }
                });
    }

    static boolean canStart(Status status) {
        return status == Status.IDLE || status == Status.ERROR;
    }
}
