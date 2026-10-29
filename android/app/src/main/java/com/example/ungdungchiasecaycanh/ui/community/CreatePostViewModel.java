package com.example.ungdungchiasecaycanh.ui.community;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.AndroidViewModel;
import android.app.Application;
import androidx.annotation.NonNull;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.CommunityRepository;
import com.example.ungdungchiasecaycanh.data.repository.ImageRepository;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.User;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class CreatePostViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean submitting;
        public final String error;
        public final String postId;
        public final Listing listing;
        public final boolean shareLoading;
        public final boolean shareInvalid;

        State(boolean s, String e, String p) {
            this(s, e, p, null, false, false);
        }

        State(boolean s, String e, String p, Listing l, boolean loading, boolean invalid) {
            submitting = s;
            error = e;
            postId = p;
            listing = l;
            shareLoading = loading;
            shareInvalid = invalid;
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(new State(false, null, null));
    private final CommunityRepository repository = new CommunityRepository();
    private final ImageRepository images;
    /** Chosen local photo (content URI), kept across rotation; uploaded only on submit. */
    private final MutableLiveData<String> photo = new MutableLiveData<>(null);

    public CreatePostViewModel(@NonNull Application application) {
        super(application);
        images = new ImageRepository(application);
    }

    public LiveData<String> getPhoto() { return photo; }

    private final MutableLiveData<User> author = new MutableLiveData<>();
    private final MutableLiveData<PostCategory> topic =
            new MutableLiveData<>(PostCategory.GENERAL);

    /** The signed-in author (name and avatar at the top of the composer); null until loaded. */
    public LiveData<User> getAuthor() {
        if (author.getValue() == null) {
            new UserRepository().getCurrentProfile()
                    .addOnSuccessListener(value -> { if (value != null) author.setValue(value); });
        }
        return author;
    }

    /** Chosen topic, kept across rotation. */
    public LiveData<PostCategory> getTopic() { return topic; }

    public void setTopic(PostCategory value) {
        if (value != null) topic.setValue(value);
    }

    public void setPhoto(String localUri) {
        State current = state.getValue();
        if (current != null && current.submitting) return;
        photo.setValue(localUri == null || localUri.trim().isEmpty() ? null : localUri);
    }
    private final ListingRepository listings = new ListingRepository();
    private String listingId = "";
    private Listing listing;

    public LiveData<State> getState() {
        return state;
    }

    public void initializeShare(String id) {
        if (id == null || id.trim().isEmpty()) return;
        listingId = id.trim();
        state.setValue(new State(false, null, null, null, true, false));
        listings.getListing(listingId).addOnSuccessListener(value -> {
            if (!canShare(value)) {
                state.postValue(new State(false, "This listing cannot be shared.", null, null, false, true));
                return;
            }
            listing = value;
            state.postValue(new State(false, null, null, value, false, false));
        }).addOnFailureListener(e -> state.postValue(new State(false, "Plant listing unavailable.", null, null, false, true)));
    }

    public void submit(String text, PostCategory category) {
        if (!listingId.isEmpty() && !canShare(listing))
            return;
        PostCategory actual = listingId.isEmpty() ? category : PostCategory.SHARING;
        String error = validate(text, actual);
        if (error != null) {
            state.setValue(new State(false, error, null, listing, false, false));
            return;
        }
        State current = state.getValue();
        if (current != null && current.submitting) return;
        state.setValue(new State(true, null, null, listing, false, false));
        String localPhoto = photo.getValue();
        Task<String> imageRef = localPhoto == null
                ? Tasks.forResult("")
                : images.upload(localPhoto);
        imageRef.continueWithTask(uploaded -> {
            if (!uploaded.isSuccessful()) throw uploaded.getException();
            return repository.createPost(text, actual, listingId, uploaded.getResult());
        }).addOnSuccessListener(id -> state.postValue(new State(false, null, id, listing, false, false)))
                .addOnFailureListener(e -> state.postValue(new State(false, getApplication().getString(
                        localPhoto == null ? R.string.post_create_error
                                : R.string.post_photo_upload_error),
                        null, listing, false, false)));
    }

    static boolean canShare(Listing listing) {
        return listing != null && listing.getStatus() == ListingStatus.ACTIVE;
    }

    static String validate(String text, PostCategory category) {
        String v = text == null ? "" : text.trim();
        if (category == null) return "Choose a category.";
        if (v.isEmpty()) return "Write something to share.";
        if (v.length() > 2000) return "Use 2000 characters or fewer.";
        return null;
    }
}
