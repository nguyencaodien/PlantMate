package com.example.ungdungchiasecaycanh.ui.garden;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.ui.photo.ImageCropActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

abstract class BasePlantFormActivity extends AppCompatActivity {
    private static final String PERMISSION_PREFS = "plantmate_permissions";
    private static final String NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested";
    protected PlantFormViewModel viewModel;
    protected TextInputLayout nicknameLayout, speciesLayout, intervalLayout;
    protected TextInputEditText nicknameInput, speciesInput, intervalInput;
    protected MaterialSwitch reminderSwitch;
    protected MaterialButton saveButton;
    private View scheduleGroup, dailyGroup;
    private MaterialButtonToggleGroup frequencyToggle;
    private ChipGroup timesGroup;
    private MaterialButton addTimeButton;
    private TextView limitHint;
    private MaterialAutoCompleteTextView roomInput;
    private MaterialButtonToggleGroup lightGroup;
    /** Offered as areas before the member has named their own. */
    private static final int[] DEFAULT_ROOMS = {R.string.garden_room_living, R.string.garden_room_balcony,
            R.string.garden_room_bedroom, R.string.garden_room_desk, R.string.garden_room_kitchen};
    /** Offered when a time is added: morning, late afternoon, noon, evening. */
    private static final int[] SUGGESTED_TIMES = {7 * 60, 17 * 60, 12 * 60, 21 * 60};
    private ImageView imagePreview;
    private MaterialButton removePhotoButton;
    private ProgressBar progress;
    protected String selectedImageUri = "";
    private boolean renderedExisting;
    private boolean applyingPlant;
    private boolean notificationPermissionRequested;

    /** Picked photos are framed square; the cropped copy lives in app storage, so Room keeps a lasting URI. */
    private final ActivityResultLauncher<ImageCropActivity.Request> cropper = registerForActivityResult(
            new ImageCropActivity.Crop(), uri -> {
                if (uri == null) return;
                selectedImageUri = uri.toString();
                renderImage();
            });
    private final ActivityResultLauncher<PickVisualMediaRequest> imagePicker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> { if (uri != null) cropper.launch(new ImageCropActivity.Request(uri, 1f, 1, 1)); });
    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                notificationPermissionRequested = true;
                getSharedPreferences(PERMISSION_PREFS, MODE_PRIVATE).edit()
                        .putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply();
                if (!granted) Toast.makeText(this, R.string.garden_notification_permission_denied,
                        Toast.LENGTH_LONG).show();
            });

    protected void initialize(@LayoutRes int layoutId, @Nullable Bundle savedInstanceState) {
        setContentView(layoutId);
        SecondaryChrome.apply(this, findViewById(R.id.plantFormRoot), findViewById(R.id.plantFormHero));
        Motion.start(findViewById(R.id.plantFormMam));
        Motion.floatLoop(findViewById(R.id.plantFormBubble));
        Motion.rise(findViewById(R.id.plantFormFields), 220);
        Motion.sheet(findViewById(R.id.plantFormBar), 560, 300);
        notificationPermissionRequested = getSharedPreferences(PERMISSION_PREFS, MODE_PRIVATE)
                .getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false)
                || (savedInstanceState != null
                && savedInstanceState.getBoolean(NOTIFICATION_PERMISSION_REQUESTED, false));
        viewModel = new ViewModelProvider(this).get(PlantFormViewModel.class);
        nicknameLayout = findViewById(R.id.plantNicknameLayout);
        speciesLayout = findViewById(R.id.plantSpeciesLayout);
        intervalLayout = findViewById(R.id.plantIntervalLayout);
        nicknameInput = findViewById(R.id.plantNicknameInput);
        speciesInput = findViewById(R.id.plantSpeciesInput);
        intervalInput = findViewById(R.id.plantIntervalInput);
        reminderSwitch = findViewById(R.id.plantReminderSwitch);
        saveButton = findViewById(R.id.plantSaveButton);
        imagePreview = findViewById(R.id.plantImagePreview);
        removePhotoButton = findViewById(R.id.plantRemovePhoto);
        progress = findViewById(R.id.plantFormProgress);
        scheduleGroup = findViewById(R.id.plantScheduleGroup);
        dailyGroup = findViewById(R.id.plantDailyGroup);
        frequencyToggle = findViewById(R.id.plantFrequencyToggle);
        timesGroup = findViewById(R.id.plantTimesGroup);
        addTimeButton = findViewById(R.id.plantAddTime);
        limitHint = findViewById(R.id.plantLimitHint);
        roomInput = findViewById(R.id.plantRoomInput);
        lightGroup = findViewById(R.id.plantLightGroup);
        roomInput.setOnFocusChangeListener((v, focused) -> { if (focused) roomInput.showDropDown(); });
        roomInput.setOnClickListener(v -> roomInput.showDropDown());
        viewModel.getPlants().observe(this, plants -> roomInput.setSimpleItems(roomSuggestions(plants)));
        ((TextView) findViewById(R.id.plantFormTitle)).setText(
                layoutId == R.layout.activity_edit_plant ? R.string.garden_edit_plant : R.string.garden_add_plant);
        saveButton.setText(layoutId == R.layout.activity_edit_plant
                ? R.string.garden_save_changes : R.string.garden_save_plant);

        findViewById(R.id.plantFormBack).setOnClickListener(v -> finish());
        findViewById(R.id.plantSelectPhoto).setOnClickListener(v -> imagePicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        removePhotoButton.setOnClickListener(v -> { selectedImageUri = ""; renderImage(); });
        reminderSwitch.setOnCheckedChangeListener((button, checked) -> {
            scheduleGroup.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (!checked) intervalInput.setText("");
            else if (!applyingPlant) maybeRequestNotificationPermission();
        });
        frequencyToggle.addOnButtonCheckedListener((group, id, checked) -> { if (checked) renderSchedule(); });
        addTimeButton.setOnClickListener(v -> pickTime(-1));
        saveButton.setOnClickListener(v -> submit());
        renderSchedule();

        viewModel.getBusy().observe(this, busy -> {
            boolean active = Boolean.TRUE.equals(busy);
            progress.setVisibility(active ? View.VISIBLE : View.GONE);
            saveButton.setEnabled(!active);
        });
        viewModel.getSaved().observe(this, saved -> { if (Boolean.TRUE.equals(saved)) finish(); });
        viewModel.getError().observe(this, message -> {
            if (message != null) nicknameLayout.setError(message);
        });
        viewModel.getPlant().observe(this, plant -> {
            if (plant != null && !renderedExisting) { renderedExisting = true; renderPlant(plant); }
        });
        renderImage();
    }

    protected abstract void submitValid(String nickname, String species, String imageUri,
                                        boolean reminders, int intervalDays, String wateringTimes,
                                        String room, String light);

    /** The member's own areas first, then the defaults they have not used. */
    private String[] roomSuggestions(List<PlantEntity> plants) {
        LinkedHashSet<String> rooms = new LinkedHashSet<>();
        if (plants != null) for (PlantEntity p : plants) if (!p.getRoom().isEmpty()) rooms.add(p.getRoom());
        for (int id : DEFAULT_ROOMS) rooms.add(getString(id));
        return rooms.toArray(new String[0]);
    }

    private String selectedLight() {
        int id = lightGroup.getCheckedButtonId();
        if (id == R.id.plantLightSun) return LightLevel.SUN.name();
        if (id == R.id.plantLightBright) return LightLevel.BRIGHT_INDIRECT.name();
        if (id == R.id.plantLightShade) return LightLevel.SHADE.name();
        return "";
    }

    private void submit() {
        nicknameLayout.setError(null); speciesLayout.setError(null); intervalLayout.setError(null);
        String nickname = text(nicknameInput);
        String species = text(speciesInput);
        if (nickname.trim().isEmpty()) { nicknameLayout.setError(getString(R.string.garden_nickname_required)); return; }
        if (nickname.trim().length() > 60) { nicknameLayout.setError(getString(R.string.garden_nickname_too_long)); return; }
        if (species.trim().length() > 100) { speciesLayout.setError(getString(R.string.garden_species_too_long)); return; }
        boolean reminders = reminderSwitch.isChecked();
        boolean daily = reminders && isDaily();
        int interval = 0;
        if (reminders && !daily) {
            try { interval = Integer.parseInt(text(intervalInput).trim()); }
            catch (NumberFormatException ignored) { intervalLayout.setError(getString(R.string.garden_interval_invalid)); return; }
            if (interval < 1 || interval > 365) { intervalLayout.setError(getString(R.string.garden_interval_invalid)); return; }
        }
        String room = roomInput.getText() == null ? "" : roomInput.getText().toString();
        submitValid(nickname, species, selectedImageUri, reminders, interval,
                daily ? GardenRepository.formatTimes(viewModel.times) : "", room, selectedLight());
    }

    private void renderPlant(PlantEntity plant) {
        nicknameInput.setText(plant.getNickname());
        speciesInput.setText(plant.getSpecies());
        roomInput.setText(plant.getRoom(), false);
        LightLevel light = LightLevel.fromName(plant.getLight());
        if (light == LightLevel.SUN) lightGroup.check(R.id.plantLightSun);
        else if (light == LightLevel.BRIGHT_INDIRECT) lightGroup.check(R.id.plantLightBright);
        else if (light == LightLevel.SHADE) lightGroup.check(R.id.plantLightShade);
        selectedImageUri = plant.getImageUri();
        applyingPlant = true;
        reminderSwitch.setChecked(plant.isReminderEnabled());
        applyingPlant = false;
        List<Integer> times = GardenRepository.parseTimes(plant.getWateringTimes());
        viewModel.times.clear();
        viewModel.times.addAll(times);
        frequencyToggle.check(times.isEmpty() ? R.id.plantFrequencyInterval : R.id.plantFrequencyDaily);
        if (plant.isReminderEnabled() && times.isEmpty()) intervalInput.setText(String.valueOf(plant.getWateringIntervalDays()));
        renderSchedule();
        renderImage();
    }

    private boolean isDaily() {
        return frequencyToggle.getCheckedButtonId() == R.id.plantFrequencyDaily;
    }

    /** Every few days shows the interval field; every day shows the reminder times (1 to 4). */
    private void renderSchedule() {
        boolean daily = isDaily();
        List<Integer> times = viewModel.times;
        if (daily && times.isEmpty()) times.add(SUGGESTED_TIMES[0]);
        intervalLayout.setVisibility(daily ? View.GONE : View.VISIBLE);
        dailyGroup.setVisibility(daily ? View.VISIBLE : View.GONE);
        timesGroup.removeAllViews();
        for (int i = 0; i < times.size(); i++) {
            int index = i;
            String label = clock(times.get(i));
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_time_chip, timesGroup, false);
            chip.setText(label);
            chip.setContentDescription(label);
            chip.setCloseIconVisible(times.size() > 1);
            chip.setCloseIconContentDescription(getString(R.string.garden_time_remove, label));
            chip.setOnClickListener(v -> pickTime(index));
            chip.setOnCloseIconClickListener(v -> { times.remove(index); renderSchedule(); });
            timesGroup.addView(chip);
        }
        addTimeButton.setVisibility(times.size() < GardenRepository.MAX_TIMES_PER_DAY ? View.VISIBLE : View.GONE);
        int perDay = daily ? times.size() : 1;
        limitHint.setText(getResources().getQuantityString(R.plurals.garden_limit_hint, perDay, perDay));
    }

    /** Adds a time ({@code index} -1) or changes one; the list stays sorted and free of repeats. */
    private void pickTime(int index) {
        List<Integer> times = viewModel.times;
        int start = index >= 0 ? times.get(index) : suggestedTime(times);
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(start / 60)
                .setMinute(start % 60)
                .setTitleText(R.string.garden_pick_time_title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            int minute = picker.getHour() * 60 + picker.getMinute();
            int existing = times.indexOf(minute);
            if (existing >= 0 && existing != index) {
                Toast.makeText(this, getString(R.string.garden_time_duplicate, clock(minute)), Toast.LENGTH_SHORT).show();
                return;
            }
            if (index >= 0) times.set(index, minute); else times.add(minute);
            Collections.sort(times);
            renderSchedule();
        });
        picker.show(getSupportFragmentManager(), "watering_time");
    }

    private static int suggestedTime(List<Integer> times) {
        for (int minute : SUGGESTED_TIMES) if (!times.contains(minute)) return minute;
        return 8 * 60;
    }

    /** 07:00 or 7:00 AM, following the phone's clock setting. */
    private String clock(int minuteOfDay) {
        Calendar time = Calendar.getInstance();
        time.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        time.set(Calendar.MINUTE, minuteOfDay % 60);
        return DateFormat.getTimeFormat(this).format(time.getTime());
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || notificationPermissionRequested
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;
        notificationPermissionRequested = true;
        getSharedPreferences(PERMISSION_PREFS, MODE_PRIVATE).edit()
                .putBoolean(NOTIFICATION_PERMISSION_REQUESTED, true).apply();
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(NOTIFICATION_PERMISSION_REQUESTED, notificationPermissionRequested);
        super.onSaveInstanceState(outState);
    }

    private void showPlaceholder() {
        imagePreview.setPadding(0, 0, 0, 0);
        imagePreview.setScaleType(ImageView.ScaleType.CENTER);
        imagePreview.setImageResource(R.drawable.ic_botanical_placeholder);
    }

    private void renderImage() {
        showPlaceholder();
        if (!selectedImageUri.isEmpty()) {
            try {
                imagePreview.setPadding(0, 0, 0, 0);
                imagePreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imagePreview.setImageURI(Uri.parse(selectedImageUri));
            } catch (Exception ignored) { showPlaceholder(); }
        }
        removePhotoButton.setVisibility(selectedImageUri.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }
}
