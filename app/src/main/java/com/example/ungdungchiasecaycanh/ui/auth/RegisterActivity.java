package com.example.ungdungchiasecaycanh.ui.auth;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;

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

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout displayNameInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputLayout locationInputLayout;
    private TextInputEditText emailInput;
    private TextInputEditText displayNameInput;
    private TextInputEditText passwordInput;
    private TextInputEditText locationInput;
    private MaterialButton registerButton;
    private boolean navigationHandled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        SecondaryChrome.applyPlain(this, findViewById(R.id.register_root));
        Motion.start(findViewById(R.id.register_mam));
        Motion.rise(findViewById(R.id.register_hero), 60);
        Motion.rise(findViewById(R.id.register_form), 160);
        MaterialButton google = findViewById(R.id.button_register_google);
        // The Google "G" keeps its own colours (branding rule): never tint it.
        google.setIconTint(null);

        emailInputLayout = findViewById(R.id.register_email_input_layout);
        displayNameInputLayout = findViewById(R.id.register_display_name_input_layout);
        passwordInputLayout = findViewById(R.id.register_password_input_layout);
        locationInputLayout = findViewById(R.id.register_location_input_layout);
        emailInput = findViewById(R.id.register_email_input);
        displayNameInput = findViewById(R.id.register_display_name_input);
        passwordInput = findViewById(R.id.register_password_input);
        locationInput = findViewById(R.id.register_location_input);
        registerButton = findViewById(R.id.button_create_account);

        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.getAuthState().observe(this, state -> {
            boolean loading = state.status == AuthViewModel.Status.LOADING;
            registerButton.setEnabled(!loading);
            google.setEnabled(!loading);
            google.setText(loading && state.operation == AuthViewModel.Operation.GOOGLE
                    ? R.string.sign_in_signing_in_short : R.string.sign_in_google_short);
            if (state.status == AuthViewModel.Status.NEEDS_PROFILE && !navigationHandled) {
                navigationHandled = true;
                viewModel.profileRequestHandled();
                startActivity(new Intent(this, CompleteProfileActivity.class));
                finishAffinity();
                return;
            }
            displayNameInputLayout.setError(state.displayNameError == 0 ? null : getString(state.displayNameError));
            emailInputLayout.setError(state.emailError == 0 ? null : getString(state.emailError));
            passwordInputLayout.setError(state.passwordError == 0 ? null : getString(state.passwordError));
            locationInputLayout.setError(state.locationError == 0
                    ? null : getString(state.locationError));

            if (state.status == AuthViewModel.Status.ERROR && state.message != 0) {
                Snackbar.make(findViewById(android.R.id.content),
                        getString(state.message), Snackbar.LENGTH_SHORT).show();
            } else if (state.status == AuthViewModel.Status.SUCCESS
                    && (state.operation == AuthViewModel.Operation.REGISTER
                        || state.operation == AuthViewModel.Operation.GOOGLE
                        || state.operation == AuthViewModel.Operation.FACEBOOK)
                    && !navigationHandled) {
                navigationHandled = true;
                startActivity(new Intent(this, MainActivity.class),
                        ActivityOptions.makeCustomAnimation(this,
                                android.R.anim.fade_in, android.R.anim.fade_out).toBundle());
                finishAffinity();
            }
        });

        registerButton.setOnClickListener(view -> viewModel.register(
                displayNameInput.getText() == null ? "" : displayNameInput.getText().toString(),
                emailInput.getText() == null ? "" : emailInput.getText().toString(),
                passwordInput.getText() == null ? "" : passwordInput.getText().toString(),
                locationInput.getText() == null ? "" : locationInput.getText().toString()));

        // Facebook Login appears once a Meta app is configured (res/values/facebook.xml).
        MaterialButton facebook = findViewById(R.id.button_continue_facebook);
        if (FacebookSignInHelper.isConfigured(this)) {
            FacebookSignInHelper facebookLogin = new FacebookSignInHelper(viewModel);
            facebook.setVisibility(View.VISIBLE);
            facebook.setOnClickListener(view -> facebookLogin.signIn(this));
            viewModel.getAuthState().observe(this, state -> facebook.setEnabled(
                    state.status != AuthViewModel.Status.LOADING));
        }

        google.setOnClickListener(view -> GoogleSignInHelper.signIn(this, new GoogleSignInHelper.Callback() {
            @Override public void onIdToken(String idToken) { viewModel.signInWithGoogle(idToken); }
            @Override public void onFailure(int message) { viewModel.googleFailed(message); }
        }));

        findViewById(R.id.action_register_back).setOnClickListener(view -> finish());
        TextView signIn = findViewById(R.id.action_sign_in_existing);
        signIn.setPaintFlags(signIn.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        signIn.setOnClickListener(view -> finish());
    }
}
