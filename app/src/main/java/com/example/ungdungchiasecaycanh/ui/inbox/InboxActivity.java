package com.example.ungdungchiasecaycanh.ui.inbox;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.google.android.material.textfield.TextInputEditText;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;

public class InboxActivity extends AppCompatActivity {
    private InboxViewModel viewModel;
    private ConversationAdapter adapter;
    private View loading, content, empty, error;
    private TextView emptyTitle, emptySupporting;
    private boolean rowsAnimated;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inbox);
        SecondaryChrome.apply(this, findViewById(R.id.inbox_root), findViewById(R.id.inbox_hero));
        Motion.rise(findViewById(R.id.inbox_search_bar), 200);
        Motion.start(findViewById(R.id.inbox_empty_mam));
        loading = findViewById(R.id.inbox_loading);
        content = findViewById(R.id.inbox_recycler);
        empty = findViewById(R.id.inbox_empty);
        error = findViewById(R.id.inbox_error);
        emptyTitle = findViewById(R.id.inbox_empty_title);
        emptySupporting = findViewById(R.id.inbox_empty_supporting);
        RecyclerView recycler = (RecyclerView) content;
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConversationAdapter(this::openConversation);
        recycler.setAdapter(adapter);
        viewModel = new ViewModelProvider(this).get(InboxViewModel.class);
        viewModel.getState().observe(this, this::render);
        findViewById(R.id.inbox_back).setOnClickListener(view -> finish());
        findViewById(R.id.inbox_retry).setOnClickListener(view -> viewModel.retry());
        TextInputEditText search = findViewById(R.id.inbox_search_input);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.setSearchQuery(s.toString());
            }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    @Override protected void onStart() {
        super.onStart();
        viewModel.setLive(true);
    }

    @Override protected void onStop() {
        viewModel.setLive(false);
        super.onStop();
    }

    private void render(InboxViewModel.UiState state) {
        loading.setVisibility(state.status == InboxViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
        content.setVisibility(state.status == InboxViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
        empty.setVisibility(state.status == InboxViewModel.Status.EMPTY ? View.VISIBLE : View.GONE);
        error.setVisibility(state.status == InboxViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
        if (state.status == InboxViewModel.Status.EMPTY) {
            emptyTitle.setText(state.noConversations
                    ? R.string.inbox_empty_title : R.string.inbox_no_search_results);
            emptySupporting.setVisibility(state.noConversations ? View.VISIBLE : View.GONE);
        }
        adapter.submitList(state.items);
        if (!rowsAnimated && state.status == InboxViewModel.Status.CONTENT && !state.items.isEmpty()) {
            rowsAnimated = true;
            Motion.staggerRows((RecyclerView) content, 260, 60);
        }
    }

    private void openConversation(String conversationId) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.EXTRA_CONVERSATION_ID, conversationId);
        startActivity(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        // Coming back from a chat: it is now seen.
        com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository settings =
                new com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository(this);
        adapter.setSeen(settings.getChatSeen(), settings.getChatSeenSince());
    }
}
