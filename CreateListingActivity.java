package com.example.ungdungchiasecaycanh.ui.listing;

import android.widget.Toast;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.ui.photo.ImageCropActivity;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.profile.PhoneVerifyActivity;
import com.example.ungdungchiasecaycanh.ui.profile.TrustRow;
import com.example.ungdungchiasecaycanh.ui.profile.VneidDemoActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * New listing form (canvas B "Đăng tin"). The draft lives in {@link CreateListingViewModel}; kind, condition,
 * height, pot, age, pick-up times and (for sales) "open to offers" are optional details that travel with it.
 */
public class CreateListingActivity extends AppCompatActivity {
    public static final String EXTRA_IMAGE_URIS = "create_listing.image_uris";
    public static final String EXTRA_NICKNAME = "create_listing.nickname";
    public static final String EXTRA_SPECIES = "create_listing.species";
    public static final String EXTRA_DESCRIPTION = "create_listing.description";
    public static final String EXTRA_TYPE = "create_listing.type";
    public static final String EXTRA_PRICE = "create_listing.price";
    public static final String EXTRA_LOOKING_FOR = "create_listing.looking_for";
    public static final String EXTRA_LOCATION = "create_listing.location";
    /** Optional {@link ListingType} name the form opens with (Home's "Post for sale" button). */
    public static final String EXTRA_PRESET_TYPE = "create_listing.preset_type";
    /** Garden plant ID: the form opens filled from that plant ("give this plant away"). */
    public static final String EXTRA_PLANT_ID = "create_listing.plant_id";
    private static final String STATE_PENDING_PHOTO = "pending_photo";
    private static final int[] TYPE_CARDS = {R.id.listing_type_give_away, R.id.listing_type_trade, R.id.listing_type_sell};
    private static final ListingType[] TYPES = {ListingType.GIVE_AWAY, ListingType.TRADE, ListingType.SELL};
    private static final int[] TYPE_ICONS = {R.drawable.ic_gift, R.drawable.ic_swap, R.drawable.ic_tag};
    private static final int[] TYPE_TITLES = {R.string.listing_type_give_away, R.string.listing_type_trade, R.string.listing_type_sell};
    private static final int[] TYPE_DESCS = {R.string.listing_type_give_desc, R.string.listing_type_trade_desc, R.string.listing_type_sell_desc};
    private static final int HEIGHT_STEP_CM = 5;
    private static final int POT_STEP_CM = 2;

    private CreateListingViewModel viewModel;
    /** "Selling needs 3 checks" sheet, shown when Sell is published without them. */
    private BottomSheetDialog sellGate;
    private ActivityResultLauncher<String> imagePicker;
    private ActivityResultLauncher<Uri> camera;
    private ActivityResultLauncher<ImageCropActivity.Request> cropper;
    private TextInputLayout nicknameLayout, speciesLayout, descriptionLayout, lookingForLayout, priceLayout, locationLayout;
    private View lookingForGroup, priceGroup;
    private ActivityResultLauncher<Intent> previewLauncher;
    private Uri pendingPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_listing);
        if (savedInstanceState != null && savedInstanceState.getString(STATE_PENDING_PHOTO) != null) {
            pendingPhoto = Uri.parse(savedInstanceState.getString(STATE_PENDING_PHOTO));
        }

        previewLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() != RESULT_OK) return;
                    String listingId = result.getData() == null ? null
                            : result.getData().getStringExtra(ListingPreviewActivity.EXTRA_LISTING_ID);
                    Intent response = new Intent();
                    if (listingId != null && !listingId.trim().isEmpty()) {
                        response.putExtra(ListingPreviewActivity.EXTRA_LISTING_ID, listingId);
                    }
                    viewModel.clearSavedDraft();
                    setResult(RESULT_OK, response);
                    finish();
                });

        viewModel = new ViewModelProvider(this).get(CreateListingViewModel.class);
        nicknameLayout = findViewById(R.id.listing_nickname_layout);
        speciesLayout = findViewById(R.id.listing_species_layout);
        descriptionLayout = findViewById(R.id.listing_description_layout);
        lookingForLayout = findViewById(R.id.listing_looking_for_layout);
        priceLayout = findViewById(R.id.listing_price_layout);
        locationLayout = findViewById(R.id.listing_location_layout);
        lookingForGroup = findViewById(R.id.listing_looking_for_group);
        priceGroup = findViewById(R.id.listing_price_group);

        bindText(findViewById(R.id.listing_nickname_input), viewModel::setNickname, nicknameLayout);
        bindText(findViewById(R.id.listing_species_input), viewModel::setSpecies, speciesLayout);
        bindText(findViewById(R.id.listing_description_input), viewModel::setDescription, descriptionLayout);
        bindText(findViewById(R.id.listing_looking_for_input), viewModel::setLookingFor, lookingForLayout);
        bindText(findViewById(R.id.listing_location_input), viewModel::setLocationText, locationLayout);
        bindText(findViewById(R.id.listing_price_input), value -> viewModel.setPrice(parsePrice(value)), priceLayout);

        setUpPhotos();
        setUpTypeCards(savedInstanceState == null ? getIntent().getStringExtra(EXTRA_PRESET_TYPE) : null);
        setUpDetails();

        findViewById(R.id.action_create_listing_back).setOnClickListener(view -> finish());
        findViewById(R.id.create_listing_save_draft).setOnClickListener(view -> viewModel.saveDraft());
        viewModel.getSaved().observe(this, done -> {
            if (!Boolean.TRUE.equals(done)) return;
            Toast.makeText(this, R.string.create_listing_draft_saved, Toast.LENGTH_SHORT).show();
            finish();
        });
        viewModel.getSeller().observe(this, seller -> {
            CreateListingDraft current = viewModel.getDraft().getValue();
            if (current != null) updateConditionalFields(current.getType());
            if (sellGate != null && sellGate.isShowing()) bindSellGate();
        });
        viewModel.getEmailNotice().observe(this, sent -> {
            if (sent == null) return;
            Toast.makeText(this, sent ? R.string.sell_gate_email_sent : R.string.sell_gate_email_failed,
                    Toast.LENGTH_LONG).show();
            viewModel.clearEmailNotice();
        });
        viewModel.getSaveErrors().observe(this, count -> {
            if (count != null && count > 0) Snackbar.make(findViewById(R.id.create_listing_root),
                    R.string.create_listing_draft_failed, Snackbar.LENGTH_LONG).show();
        });
        viewModel.getRestored().observe(this, restored -> {
            if (!Boolean.TRUE.equals(restored)) return;
            viewModel.consumeRestored();
            fillFromDraft();
            Snackbar.make(findViewById(R.id.create_listing_root), R.string.create_listing_draft_restored,
                    Snackbar.LENGTH_LONG)
                    .setAction(R.string.create_listing_draft_discard, v -> viewModel.discardSavedDraft(() -> {
                        startActivity(new Intent(this, CreateListingActivity.class));
                        finish();
                    }))
                    .show();
        });
        viewModel.getPrefilled().observe(this, filled -> {
            if (!Boolean.TRUE.equals(filled)) return;
            viewModel.consumePrefilled();
            fillFromDraft();
        });
        long plantId = getIntent().getLongExtra(EXTRA_PLANT_ID, -1);
        if (savedInstanceState == null && plantId > 0) viewModel.prefillFromPlant(plantId);
        else if (savedInstanceState == null) viewModel.restoreSavedDraft();
        findViewById(R.id.button_preview_listing).setOnClickListener(view -> openPreview());
        viewModel.getDraft().observe(this, this::render);
        bindMotion();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (pendingPhoto != null) outState.putString(STATE_PENDING_PHOTO, pendingPhoto.toString());
    }

    private void bindMotion() {
        Motion.rise(findViewById(R.id.create_listing_type_title), 60);
        Motion.rise(findViewById(R.id.listing_type_selector), 100);
        Motion.rise(findViewById(R.id.create_listing_photos_title), 160);
        Motion.rise(findViewById(R.id.listing_image_preview), 200);
        Motion.sheet((View) findViewById(R.id.button_preview_listing).getParent(), 560, 300);
    }

    // ----- photos -------------------------------------------------------------------------------

    private void setUpPhotos() {
        ListingImageDraftAdapter adapter = new ListingImageDraftAdapter(
                position -> viewModel.removeImageAt(position), this::takePhoto, () -> {
                    if (viewModel.getSelectedImageCount() < CreateListingDraft.MAX_IMAGES) imagePicker.launch("image/*");
                });
        RecyclerView photos = findViewById(R.id.listing_image_preview);
        photos.setAdapter(adapter);
        viewModel.getDraft().observe(this, draft -> adapter.submitList(draft.getImageUris()));
        cropper = registerForActivityResult(new ImageCropActivity.Crop(), uri -> {
            viewModel.finishCrop(uri == null ? null : uri.toString());
            cropNext();
        });
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
            if (uris == null) return;
            ArrayList<String> values = new ArrayList<>();
            for (Uri uri : uris) values.add(uri.toString());
            viewModel.queueCrops(values);
            cropNext();
        });
        camera = registerForActivityResult(new ActivityResultContracts.TakePicture(), saved -> {
            if (Boolean.TRUE.equals(saved) && pendingPhoto != null) {
                viewModel.queueCrops(Collections.singletonList(pendingPhoto.toString()));
                cropNext();
            }
            pendingPhoto = null;
        });
    }

    /** Camera app writes into app-private storage through the FileProvider, then the crop step runs. */
    private void takePhoto() {
        if (viewModel.getSelectedImageCount() >= CreateListingDraft.MAX_IMAGES) return;
        try {
            File dir = new File(getFilesDir(), "photos");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("No photo folder");
            File file = new File(dir, "listing_" + System.currentTimeMillis() + ".jpg");
            pendingPhoto = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            camera.launch(pendingPhoto);
        } catch (IOException | IllegalArgumentException | ActivityNotFoundException error) {
            pendingPhoto = null;
            Snackbar.make(findViewById(R.id.create_listing_root), R.string.doctor_camera_unavailable, Snackbar.LENGTH_LONG).show();
        }
    }

    /** Opens the crop step for the next picked photo; photos are square like the listing cards. */
    private void cropNext() {
        String next = viewModel.nextCrop();
        if (next != null) cropper.launch(new ImageCropActivity.Request(Uri.parse(next), 1f,
                viewModel.cropStep(), viewModel.cropSteps()));
    }

    // ----- type ---------------------------------------------------------------------------------

    private void setUpTypeCards(String preset) {
        for (int i = 0; i < TYPE_CARDS.length; i++) {
            View card = findViewById(TYPE_CARDS[i]);
            ((ImageView) card.findViewById(R.id.type_card_icon)).setImageResource(TYPE_ICONS[i]);
            ((TextView) card.findViewById(R.id.type_card_title)).setText(TYPE_TITLES[i]);
            ((TextView) card.findViewById(R.id.type_card_desc)).setText(TYPE_DESCS[i]);
            ListingType type = TYPES[i];
            card.setOnClickListener(v -> selectType(type));
        }
        ListingType initial = viewModel.getDraft().getValue() == null ? ListingType.GIVE_AWAY : viewModel.getDraft().getValue().getType();
        if (preset != null) {
            try { initial = ListingType.valueOf(preset); } catch (IllegalArgumentException ignored) { }
        }
        selectType(initial);
    }

    private void selectType(ListingType type) {
        viewModel.setType(type);
        if (type == ListingType.SELL) viewModel.checkSeller();
        for (int i = 0; i < TYPE_CARDS.length; i++) {
            View card = findViewById(TYPE_CARDS[i]);
            boolean on = TYPES[i] == type;
            card.setSelected(on);
            card.setContentDescription(getString(TYPE_TITLES[i]) + ", " + getString(TYPE_DESCS[i]));
        }
        updateConditionalFields(type);
    }

    private void updateConditionalFields(ListingType type) {
        boolean showLookingFor = type == ListingType.TRADE;
        boolean showPrice = type == ListingType.SELL;
        if (showLookingFor && lookingForGroup.getVisibility() != View.VISIBLE) Motion.fromRight(lookingForGroup, 0);
        if (showPrice && priceGroup.getVisibility() != View.VISIBLE) Motion.fromRight(priceGroup, 0);
        lookingForGroup.setVisibility(showLookingFor ? View.VISIBLE : View.GONE);
        priceGroup.setVisibility(showPrice ? View.VISIBLE : View.GONE);
        findViewById(R.id.listing_sell_verify_note).setVisibility(showPrice && !viewModel.canSell() ? View.VISIBLE : View.GONE);
        lookingForLayout.setError(null);
        priceLayout.setError(null);
    }

    // ----- details ------------------------------------------------------------------------------

    private void setUpDetails() {
        ListingDetails current = viewModel.getDraft().getValue() == null ? ListingDetails.NONE
                : viewModel.getDraft().getValue().getDetails();
        ChipGroup conditions = findViewById(R.id.create_listing_condition_group);
        for (ListingDetails.Condition c : ListingDetails.Condition.values()) {
            if (c == ListingDetails.Condition.WITH_POT) continue; // its own switch below
            addChoice(conditions, getString(ListingDetailLabels.of(c)), current.getCondition().contains(c),
                    on -> viewModel.setCondition(c, on)).setTag(c);
        }
        ChipGroup pickups = findViewById(R.id.create_listing_pickup_group);
        for (ListingDetails.PickupTime time : ListingDetails.PickupTime.values()) {
            addChoice(pickups, getString(ListingDetailLabels.of(time)), current.getPickupTimes().contains(time),
                    on -> viewModel.setPickupTime(time, on)).setTag(time);
        }
        ChipGroup ages = findViewById(R.id.create_listing_age_group);
        for (ListingDetails.Age age : ListingDetails.Age.values()) {
            addChoice(ages, getString(ListingDetailLabels.of(age)), age == current.getAge(), on -> {
                if (on) viewModel.setAge(age);
                else if (ages.getCheckedChipId() == View.NO_ID) viewModel.setAge(null);
            }).setTag(age);
        }

        bindStepper(R.id.create_listing_height_stepper, HEIGHT_STEP_CM, ListingDetails.MAX_HEIGHT_CM,
                () -> draftDetails().getHeightCm(), viewModel::setHeightCm, R.string.create_listing_height_label);
        bindStepper(R.id.create_listing_pot_stepper, POT_STEP_CM, ListingDetails.MAX_POT_CM,
                () -> draftDetails().getPotCm(), viewModel::setPotCm, R.string.create_listing_pot_label);

        MaterialSwitch withPot = findViewById(R.id.create_listing_with_pot);
        withPot.setChecked(current.getCondition().contains(ListingDetails.Condition.WITH_POT));
        withPot.setOnCheckedChangeListener((b, on) -> viewModel.setCondition(ListingDetails.Condition.WITH_POT, on));
        MaterialSwitch negotiable = findViewById(R.id.listing_negotiable);
        CreateListingDraft draft = viewModel.getDraft().getValue();
        negotiable.setChecked(draft != null && draft.isNegotiableChosen());
        negotiable.setOnCheckedChangeListener((b, on) -> viewModel.setNegotiable(on));

        android.widget.AutoCompleteTextView kind = findViewById(R.id.create_listing_category);
        ListingDetails.Category[] kinds = ListingDetails.Category.values();
        List<String> names = new ArrayList<>();
        for (ListingDetails.Category c : kinds) names.add(getString(ListingDetailLabels.of(c)));
        kind.setAdapter(new android.widget.ArrayAdapter<>(this, R.layout.item_dropdown_option, names));
        if (current.getCategory() != null) kind.setText(getString(ListingDetailLabels.of(current.getCategory())), false);
        kind.setOnItemClickListener((parent, view, position, id) -> viewModel.setCategory(kinds[position]));
    }

    private ListingDetails draftDetails() {
        CreateListingDraft d = viewModel.getDraft().getValue();
        return d == null ? ListingDetails.NONE : d.getDetails();
    }

    /** Minus / value / plus in {@code step} centimetres, from "not set" (0) up to {@code max}. */
    private void bindStepper(int id, int step, int max, java.util.function.IntSupplier value,
                             java.util.function.IntConsumer set, int label) {
        View stepper = findViewById(id);
        stepper.findViewById(R.id.stepper_minus).setOnClickListener(v -> set.accept(Math.max(0, value.getAsInt() - step)));
        stepper.findViewById(R.id.stepper_plus).setOnClickListener(v -> set.accept(Math.min(max, value.getAsInt() + step)));
        stepper.findViewById(R.id.stepper_minus).setContentDescription(getString(label) + ", " + getString(R.string.stepper_less));
        stepper.findViewById(R.id.stepper_plus).setContentDescription(getString(label) + ", " + getString(R.string.stepper_more));
    }

    private void showStepper(int id, int cm) {
        View stepper = findViewById(id);
        ((TextView) stepper.findViewById(R.id.stepper_value)).setText(cm == 0
                ? getString(R.string.create_listing_not_set) : getString(R.string.create_listing_cm, cm));
        stepper.findViewById(R.id.stepper_minus).setEnabled(cm > 0);
        stepper.findViewById(R.id.stepper_minus).setAlpha(cm > 0 ? 1f : 0.38f);
    }

    /** Puts a restored draft into the fields; the text watchers feed the same values back. */
    private void fillFromDraft() {
        CreateListingDraft d = viewModel.getDraft().getValue();
        if (d == null) return;
        ((TextView) findViewById(R.id.listing_nickname_input)).setText(d.getNickname());
        ((TextView) findViewById(R.id.listing_species_input)).setText(d.getSpecies());
        ((TextView) findViewById(R.id.listing_description_input)).setText(d.getDescription());
        ((TextView) findViewById(R.id.listing_looking_for_input)).setText(d.getLookingFor());
        ((TextView) findViewById(R.id.listing_location_input)).setText(d.getLocationText());
        ((TextView) findViewById(R.id.listing_price_input)).setText(d.getPrice() > 0 ? String.valueOf(d.getPrice()) : "");
        selectType(d.getType());
        ListingDetails details = d.getDetails();
        checkTagged(findViewById(R.id.create_listing_condition_group), details.getCondition());
        checkTagged(findViewById(R.id.create_listing_pickup_group), details.getPickupTimes());
        checkTagged(findViewById(R.id.create_listing_age_group),
                details.getAge() == null ? Collections.emptyList() : Collections.singletonList(details.getAge()));
        ((MaterialSwitch) findViewById(R.id.create_listing_with_pot)).setChecked(
                details.getCondition().contains(ListingDetails.Condition.WITH_POT));
        ((MaterialSwitch) findViewById(R.id.listing_negotiable)).setChecked(d.isNegotiableChosen());
        ((android.widget.AutoCompleteTextView) findViewById(R.id.create_listing_category)).setText(
                details.getCategory() == null ? "" : getString(ListingDetailLabels.of(details.getCategory())), false);
    }

    /** Checks the chips whose tag is in {@code values}. */
    private static void checkTagged(ChipGroup group, List<?> values) {
        for (int i = 0; i < group.getChildCount(); i++) {
            Chip chip = (Chip) group.getChildAt(i);
            chip.setChecked(values.contains(chip.getTag()));
        }
    }

    private Chip addChoice(ChipGroup group, String label, boolean checked, Consumer<Boolean> onChange) {
        Chip chip = (Chip) LayoutInflater.from(this).inflate(R.layout.item_choice_chip, group, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setChecked(checked);
        chip.setOnCheckedChangeListener((button, isChecked) -> onChange.accept(isChecked));
        group.addView(chip);
        return chip;
    }

    // ----- state --------------------------------------------------------------------------------

    private void render(CreateListingDraft draft) {
        ((TextView) findViewById(R.id.listing_photo_count)).setText(
                getString(R.string.listing_photo_count, draft.getImageUris().size(), CreateListingDraft.MAX_IMAGES));
        ListingDetails details = draft.getDetails();
        showStepper(R.id.create_listing_height_stepper, details.getHeightCm());
        showStepper(R.id.create_listing_pot_stepper, details.getPotCm());
    }

    private void bindText(TextInputEditText input, Consumer<String> consumer, TextInputLayout layout) {
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                consumer.accept(s == null ? "" : s.toString());
                layout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private long parsePrice(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private void openPreview() {
        CreateListingDraft.ValidationResult result = viewModel.validateDraft();
        clearErrors();
        if (!result.isValid()) {
            showErrors(result);
            return;
        }
        CreateListingDraft draft = viewModel.getDraft().getValue();
        if (draft == null) return;
        if (draft.getType() == ListingType.SELL && !viewModel.canSell()) {
            showSellGate();
            viewModel.checkSeller();
            return;
        }
        Intent intent = new Intent(this, ListingPreviewActivity.class)
                .putStringArrayListExtra(EXTRA_IMAGE_URIS, new ArrayList<>(draft.getImageUris()))
                .putExtra(EXTRA_NICKNAME, draft.getNickname())
                .putExtra(EXTRA_SPECIES, draft.getSpecies())
                .putExtra(EXTRA_DESCRIPTION, draft.getDescription())
                .putExtra(EXTRA_TYPE, draft.getType().name())
                .putExtra(EXTRA_PRICE, draft.getPrice())
                .putExtra(EXTRA_LOOKING_FOR, draft.getLookingFor())
                .putExtra(EXTRA_LOCATION, draft.getLocationText());
        previewLauncher.launch(ListingDetailsExtras.put(intent, draft.getDetails()));
    }

    private void clearErrors() {
        nicknameLayout.setError(null);
        speciesLayout.setError(null);
        descriptionLayout.setError(null);
        lookingForLayout.setError(null);
        priceLayout.setError(null);
        locationLayout.setError(null);
    }

    private void showErrors(CreateListingDraft.ValidationResult result) {
        View first = null;
        if (result.nickname != CreateListingDraft.Error.NONE) { nicknameLayout.setError(errorText(result.nickname)); first = nicknameLayout; }
        if (result.species != CreateListingDraft.Error.NONE) { speciesLayout.setError(errorText(result.species)); if (first == null) first = speciesLayout; }
        if (result.lookingFor != CreateListingDraft.Error.NONE) { lookingForLayout.setError(errorText(result.lookingFor)); if (first == null) first = lookingForLayout; }
        if (result.price != CreateListingDraft.Error.NONE) { priceLayout.setError(getString(R.string.listing_price_required)); if (first == null) first = priceLayout; }
        if (result.description != CreateListingDraft.Error.NONE) { descriptionLayout.setError(errorText(result.description)); if (first == null) first = descriptionLayout; }
        if (result.location != CreateListingDraft.Error.NONE) { locationLayout.setError(errorText(result.location)); if (first == null) first = locationLayout; }
        if (first != null) {
            View target = first;
            View scroll = findViewById(R.id.create_listing_scroll);
            scroll.post(() -> scroll.scrollTo(0, Math.max(0, target.getTop() - 120)));
        }
    }

    private String errorText(CreateListingDraft.Error error) {
        switch (error) {
            case NICKNAME_REQUIRED: return getString(R.string.listing_nickname_required);
            case NICKNAME_TOO_LONG: return getString(R.string.listing_nickname_too_long);
            case SPECIES_TOO_LONG: return getString(R.string.listing_species_too_long);
            case DESCRIPTION_REQUIRED: return getString(R.string.listing_description_required);
            case DESCRIPTION_TOO_LONG: return getString(R.string.listing_description_too_long);
            case LOCATION_REQUIRED: return getString(R.string.listing_location_required);
            case LOCATION_TOO_LONG: return getString(R.string.listing_location_too_long);
            case LOOKING_FOR_REQUIRED: return getString(R.string.listing_looking_for_required);
            case LOOKING_FOR_TOO_LONG: return getString(R.string.listing_looking_for_too_long);
            default: return getString(R.string.listing_invalid_value);
        }
    }

    // ----- sell checks ---------------------------------------------------------------------------

    @Override protected void onResume() {
        super.onResume();
        // Coming back from the phone or VNeID screen, or from the email link: check again.
        CreateListingDraft draft = viewModel.getDraft().getValue();
        if (draft != null && draft.getType() == ListingType.SELL) viewModel.checkSeller();
    }

    @Override protected void onDestroy() {
        if (sellGate != null) sellGate.dismiss();
        super.onDestroy();
    }

    private void showSellGate() {
        if (sellGate == null) {
            sellGate = new BottomSheetDialog(this);
            sellGate.setContentView(R.layout.bottom_sheet_sell_gate);
        }
        bindSellGate();
        sellGate.show();
    }

    private void bindSellGate() {
        User seller = viewModel.getSeller().getValue();
        TrustRow.bind(sellGate.findViewById(R.id.sell_gate_email), R.string.trust_email, 0,
                seller != null && seller.isEmailVerified(), v -> viewModel.sendEmailVerification());
        TrustRow.bind(sellGate.findViewById(R.id.sell_gate_phone), R.string.trust_phone, 0,
                seller != null && seller.isPhoneVerified(), v -> startActivity(new Intent(this, PhoneVerifyActivity.class)));
        TrustRow.bind(sellGate.findViewById(R.id.sell_gate_vneid), R.string.trust_vneid, 0,
                seller != null && seller.isVneidVerified(), v -> startActivity(new Intent(this, VneidDemoActivity.class)));
        MaterialButton go = sellGate.findViewById(R.id.sell_gate_continue);
        go.setEnabled(viewModel.canSell());
        go.setOnClickListener(v -> {
            sellGate.dismiss();
            openPreview();
        });
    }
}
