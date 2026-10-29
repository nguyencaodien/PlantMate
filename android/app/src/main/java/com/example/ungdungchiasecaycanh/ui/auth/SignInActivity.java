package com.example.ungdungchiasecaycanh.ui.auth;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.ActivityOptions;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.MainActivity;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class SignInActivity extends AppCompatActivity {

    private TextInputLayout emailInputLayout;
    private TextInputLayout passwordInputLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton signInButton;
    private boolean navigationHandled;
    /** The photo columns of the brand block: middle drifts up, the sides drift down. */
    private static final int[] COLUMNS = {R.id.sign_in_column_1, R.id.sign_in_column_2, R.id.sign_in_column_3};
    private static final long DRIFT_MS = 26_000;
    /** The brand block's looping motion, running only between onStart and onStop. */
    private final List<Animator> ambient = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        bindInsets();
        fillColumns();
        emailInputLayout = findViewById(R.id.email_input_layout);
        passwordInputLayout = findViewById(R.id.password_input_layout);
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        signInButton = findViewById(R.id.button_sign_in);
        // The Google "G" keeps its own colours (branding rule): never tint it.
        ((MaterialButton) findViewById(R.id.button_continue_google)).setIconTint(null);

        AuthViewModel viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        viewModel.getAuthState().observe(this, state -> {
            boolean loading = state.status == AuthViewModel.Status.LOADING;
            signInButton.setEnabled(!loading);
            MaterialButton google = findViewById(R.id.button_continue_google);
            google.setEnabled(!loading);
            google.setText(loading && state.operation == AuthViewModel.Operation.GOOGLE
                    ? R.string.sign_in_signing_in_short : R.string.sign_in_google_short);
            if (state.status == AuthViewModel.Status.NEEDS_PROFILE && !navigationHandled) {
                navigationHandled = true;
                viewModel.profileRequestHandled();
                startActivity(new Intent(this, CompleteProfileActivity.class));
                finish();
                return;
            }
            emailInputLayout.setError(state.emailError == 0 ? null : getString(state.emailError));
            passwordInputLayout.setError(state.passwordError == 0 ? null : getString(state.passwordError));

            if (state.status == AuthViewModel.Status.ERROR && state.message != 0) {
                Snackbar.make(findViewById(android.R.id.content),
                        getString(state.message), Snackbar.LENGTH_SHORT).show();
            } else if (state.status == AuthViewModel.Status.SUCCESS
                    && (state.operation == AuthViewModel.Operation.SIGN_IN
                        || state.operation == AuthViewModel.Operation.GOOGLE
                        || state.operation == AuthViewModel.Operation.FACEBOOK)
                    && !navigationHandled) {
                navigationHandled = true;
                startActivity(new Intent(this, MainActivity.class),
                        ActivityOptions.makeCustomAnimation(this,
                                android.R.anim.fade_in, android.R.anim.fade_out).toBundle());
                finish();
            }
        });

        signInButton.setOnClickListener(view -> viewModel.signIn(
                emailInput.getText() == null ? "" : emailInput.getText().toString(),
                passwordInput.getText() == null ? "" : passwordInput.getText().toString()));
        passwordInput.setOnEditorActionListener((v, actionId, event) -> signInButton.performClick());

        // Facebook Login appears once a Meta app is configured (res/values/facebook.xml).
        MaterialButton facebook = findViewById(R.id.button_continue_facebook);
        if (FacebookSignInHelper.isConfigured(this)) {
            FacebookSignInHelper facebookLogin = new FacebookSignInHelper(viewModel);
            facebook.setVisibility(View.VISIBLE);
            facebook.setOnClickListener(view -> facebookLogin.signIn(this));
            viewModel.getAuthState().observe(this, state -> facebook.setEnabled(
                    state.status != AuthViewModel.Status.LOADING));
        }

        findViewById(R.id.button_continue_google).setOnClickListener(view ->
                GoogleSignInHelper.signIn(this, new GoogleSignInHelper.Callback() {
                    @Override public void onIdToken(String idToken) { viewModel.signInWithGoogle(idToken); }
                    @Override public void onFailure(int message) { viewModel.googleFailed(message); }
                }));

        TextView join = findViewById(R.id.action_join_garden);
        join.setPaintFlags(join.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        join.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.action_forgot_password).setOnClickListener(view -> startActivity(
                new Intent(this, ForgotPasswordActivity.class).putExtra(ForgotPasswordActivity.EXTRA_EMAIL,
                        emailInput.getText() == null ? "" : emailInput.getText().toString())));

        viewModel.checkExistingSession();
    }

    /** Edge to edge: the brand block runs under the status bar (its top fade keeps the icons readable); the form pads for the navigation bar and keyboard. */
    private void bindInsets() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        // The block is the logo's deep green in both themes: light status bar icons.
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        View content = findViewById(R.id.sign_in_content);
        int bottom = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sign_in_root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(),
                    bottom + Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /** Each column holds its three photos twice, in its own order, so a half-height drift loops seamlessly. */
    private void fillColumns() {
        int[] photos = {R.drawable.photo_signin_monstera, R.drawable.photo_signin_fern, R.drawable.photo_signin_snake};
        int tile = getResources().getDimensionPixelSize(R.dimen.sign_in_tile_height);
        int gap = getResources().getDimensionPixelSize(R.dimen.sign_in_tile_gap);
        ShapeAppearanceModel rounded = ShapeAppearanceModel.builder()
                .setAllCornerSizes(14 * getResources().getDisplayMetrics().density).build();
        float half = getResources().getDimension(R.dimen.sign_in_column_height) / 2f;
        for (int c = 0; c < COLUMNS.length; c++) {
            LinearLayout column = findViewById(COLUMNS[c]);
            for (int i = 0; i < 2 * photos.length; i++) {
                ShapeableImageView photo = new ShapeableImageView(this);
                photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
                photo.setShapeAppearanceModel(rounded);
                photo.setImageResource(photos[(i + c) % photos.length]);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, tile);
                params.bottomMargin = gap;
                column.addView(photo, params);
            }
            // The sides start half a column up and drift down; this is also where they rest with reduced motion.
            if (c != 1) column.setTranslationY(-half);
        }
    }

    /** The photo columns drift while the screen is visible (still with reduced motion). */
    @Override
    protected void onStart() {
        super.onStart();
        if (!Motion.enabled(this)) return;
        float half = getResources().getDimension(R.dimen.sign_in_column_height) / 2f;
        for (int c = 0; c < COLUMNS.length; c++) {
            ObjectAnimator drift = c == 1
                    ? ObjectAnimator.ofFloat(findViewById(COLUMNS[c]), View.TRANSLATION_Y, 0f, -half)
                    : ObjectAnimator.ofFloat(findViewById(COLUMNS[c]), View.TRANSLATION_Y, -half, 0f);
            drift.setDuration(DRIFT_MS);
            drift.setInterpolator(new LinearInterpolator());
            drift.setRepeatCount(ValueAnimator.INFINITE);
            ambient.add(drift);
            drift.start();
        }
    }

    @Override
    protected void onStop() {
        for (Animator a : ambient) a.cancel();
        ambient.clear();
        super.onStop();
    }
}
