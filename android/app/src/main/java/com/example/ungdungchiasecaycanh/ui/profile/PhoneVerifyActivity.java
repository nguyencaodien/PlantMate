package com.example.ungdungchiasecaycanh.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Phone check (demo): the code appears on screen instead of an SMS. */
public class PhoneVerifyActivity extends AppCompatActivity {

    private VerifyViewModel viewModel;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_verify);
        SecondaryChrome.apply(this, findViewById(R.id.phoneRoot), findViewById(R.id.phoneHero));
        viewModel = new ViewModelProvider(this).get(VerifyViewModel.class);

        TextInputLayout phoneLayout = findViewById(R.id.phoneNumberLayout);
        TextInputLayout codeLayout = findViewById(R.id.phoneCodeLayout);
        TextInputEditText phone = findViewById(R.id.phoneNumber);
        TextInputEditText code = findViewById(R.id.phoneCode);
        TextView demo = findViewById(R.id.phoneDemoCode);
        MaterialButton send = findViewById(R.id.phoneSend);
        MaterialButton confirm = findViewById(R.id.phoneConfirm);

        findViewById(R.id.phoneBack).setOnClickListener(v -> finish());
        send.setOnClickListener(v -> viewModel.sendCode(String.valueOf(phone.getText())));
        confirm.setOnClickListener(v -> viewModel.verify(String.valueOf(code.getText())));

        viewModel.getStatus().observe(this, status -> {
            boolean hasCode = viewModel.getCode() != null;
            demo.setVisibility(hasCode ? View.VISIBLE : View.GONE);
            if (hasCode) demo.setText(getString(R.string.phone_demo_code, viewModel.getCode()));
            codeLayout.setVisibility(hasCode ? View.VISIBLE : View.GONE);
            confirm.setVisibility(hasCode ? View.VISIBLE : View.GONE);
            send.setText(hasCode ? R.string.phone_send_again : R.string.phone_send);
            phoneLayout.setError(status == VerifyViewModel.Status.WRONG_PHONE ? getString(R.string.phone_wrong_number) : null);
            codeLayout.setError(status == VerifyViewModel.Status.WRONG_CODE ? getString(R.string.phone_wrong_code)
                    : status == VerifyViewModel.Status.FAILED ? getString(R.string.phone_save_failed) : null);
            confirm.setEnabled(status != VerifyViewModel.Status.SAVING);
            if (status == VerifyViewModel.Status.DONE) {
                Toast.makeText(this, R.string.phone_done, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }
}
