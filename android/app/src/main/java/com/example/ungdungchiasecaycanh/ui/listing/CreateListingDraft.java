package com.example.ungdungchiasecaycanh.ui.listing;

import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Local-only editable state. Its image URIs must never be sent to Firestore. */
/** Serializable so the unsaved form survives process death (CreateListingViewModel's SavedStateHandle). */
public final class CreateListingDraft implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int MAX_IMAGES = 5;

    /** The seven parts of the form the progress bar counts (type always has a value). */
    public enum Step { PHOTOS, NAME, CONDITION, HEIGHT, LIGHT, DESCRIPTION, PICKUP }

    public enum Error {
        NONE, IMAGES_TOO_MANY, NICKNAME_REQUIRED, NICKNAME_TOO_LONG,
        SPECIES_TOO_LONG, DESCRIPTION_REQUIRED, DESCRIPTION_TOO_LONG,
        LOCATION_REQUIRED, LOCATION_TOO_LONG, LOOKING_FOR_REQUIRED,
        LOOKING_FOR_TOO_LONG, SELL_PRICE_REQUIRED
    }

    public static final class ValidationResult {
        public final Error images;
        public final Error nickname;
        public final Error species;
        public final Error description;
        public final Error lookingFor;
        public final Error price;
        public final Error location;

        private ValidationResult(Error images, Error nickname, Error species,
                                 Error description, Error lookingFor, Error price,
                                 Error location) {
            this.images = images;
            this.nickname = nickname;
            this.species = species;
            this.description = description;
            this.lookingFor = lookingFor;
            this.price = price;
            this.location = location;
        }

        public boolean isValid() {
            return images == Error.NONE && nickname == Error.NONE
                    && species == Error.NONE && description == Error.NONE
                    && lookingFor == Error.NONE && price == Error.NONE
                    && location == Error.NONE;
        }
    }

    private final List<String> imageUris = new ArrayList<>();
    private String nickname = "";
    private String species = "";
    private String description = "";
    private ListingType type = ListingType.GIVE_AWAY;
    private long price;
    private String lookingFor = "";
    private String locationText = "";
    private final List<ListingDetails.Condition> condition = new ArrayList<>();
    private int heightCm;
    private LightLevel light;
    private final List<ListingDetails.PickupTime> pickupTimes = new ArrayList<>();
    private ListingDetails.Category category;
    private int potCm;
    private ListingDetails.Age age;
    private boolean negotiable;

    public boolean addImageUri(String uri) {
        if (uri == null || uri.trim().isEmpty() || imageUris.size() >= MAX_IMAGES) {
            return false;
        }
        imageUris.add(uri);
        return true;
    }

    public boolean removeImageAt(int index) {
        if (index < 0 || index >= imageUris.size()) {
            return false;
        }
        imageUris.remove(index);
        return true;
    }

    public List<String> getImageUris() {
        return Collections.unmodifiableList(imageUris);
    }

    public boolean hasValidImageCount() {
        return imageUris.size() <= MAX_IMAGES;
    }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = normalize(nickname); }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = normalize(species); }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = normalize(description); }
    public ListingType getType() { return type; }
    public long getPrice() { return price; }
    public String getLookingFor() { return lookingFor; }
    public String getLocationText() { return locationText; }

    public void setPrice(long price) { this.price = price; }
    public void setLookingFor(String lookingFor) {
        this.lookingFor = normalize(lookingFor);
        if (type != ListingType.TRADE) {
            this.lookingFor = "";
        }
    }

    public void setTransactionDetails(ListingType type, long price, String lookingFor) {
        this.type = type == null ? ListingType.GIVE_AWAY : type;
        this.price = this.type == ListingType.SELL ? price : 0;
        this.lookingFor = this.type == ListingType.TRADE ? normalize(lookingFor) : "";
    }

    public void setLocationText(String locationText) {
        this.locationText = normalize(locationText);
    }

    public void setCondition(ListingDetails.Condition value, boolean on) {
        condition.remove(value);
        if (on) condition.add(value);
    }

    public void setHeightCm(int heightCm) {
        this.heightCm = Math.max(0, Math.min(ListingDetails.MAX_HEIGHT_CM, heightCm));
    }

    public void setLight(LightLevel light) { this.light = light; }

    public void setPickupTime(ListingDetails.PickupTime value, boolean on) {
        pickupTimes.remove(value);
        if (on) pickupTimes.add(value);
    }

    public void setCategory(ListingDetails.Category category) { this.category = category; }

    public void setPotCm(int potCm) {
        this.potCm = Math.max(0, Math.min(ListingDetails.MAX_POT_CM, potCm));
    }

    public void setAge(ListingDetails.Age age) { this.age = age; }

    /** Only a sale can be negotiable; other types publish false. */
    public void setNegotiable(boolean negotiable) { this.negotiable = negotiable; }

    /** The switch as the member left it, whatever the type (drafts keep it). */
    public boolean isNegotiableChosen() { return negotiable; }

    public ListingDetails getDetails() {
        return new ListingDetails(condition, heightCm, light, pickupTimes, category, potCm, age,
                negotiable && getType() == ListingType.SELL);
    }

    /** Parts still empty, in form order. */
    public List<Step> missingSteps() {
        List<Step> missing = new ArrayList<>();
        if (imageUris.isEmpty()) missing.add(Step.PHOTOS);
        if (nickname.isEmpty()) missing.add(Step.NAME);
        if (condition.isEmpty()) missing.add(Step.CONDITION);
        if (heightCm == 0) missing.add(Step.HEIGHT);
        if (light == null) missing.add(Step.LIGHT);
        if (description.isEmpty()) missing.add(Step.DESCRIPTION);
        if (pickupTimes.isEmpty()) missing.add(Step.PICKUP);
        return missing;
    }

    public ValidationResult validate() {
        Error imageError = hasValidImageCount() ? Error.NONE : Error.IMAGES_TOO_MANY;
        Error nicknameError = nickname.isEmpty() ? Error.NICKNAME_REQUIRED
                : nickname.length() > 60 ? Error.NICKNAME_TOO_LONG : Error.NONE;
        Error speciesError = species.length() > 100 ? Error.SPECIES_TOO_LONG : Error.NONE;
        Error descriptionError = description.isEmpty() ? Error.DESCRIPTION_REQUIRED
                : description.length() > 1000 ? Error.DESCRIPTION_TOO_LONG : Error.NONE;
        Error locationError = locationText.isEmpty() ? Error.LOCATION_REQUIRED
                : locationText.length() > 100 ? Error.LOCATION_TOO_LONG : Error.NONE;
        Error lookingForError = Error.NONE;
        Error priceError = Error.NONE;
        if (type == ListingType.TRADE) {
            lookingForError = lookingFor.isEmpty() ? Error.LOOKING_FOR_REQUIRED
                    : lookingFor.length() > 200 ? Error.LOOKING_FOR_TOO_LONG : Error.NONE;
        } else if (type == ListingType.SELL && price <= 0) {
            priceError = Error.SELL_PRICE_REQUIRED;
        }
        return new ValidationResult(imageError, nicknameError, speciesError,
                descriptionError, lookingForError, priceError, locationError);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
