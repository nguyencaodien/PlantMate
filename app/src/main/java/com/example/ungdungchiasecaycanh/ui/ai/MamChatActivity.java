package com.example.ungdungchiasecaycanh.ui.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.MainActivity;
import com.example.ungdungchiasecaycanh.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

/** Ask Mầm: a rule-based care helper (MamAdvisor). No network, no AI, no diagnosis. */
public class MamChatActivity extends AppCompatActivity {
    private MamChatViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mam_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mam_root), (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        viewModel = new ViewModelProvider(this).get(MamChatViewModel.class);
        RecyclerView list = findViewById(R.id.mam_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        list.setLayoutManager(layoutManager);
        MamChatAdapter adapter = new MamChatAdapter(this::openAction);
        list.setAdapter(adapter);

        ImageView avatar = findViewById(R.id.mam_avatar);
        Motion.start(avatar);
        Motion.rise((View) avatar.getParent(), 60);
        Motion.sheet((View) findViewById(R.id.mam_input).getParent().getParent(), 600, 160);
        findViewById(R.id.mam_back).setOnClickListener(v -> finish());
        // Image messages are out of MVP: a photo question goes to Plant doctor.
        findViewById(R.id.mam_photo).setOnClickListener(v -> openAction(MamAdvisor.Action.OPEN_DOCTOR));

        ChipGroup suggestions = findViewById(R.id.mam_suggestions);
        for (String q : MamAdvisor.suggestions()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_suggestion_chip, suggestions, false);
            chip.setText(q);
            chip.setOnClickListener(v -> viewModel.ask(q));
            suggestions.addView(chip);
        }

        EditText input = findViewById(R.id.mam_input);
        findViewById(R.id.mam_send).setOnClickListener(v -> send(input));
        input.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER;
            if (actionId == EditorInfo.IME_ACTION_SEND || enter) {
                send(input);
                return true;
            }
            return false;
        });

        TextView status = findViewById(R.id.mam_status);
        viewModel.getState().observe(this, state -> {
            adapter.submit(state);
            list.scrollToPosition(adapter.getItemCount() - 1);
            status.setText(state.answering ? R.string.mam_status_typing : R.string.mam_status);
        });
    }

    private void send(EditText input) {
        viewModel.ask(String.valueOf(input.getText()));
        input.setText("");
    }

    private void openAction(MamAdvisor.Action action) {
        String tab = action == MamAdvisor.Action.OPEN_DOCTOR ? MainActivity.TAB_DOCTOR : MainActivity.TAB_GARDEN;
        startActivity(new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_SELECT_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}
