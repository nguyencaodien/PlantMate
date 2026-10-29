package com.example.ungdungchiasecaycanh.ui.doctor;

import android.widget.CheckBox;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.ui.garden.AddPlantActivity;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailActivity;
import java.util.List;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.repository.DoctorRepository;
import com.example.ungdungchiasecaycanh.ui.ai.MamChatActivity;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * One saved Plant Doctor result, reloaded from Room by its history ID. Mầm states the answer first,
 * in a pose that matches it (happy, worried or unsure), then the plant / condition card and care steps.
 */
public class DoctorResultActivity extends AppCompatActivity {
    public static final String EXTRA_HISTORY_ID = "extra_doctor_history_id";
    private boolean animated;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_result);
        long historyId = getIntent().getLongExtra(EXTRA_HISTORY_ID, -1);
        if (historyId <= 0) { finish(); return; }
        SecondaryChrome.apply(this, findViewById(R.id.doctorResultRoot), findViewById(R.id.doctorResultHero));
        findViewById(R.id.doctorResultBack).setOnClickListener(v -> finish());
        findViewById(R.id.doctorResultErrorBack).setOnClickListener(v -> finish());
        findViewById(R.id.doctorResultHistory).setOnClickListener(v ->
                startActivity(new Intent(this, DoctorHistoryActivity.class)));
        findViewById(R.id.doctorResultAskMam).setOnClickListener(v ->
                startActivity(new Intent(this, MamChatActivity.class)));
        DoctorViewModel viewModel = new ViewModelProvider(this).get(DoctorViewModel.class);
        findViewById(R.id.doctorResultRetry).setOnClickListener(v -> viewModel.retryHistory());
        viewModel.getDetailLoading().observe(this, loading ->
                findViewById(R.id.doctorResultLoading).setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE));
        viewModel.getDetail().observe(this, detail -> {
            bind(detail);
            if (detail != null) viewModel.loadGivers(detail.history.getPlantName(), detail.history.getScientificName());
        });
        viewModel.getGivers().observe(this, this::bindGivers);
        viewModel.getDetailMissing().observe(this, missing -> {
            if (Boolean.TRUE.equals(missing)) showFailure(R.string.doctor_history_unavailable);
        });
        viewModel.getDetailError().observe(this, error -> {
            if (Boolean.TRUE.equals(error)) showFailure(R.string.doctor_history_load_error);
        });
        findViewById(R.id.doctorResultFeedbackYes).setOnClickListener(v -> viewModel.submitFeedback(true, "", false));
        findViewById(R.id.doctorResultFeedbackNo).setOnClickListener(v -> askWhatItWas(viewModel));
        viewModel.getFeedback().observe(this, status -> {
            boolean sending = status == DoctorViewModel.FeedbackStatus.SENDING;
            findViewById(R.id.doctorResultFeedbackYes).setEnabled(!sending);
            findViewById(R.id.doctorResultFeedbackNo).setEnabled(!sending);
            if (status == DoctorViewModel.FeedbackStatus.SENT) showFeedbackDone();
            if (status == DoctorViewModel.FeedbackStatus.ERROR) {
                Snackbar.make(findViewById(R.id.doctorResultRoot), R.string.doctor_feedback_error, Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.loadHistory(historyId);
    }

    /** "Not right": what it really was (optional) and whether to share the photo for training. */
    private void askWhatItWas(DoctorViewModel viewModel) {
        View form = getLayoutInflater().inflate(R.layout.dialog_doctor_feedback, null);
        TextView correction = form.findViewById(R.id.doctorFeedbackCorrection);
        CheckBox share = form.findViewById(R.id.doctorFeedbackSharePhoto);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.doctor_feedback_dialog_title)
                .setView(form)
                .setPositiveButton(R.string.doctor_feedback_send, (dialog, which) ->
                        viewModel.submitFeedback(false, correction.getText().toString(), share.isChecked()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showFeedbackDone() {
        findViewById(R.id.doctorResultFeedbackButtons).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.doctorResultFeedbackBody)).setText(R.string.doctor_feedback_thanks);
    }

    private void bind(DoctorRepository.HistoryDetail detail) {
        if (detail == null) return;
        findViewById(R.id.doctorResultContent).setVisibility(View.VISIBLE);
        findViewById(R.id.doctorResultError).setVisibility(View.GONE);
        DoctorHistoryEntity history = detail.history;

        String name = history.getPlantName();
        text(R.id.doctorResultPlantName, name == null ? getString(R.string.doctor_result_unknown_plant) : name);
        TextView scientific = findViewById(R.id.doctorResultScientific);
        scientific.setVisibility(history.getScientificName() == null ? View.GONE : View.VISIBLE);
        scientific.setText(history.getScientificName());
        text(R.id.doctorResultDate, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(history.getCreatedAt())));

        ImageView image = findViewById(R.id.doctorResultImage);
        try {
            image.setImageURI(Uri.parse(history.getImageUri()));
            if (image.getDrawable() == null) image.setImageResource(R.drawable.ic_botanical_placeholder);
        } catch (Exception ignored) {
            image.setImageResource(R.drawable.ic_botanical_placeholder);
        }
        bindHealth(history.getHealthy());
        bindMam(name, history.getHealthy(), history.getConfidence());
        if (history.getFeedback() != null) showFeedbackDone();

        text(R.id.doctorResultDiagnosis, history.getDiagnosis());
        Double confidence = history.getConfidence();
        findViewById(R.id.doctorResultConfidenceRow).setVisibility(confidence == null ? View.GONE : View.VISIBLE);
        if (confidence != null) {
            int percent = (int) Math.round(confidence * 100d);
            text(R.id.doctorResultConfidence, String.format(Locale.getDefault(), "%d%%", percent));
            ((LinearProgressIndicator) findViewById(R.id.doctorResultConfidenceBar)).setProgressCompat(percent, !animated);
        }
        PlantEntity plant = detail.plant;
        TextView linked = findViewById(R.id.doctorResultPlant);
        linked.setVisibility(plant == null ? View.GONE : View.VISIBLE);
        if (plant != null) linked.setText(getString(R.string.doctor_linked_plant, plant.getNickname()));

        LinearLayout steps = findViewById(R.id.doctorResultSteps);
        steps.removeAllViews();
        // Ticked steps live in the ViewModel, so rotating the screen keeps them (BUG-016).
        java.util.Set<Integer> done = new ViewModelProvider(this).get(DoctorViewModel.class).doneSteps;
        for (String line : history.getGuidance().split("\n")) {
            String step = line.trim();
            if (step.isEmpty()) continue;
            View row = getLayoutInflater().inflate(R.layout.item_treatment_check, steps, false);
            int index = steps.getChildCount();
            if (index == 0) row.setBackground(null);
            CheckBox box = row.findViewById(R.id.treatmentCheck);
            box.setText(step);
            // Every row's box shares one ID, so Android's own state restore would copy one box onto all of
            // them after rotation; the ViewModel is the only source (BUG-016).
            box.setSaveEnabled(false);
            box.setChecked(done.contains(index));
            row.findViewById(R.id.treatmentDone).setVisibility(box.isChecked() ? View.VISIBLE : View.GONE);
            box.setOnCheckedChangeListener((b, checked) -> {
                if (checked) done.add(index); else done.remove(index);
                row.findViewById(R.id.treatmentDone).setVisibility(checked ? View.VISIBLE : View.GONE);
                bindStepsDone(steps);
            });
            steps.addView(row);
        }
        bindStepsDone(steps);
        View add = findViewById(R.id.doctorResultAddPlant);
        add.setVisibility(plant == null && name != null ? View.VISIBLE : View.GONE);
        add.setOnClickListener(v -> startActivity(new Intent(this, AddPlantActivity.class)
                .putExtra(AddPlantActivity.EXTRA_NICKNAME, name)
                .putExtra(AddPlantActivity.EXTRA_SPECIES, history.getScientificName())));
        findViewById(R.id.doctorResultShare).setOnClickListener(v -> {
            Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,
                    getString(R.string.doctor_result_share_text, name == null ? "" : name, history.getDiagnosis()));
            startActivity(Intent.createChooser(send, getString(R.string.doctor_result_share)));
        });

        if (!animated) {
            animated = true;
            Motion.rise(findViewById(R.id.doctorResultPhotoCard), 60);
            Motion.rise(findViewById(R.id.doctorResultMamRow), 140);
            Motion.rise(findViewById(R.id.doctorResultDiagnosisCard), 220);
            Motion.rise(findViewById(R.id.doctorResultCareCard), 300);
        }
    }

    /** "N / M done" above the care steps. */
    private void bindStepsDone(LinearLayout steps) {
        int done = 0;
        for (int i = 0; i < steps.getChildCount(); i++) {
            if (((CheckBox) steps.getChildAt(i).findViewById(R.id.treatmentCheck)).isChecked()) done++;
        }
        TextView count = findViewById(R.id.doctorResultStepsDone);
        count.setVisibility(steps.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        count.setText(getString(R.string.doctor_result_steps_done, done, steps.getChildCount()));
    }

    /** "3 neighbours are giving away …": opens the first matching listing. */
    private void bindGivers(List<Listing> givers) {
        View card = findViewById(R.id.doctorResultGivers);
        card.setVisibility(givers == null || givers.isEmpty() ? View.GONE : View.VISIBLE);
        if (givers == null || givers.isEmpty()) return;
        DoctorRepository.HistoryDetail d = new ViewModelProvider(this).get(DoctorViewModel.class).getDetail().getValue();
        String name = d == null || d.history.getPlantName() == null ? "" : d.history.getPlantName();
        ((TextView) findViewById(R.id.doctorResultGiversText)).setText(getResources().getQuantityString(
                R.plurals.doctor_result_givers, givers.size(), givers.size(), name));
        card.setOnClickListener(v -> startActivity(new Intent(this, ListingDetailActivity.class)
                .putExtra(ListingDetailActivity.EXTRA_LISTING_ID, givers.get(0).getListingId())));
    }

    /** Below this confidence Mầm says she is not sure. */
    static final double SURE_ENOUGH = 0.6;

    /** Mầm's pose for an answer: unsure when confidence is low, else happy or worried by health. */
    static int mamPose(Boolean healthy, Double confidence) {
        if (confidence != null && confidence < SURE_ENOUGH) return R.drawable.ic_mam_unsure;
        if (healthy == null) return R.drawable.ic_mam_look;
        return healthy ? R.drawable.ic_mam_happy : R.drawable.ic_mam_worry;
    }

    /** Mầm's plain one-line answer, built only from what the result actually says. */
    private void bindMam(String name, Boolean healthy, Double confidence) {
        ((ImageView) findViewById(R.id.doctorResultMam)).setImageResource(mamPose(healthy, confidence));
        StringBuilder says = new StringBuilder(name == null ? getString(R.string.doctor_mam_says_unknown)
                : getString(R.string.doctor_mam_says_known, name));
        if (healthy != null) says.append(' ').append(getString(healthy
                ? R.string.doctor_mam_says_healthy : R.string.doctor_mam_says_care));
        if (confidence != null && confidence < SURE_ENOUGH) says.append(' ').append(getString(R.string.doctor_mam_says_unsure));
        text(R.id.doctorResultMamSays, says.toString());
    }

    /** Condition flag: a coloured dot and words (green healthy, amber needs care), never colour alone. */
    private void bindHealth(Boolean healthy) {
        TextView flag = findViewById(R.id.doctorResultHealth);
        flag.setVisibility(healthy == null ? View.GONE : View.VISIBLE);
        if (healthy == null) return;
        flag.setText(healthy ? R.string.doctor_health_ok : R.string.doctor_health_needs_care);
        int ink = ContextCompat.getColor(this, healthy ? R.color.color_primary : R.color.status_reserved_ink);
        int dotColor = ContextCompat.getColor(this, healthy ? R.color.color_primary : R.color.status_reserved_dot);
        flag.setTextColor(ink);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(dotColor);
        int size = (int) (8 * getResources().getDisplayMetrics().density);
        dot.setSize(size, size);
        flag.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null);
    }

    private void showFailure(int message) {
        findViewById(R.id.doctorResultContent).setVisibility(View.GONE);
        findViewById(R.id.doctorResultError).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.doctorResultErrorText)).setText(message);
    }

    private void text(int id, String value) { ((TextView) findViewById(id)).setText(value); }
}
