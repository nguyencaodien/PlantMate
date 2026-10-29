package com.example.ungdungchiasecaycanh.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailActivity;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;
import java.util.Map;

/** Admin: edit any field of a listing, change its status, or delete it. */
public class AdminListingEditActivity extends AppCompatActivity {

    public static final String EXTRA_LISTING_ID = "listingId";

    private AdminListingEditViewModel viewModel;
    private String listingId;
    private boolean formBound;
    private int shownSaved, shownDeleted, shownError;

    private TextInputLayout nicknameLayout, speciesLayout, descriptionLayout, priceLayout, lookingLayout, locationLayout, noteLayout, imagesLayout;
    private TextInputEditText nickname, species, description, price, looking, location, note, images;
    private ChipGroup typeGroup, statusGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_listing_edit);
        SecondaryChrome.apply(this, findViewById(R.id.adminListing), findViewById(R.id.adminListingHero));
        listingId = getIntent().getStringExtra(EXTRA_LISTING_ID);
        ((TextView) findViewById(R.id.adminListingTitle)).setText(R.string.admin_edit_listing);

        nicknameLayout = findViewById(R.id.adminListingNicknameLayout);
        speciesLayout = findViewById(R.id.adminListingSpeciesLayout);
        descriptionLayout = findViewById(R.id.adminListingDescriptionLayout);
        priceLayout = findViewById(R.id.adminListingPriceLayout);
        lookingLayout = findViewById(R.id.adminListingLookingLayout);
        locationLayout = findViewById(R.id.adminListingLocationLayout);
        noteLayout = findViewById(R.id.adminListingNoteLayout);
        nickname = findViewById(R.id.adminListingNickname);
        species = findViewById(R.id.adminListingSpecies);
        description = findViewById(R.id.adminListingDescription);
        price = findViewById(R.id.adminListingPrice);
        looking = findViewById(R.id.adminListingLooking);
        location = findViewById(R.id.adminListingLocation);
        note = findViewById(R.id.adminListingNote);
        typeGroup = findViewById(R.id.adminListingType);
        statusGroup = findViewById(R.id.adminListingStatus);
        typeGroup.setOnCheckedStateChangeListener((g, ids) -> showTypeFields());
        imagesLayout = findViewById(R.id.adminListingImagesLayout);
        images = findViewById(R.id.adminListingImages);
        images.setOnFocusChangeListener((v, hasFocus) -> { if (!hasFocus) refreshPreview(); });

        MaterialButton save = findViewById(R.id.adminListingSave);
        MaterialButton delete = findViewById(R.id.adminListingDelete);
        findViewById(R.id.adminListingBack).setOnClickListener(v -> finish());
        findViewById(R.id.adminListingRetry).setOnClickListener(v -> viewModel.load(listingId));
        findViewById(R.id.adminListingView).setOnClickListener(v -> startActivity(
                new Intent(this, ListingDetailActivity.class).putExtra(ListingDetailActivity.EXTRA_LISTING_ID, listingId)));
        save.setOnClickListener(v -> save());
        delete.setOnClickListener(v -> AdminDialogs.confirm(this, R.string.admin_delete_confirm_title,
                R.string.admin_delete_listing_body, R.string.admin_delete, () -> {
                    String n = AdminDialogs.note(this, noteLayout, note);
                    if (n != null) viewModel.delete(n);
                }));

        View scroll = findViewById(R.id.adminListingScroll);
        View loading = findViewById(R.id.adminListingLoading);
        View error = findViewById(R.id.adminListingError);
        viewModel = new ViewModelProvider(this).get(AdminListingEditViewModel.class);
        viewModel.getState().observe(this, state -> {
            boolean content = state.status == AdminEditState.Status.CONTENT;
            loading.setVisibility(state.status == AdminEditState.Status.LOADING ? View.VISIBLE : View.GONE);
            error.setVisibility(state.status == AdminEditState.Status.ERROR
                    || state.status == AdminEditState.Status.NOT_FOUND ? View.VISIBLE : View.GONE);
            scroll.setVisibility(content ? View.VISIBLE : View.GONE);
            save.setEnabled(!state.busy);
            delete.setEnabled(!state.busy);
            if (content && state.data != null) bind(state.data);
            if (state.savedVersion > shownSaved) {
                shownSaved = state.savedVersion;
                note.setText("");
                AdminDialogs.snack(scroll, R.string.admin_saved);
            }
            if (state.deletedVersion > shownDeleted) {
                shownDeleted = state.deletedVersion;
                Toast.makeText(this, R.string.admin_deleted, Toast.LENGTH_SHORT).show();
                finish();
            }
            if (state.errorVersion > shownError) {
                shownError = state.errorVersion;
                AdminDialogs.snack(scroll, R.string.admin_save_error);
            }
        });
        if (savedInstanceState == null || !formBound) viewModel.load(listingId);
    }

    private void bind(AdminListingEditViewModel.Data data) {
        Listing l = data.listing;
        ((TextView) findViewById(R.id.adminListingOwner)).setText(getString(R.string.admin_owner,
                data.ownerName.isEmpty() ? getString(R.string.detail_member_fallback) : data.ownerName));
        if (formBound) return;   // keep what the admin is typing
        formBound = true;
        nickname.setText(l.getNickname());
        species.setText(l.getSpecies());
        description.setText(l.getDescription());
        price.setText(l.getPrice() > 0 ? String.valueOf(l.getPrice()) : "");
        looking.setText(l.getLookingFor());
        location.setText(l.getLocationText());
        images.setText(TextUtils.join("\n", l.getImageUrls()));
        refreshPreview();
        typeGroup.check(l.getType() == ListingType.TRADE ? R.id.adminTypeTrade
                : l.getType() == ListingType.SELL ? R.id.adminTypeSell : R.id.adminTypeGive);
        ListingStatus s = l.getStatus();
        statusGroup.check(s == ListingStatus.RESERVED ? R.id.adminStatusReserved
                : s == ListingStatus.REHOMED ? R.id.adminStatusRehomed
                : s == ListingStatus.UNAVAILABLE ? R.id.adminStatusUnavailable : R.id.adminStatusActive);
        showTypeFields();
    }

    /** Shows the cover (first valid link) of the typed photo list. */
    private void refreshPreview() {
        List<String> urls = AdminValidation.parseImageUrls(AdminDialogs.text(images));
        String cover = !urls.isEmpty() && AdminValidation.imageUrl(urls.get(0)) == 0 ? urls.get(0) : "";
        int error = AdminValidation.imageUrls(AdminDialogs.text(images));
        imagesLayout.setError(error == 0 ? null : getString(error));
        RemoteImageLoader.load(findViewById(R.id.adminListingImagePreview),
                cover, getResources().getDisplayMetrics().widthPixels);
    }

    private ListingType selectedType() {
        int id = typeGroup.getCheckedChipId();
        return id == R.id.adminTypeTrade ? ListingType.TRADE : id == R.id.adminTypeSell ? ListingType.SELL : ListingType.GIVE_AWAY;
    }

    private ListingStatus selectedStatus() {
        int id = statusGroup.getCheckedChipId();
        return id == R.id.adminStatusReserved ? ListingStatus.RESERVED
                : id == R.id.adminStatusRehomed ? ListingStatus.REHOMED
                : id == R.id.adminStatusUnavailable ? ListingStatus.UNAVAILABLE : ListingStatus.ACTIVE;
    }

    private void showTypeFields() {
        ListingType type = selectedType();
        findViewById(R.id.adminListingPriceGroup).setVisibility(type == ListingType.SELL ? View.VISIBLE : View.GONE);
        findViewById(R.id.adminListingLookingGroup).setVisibility(type == ListingType.TRADE ? View.VISIBLE : View.GONE);
    }

    private void save() {
        ListingType type = selectedType();
        Map<String, Integer> errors = AdminValidation.listing(AdminDialogs.text(nickname), AdminDialogs.text(species),
                AdminDialogs.text(description), type, AdminDialogs.text(price), AdminDialogs.text(looking),
                AdminDialogs.text(location));
        boolean invalid = AdminDialogs.error(this, nicknameLayout, errors.get(AdminValidation.NICKNAME));
        invalid |= AdminDialogs.error(this, speciesLayout, errors.get(AdminValidation.SPECIES));
        invalid |= AdminDialogs.error(this, descriptionLayout, errors.get(AdminValidation.DESCRIPTION));
        invalid |= AdminDialogs.error(this, priceLayout, errors.get(AdminValidation.PRICE));
        invalid |= AdminDialogs.error(this, lookingLayout, errors.get(AdminValidation.LOOKING_FOR));
        invalid |= AdminDialogs.error(this, locationLayout, errors.get(AdminValidation.LOCATION));
        invalid |= AdminDialogs.error(this, imagesLayout, AdminValidation.imageUrls(AdminDialogs.text(images)));
        String n = AdminDialogs.note(this, noteLayout, note);
        if (invalid || n == null) return;
        viewModel.save(AdminDialogs.text(nickname), AdminDialogs.text(species), AdminDialogs.text(description), type,
                Math.max(0, AdminValidation.parsePrice(AdminDialogs.text(price))), AdminDialogs.text(looking),
                AdminDialogs.text(location), selectedStatus(),
                AdminValidation.parseImageUrls(AdminDialogs.text(images)), n);
    }
}
