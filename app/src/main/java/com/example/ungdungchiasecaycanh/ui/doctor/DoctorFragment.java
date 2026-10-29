package com.example.ungdungchiasecaycanh.ui.doctor;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.remote.DoctorAiException;
import com.example.ungdungchiasecaycanh.ui.ai.MamAdvisor;
import com.example.ungdungchiasecaycanh.ui.ai.MamChatActivity;
import com.example.ungdungchiasecaycanh.util.PhotoQuality;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DoctorFragment extends Fragment {
    /** Sent by the raised Doctor button: open the camera as soon as this tab is showing. */
    public static final String REQUEST_OPEN_CAMERA = "doctor_open_camera";

    private DoctorViewModel viewModel;
    private ImageView preview;
    private MaterialButton analyze;
    private TextView stateText;
    private Spinner plantSpinner;
    private final List<PlantEntity> plants = new ArrayList<>();
    private final ActivityResultLauncher<PickVisualMediaRequest> picker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), this::onImageSelected);
    /** In-app camera; returns the photo (or a library pick) as the result data. */
    private final ActivityResultLauncher<Intent> scanner = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null
                        && result.getData().getData() != null) {
                    viewModel.selectImage(result.getData().getData().toString());
                }
            });
    private ObjectAnimator scanAnimator;

    public DoctorFragment() { super(R.layout.fragment_doctor); }

    private MamAdvisor.Symptom shownSymptom;

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preview = view.findViewById(R.id.doctorImagePreview);
        analyze = view.findViewById(R.id.doctorAnalyze);
        stateText = view.findViewById(R.id.doctorStateText);
        plantSpinner = view.findViewById(R.id.doctorPlantSpinner);
        viewModel = new ViewModelProvider(this).get(DoctorViewModel.class);

        view.findViewById(R.id.doctorChooseImage).setOnClickListener(v -> picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        view.findViewById(R.id.doctorTakePhoto).setOnClickListener(v -> takePhoto());
        getParentFragmentManager().setFragmentResultListener(REQUEST_OPEN_CAMERA, getViewLifecycleOwner(), (key, result) -> takePhoto());
        view.findViewById(R.id.doctorScanFrame).setOnClickListener(v -> takePhoto());
        Motion.rise(view.findViewById(R.id.doctorAiCard), 100);
        view.findViewById(R.id.doctorRecentAll).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DoctorHistoryActivity.class)));
        viewModel.getHistory().observe(getViewLifecycleOwner(), this::bindRecent);
        view.findViewById(R.id.doctorClearImage).setOnClickListener(v -> viewModel.selectImage(null));
        view.findViewById(R.id.doctorViewHistory).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DoctorHistoryActivity.class)));
        analyze.setOnClickListener(v -> viewModel.analyze());
        View hero = view.findViewById(R.id.doctorHero);
        Motion.start(view.findViewById(R.id.doctorMam));
        Motion.rise(((ViewGroup) hero).getChildAt(0), 60);
        Motion.rise(view.findViewById(R.id.doctorSymptomTitle), 120);
        Motion.rise(view.findViewById(R.id.doctorSymptoms), 160);
        Motion.rise(view.findViewById(R.id.doctorGuidanceCard), 260);
        view.findViewById(R.id.doctorAskMam).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), MamChatActivity.class)));
        ChipGroup symptoms = view.findViewById(R.id.doctorSymptoms);
        for (MamAdvisor.Symptom symptom : MamAdvisor.Symptom.values()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_choice_chip, symptoms, false);
            chip.setId(View.generateViewId());
            chip.setText(MamAdvisor.symptomLabel(symptom));
            chip.setTag(symptom);
            chip.setOnClickListener(v -> viewModel.selectSymptom(symptom));
            symptoms.addView(chip);
        }
        viewModel.getSymptom().observe(getViewLifecycleOwner(), symptom -> renderSymptom(view, symptom));

        viewModel.getPlants().observe(getViewLifecycleOwner(), this::bindPlants);
        plantSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                viewModel.selectPlant(position == 0 ? null : plants.get(position - 1).getId());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { viewModel.selectPlant(null); }
        });
        viewModel.getAnalysis().observe(getViewLifecycleOwner(), this::render);
    }

    /** Checks the picked symptom chip and shows Mầm's fixed guidance for it. */
    private void renderSymptom(View view, MamAdvisor.Symptom selected) {
        if (shownSymptom != null && shownSymptom != selected) {
            Motion.swap(view.findViewById(R.id.doctorGuidanceCard));
        }
        shownSymptom = selected;
        ChipGroup symptoms = view.findViewById(R.id.doctorSymptoms);
        for (int i = 0; i < symptoms.getChildCount(); i++) {
            Chip chip = (Chip) symptoms.getChildAt(i);
            chip.setChecked(chip.getTag() == selected);
        }
        MamAdvisor.Guidance g = MamAdvisor.forSymptom(selected);
        ((TextView) view.findViewById(R.id.doctorGuidanceBadge)).setText(getString(
                R.string.doctor_care_steps_for, MamAdvisor.symptomLabel(selected).toLowerCase(Locale.ROOT)));
        ((TextView) view.findViewById(R.id.doctorGuidanceTitle)).setText(g.title);
        ((TextView) view.findViewById(R.id.doctorGuidanceWatch)).setText(g.watchFor);
        LinearLayout steps = view.findViewById(R.id.doctorGuidanceSteps);
        steps.removeAllViews();
        for (int i = 0; i < g.steps.size(); i++) {
            View row = getLayoutInflater().inflate(R.layout.item_mam_step, steps, false);
            ((TextView) row.findViewById(R.id.mam_step_number)).setText(String.valueOf(i + 1));
            ((TextView) row.findViewById(R.id.mam_step_text)).setText(g.steps.get(i));
            steps.addView(row);
        }
    }

    /** The two latest checks: photo, what was found, the plant and when; tap opens the result. */
    private void bindRecent(List<DoctorHistoryEntity> history) {
        View root = getView();
        if (root == null) return;
        LinearLayout list = root.findViewById(R.id.doctorRecent);
        list.removeAllViews();
        int count = history == null ? 0 : Math.min(2, history.size());
        root.findViewById(R.id.doctorRecentSection).setVisibility(count == 0 ? View.GONE : View.VISIBLE);
        for (int i = 0; i < count; i++) {
            DoctorHistoryEntity item = history.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_doctor_recent, list, false);
            if (i > 0) row.setBackgroundResource(R.drawable.bg_top_hairline_transparent);
            com.example.ungdungchiasecaycanh.util.LocalImageLoader.load(row.findViewById(R.id.recentPhoto),
                    item.getImageUri(), (int) (52 * getResources().getDisplayMetrics().density), null);
            String found = Boolean.TRUE.equals(item.getHealthy()) ? getString(R.string.doctor_recent_healthy) : item.getDiagnosis();
            ((TextView) row.findViewById(R.id.recentTitle)).setText(found);
            String plant = plantName(item.getPlantId());
            String when = android.text.format.DateUtils.formatDateTime(requireContext(), item.getCreatedAt(),
                    android.text.format.DateUtils.FORMAT_SHOW_DATE | android.text.format.DateUtils.FORMAT_NUMERIC_DATE);
            ((TextView) row.findViewById(R.id.recentSub)).setText(plant.isEmpty() ? when
                    : getString(R.string.doctor_recent_sub, plant, when));
            row.setOnClickListener(v -> startActivity(new Intent(requireContext(), DoctorResultActivity.class)
                    .putExtra(DoctorResultActivity.EXTRA_HISTORY_ID, item.getId())));
            list.addView(row);
        }
    }

    private String plantName(@Nullable Long plantId) {
        if (plantId == null) return "";
        for (PlantEntity p : plants) if (p.getId() == plantId) return p.getNickname();
        return "";
    }

    /** Opens Plant Doctor's camera (live framing and photo checks). */
    private void takePhoto() {
        scanner.launch(new Intent(requireContext(), DoctorScanActivity.class));
    }

    private void onImageSelected(Uri uri) {
        if (uri == null) return;
        try {
            // Keeps the photo readable for Doctor history after a restart where the picker allows it.
            requireContext().getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Readable for this session; analysis works, an old history thumbnail may not reload.
        }
        viewModel.selectImage(uri.toString());
    }

    private void bindPlants(List<PlantEntity> values) {
        plants.clear(); if (values != null) plants.addAll(values);
        List<String> labels = new ArrayList<>(); labels.add(getString(R.string.doctor_no_linked_plant));
        for (PlantEntity plant : plants) labels.add(plant.getSpecies().isEmpty()
                ? plant.getNickname() : plant.getNickname() + " · " + plant.getSpecies());
        plantSpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, labels));
    }

    private void render(DoctorViewModel.AnalysisState state) {
        View root = requireView();
        boolean hasImage = state.imageUri != null;
        boolean analyzing = state.status == DoctorViewModel.AnalysisStatus.ANALYZING;
        boolean readable = true;
        if (hasImage) {
            try {
                preview.setImageURI(null);
                preview.setImageURI(Uri.parse(state.imageUri));
                readable = preview.getDrawable() != null;
            } catch (Exception ignored) {
                readable = false;
            }
        }
        preview.setVisibility(hasImage && readable ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.doctorScanEmpty).setVisibility(hasImage && readable ? View.GONE : View.VISIBLE);
        root.findViewById(R.id.doctorClearImage).setVisibility(hasImage && !analyzing ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.doctorProgress).setVisibility(analyzing ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.doctorTakePhoto).setEnabled(!analyzing);
        root.findViewById(R.id.doctorChooseImage).setEnabled(!analyzing);
        root.findViewById(R.id.doctorScanFrame).setEnabled(!analyzing);
        plantSpinner.setEnabled(!analyzing);
        setScanning(root.findViewById(R.id.doctorScanLine), analyzing);
        analyze.setEnabled(hasImage && readable && !analyzing && viewModel.hasAuthenticatedUser());
        if (!viewModel.hasAuthenticatedUser()) {
            stateText.setText(R.string.doctor_auth_required);
        } else if (hasImage && !readable) {
            stateText.setText(R.string.doctor_image_unavailable);
        } else {
            switch (state.status) {
                case ANALYZING: stateText.setText(R.string.doctor_analyzing); break;
                case ERROR: stateText.setText(errorMessage(state.errorKind)); break;
                default: stateText.setText(hasImage ? R.string.doctor_ready : R.string.doctor_select_image_prompt);
            }
        }
        if (state.status == DoctorViewModel.AnalysisStatus.QUALITY_WARNING) {
            viewModel.qualityWarningShown();
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(state.quality == PhotoQuality.Result.TOO_DARK
                            ? R.string.doctor_quality_dark_title : R.string.doctor_quality_blurry_title)
                    .setMessage(R.string.doctor_quality_body)
                    .setPositiveButton(R.string.doctor_quality_retake, (dialog, which) -> takePhoto())
                    .setNegativeButton(R.string.doctor_quality_anyway, (dialog, which) -> viewModel.analyze(true))
                    .show();
        }
        if (state.status == DoctorViewModel.AnalysisStatus.RESULT && state.historyId != null) {
            viewModel.resultShown();
            Intent intent = new Intent(requireContext(), DoctorResultActivity.class);
            intent.putExtra(DoctorResultActivity.EXTRA_HISTORY_ID, state.historyId);
            startActivity(intent);
        }
    }

    private static int errorMessage(DoctorAiException.Kind kind) {
        if (kind == null) return R.string.doctor_analysis_error;
        switch (kind) {
            case NOT_A_PLANT: return R.string.doctor_error_not_plant;
            case IMAGE_UNREADABLE: return R.string.doctor_image_unavailable;
            case QUOTA: return R.string.doctor_error_quota;
            case BUSY: return R.string.doctor_error_busy;
            case BLOCKED: return R.string.doctor_error_blocked;
            case NOT_CONFIGURED: return R.string.doctor_error_not_configured;
            case APP_CHECK: return R.string.doctor_error_app_check;
            default: return R.string.doctor_analysis_error;
        }
    }

    /** A green line sweeps over the photo while the AI looks at it. */
    private void setScanning(View line, boolean scanning) {
        if (!scanning) {
            if (scanAnimator != null) scanAnimator.cancel();
            scanAnimator = null;
            line.setVisibility(View.GONE);
            return;
        }
        line.setVisibility(View.VISIBLE);
        if (scanAnimator != null || !Motion.enabled(requireContext())) return;
        View frame = (View) line.getParent();
        frame.post(() -> {
            if (line.getVisibility() != View.VISIBLE || scanAnimator != null) return;
            scanAnimator = ObjectAnimator.ofFloat(line, View.TRANSLATION_Y,
                    0f, frame.getHeight() - line.getHeight());
            scanAnimator.setDuration(1400);
            scanAnimator.setRepeatMode(ValueAnimator.REVERSE);
            scanAnimator.setRepeatCount(ValueAnimator.INFINITE);
            scanAnimator.setInterpolator(Motion.EASE_IN_OUT);
            scanAnimator.start();
        });
    }

    @Override public void onDestroyView() {
        if (scanAnimator != null) scanAnimator.cancel();
        scanAnimator = null;
        super.onDestroyView();
    }
}
