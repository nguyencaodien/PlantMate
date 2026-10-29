package com.example.ungdungchiasecaycanh.ui.community;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.ui.photo.ImageCropActivity;
import com.example.ungdungchiasecaycanh.util.LocalImageLoader;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

/** "New story" (Community green moments): photo, short title, optional caption; friends see it for a day. */
public class CreateStoryActivity extends AppCompatActivity {
    /** Story photos are 9:16, like the viewer. */
    private static final float STORY_ASPECT = 9f / 16f;
    private static final int[] SUGGESTIONS = {R.string.story_suggest_new_leaves, R.string.story_suggest_rescue,
            R.string.story_suggest_propagate, R.string.story_suggest_meetup};

    private CreateStoryViewModel vm;
    private TextInputEditText title;
    private TextInputEditText caption;
    private String shownPhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_story);
        SecondaryChrome.applyPlain(this, findViewById(R.id.storyCreateRoot));
        Motion.rise(findViewById(R.id.storyCreateForm), 100);
        vm = new ViewModelProvider(this).get(CreateStoryViewModel.class);
        title = findViewById(R.id.storyTitleInput);
        caption = findViewById(R.id.storyCaptionInput);
        findViewById(R.id.storyCreateClose).setOnClickListener(v -> finish());

        ActivityResultLauncher<ImageCropActivity.Request> crop = registerForActivityResult(
                new ImageCropActivity.Crop(), uri -> { if (uri != null) vm.setPhoto(uri.toString()); });
        ActivityResultLauncher<PickVisualMediaRequest> picker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> { if (uri != null) crop.launch(new ImageCropActivity.Request(uri, STORY_ASPECT, 1, 1)); });
        findViewById(R.id.storyPhotoFrame).setOnClickListener(v -> picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));

        ChipGroup suggestions = findViewById(R.id.storyTitleSuggestions);
        for (int text : SUGGESTIONS) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_choice_chip, suggestions, false);
            chip.setId(View.generateViewId());
            chip.setText(text);
            chip.setCheckedIconVisible(false);
            chip.setOnClickListener(v -> {
                title.setText(chip.getText());
                title.setSelection(title.length());
            });
            suggestions.addView(chip);
        }

        MaterialButton publish = findViewById(R.id.storyPublish);
        publish.setOnClickListener(v -> vm.publish(text(title), text(caption)));
        vm.getState().observe(this, s -> {
            bindPhoto(s.photo);
            publish.setEnabled(!s.publishing);
            publish.setText(s.publishing ? R.string.story_publishing : R.string.story_publish);
            TextView error = findViewById(R.id.storyCreateError);
            error.setVisibility(s.error == 0 ? View.GONE : View.VISIBLE);
            if (s.error != 0) error.setText(s.error);
            if (s.published) {
                setResult(RESULT_OK);
                finish(); // no reusable creation form left in the Back stack
            }
        });
    }

    private void bindPhoto(String photo) {
        if (photo == null ? shownPhoto == null : photo.equals(shownPhoto)) return;
        shownPhoto = photo;
        android.widget.ImageView image = findViewById(R.id.storyPhoto);
        boolean has = photo != null;
        findViewById(R.id.storyPhotoEmpty).setVisibility(has ? View.GONE : View.VISIBLE);
        findViewById(R.id.storyPhotoChange).setVisibility(has ? View.VISIBLE : View.GONE);
        image.setVisibility(has ? View.VISIBLE : View.GONE);
        if (has) LocalImageLoader.load(image, photo, getResources().getDisplayMetrics().widthPixels, null);
        findViewById(R.id.storyPhotoFrame).setContentDescription(getString(has ? R.string.story_change_photo
                : R.string.story_pick_photo));
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
