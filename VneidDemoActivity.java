package com.example.ungdungchiasecaycanh.ui.profile;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import android.app.Activity;
import android.content.Intent;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Demo of how VNeID identity checks would work. PlantMate is not connected to the national e-ID
 * system: no identity data is sent, only "VNeID step done" is saved on the profile (selling needs it).
 */
public class VneidDemoActivity extends AppCompatActivity {
    private static final String STATE_STEP = "step";
    private static final int INTRO = 0, WAITING = 1, DONE = 2;
    /** How long the simulated "confirm in VNeID" wait lasts. */
    private static final long WAIT_MS = 2500;

    private final Handler main = new Handler(Looper.getMainLooper());
    private int step = INTRO;
    private VerifyViewModel viewModel;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vneid_demo);
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(VerifyViewModel.class);
        SecondaryChrome.apply(this, findViewById(R.id.vneidRoot), findViewById(R.id.vneidHero));
        findViewById(R.id.vneidBack).setOnClickListener(v -> finish());
        findViewById(R.id.vneidAction).setOnClickListener(v -> {
            if (step == DONE) finish(); else show(WAITING);
        });
        // Always starts at the intro: the device flag below is not tied to an account, so it must not
        // publish "VNeID done" for whoever is signed in now.
        if (savedInstanceState != null) step = savedInstanceState.getInt(STATE_STEP, INTRO);
        show(step);
    }

    private void show(int value) {
        step = value;
        main.removeCallbacksAndMessages(null);
        TextView title = findViewById(R.id.vneidStepTitle);
        TextView body = findViewById(R.id.vneidStepBody);
        MaterialButton action = findViewById(R.id.vneidAction);
        findViewById(R.id.vneidProgress).setVisibility(step == WAITING ? View.VISIBLE : View.GONE);
        action.setVisibility(step == WAITING ? View.GONE : View.VISIBLE);
        if (step == INTRO) {
            title.setText(R.string.vneid_intro_title);
            body.setText(R.string.vneid_intro_body);
            action.setText(R.string.vneid_start);
        } else if (step == WAITING) {
            title.setText(R.string.vneid_waiting_title);
            body.setText(R.string.vneid_waiting_body);
            main.postDelayed(() -> {
                viewModel.markVneid(); // only after the step itself, never for a restored screen
                show(DONE);
            }, WAIT_MS);
        } else {
            new SettingsRepository(this).setVneidDemoDone();
            setResult(RESULT_OK);
            title.setText(R.string.vneid_done_title);
            body.setText(R.string.vneid_done_body);
            action.setText(R.string.vneid_continue);
        }
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, step);
    }

    @Override protected void onDestroy() {
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * Selling and buying ask for the VNeID step first (a demo, see the class note). Returns true
     * when the caller may go on; otherwise explains and offers to open the step.
     */
    public static boolean passGate(Activity activity) {
        if (new SettingsRepository(activity).isVneidDemoDone()) return true;
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.vneid_gate_title)
                .setMessage(R.string.vneid_gate_body)
                .setPositiveButton(R.string.vneid_gate_go,
                        (dialog, which) -> activity.startActivity(new Intent(activity, VneidDemoActivity.class)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
        return false;
    }
}
