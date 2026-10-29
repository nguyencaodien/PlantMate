package com.example.ungdungchiasecaycanh.ui.auth;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.MainActivity;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Last step of a first Google sign-in: name and area, then into Home with the welcome hand-off. */
public class CompleteProfileActivity extends AppCompatActivity {
    private boolean navigated;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);
        CompleteProfileViewModel viewModel = new ViewModelProvider(this).get(CompleteProfileViewModel.class);
        if (!viewModel.isSignedIn()) { backToSignIn(); return; }
        SecondaryChrome.applyPlain(this, findViewById(R.id.completeRoot));
        Motion.rise(findViewById(R.id.completeHero), 60);
        Motion.start(findViewById(R.id.completeMam));
        Motion.rise(findViewById(R.id.completeForm), 120);

        TextInputLayout nameLayout = findViewById(R.id.completeNameLayout);
        TextInputLayout locationLayout = findViewById(R.id.completeLocationLayout);
        TextInputEditText name = findViewById(R.id.completeName);
        TextInputEditText location = findViewById(R.id.completeLocation);
        MaterialButton save = findViewById(R.id.completeSave);
        if (savedInstanceState == null) name.setText(viewModel.suggestedName());

        save.setOnClickListener(v -> viewModel.save(String.valueOf(name.getText()), String.valueOf(location.getText())));
        findViewById(R.id.completeCancel).setOnClickListener(v -> { viewModel.cancel(); backToSignIn(); });
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { viewModel.cancel(); backToSignIn(); }
        });

        viewModel.getState().observe(this, state -> {
            boolean saving = state.status == CompleteProfileViewModel.Status.SAVING;
            save.setEnabled(!saving);
            save.setText(saving ? R.string.edit_profile_saving : R.string.complete_profile_save);
            nameLayout.setError(state.nameError == 0 ? null : getString(state.nameError));
            locationLayout.setError(state.locationError == 0 ? null : getString(state.locationError));
            if (state.status == CompleteProfileViewModel.Status.ERROR) {
                Snackbar.make(save, R.string.auth_profile_setup_error, Snackbar.LENGTH_LONG).show();
            }
            if (state.status == CompleteProfileViewModel.Status.DONE && !navigated) {
                navigated = true;
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
        });
    }

    private void backToSignIn() {
        startActivity(new Intent(this, SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
