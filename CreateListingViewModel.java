package com.example.ungdungchiasecaycanh.ui.listing;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.ArrayList;
import java.util.List;

import com.example.ungdungchiasecaycanh.data.local.entity.ListingDraftEntity;
import com.example.ungdungchiasecaycanh.data.repository.AuthRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingDraftRepository;
import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

public class CreateListingViewModel extends AndroidViewModel {
    private static final String STATE_DRAFT = "draft";
    /** The unsaved form; kept in the saved state so Android restoring the task after process death keeps it. */
    private final CreateListingDraft draft;
    private final MutableLiveData<CreateListingDraft> draftState;
    private final ListingDraftRepository drafts;
    /** Saved draft put back into the form (once), and "draft saved". */
    private final MutableLiveData<Boolean> restored = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> saveErrors = new MutableLiveData<>(0);
    private boolean draftChecked;

    public CreateListingViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);
        CreateListingDraft kept = state.get(STATE_DRAFT);
        draft = kept != null ? kept : new CreateListingDraft();
        state.set(STATE_DRAFT, draft); // the same object: its later edits are what gets saved
        draftState = new MutableLiveData<>(draft);
        drafts = new ListingDraftRepository(application);
        garden = new GardenRepository(application);
    }

    /** The form was filled from a Garden plant (once). */
    private final MutableLiveData<Boolean> prefilled = new MutableLiveData<>(false);
    private final GardenRepository garden;

    public LiveData<Boolean> getRestored() { return restored; }
    public LiveData<Boolean> getPrefilled() { return prefilled; }
    public void consumePrefilled() { prefilled.setValue(false); }

    /**
     * "Give this plant away" from the Garden: name, species, light and photo come from the member's
     * own plant (account-scoped Room); the saved draft is left alone.
     */
    public void prefillFromPlant(long plantId) {
        if (draftChecked) return;
        draftChecked = true;
        garden.getPlant(plantId, new GardenRepository.Callback<PlantEntity>() {
            @Override public void onSuccess(PlantEntity plant) {
                if (plant == null) return;
                fromPlant(plant, draft);
                draftState.setValue(draft);
                prefilled.setValue(true);
            }
            @Override public void onError(Exception error) { }
        });
    }

    static void fromPlant(PlantEntity plant, CreateListingDraft d) {
        d.setTransactionDetails(ListingType.GIVE_AWAY, 0, "");
        d.setNickname(plant.getNickname());
        d.setSpecies(plant.getSpecies());
        d.setLight(LightLevel.fromName(plant.getLight()));
        if (!plant.getImageUri().trim().isEmpty() && d.getImageUris().isEmpty()) d.addImageUri(plant.getImageUri());
    }
    public LiveData<Boolean> getSaved() { return saved; }
    public LiveData<Integer> getSaveErrors() { return saveErrors; }
    public void consumeRestored() { restored.setValue(false); }

    /** Puts the account's saved draft back into an empty form, once per form. */
    public void restoreSavedDraft() {
        if (draftChecked) return;
        draftChecked = true;
        drafts.load(new GardenRepository.Callback<ListingDraftEntity>() {
            @Override public void onSuccess(ListingDraftEntity value) {
                if (value == null || !draft.getNickname().isEmpty() || !draft.getImageUris().isEmpty()) return;
                apply(value, draft);
                draftState.setValue(draft);
                restored.setValue(true);
            }
            @Override public void onError(Exception error) { }
        });
    }

    public void saveDraft() {
        String uid = drafts.currentUid();
        if (uid == null) { failSave(); return; }
        drafts.save(toEntity(uid, draft, System.currentTimeMillis()), new GardenRepository.Callback<Void>() {
            @Override public void onSuccess(Void value) { saved.setValue(true); }
            @Override public void onError(Exception error) { failSave(); }
        });
    }

    /** After publishing, the draft is done. */
    public void clearSavedDraft() { drafts.clear(); }

    /** "Discard draft": deletes it, then {@code done} starts a fresh form. */
    public void discardSavedDraft(Runnable done) { drafts.clear(done); }

    private void failSave() {
        Integer n = saveErrors.getValue();
        saveErrors.setValue(n == null ? 1 : n + 1);
    }

    static ListingDraftEntity toEntity(String uid, CreateListingDraft d, long now) {
        ListingDetails details = d.getDetails();
        return new ListingDraftEntity(uid, d.getType().name(), d.getNickname(), d.getSpecies(), d.getLookingFor(),
                d.getPrice(), d.getDescription(), d.getLocationText(), String.join("\n", d.getImageUris()),
                String.join(",", ListingDetails.names(details.getCondition())), details.getHeightCm(),
                details.getLight() == null ? "" : details.getLight().name(),
                String.join(",", ListingDetails.names(details.getPickupTimes())), now,
                details.getCategory() == null ? "" : details.getCategory().name(), details.getPotCm(),
                details.getAge() == null ? "" : details.getAge().name(), d.isNegotiableChosen());
    }

    /** Fills an empty draft from a saved one; unknown stored values are skipped. */
    static void apply(ListingDraftEntity e, CreateListingDraft d) {
        ListingType type;
        try { type = ListingType.valueOf(e.getType()); } catch (IllegalArgumentException ignored) { type = ListingType.GIVE_AWAY; }
        d.setTransactionDetails(type, e.getPrice(), e.getLookingFor());
        d.setNickname(e.getNickname());
        d.setSpecies(e.getSpecies());
        d.setDescription(e.getDescription());
        d.setLocationText(e.getLocationText());
        for (String uri : e.getImageUris().split("\n")) if (!uri.trim().isEmpty()) d.addImageUri(uri.trim());
        for (ListingDetails.Condition c : ListingDetails.parse(ListingDetails.Condition.class, split(e.getConditions()))) d.setCondition(c, true);
        for (ListingDetails.PickupTime t : ListingDetails.parse(ListingDetails.PickupTime.class, split(e.getPickupTimes()))) d.setPickupTime(t, true);
        d.setHeightCm(e.getHeightCm());
        d.setLight(LightLevel.fromName(e.getLight()));
        d.setCategory(ListingDetails.parseOne(ListingDetails.Category.class, e.getCategory()));
        d.setPotCm(e.getPotCm());
        d.setAge(ListingDetails.parseOne(ListingDetails.Age.class, e.getAge()));
        d.setNegotiable(e.isNegotiable());
    }

    private static List<String> split(String csv) {
        List<String> out = new ArrayList<>();
        for (String part : csv.split(",")) if (!part.trim().isEmpty()) out.add(part.trim());
        return out;
    }

    public LiveData<CreateListingDraft> getDraft() {
        return draftState;
    }

    public boolean addImageUri(String uri) {
        boolean added = draft.addImageUri(uri);
        if (added) {
            draftState.setValue(draft);
        }
        return added;
    }

    public int addImageUris(List<String> uris) {
        int added = 0;
        if (uris != null) {
            for (String uri : uris) {
                if (addImageUri(uri)) {
                    added++;
                }
            }
        }
        return added;
    }

    public boolean removeImageAt(int index) {
        boolean removed = draft.removeImageAt(index);
        if (removed) {
            draftState.setValue(draft);
        }
        return removed;
    }

    private final AuthRepository auth = new AuthRepository();

    private final UserRepository users = new UserRepository();
    private final MutableLiveData<User> seller = new MutableLiveData<>();
    private final MutableLiveData<Boolean> emailNotice = new MutableLiveData<>();

    /** The member's own profile, for the three sell checks (email, phone, VNeID). */
    public LiveData<User> getSeller() { return seller; }

    /** True once a confirmation email was sent, false when sending failed; null when nothing to say. */
    public LiveData<Boolean> getEmailNotice() { return emailNotice; }

    public void checkSeller() {
        users.refreshCurrentProfile().addOnSuccessListener(user -> {
            if (user != null) seller.postValue(user);
        });
    }

    /** Selling needs all three checks; the rules enforce the same when the listing is published. */
    public boolean canSell() {
        User user = seller.getValue();
        return user != null && user.canSell() && auth.isEmailVerified();
    }

    public void sendEmailVerification() {
        auth.sendEmailVerification()
                .addOnSuccessListener(ignored -> emailNotice.postValue(true))
                .addOnFailureListener(error -> emailNotice.postValue(false));
    }

    public void clearEmailNotice() { emailNotice.setValue(null); }

    /** Picked photos still waiting for the crop step (kept across rotation). */
    private final List<String> pendingCrops = new ArrayList<>();
    private int cropBatch;

    /** Queues picked photos for cropping, only as many as there are free photo slots. */
    public void queueCrops(List<String> uris) {
        if (pendingCrops.isEmpty()) cropBatch = 0;
        int free = CreateListingDraft.MAX_IMAGES - getSelectedImageCount() - pendingCrops.size();
        for (String uri : uris) {
            if (free-- <= 0) break;
            pendingCrops.add(uri);
            cropBatch++;
        }
    }

    @Nullable
    public String nextCrop() {
        return pendingCrops.isEmpty() ? null : pendingCrops.get(0);
    }

    public int cropStep() {
        return cropBatch - pendingCrops.size() + 1;
    }

    public int cropSteps() {
        return cropBatch;
    }

    /** The editor closed: keep the cropped photo (null when the member skipped it) and move on. */
    public void finishCrop(@Nullable String croppedUri) {
        if (!pendingCrops.isEmpty()) pendingCrops.remove(0);
        if (croppedUri != null) addImageUri(croppedUri);
    }

    public int getSelectedImageCount() {
        return draft.getImageUris().size();
    }

    public boolean hasValidImageCount() {
        return draft.hasValidImageCount();
    }

    public void setNickname(String value) {
        draft.setNickname(value);
        draftState.setValue(draft);
    }

    public void setSpecies(String value) {
        draft.setSpecies(value);
        draftState.setValue(draft);
    }

    public void setDescription(String value) {
        draft.setDescription(value);
        draftState.setValue(draft);
    }

    public void setLocationText(String value) {
        draft.setLocationText(value);
        draftState.setValue(draft);
    }

    public void setType(ListingType type) {
        draft.setTransactionDetails(type, draft.getPrice(), draft.getLookingFor());
        draftState.setValue(draft);
    }

    public void setPrice(long price) {
        draft.setPrice(price);
        draftState.setValue(draft);
    }

    public void setLookingFor(String value) {
        draft.setLookingFor(value);
        draftState.setValue(draft);
    }

    public void setCondition(ListingDetails.Condition value, boolean on) {
        draft.setCondition(value, on);
        draftState.setValue(draft);
    }

    public void setHeightCm(int heightCm) {
        draft.setHeightCm(heightCm);
        draftState.setValue(draft);
    }

    public void setLight(LightLevel light) {
        draft.setLight(light);
        draftState.setValue(draft);
    }

    public void setCategory(ListingDetails.Category category) {
        draft.setCategory(category);
        draftState.setValue(draft);
    }

    public void setPotCm(int potCm) {
        draft.setPotCm(potCm);
        draftState.setValue(draft);
    }

    public void setAge(ListingDetails.Age age) {
        draft.setAge(age);
        draftState.setValue(draft);
    }

    public void setNegotiable(boolean negotiable) {
        draft.setNegotiable(negotiable);
        draftState.setValue(draft);
    }

    public void setPickupTime(ListingDetails.PickupTime value, boolean on) {
        draft.setPickupTime(value, on);
        draftState.setValue(draft);
    }

    public CreateListingDraft.ValidationResult validateDraft() {
        return draft.validate();
    }
}
