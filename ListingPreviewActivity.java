package com.example.ungdungchiasecaycanh.ui.listing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;

public class ListingPreviewActivity extends AppCompatActivity {
    public static final String EXTRA_LISTING_ID = "listingId";
    private ListingPreviewViewModel viewModel;
    private MaterialButton publishButton;
    private View publishProgress;
    private TextView publishError;
    private String nickname;
    private String species;
    private String description;
    private ListingType type;
    private long price;
    private String lookingFor;
    private String location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_preview);

        ArrayList<String> images = getIntent().getStringArrayListExtra(
                CreateListingActivity.EXTRA_IMAGE_URIS);
        if (images == null) images = new ArrayList<>();

        ImageView cover = findViewById(R.id.preview_cover_image);
        if (!images.isEmpty()) {
            cover.setPadding(0, 0, 0, 0);
            cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
            cover.setImageURI(Uri.parse(images.get(0)));
        } else {
            cover.setImageResource(R.drawable.ic_botanical_placeholder);
        }

        ListingImageDraftAdapter galleryAdapter = new ListingImageDraftAdapter(null);
        RecyclerView gallery = findViewById(R.id.preview_image_gallery);
        gallery.setAdapter(galleryAdapter);
        gallery.setNestedScrollingEnabled(false);
        galleryAdapter.submitList(images.size() > 1
                ? new ArrayList<>(images.subList(1, images.size()))
                : new ArrayList<>());
        gallery.setVisibility(images.size() > 1 ? View.VISIBLE : View.GONE);

        nickname = getIntent().getStringExtra(CreateListingActivity.EXTRA_NICKNAME);
        species = getIntent().getStringExtra(CreateListingActivity.EXTRA_SPECIES);
        description = getIntent().getStringExtra(CreateListingActivity.EXTRA_DESCRIPTION);
        location = getIntent().getStringExtra(CreateListingActivity.EXTRA_LOCATION);
        lookingFor = getIntent().getStringExtra(CreateListingActivity.EXTRA_LOOKING_FOR);
        price = getIntent().getLongExtra(CreateListingActivity.EXTRA_PRICE, 0);
        setText(R.id.preview_nickname, nickname);
        setOptionalText(R.id.preview_species, species);
        setText(R.id.preview_description, description);
        setText(R.id.preview_location, location);

        try {
            type = ListingType.valueOf(getIntent().getStringExtra(
                    CreateListingActivity.EXTRA_TYPE));
        } catch (IllegalArgumentException | NullPointerException ignored) {
            type = null;
        }
        TextView typeValue = findViewById(R.id.preview_type_value);
        bindTypeStyle(type);
        if (type == ListingType.TRADE) {
            typeValue.setText(getString(R.string.listing_type_trade_detail,
                    lookingFor));
        } else if (type == ListingType.SELL) {
            typeValue.setText(getString(R.string.listing_type_sell_detail, ListingAdapter.formatVnd(price)));
        } else {
            typeValue.setText(R.string.listing_price_free);
        }

        findViewById(R.id.action_preview_back).setOnClickListener(view -> finish());
        publishButton = findViewById(R.id.button_publish_listing);
        publishProgress = findViewById(R.id.preview_publish_progress);
        publishError = findViewById(R.id.preview_publish_error);
        viewModel = new ViewModelProvider(this).get(ListingPreviewViewModel.class);
        final ArrayList<String> photos = images;
        final ListingDetails details = ListingDetailsExtras.get(getIntent());
        publishButton.setOnClickListener(view -> viewModel.publish(nickname, species,
                description, type, price, lookingFor, photos, location, details));
        viewModel.getState().observe(this, this::renderPublishState);
    }

    private void renderPublishState(ListingPreviewViewModel.State state) {
        boolean publishing = state.status == ListingPreviewViewModel.Status.PUBLISHING;
        publishButton.setEnabled(!publishing
                && state.status != ListingPreviewViewModel.Status.SUCCESS);
        publishButton.setText(publishing ? R.string.listing_publishing : R.string.action_publish);
        publishProgress.setVisibility(publishing ? View.VISIBLE : View.GONE);
        publishError.setVisibility(state.status == ListingPreviewViewModel.Status.ERROR
                ? View.VISIBLE : View.GONE);
        publishError.setText(state.needsVerification ? R.string.listing_sell_verify_error : R.string.listing_publish_error);
        findViewById(R.id.action_preview_back).setEnabled(!publishing);
        if (state.status == ListingPreviewViewModel.Status.SUCCESS) {
            setResult(RESULT_OK, new Intent()
                    .putExtra(EXTRA_LISTING_ID, state.listingId));
            finish();
        }
    }

    private void bindTypeStyle(ListingType listingType) {
        int tag = R.drawable.bg_badge_give_away, frame = R.drawable.bg_frame_give_away;
        int color = R.color.badge_give_text, label = R.string.listing_type_give_away_detail;
        if (listingType == ListingType.TRADE) {
            tag = R.drawable.bg_badge_trade; frame = R.drawable.bg_frame_trade;
            color = R.color.badge_trade_text; label = R.string.listing_type_trade;
        } else if (listingType == ListingType.SELL) {
            tag = R.drawable.bg_badge_sell; frame = R.drawable.bg_frame_sell;
            color = R.color.badge_sell_text; label = R.string.listing_type_sell;
        }
        int resolved = ContextCompat.getColor(this, color);
        TextView typeTag = findViewById(R.id.preview_type_tag);
        typeTag.setText(label);
        typeTag.setBackgroundResource(tag);
        typeTag.setTextColor(resolved);
        ((TextView) findViewById(R.id.preview_type_value)).setTextColor(
                ContextCompat.getColor(this, R.color.color_text_primary));
        findViewById(R.id.preview_cover_frame).setBackgroundResource(frame);
    }

    private void setText(int id, String value) {
        ((TextView) findViewById(id)).setText(value == null ? "" : value);
    }

    private void setOptionalText(int id, String value) {
        TextView view = findViewById(id);
        if (value == null || value.trim().isEmpty()) {
            view.setVisibility(View.GONE);
        } else {
            view.setText(value);
        }
    }
}
