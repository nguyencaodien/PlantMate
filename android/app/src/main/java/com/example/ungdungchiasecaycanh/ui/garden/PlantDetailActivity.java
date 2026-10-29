package com.example.ungdungchiasecaycanh.ui.garden;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.MainActivity;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantPhotoEntity;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.ui.doctor.DoctorHistoryActivity;
import com.example.ungdungchiasecaycanh.ui.listing.CreateListingActivity;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailLabels;
import com.example.ungdungchiasecaycanh.ui.photo.ImageCropActivity;
import com.example.ungdungchiasecaycanh.util.LocalImageLoader;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Plant detail (canvas B): photo, a white sheet with name, health and pills, then Overview / Care plan / Journal
 * tabs. Everything shown comes from the plant, its care log, photos and its latest Plant Doctor check.
 */
public class PlantDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PLANT_ID = "extra_plant_id";
    private PlantDetailViewModel viewModel;
    private long plantId;
    private CareLogAdapter careLogAdapter;
    private AddCareLogBottomSheet activeSheet;
    private View careHistoryEmpty;
    private PlantEntity shownPlant;
    private List<CareLogEntity> shownLogs;
    private Boolean shownHealth;
    private static final CareType[] LOG_TYPES = {CareType.WATER, CareType.FERTILIZE, CareType.REPOT,
            CareType.PRUNE, CareType.NOTE};
    private final ActivityResultLauncher<Intent> editLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == EditPlantActivity.RESULT_PLANT_DELETED) finish();
                else viewModel.retry();
            });
    /** New timeline photos are framed square and copied into app storage before they are saved. */
    private final ActivityResultLauncher<ImageCropActivity.Request> cropper = registerForActivityResult(
            new ImageCropActivity.Crop(), uri -> { if (uri != null) viewModel.addPhoto(uri.toString()); });
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(),
            uri -> { if (uri != null) cropper.launch(new ImageCropActivity.Request(uri, 1f, 1, 1)); });
    private List<PlantPhotoEntity> shownPhotos;
    private int shownPhotoErrors;
    private boolean darkStatusIcons;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plant_detail);
        // Edge to edge: the photo runs under the status bar; the floating buttons and the
        // scroll content keep clear of the system bars.
        View topBar = findViewById(R.id.plantDetailTopBar);
        View scroll = findViewById(R.id.plantDetailContent);
        View bottom = findViewById(R.id.plantDetailBottom);
        int topBarPadding = topBar.getPaddingTop();
        int bottomPadding = bottom.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.plantDetailRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            topBar.setPadding(topBar.getPaddingLeft(), topBarPadding + bars.top, topBar.getPaddingRight(), topBar.getPaddingBottom());
            scroll.setPadding(bars.left, 0, bars.right, bars.bottom);
            bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(),
                    bottomPadding + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        plantId = getIntent().getLongExtra(EXTRA_PLANT_ID, -1);
        if (plantId <= 0) { finish(); return; }
        findViewById(R.id.plantDetailBack).setOnClickListener(v -> finish());
        findViewById(R.id.plantDetailRetry).setOnClickListener(v -> viewModel.retry());
        View.OnClickListener edit = v -> editLauncher.launch(
                new Intent(this, EditPlantActivity.class).putExtra(EditPlantActivity.EXTRA_PLANT_ID, plantId));
        findViewById(R.id.plantDetailEdit).setOnClickListener(edit);
        findViewById(R.id.plantPlanEdit).setOnClickListener(edit);
        findViewById(R.id.plantWaterNow).setOnClickListener(v -> viewModel.recordCare(CareType.WATER, ""));
        View.OnClickListener addPhoto = v -> photoPicker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
        findViewById(R.id.plantAddPhoto).setOnClickListener(addPhoto);
        findViewById(R.id.plantAddPhotoTile).setOnClickListener(addPhoto);
        findViewById(R.id.plantScan).setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_SELECT_TAB, MainActivity.TAB_DOCTOR)));
        findViewById(R.id.plantGiveAway).setOnClickListener(v -> startActivity(new Intent(this, CreateListingActivity.class)
                .putExtra(CreateListingActivity.EXTRA_PLANT_ID, plantId)));
        // Once the photo scrolls away the round buttons get a white bar so they never sit on text.
        int surface = ContextCompat.getColor(this, R.color.color_background);
        ((NestedScrollView) scroll).setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, x, y, ox, oy) -> {
            int fadeStart = findViewById(R.id.plantDetailPhotoFrame).getHeight() - topBar.getHeight() * 2;
            float t = Math.max(0f, Math.min(1f, (y - fadeStart) / (float) Math.max(1, topBar.getHeight())));
            topBar.setBackgroundColor(ColorUtils.setAlphaComponent(surface, Math.round(255 * t)));
            // Light icons over the photo, dark ones once the bar is white (night mode keeps light icons).
            boolean dark = t > 0.5f && SecondaryChrome.useDarkStatusIcons(this);
            if (dark != darkStatusIcons) {
                darkStatusIcons = dark;
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(dark);
            }
        });
        setUpTabs();

        RecyclerView history = findViewById(R.id.careHistoryList);
        history.setLayoutManager(new LinearLayoutManager(this));
        history.setNestedScrollingEnabled(false);
        careLogAdapter = new CareLogAdapter();
        history.setAdapter(careLogAdapter);
        careHistoryEmpty = findViewById(R.id.careHistoryEmpty);
        findViewById(R.id.plantLogCare).setOnClickListener(v -> chooseCareType());
        findViewById(R.id.plantHistoryToggle).setOnClickListener(v -> {
            careLogAdapter.setExpanded(!careLogAdapter.isExpanded());
            bindHistoryToggle();
        });

        ProgressBar loading = findViewById(R.id.plantDetailLoading);
        View content = scroll;
        View error = findViewById(R.id.plantDetailErrorState);
        TextView errorText = findViewById(R.id.plantDetailErrorText);
        viewModel = new ViewModelProvider(this).get(PlantDetailViewModel.class);
        getSupportFragmentManager().setFragmentResultListener(AddCareLogBottomSheet.RESULT_KEY,
                this, (requestKey, result) -> {
                    try {
                        CareType requestedType = CareType.valueOf(result.getString(AddCareLogBottomSheet.RESULT_TYPE, ""));
                        viewModel.recordCare(requestedType, result.getString(AddCareLogBottomSheet.RESULT_NOTE, ""));
                    } catch (IllegalArgumentException ignored) {
                        Snackbar.make(findViewById(R.id.plantDetailContent), R.string.care_action_error, Snackbar.LENGTH_LONG).show();
                    }
                });
        viewModel.getLoading().observe(this, active -> loading.setVisibility(Boolean.TRUE.equals(active) ? View.VISIBLE : View.GONE));
        viewModel.getPlant().observe(this, plant -> {
            if (plant == null) return;
            if (content.getVisibility() != View.VISIBLE) playEntrance();
            content.setVisibility(View.VISIBLE); error.setVisibility(View.GONE);
            findViewById(R.id.plantDetailBottom).setVisibility(View.VISIBLE); bind(plant);
        });
        viewModel.getUnavailable().observe(this, unavailable -> {
            if (Boolean.TRUE.equals(unavailable)) {
                content.setVisibility(View.GONE); error.setVisibility(View.VISIBLE);
                findViewById(R.id.plantDetailBottom).setVisibility(View.GONE);
                errorText.setText(R.string.garden_plant_unavailable);
            }
        });
        viewModel.getError().observe(this, message -> {
            if (message != null) {
                content.setVisibility(View.GONE); error.setVisibility(View.VISIBLE);
                findViewById(R.id.plantDetailBottom).setVisibility(View.GONE);
                errorText.setText(R.string.garden_load_error);
            }
        });
        viewModel.getCareLogs().observe(this, logs -> {
            careLogAdapter.submitList(logs);
            boolean empty = logs == null || logs.isEmpty();
            careHistoryEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            shownLogs = logs;
            bindHistoryToggle();
            bindStats();
        });
        viewModel.getCareSubmitting().observe(this, submitting -> {
            boolean busy = Boolean.TRUE.equals(submitting);
            findViewById(R.id.plantLogCare).setEnabled(!busy);
            findViewById(R.id.plantWaterNow).setEnabled(!busy);
            if (activeSheet != null) activeSheet.setSubmitting(busy);
            else {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag("care-entry");
                if (fragment instanceof AddCareLogBottomSheet) ((AddCareLogBottomSheet) fragment).setSubmitting(busy);
            }
        });
        viewModel.getCareSaved().observe(this, saved -> {
            if (Boolean.TRUE.equals(saved) && activeSheet != null) {
                activeSheet.dismissAllowingStateLoss(); activeSheet = null;
            } else if (Boolean.TRUE.equals(saved)) {
                FragmentManager manager = getSupportFragmentManager();
                Fragment fragment = manager.findFragmentByTag("care-entry");
                if (fragment instanceof AddCareLogBottomSheet)
                    ((AddCareLogBottomSheet) fragment).dismissAllowingStateLoss();
            }
        });
        viewModel.getCareError().observe(this, message -> {
            if (message == null) return;
            if (activeSheet != null) activeSheet.showError(message);
            else Snackbar.make(findViewById(R.id.plantDetailContent), message, Snackbar.LENGTH_LONG).show();
        });
        viewModel.getPhotos().observe(this, photos -> { shownPhotos = photos; bindPhotos(); });
        viewModel.getHealth().observe(this, this::bindHealth);
        viewModel.getLatestCheck().observe(this, this::bindLatestCheck);
        viewModel.getPhotoErrors().observe(this, count -> {
            if (count != null && count > shownPhotoErrors) {
                shownPhotoErrors = count;
                Snackbar.make(findViewById(R.id.plantDetailContent), R.string.plant_photo_failed, Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.load(plantId);
    }

    /** Overview / Care plan / Journal; each tab shows one pane. */
    private void setUpTabs() {
        TabLayout tabs = findViewById(R.id.plantTabs);
        int[] labels = {R.string.plant_tab_overview, R.string.plant_tab_plan, R.string.plant_tab_log};
        for (int label : labels) tabs.addTab(tabs.newTab().setText(label));
        int[] panes = {R.id.plantPaneOverview, R.id.plantPanePlan, R.id.plantPaneLog};
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                for (int i = 0; i < panes.length; i++) {
                    findViewById(panes[i]).setVisibility(i == tab.getPosition() ? View.VISIBLE : View.GONE);
                }
                View pane = findViewById(panes[tab.getPosition()]);
                if (Motion.enabled(PlantDetailActivity.this)) {
                    pane.setAlpha(0f);
                    pane.animate().alpha(1f).setDuration(200).start();
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });
    }

    /** Plant Doctor's latest verdict: the tag by the name and the "Health" column. */
    private void bindHealth(Boolean healthy) {
        shownHealth = healthy;
        TextView tag = findViewById(R.id.plantHealth);
        tag.setVisibility(healthy == null ? View.GONE : View.VISIBLE);
        if (healthy != null) {
            tag.setText(healthy ? R.string.plant_health_ok : R.string.plant_health_check);
            tag.setBackgroundResource(healthy ? R.drawable.bg_tag_soft : R.drawable.bg_tag_ink);
            tag.setTextColor(ContextCompat.getColor(this, healthy ? R.color.color_primary : R.color.color_background));
        }
        text(R.id.plantHealthValue, getString(healthy == null ? R.string.plant_health_unknown
                : healthy ? R.string.plant_health_good : R.string.plant_health_needs));
    }

    private void bindLatestCheck(@Nullable DoctorHistoryEntity check) {
        findViewById(R.id.plantLastCheckSection).setVisibility(check == null ? View.GONE : View.VISIBLE);
        if (check == null) return;
        String title = Boolean.TRUE.equals(check.getHealthy()) ? getString(R.string.plant_check_healthy) : check.getDiagnosis();
        text(R.id.plantLastCheckTitle, title.isEmpty() ? getString(R.string.plant_health_check) : title);
        String when = DateUtils.formatDateTime(this, check.getCreatedAt(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE);
        text(R.id.plantLastCheckSub, when);
        LocalImageLoader.load(findViewById(R.id.plantLastCheckPhoto), check.getImageUri(), dp(56), null);
        findViewById(R.id.plantLastCheck).setOnClickListener(v -> startActivity(new Intent(this, DoctorHistoryActivity.class)));
    }

    /** "Add photo" first, then the plant's photos newest first, the cover photo last. */
    private void bindPhotos() {
        LinearLayout strip = findViewById(R.id.plantPhotos);
        while (strip.getChildCount() > 1) strip.removeViewAt(1);
        SimpleDateFormat month = new SimpleDateFormat("'" + getString(R.string.plant_month_prefix) + "' M/yyyy", Locale.getDefault());
        int px = dp(112);
        if (shownPhotos != null) for (PlantPhotoEntity photo : shownPhotos) {
            strip.addView(photoTile(strip, photo.getImageUri(), month.format(new Date(photo.getCreatedAt())), px));
        }
        if (shownPlant != null && !shownPlant.getImageUri().isEmpty()) {
            strip.addView(photoTile(strip, shownPlant.getImageUri(), month.format(new Date(shownPlant.getCreatedAt())), px));
        }
    }

    private View photoTile(ViewGroup parent, String uri, String label, int px) {
        View tile = getLayoutInflater().inflate(R.layout.item_plant_photo, parent, false);
        LocalImageLoader.load(tile.findViewById(R.id.plantPhotoImage), uri, px, null);
        ((TextView) tile.findViewById(R.id.plantPhotoDate)).setText(label);
        return tile;
    }

    /** Care conditions (light, place, watering) and the care plan (watering, feeding, reminders). */
    private void bindPlan(PlantEntity plant) {
        ViewGroup conditions = findViewById(R.id.plantPlan);
        conditions.removeAllViews();
        LightLevel light = LightLevel.fromName(plant.getLight());
        row(conditions, R.drawable.ic_sun, R.string.plant_cond_light, null,
                light == null ? getString(R.string.plant_cond_unknown) : getString(ListingDetailLabels.of(light)));
        row(conditions, R.drawable.ic_nav_home, R.string.plant_cond_place, null,
                plant.getRoom().isEmpty() ? getString(R.string.garden_area_unset) : plant.getRoom());
        row(conditions, R.drawable.ic_water_drop, R.string.plant_cond_water, null, waterPlan(plant));

        ViewGroup schedule = findViewById(R.id.plantSchedule);
        schedule.removeAllViews();
        row(schedule, R.drawable.ic_water_drop, R.string.plant_schedule_water, waterPlan(plant),
                getString(plant.isReminderEnabled() ? R.string.plant_reminders_on : R.string.plant_reminders_off));
        Long fed = lastOf(CareType.FERTILIZE);
        row(schedule, R.drawable.ic_leaf, R.string.plant_schedule_feed,
                fed == null ? getString(R.string.plant_feed_never)
                        : getString(R.string.plant_schedule_last, CareLogAdapter.daysAgo(this, fed, System.currentTimeMillis())), "");
    }

    private String waterPlan(PlantEntity plant) {
        List<Integer> times = GardenRepository.parseTimes(plant.getWateringTimes());
        if (!plant.isReminderEnabled() && plant.getWateringIntervalDays() < 1) return getString(R.string.plant_plan_reminders_off);
        if (!times.isEmpty()) {
            List<String> clocks = new ArrayList<>();
            Calendar c = Calendar.getInstance();
            for (int minute : times) {
                c.set(Calendar.HOUR_OF_DAY, minute / 60);
                c.set(Calendar.MINUTE, minute % 60);
                clocks.add(DateFormat.getTimeFormat(this).format(c.getTime()));
            }
            return getString(R.string.plant_plan_daily, String.join(", ", clocks));
        }
        return getResources().getQuantityString(R.plurals.plant_plan_every,
                plant.getWateringIntervalDays(), plant.getWateringIntervalDays());
    }

    private void row(ViewGroup parent, int icon, int label, @Nullable String sub, String value) {
        View row = getLayoutInflater().inflate(R.layout.item_plant_condition, parent, false);
        if (parent.getChildCount() > 0) row.setBackgroundResource(R.drawable.bg_top_hairline_panel);
        ((ImageView) row.findViewById(R.id.conditionIcon)).setImageResource(icon);
        ((TextView) row.findViewById(R.id.conditionLabel)).setText(label);
        TextView subView = row.findViewById(R.id.conditionSub);
        subView.setVisibility(sub == null || sub.isEmpty() ? View.GONE : View.VISIBLE);
        subView.setText(sub);
        ((TextView) row.findViewById(R.id.conditionValue)).setText(value);
        row.setContentDescription(getString(label) + ", " + (sub == null ? "" : sub + ", ") + value);
        parent.addView(row);
    }

    @Nullable
    private Long lastOf(CareType type) {
        Long last = null;
        if (shownLogs != null) for (CareLogEntity log : shownLogs) {
            if (log.getType() == type && (last == null || log.getCreatedAt() > last)) last = log.getCreatedAt();
        }
        return last;
    }

    /** Photo settles, the sheet slides up and the sections rise. */
    private void playEntrance() {
        Motion.zoom(findViewById(R.id.plantDetailPhotoFrame));
        Motion.sheet(findViewById(R.id.plantDetailSheet), 640, 200);
        int[] ids = {R.id.plantTabs, R.id.plantDetailStats, R.id.plantHeatmapSection, R.id.plantPlan};
        for (int i = 0; i < ids.length; i++) Motion.rise(findViewById(ids[i]), 320 + i * 60L);
        Motion.rise(findViewById(R.id.plantDetailBottom), 420);
    }

    /** One "Log care" button: pick what was done, then add an optional note. */
    private void chooseCareType() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_care_type, null, false);
        LinearLayout rows = sheet.findViewById(R.id.careTypeRows);
        int[] hints = {R.string.care_hint_water, R.string.care_hint_fertilize, R.string.care_hint_repot,
                R.string.care_hint_prune, R.string.care_hint_note};
        for (int i = 0; i < LOG_TYPES.length; i++) {
            CareType type = LOG_TYPES[i];
            View row = getLayoutInflater().inflate(R.layout.item_care_type, rows, false);
            int[] colors = CareLogAdapter.colors(type);
            row.findViewById(R.id.careTypeTile).setBackgroundTintList(ContextCompat.getColorStateList(this, colors[0]));
            ImageView icon = row.findViewById(R.id.careTypeIcon);
            icon.setImageResource(CareLogAdapter.icon(type));
            icon.setImageTintList(ContextCompat.getColorStateList(this, colors[1]));
            ((TextView) row.findViewById(R.id.careTypeName)).setText(CareLogAdapter.label(type));
            ((TextView) row.findViewById(R.id.careTypeHint)).setText(hints[i]);
            row.setOnClickListener(v -> {
                dialog.dismiss();
                showCareSheet(type);
            });
            rows.addView(row);
        }
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void showCareSheet(CareType type) {
        AddCareLogBottomSheet sheet = AddCareLogBottomSheet.newInstance(type);
        activeSheet = sheet;
        sheet.show(getSupportFragmentManager(), "care-entry");
    }

    private void bindHistoryToggle() {
        TextView toggle = findViewById(R.id.plantHistoryToggle);
        toggle.setVisibility(careLogAdapter.getTotalCount() > CareLogAdapter.COLLAPSED ? View.VISIBLE : View.GONE);
        toggle.setText(careLogAdapter.isExpanded() ? R.string.plant_show_less : R.string.plant_show_all);
    }

    private void bind(PlantEntity plant) {
        shownPlant = plant;
        ImageView image = findViewById(R.id.plantDetailImage);
        int placeholderPadding = getResources().getDimensionPixelSize(R.dimen.spacing_xl);
        image.setPadding(placeholderPadding, placeholderPadding, placeholderPadding, placeholderPadding);
        image.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        image.setImageResource(R.drawable.ic_plant_sprout);
        LocalImageLoader.load(image, plant.getImageUri(), getResources().getDisplayMetrics().widthPixels, () -> {
            image.setPadding(0, 0, 0, 0);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        });
        text(R.id.plantDetailNickname, plant.getNickname());
        TextView species = findViewById(R.id.plantDetailSpecies);
        species.setText(plant.getSpecies());
        species.setVisibility(plant.getSpecies().isEmpty() ? View.GONE : View.VISIBLE);

        TextView room = findViewById(R.id.plantRoomChip);
        room.setText(plant.getRoom());
        room.setVisibility(plant.getRoom().isEmpty() ? View.GONE : View.VISIBLE);
        TextView age = findViewById(R.id.plantAgeChip);
        long days = Math.max(0, (System.currentTimeMillis() - plant.getCreatedAt()) / 86_400_000L);
        age.setVisibility(plant.getCreatedAt() > 0 ? View.VISIBLE : View.GONE);
        age.setText(days >= 30 ? getResources().getQuantityString(R.plurals.plant_with_you_months, (int) (days / 30), (int) (days / 30))
                : days == 0 ? getString(R.string.plant_with_you_today)
                : getResources().getQuantityString(R.plurals.plant_with_you_days, (int) days, (int) days));
        bindPhotos();
        bindStats();
    }

    /** Next watering, last feeding, the pills and the 30-day care map, from the plant and its own logs. */
    private void bindStats() {
        PlantEntity plant = shownPlant;
        if (plant == null) return;
        long now = System.currentTimeMillis();
        WateringStatus status = WateringStatus.of(plant, now);
        boolean due = status.kind == WateringStatus.Kind.DUE;
        String next = due ? getString(R.string.plant_value_today)
                : status.kind != WateringStatus.Kind.UPCOMING ? getString(R.string.plant_value_off)
                : status.daily ? WateringStatus.dailyLabel(this, status.nextAt, now)
                : status.daysUntil <= 1 ? getString(R.string.plant_value_tomorrow)
                : getResources().getQuantityString(R.plurals.plant_next_in, status.daysUntil, status.daysUntil);
        TextView nextValue = findViewById(R.id.plantNextValue);
        nextValue.setText(next);
        nextValue.setTextColor(ContextCompat.getColor(this, due ? R.color.color_primary : R.color.color_text_primary));
        findViewById(R.id.plantNextTile).setContentDescription(getString(R.string.plant_stat_water) + ", " + next);
        TextView waterChip = findViewById(R.id.plantWaterChip);
        boolean scheduled = due || status.kind == WateringStatus.Kind.UPCOMING;
        waterChip.setVisibility(scheduled ? View.VISIBLE : View.GONE);
        waterChip.setText(due ? getString(R.string.garden_tag_water) : next);

        Long fed = lastOf(CareType.FERTILIZE);
        String feed = fed == null ? getString(R.string.plant_feed_never) : CareLogAdapter.daysAgo(this, fed, now);
        text(R.id.plantFeedValue, feed);
        findViewById(R.id.plantFeedTile).setContentDescription(getString(R.string.plant_stat_feed) + ", " + feed);
        if (shownHealth == null) text(R.id.plantHealthValue, getString(R.string.plant_health_unknown));

        int[] days = CareHeatmapView.fromLogs(shownLogs, now);
        CareHeatmapView heatmap = findViewById(R.id.plantHeatmap);
        heatmap.setDays(days);
        heatmap.setOnDayClickListener(index -> showHeatmapDay(index, System.currentTimeMillis()));
        int cared = 0;
        for (int kind : days) if (kind != CareHeatmapView.NONE) cared++;
        text(R.id.plantHeatmapCaption, getResources().getQuantityString(R.plurals.plant_care_times, cared, cared));
        bindPlan(plant);
    }

    /** Care logged on the tapped heatmap day (index 29 = today), newest last, with an icon per kind. */
    private void showHeatmapDay(int index, long now) {
        java.util.Calendar day = java.util.Calendar.getInstance();
        day.setTimeInMillis(now);
        day.add(java.util.Calendar.DAY_OF_MONTH, index - (CareHeatmapView.DAYS - 1));
        List<CareLogEntity> logs = new java.util.ArrayList<>();
        if (shownLogs != null) for (CareLogEntity log : shownLogs) {
            if (log.getType() != CareType.NOTE
                    && CareLogAdapter.calendarDaysBetween(log.getCreatedAt(), day.getTimeInMillis()) == 0) logs.add(log);
        }
        logs.sort((a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
        String date = new java.text.SimpleDateFormat("EEEE d/M", java.util.Locale.getDefault()).format(day.getTime());
        date = date.substring(0, 1).toUpperCase(java.util.Locale.getDefault()) + date.substring(1);
        findViewById(R.id.plantHeatmapDay).setVisibility(View.VISIBLE);
        text(R.id.plantHeatmapDayTitle, logs.isEmpty() ? getString(R.string.plant_heatmap_day_empty, date)
                : getResources().getQuantityString(R.plurals.plant_heatmap_day_title, logs.size(), date, logs.size()));
        android.widget.LinearLayout list = findViewById(R.id.plantHeatmapDayList);
        list.removeAllViews();
        for (CareLogEntity log : logs) {
            TextView row = new TextView(this);
            row.setText(getString(CareLogAdapter.label(log.getType())) + " · "
                    + android.text.format.DateFormat.getTimeFormat(this).format(new java.util.Date(log.getCreatedAt())));
            row.setTextAppearance(R.style.TextAppearance_PlantMate_Body);
            row.setTextSize(14);
            row.setMinHeight(dp(40));
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setCompoundDrawablePadding(dp(10));
            int swatch = log.getType() == CareType.WATER ? R.drawable.bg_legend_water
                    : log.getType() == CareType.FERTILIZE ? R.drawable.bg_legend_feed : R.drawable.bg_legend_other;
            row.setCompoundDrawablesRelativeWithIntrinsicBounds(swatch, 0, 0, 0);
            list.addView(row);
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }
}
