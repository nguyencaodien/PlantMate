package com.example.ungdungchiasecaycanh.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.AccountRepository;
import com.example.ungdungchiasecaycanh.ui.auth.GoogleSignInHelper;
import com.example.ungdungchiasecaycanh.ui.auth.SignInActivity;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Settings → Delete account. Lists what is deleted and what is kept, confirms the member (Google,
 * password, or a fresh sign-in), deletes everything, then opens sign-in with no history behind it.
 */
public class DeleteAccountActivity extends AppCompatActivity {
    private DeleteAccountViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);
        SecondaryChrome.apply(this, findViewById(R.id.deleteAccountRoot), findViewById(R.id.deleteAccountHero));
        vm = new ViewModelProvider(this).get(DeleteAccountViewModel.class);

        AccountRepository.Method method = vm.method();
        findViewById(R.id.deleteAccountPasswordLayout).setVisibility(
                method == AccountRepository.Method.PASSWORD ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.deleteAccountMethodNote)).setText(
                method == AccountRepository.Method.GOOGLE ? getString(R.string.delete_account_note_google)
                : method == AccountRepository.Method.PASSWORD ? getString(R.string.delete_account_note_password, vm.email())
                : getString(R.string.delete_account_note_recent));

        MaterialButton delete = findViewById(R.id.deleteAccountButton);
        CompoundButton confirm = findViewById(R.id.deleteAccountConfirm);
        confirm.setOnCheckedChangeListener((b, checked) -> bindButton(delete, checked));
        bindButton(delete, confirm.isChecked());
        delete.setOnClickListener(v -> {
            if (method == AccountRepository.Method.PASSWORD) {
                TextInputEditText password = findViewById(R.id.deleteAccountPassword);
                vm.deleteWithPassword(password.getText() == null ? "" : password.getText().toString());
            } else if (method == AccountRepository.Method.GOOGLE) {
                GoogleSignInHelper.signIn(this, new GoogleSignInHelper.Callback() {
                    @Override public void onIdToken(String idToken) { vm.deleteWithGoogle(idToken); }
                    @Override public void onFailure(int message) { vm.googleCancelled(message); }
                });
            } else {
                vm.deleteAfterRecentSignIn();
            }
        });

        findViewById(R.id.deleteAccountBack).setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                // Leaving halfway would hide a deletion that keeps running; stay until it ends.
                DeleteAccountViewModel.State s = vm.getState().getValue();
                if (s == null || s.phase != DeleteAccountViewModel.Phase.WORKING) finish();
            }
        });
        vm.getState().observe(this, this::render);
    }

    private void bindButton(MaterialButton button, boolean confirmed) {
        DeleteAccountViewModel.State s = vm.getState().getValue();
        boolean enabled = confirmed && (s == null || s.phase == DeleteAccountViewModel.Phase.READY);
        button.setEnabled(enabled);
        button.setAlpha(enabled ? 1f : 0.5f);
    }

    private void render(DeleteAccountViewModel.State s) {
        boolean working = s.phase == DeleteAccountViewModel.Phase.WORKING;
        findViewById(R.id.deleteAccountProgress).setVisibility(working ? View.VISIBLE : View.GONE);
        findViewById(R.id.deleteAccountConfirm).setEnabled(!working);
        findViewById(R.id.deleteAccountPassword).setEnabled(!working);
        bindButton(findViewById(R.id.deleteAccountButton), ((CompoundButton) findViewById(R.id.deleteAccountConfirm)).isChecked());
        TextView error = findViewById(R.id.deleteAccountError);
        error.setVisibility(s.error == 0 ? View.GONE : View.VISIBLE);
        if (s.error != 0) error.setText(s.error);
        if (s.phase == DeleteAccountViewModel.Phase.DONE) {
            GoogleSignInHelper.clearState(this);
            Toast.makeText(this, R.string.delete_account_done, Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, SignInActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }
    }
}
