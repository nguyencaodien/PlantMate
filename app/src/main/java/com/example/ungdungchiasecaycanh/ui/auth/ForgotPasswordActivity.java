package com.example.ungdungchiasecaycanh.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Sends a password-reset email. Opened from Sign in; the typed email is prefilled. */
public class ForgotPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        SecondaryChrome.applyPlain(this, findViewById(R.id.forgotRoot));
        Motion.rise(findViewById(R.id.forgotHero), 60);
        Motion.start(findViewById(R.id.forgotMam));
        Motion.floatLoop(findViewById(R.id.forgotBubble));
        Motion.rise(findViewById(R.id.forgotForm), 220);

        TextInputLayout emailLayout = findViewById(R.id.forgotEmailLayout);
        TextInputEditText email = findViewById(R.id.forgotEmail);
        MaterialButton send = findViewById(R.id.forgotSend);
        View form = findViewById(R.id.forgotForm);
        View sent = findViewById(R.id.forgotSent);
        View error = findViewById(R.id.forgotError);
        TextView sentBody = findViewById(R.id.forgotSentBody);

        String prefill = getIntent().getStringExtra(EXTRA_EMAIL);
        if (savedInstanceState == null && prefill != null) email.setText(prefill.trim());

        PasswordResetViewModel viewModel = new ViewModelProvider(this).get(PasswordResetViewModel.class);
        findViewById(R.id.forgotBack).setOnClickListener(v -> finish());
        findViewById(R.id.forgotDone).setOnClickListener(v -> finish());
        findViewById(R.id.forgotResend).setOnClickListener(v -> viewModel.reset());
        send.setOnClickListener(v -> viewModel.send(
                email.getText() == null ? "" : email.getText().toString()));

        viewModel.getState().observe(this, state -> {
            boolean isSent = state.status == PasswordResetViewModel.Status.SENT;
            boolean sending = state.status == PasswordResetViewModel.Status.SENDING;
            if (isSent && sent.getVisibility() != View.VISIBLE) Motion.pop(sent, 480, 0);
            form.setVisibility(isSent ? View.GONE : View.VISIBLE);
            sent.setVisibility(isSent ? View.VISIBLE : View.GONE);
            error.setVisibility(state.status == PasswordResetViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
            emailLayout.setError(state.emailError == 0 ? null : getString(state.emailError));
            send.setEnabled(!sending);
            send.setText(sending ? R.string.forgot_sending : R.string.forgot_send);
            if (isSent) sentBody.setText(getString(R.string.forgot_sent_body, state.email));
        });
    }
}
