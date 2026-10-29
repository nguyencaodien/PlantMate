package com.example.ungdungchiasecaycanh.ui.community;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailViewModel;
import com.example.ungdungchiasecaycanh.ui.photo.ImageCropActivity;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.LocalImageLoader;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Composer (Roundtable-inspired): close, Publish, the author with audience and topic chips, a
 * borderless text field, a photo preview and a tool row with the character counter.
 */
public class CreatePostActivity extends AppCompatActivity {
    public static final String EXTRA_LISTING_ID = "listingId";
    static final int MAX_CHARS = 2000;
    private static final PostCategory[] TOPICS = {PostCategory.GENERAL, PostCategory.SHARING,
            PostCategory.QUESTION, PostCategory.PLANT_WIN, PostCategory.TIP};
    private EditText input;
    private MaterialButton submit;
    private TextView error;
    private CreatePostViewModel vm;
    private boolean finished;
    private boolean topicLocked;
    private boolean busy;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_create_post);
        View root = findViewById(R.id.createPostRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(SecondaryChrome.useDarkStatusIcons(this));
        Motion.rise(findViewById(R.id.createPostForm), 120);
        Motion.sheet(findViewById(R.id.createPostTools), 420, 200);

        input = findViewById(R.id.postTextInput);
        submit = findViewById(R.id.postSubmit);
        error = findViewById(R.id.postError);
        findViewById(R.id.createPostBack).setOnClickListener(v -> finish());
        vm = new ViewModelProvider(this).get(CreatePostViewModel.class);

        String listingId = getIntent().getStringExtra(EXTRA_LISTING_ID);
        if (listingId != null && !listingId.trim().isEmpty()) {
            // Sharing a listing is always a "Sharing" post.
            topicLocked = true;
            vm.setTopic(PostCategory.SHARING);
            vm.initializeShare(listingId);
        }
        View.OnClickListener chooseTopic = v -> { if (!topicLocked && !busy) showTopicPicker(); };
        findViewById(R.id.postCategoryChip).setOnClickListener(chooseTopic);
        findViewById(R.id.postToolCategory).setOnClickListener(chooseTopic);
        findViewById(R.id.postToolCategory).setEnabled(!topicLocked);
        vm.getTopic().observe(this, this::renderTopic);

        // Picked photos are framed at 3:2, the shape the feed and post detail show.
        ActivityResultLauncher<ImageCropActivity.Request> crop = registerForActivityResult(
                new ImageCropActivity.Crop(), uri -> { if (uri != null) vm.setPhoto(uri.toString()); });
        ActivityResultLauncher<PickVisualMediaRequest> picker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> { if (uri != null) crop.launch(new ImageCropActivity.Request(uri, 1.5f, 1, 1)); });
        findViewById(R.id.postToolPhoto).setOnClickListener(v -> picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        findViewById(R.id.postPhotoRemove).setOnClickListener(v -> vm.setPhoto(null));
        vm.getPhoto().observe(this, this::renderPhoto);

        vm.getAuthor().observe(this, author -> {
            String name = author == null || author.getDisplayName().trim().isEmpty()
                    ? getString(R.string.community_member) : author.getDisplayName();
            ((TextView) findViewById(R.id.createPostAuthor)).setText(name);
            AvatarBinder.bind(findViewById(R.id.createPostAvatar), name, author == null ? "" : author.getAvatarUrl(),
                    R.drawable.bg_lime_circle);
        });

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderCounter();
                error.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        renderCounter();
        input.requestFocus();

        submit.setOnClickListener(v -> {
            error.setVisibility(View.GONE);
            PostCategory topic = vm.getTopic().getValue();
            vm.submit(String.valueOf(input.getText()), topic == null ? PostCategory.GENERAL : topic);
        });
        vm.getState().observe(this, s -> {
            busy = s.submitting;
            submit.setEnabled(!s.submitting && !s.shareLoading && !s.shareInvalid);
            submit.setText(s.submitting ? R.string.post_submitting : R.string.composer_publish);
            renderAttachedListing(s.listing, s.shareInvalid);
            if (s.error != null) {
                error.setText(s.error);
                error.setVisibility(View.VISIBLE);
                Snackbar.make(submit, s.error, Snackbar.LENGTH_SHORT).show();
            }
            if (s.postId != null && !finished) {
                finished = true;
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    private void renderCounter() {
        int length = input.getText() == null ? 0 : input.getText().toString().trim().length();
        ((TextView) findViewById(R.id.postCounter)).setText(getString(R.string.composer_counter, length, MAX_CHARS));
    }

    private void showTopicPicker() {
        String[] labels = new String[TOPICS.length];
        PostCategory current = vm.getTopic().getValue();
        int checked = 0;
        for (int i = 0; i < TOPICS.length; i++) {
            labels[i] = getString(labelFor(TOPICS[i]));
            if (TOPICS[i] == current) checked = i;
        }
        new MaterialAlertDialogBuilder(this).setTitle(R.string.composer_pick_category)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    vm.setTopic(TOPICS[which]);
                    dialog.dismiss();
                }).show();
    }

    private void renderTopic(PostCategory topic) {
        String label = getString(labelFor(topic));
        TextView chip = findViewById(R.id.postCategoryChip);
        chip.setText(label);
        chip.setContentDescription(getString(R.string.composer_category_desc, label));
        chip.setEnabled(!topicLocked);
    }

    static int labelFor(PostCategory category) {
        if (category == null) return R.string.post_category_general;
        switch (category) {
            case SHARING: return R.string.post_category_sharing;
            case QUESTION: return R.string.post_category_question;
            case PLANT_WIN: return R.string.post_category_win;
            case TIP: return R.string.post_category_tip;
            default: return R.string.post_category_general;
        }
    }

    private void renderPhoto(String uri) {
        ImageView preview = findViewById(R.id.postPhotoPreview);
        View card = findViewById(R.id.postPhotoCard);
        MaterialButton tool = findViewById(R.id.postToolPhoto);
        boolean has = uri != null;
        card.setVisibility(has ? View.VISIBLE : View.GONE);
        preview.setImageDrawable(null);
        if (has) LocalImageLoader.load(preview, uri, getResources().getDisplayMetrics().widthPixels, null);
        // The active tool sits on a soft green square, as in the design.
        tool.setBackgroundTintList(has ? ContextCompat.getColorStateList(this, R.color.color_primary_tint)
                : ColorStateList.valueOf(0));
    }

    private void renderAttachedListing(Listing listing, boolean invalid) {
        View preview = findViewById(R.id.postAttachedListing);
        preview.setVisibility(listing != null || invalid ? View.VISIBLE : View.GONE);
        TextView name = findViewById(R.id.postAttachedListingName);
        if (listing != null || invalid) {
            name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_plantmate_brand, 0, 0, 0);
            name.setCompoundDrawablePadding((int) (8 * getResources().getDisplayMetrics().density));
        }
        if (listing != null) {
            name.setText(listing.getNickname());
            List<String> meta = new ArrayList<>();
            if (listing.getSpecies() != null && !listing.getSpecies().trim().isEmpty()) meta.add(listing.getSpecies().trim());
            meta.add(ListingDetailViewModel.typeLabel(this, listing));
            meta.add(listing.getLocationText());
            ((TextView) findViewById(R.id.postAttachedListingMeta)).setText(TextUtils.join(" · ", meta));
        } else if (invalid) {
            name.setText(R.string.inbox_listing_unavailable);
            ((TextView) findViewById(R.id.postAttachedListingMeta)).setText("");
        }
    }
}
