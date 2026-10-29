package com.example.ungdungchiasecaycanh.ui.auth;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

/**
 * Shown once per account on top of MainActivity: pick an area, say what you came for (Home orders
 * its sections by it), and choose whether Mầm sends watering reminders.
 */
public class OnboardingActivity extends AppCompatActivity {
    private static final String[] INTENTS = {SettingsRepository.INTENT_RECEIVE, SettingsRepository.INTENT_GIVE,
            SettingsRepository.INTENT_TRADE, SettingsRepository.INTENT_CARE};
    private static final int[] INTENT_LABELS = {R.string.onboarding_intent_receive, R.string.onboarding_intent_give,
            R.string.onboarding_intent_trade, R.string.onboarding_intent_care};
    private static final int[] INTENT_SUBS = {R.string.onboarding_intent_receive_sub, R.string.onboarding_intent_give_sub,
            R.string.onboarding_intent_trade_sub, R.string.onboarding_intent_care_sub};

    private OnboardingViewModel viewModel;
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> done(granted));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);

        EditText area = findViewById(R.id.onboarding_area);
        viewModel.getArea().observe(this, value -> {
            if (value != null && area.getText().length() == 0) area.setText(value);
        });
        viewModel.getSaveFailed().observe(this, failed -> {
            if (Boolean.TRUE.equals(failed)) Snackbar.make(area, R.string.onboarding_area_failed, Snackbar.LENGTH_LONG).show();
        });

        // Common areas fill the field with one tap; any other area is typed.
        ChipGroup areas = findViewById(R.id.onboarding_area_chips);
        for (String name : getResources().getStringArray(R.array.onboarding_areas)) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, areas, false);
            chip.setId(View.generateViewId());
            chip.setText(name);
            chip.setOnClickListener(v -> {
                if (chip.isChecked()) area.setText(getString(R.string.onboarding_area_city, name));
            });
            areas.addView(chip);
        }

        LinearLayout choices = findViewById(R.id.onboarding_intents);
        for (int i = 0; i < INTENTS.length; i++) {
            View choice = getLayoutInflater().inflate(R.layout.item_onboarding_choice, choices, false);
            ((TextView) choice.findViewById(R.id.choice_title)).setText(INTENT_LABELS[i]);
            ((TextView) choice.findViewById(R.id.choice_sub)).setText(INTENT_SUBS[i]);
            String intent = INTENTS[i];
            choice.setSelected(viewModel.getIntents().contains(intent));
            choice.setOnClickListener(v -> {
                v.setSelected(!v.isSelected());
                viewModel.toggleIntent(intent, v.isSelected());
            });
            choices.addView(choice);
        }

        findViewById(R.id.onboarding_skip).setOnClickListener(v -> done(true));
        findViewById(R.id.onboarding_later).setOnClickListener(v -> done(false));
        findViewById(R.id.onboarding_back).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        findViewById(R.id.onboarding_next).setOnClickListener(v -> {
            Integer step = viewModel.getStep().getValue();
            if (step == null || step == 0) viewModel.saveArea(area.getText().toString());
            else if (step == 1) viewModel.next();
            else askForReminders();
        });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                Integer step = viewModel.getStep().getValue();
                if (step != null && step > 0) viewModel.back();
                else done(true);
            }
        });
        viewModel.getStep().observe(this, this::render);
    }

    private void render(int step) {
        findViewById(R.id.onboarding_step_area).setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.onboarding_step_intents).setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.onboarding_step_reminders).setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        findViewById(R.id.onboarding_later).setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        LinearProgressIndicator progress = findViewById(R.id.onboarding_progress);
        progress.setProgressCompat((step + 1) * 100 / OnboardingViewModel.STEPS, true);
        progress.setContentDescription(getString(R.string.onboarding_progress_desc, step + 1, OnboardingViewModel.STEPS));
        findViewById(R.id.onboarding_back).setVisibility(step > 0 ? View.VISIBLE : View.GONE);
        ((MaterialButton) findViewById(R.id.onboarding_next)).setText(
                step == 2 ? R.string.onboarding_reminders_on : R.string.onboarding_next);
    }

    /** Android 13+ asks for the notification permission; reminders stay off if it is refused. */
    private void askForReminders() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            done(true);
        }
    }

    private void done(boolean remindersOn) {
        viewModel.finish(remindersOn);
        finish();
    }
}
