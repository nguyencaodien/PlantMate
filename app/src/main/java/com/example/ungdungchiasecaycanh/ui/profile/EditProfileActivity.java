package com.example.ungdungchiasecaycanh.ui.profile;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.example.ungdungchiasecaycanh.util.Bitmaps;


public class EditProfileActivity extends AppCompatActivity {
    private EditProfileViewModel viewModel;
    private TextInputLayout nameLayout, locationLayout, bioLayout;
    private TextInputEditText nameInput, locationInput, bioInput;
    private MaterialButton save;
    private boolean formBound;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        SecondaryChrome.apply(this, findViewById(R.id.editProfileRoot), findViewById(R.id.editProfileHero));
        Motion.pop(findViewById(R.id.editProfileAvatarRing), 560, 220);
        Motion.rise(findViewById(R.id.editProfileForm), 300);
        nameLayout = findViewById(R.id.editProfileNameLayout);
        locationLayout = findViewById(R.id.editProfileLocationLayout);
        bioLayout = findViewById(R.id.editProfileBioLayout);
        nameInput = findViewById(R.id.editProfileName);
        locationInput = findViewById(R.id.editProfileLocation);
        bioInput = findViewById(R.id.editProfileBio);
        save = findViewById(R.id.editProfileSave);
        findViewById(R.id.editProfileBack).setOnClickListener(v -> finish());
        findViewById(R.id.editProfileRetry).setOnClickListener(v -> viewModel.retryLoad());
        save.setOnClickListener(v -> submit());
        viewModel = new ViewModelProvider(this).get(EditProfileViewModel.class);
        // The avatar follows the chosen picture; without one it shows the initials as the name is typed.
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderAvatar(viewModel.getAvatar().getValue());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        View.OnClickListener openPicker = v -> {
            if (getSupportFragmentManager().findFragmentByTag(AvatarPickerBottomSheet.TAG) == null) {
                new AvatarPickerBottomSheet().show(getSupportFragmentManager(), AvatarPickerBottomSheet.TAG);
            }
        };
        findViewById(R.id.editProfileAvatarRing).setOnClickListener(openPicker);
        findViewById(R.id.editProfileAvatarRing).setContentDescription(getString(R.string.avatar_change));
        findViewById(R.id.editProfileChangeAvatar).setOnClickListener(openPicker);
        viewModel.getAvatar().observe(this, this::renderAvatar);
        viewModel.getState().observe(this, this::render);
        viewModel.load();
    }

    private void submit() {
        EditProfileViewModel.Validation validation = viewModel.save(text(nameInput), text(locationInput), text(bioInput));
        nameLayout.setError(validation.displayNameError == 0 ? null : getString(validation.displayNameError));
        locationLayout.setError(validation.locationError == 0 ? null : getString(validation.locationError));
        bioLayout.setError(validation.bioError == 0 ? null : getString(validation.bioError));
    }

    private void render(EditProfileViewModel.State state) {
        boolean loading = state.status == EditProfileViewModel.Status.LOADING;
        boolean saving = state.status == EditProfileViewModel.Status.SAVING;
        boolean loadError = state.status == EditProfileViewModel.Status.ERROR && state.user == null;
        findViewById(R.id.editProfileLoading).setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.editProfileError).setVisibility(loadError ? View.VISIBLE : View.GONE);
        findViewById(R.id.editProfileContent).setVisibility(!loading && !loadError ? View.VISIBLE : View.GONE);
        save.setEnabled(!saving);
        save.setText(saving ? R.string.edit_profile_saving : R.string.edit_profile_save);
        if (state.user != null && !formBound) { bind(state.user); formBound = true; }
        if (state.status == EditProfileViewModel.Status.ERROR && state.user != null)
            findViewById(R.id.editProfileSaveError).setVisibility(View.VISIBLE);
        else findViewById(R.id.editProfileSaveError).setVisibility(View.GONE);
        if (state.status == EditProfileViewModel.Status.SUCCESS) {
            setResult(Activity.RESULT_OK); finish();
        }
    }

    /** Local photo (not uploaded yet), preset, stored photo or initials. */
    private void renderAvatar(String value) {
        TextView avatar = findViewById(R.id.editProfileAvatar);
        String name = text(nameInput);
        if (value != null && value.startsWith("content://")) {
            try {
                Bitmap bitmap = Bitmaps.decode(getContentResolver(), Uri.parse(value), 256);
                int side = Math.min(bitmap.getWidth(), bitmap.getHeight());
                RoundedBitmapDrawable round = RoundedBitmapDrawableFactory.create(getResources(), Bitmap.createBitmap(
                        bitmap, (bitmap.getWidth() - side) / 2, (bitmap.getHeight() - side) / 2, side, side));
                round.setCircular(true);
                avatar.setText("");
                avatar.setBackground(round);
                return;
            } catch (Exception ignored) {
                // Fall back to initials below.
            }
            AvatarBinder.bind(avatar, name, "", R.drawable.bg_lime_circle);
            return;
        }
        AvatarBinder.bind(avatar, name, value, R.drawable.bg_lime_circle);
    }

    private void bind(User user) {
        nameInput.setText(user.getDisplayName());
        locationInput.setText(user.getLocationText());
        bioInput.setText(user.getBio() == null ? "" : user.getBio());
    }
    private static String text(TextInputEditText input) { return input.getText() == null ? "" : input.getText().toString(); }
}
