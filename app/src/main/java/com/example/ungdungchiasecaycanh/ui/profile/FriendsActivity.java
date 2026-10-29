package com.example.ungdungchiasecaycanh.ui.profile;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

/** Friends and friend requests; new friends are made from a member's profile ("Add friend"). */
public class FriendsActivity extends AppCompatActivity {
    private FriendsViewModel vm;
    private FriendAdapter adapter;
    private int shownErrors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        SecondaryChrome.applyPlain(this, findViewById(R.id.friendsRoot));
        findViewById(R.id.friendsBack).setOnClickListener(v -> finish());
        vm = new ViewModelProvider(this).get(FriendsViewModel.class);
        adapter = new FriendAdapter(new FriendAdapter.Listener() {
            @Override public void onOpen(String uid) {
                startActivity(MemberProfileActivity.intent(FriendsActivity.this, uid, MemberProfileActivity.CONTEXT_COMMUNITY));
            }
            @Override public void onAccept(String uid) { vm.accept(uid); }
            @Override public void onDecline(String uid) { vm.decline(uid); }
            @Override public void onCancel(String uid) { vm.cancel(uid); }
            @Override public void onUnfriend(FriendRepository.Person person) { confirmUnfriend(person); }
            @Override public void onAdd(String uid) { vm.add(uid); }
        });
        ((androidx.recyclerview.widget.RecyclerView) findViewById(R.id.friendsList)).setAdapter(adapter);
        findViewById(R.id.friendsTabFriends).setOnClickListener(v -> vm.selectTab(FriendsViewModel.Tab.FRIENDS));
        findViewById(R.id.friendsTabRequests).setOnClickListener(v -> vm.selectTab(FriendsViewModel.Tab.REQUESTS));
        findViewById(R.id.friendsRetry).setOnClickListener(v -> vm.load());
        vm.getState().observe(this, this::render);
        vm.getActionErrors().observe(this, n -> {
            if (n == null || n <= shownErrors) return;
            shownErrors = n;
            Snackbar.make(findViewById(R.id.friendsRoot), R.string.friends_action_error, Snackbar.LENGTH_SHORT).show();
        });
    }

    @Override protected void onResume() {
        super.onResume();
        vm.load(); // a profile opened from here may have changed a friendship
    }

    private void render(FriendsViewModel.State s) {
        boolean requests = s.tab == FriendsViewModel.Tab.REQUESTS;
        TextView friendsTab = findViewById(R.id.friendsTabFriends);
        TextView requestsTab = findViewById(R.id.friendsTabRequests);
        friendsTab.setText(getString(R.string.friends_tab_friends, s.friends.size()));
        requestsTab.setText(getString(R.string.friends_tab_requests, s.incoming.size() + s.outgoing.size()));
        friendsTab.setSelected(!requests);
        requestsTab.setSelected(requests);

        List<FriendAdapter.Row> rows = new ArrayList<>();
        if (requests) {
            for (FriendRepository.Person p : s.incoming) rows.add(new FriendAdapter.Row(p, FriendAdapter.Kind.INCOMING));
            for (FriendRepository.Person p : s.outgoing) rows.add(new FriendAdapter.Row(p, FriendAdapter.Kind.OUTGOING));
        } else {
            for (FriendRepository.Person p : s.friends) rows.add(new FriendAdapter.Row(p, FriendAdapter.Kind.FRIEND));
            for (FriendsViewModel.Suggestion g : s.suggestions)
                rows.add(new FriendAdapter.Row(g.person, FriendAdapter.Kind.SUGGESTED, g.sameArea));
        }
        adapter.submit(rows, s.busy);
        boolean content = !s.loading && !s.error;
        findViewById(R.id.friendsLoading).setVisibility(s.loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.friendsError).setVisibility(!s.loading && s.error ? View.VISIBLE : View.GONE);
        findViewById(R.id.friendsList).setVisibility(content && !rows.isEmpty() ? View.VISIBLE : View.GONE);
        findViewById(R.id.friendsEmpty).setVisibility(content && rows.isEmpty() ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.friendsEmptyTitle)).setText(requests
                ? R.string.friends_requests_empty_title : R.string.friends_empty_title);
        ((TextView) findViewById(R.id.friendsEmptySupporting)).setText(requests
                ? R.string.friends_requests_empty_supporting : R.string.friends_empty_supporting);
    }

    private void confirmUnfriend(FriendRepository.Person person) {
        String name = person.name().isEmpty() ? getString(R.string.detail_member_fallback) : person.name();
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.friend_unfriend_title, name))
                .setMessage(R.string.friend_unfriend_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.friend_action_unfriend, (d, w) -> vm.unfriend(person.uid))
                .show();
    }
}
