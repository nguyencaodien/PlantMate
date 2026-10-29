package com.example.ungdungchiasecaycanh.ui.notifications;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.MainActivity;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.ui.community.PostDetailActivity;
import com.example.ungdungchiasecaycanh.ui.garden.PlantDetailActivity;
import com.example.ungdungchiasecaycanh.ui.profile.MemberProfileActivity;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

/** Opened from the Home bell; each row opens its plant, post or member by stable ID. */
public class NotificationsActivity extends AppCompatActivity {
    private NotificationsViewModel viewModel;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);
        SecondaryChrome.apply(this, findViewById(R.id.notifications_root), findViewById(R.id.notifications_hero));
        viewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        NotificationAdapter adapter = new NotificationAdapter(new NotificationAdapter.Listener() {
            @Override public void onOpen(NotificationsViewModel.Row row) { open(row); }
            @Override public void onAccept(NotificationsViewModel.Row row) { viewModel.acceptFriend(row.id); }
            @Override public void onLater(NotificationsViewModel.Row row) { viewModel.later(row.id); }
        });
        RecyclerView list = findViewById(R.id.notifications_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.notifications_back).setOnClickListener(v -> finish());
        findViewById(R.id.notifications_read_all).setOnClickListener(v -> viewModel.markAllRead());
        findViewById(R.id.notifications_retry).setOnClickListener(v -> viewModel.retry());
        ChipGroup filters = findViewById(R.id.notifications_filters);
        filters.setOnCheckedStateChangeListener((group, checked) -> {
            int id = checked.isEmpty() ? R.id.notifications_filter_all : checked.get(0);
            viewModel.setFilter(id == R.id.notifications_filter_care ? NotificationsViewModel.Filter.CARE
                    : id == R.id.notifications_filter_community ? NotificationsViewModel.Filter.COMMUNITY
                    : NotificationsViewModel.Filter.ALL);
        });

        viewModel.getState().observe(this, state -> {
            findViewById(R.id.notifications_loading).setVisibility(state.status == NotificationsViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
            findViewById(R.id.notifications_empty).setVisibility(state.status == NotificationsViewModel.Status.EMPTY ? View.VISIBLE : View.GONE);
            findViewById(R.id.notifications_error).setVisibility(state.status == NotificationsViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
            list.setVisibility(state.status == NotificationsViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
            View readAll = findViewById(R.id.notifications_read_all);
            readAll.setEnabled(state.anyUnread);
            readAll.setAlpha(state.anyUnread ? 1f : 0.4f);
            adapter.submit(withSections(state.rows));
        });
    }

    /** "Today" for today's rows and friend requests, "Earlier" for the rest. */
    private List<Object> withSections(List<NotificationsViewModel.Row> rows) {
        long today = NotificationsViewModel.startOfDay(System.currentTimeMillis());
        List<Object> out = new ArrayList<>();
        boolean earlier = false;
        for (NotificationsViewModel.Row r : rows) {
            boolean isToday = r.at == 0 || r.at >= today;
            if (out.isEmpty() && isToday) out.add(getString(R.string.notifications_today));
            if (!isToday && !earlier) {
                out.add(getString(R.string.notifications_earlier));
                earlier = true;
            }
            out.add(r);
        }
        return out;
    }

    private void open(NotificationsViewModel.Row row) {
        viewModel.markRead(row);
        switch (row.kind) {
            case WATER:
                if (row.plantId > 0) {
                    startActivity(new Intent(this, PlantDetailActivity.class).putExtra(PlantDetailActivity.EXTRA_PLANT_ID, row.plantId));
                } else {
                    startActivity(new Intent(this, MainActivity.class)
                            .putExtra(MainActivity.EXTRA_SELECT_TAB, MainActivity.TAB_GARDEN)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                }
                break;
            case FRIEND:
                startActivity(MemberProfileActivity.intent(this, row.actorId, MemberProfileActivity.CONTEXT_COMMUNITY));
                break;
            default:
                if (row.postId != null && !row.postId.isEmpty()) {
                    startActivity(new Intent(this, PostDetailActivity.class).putExtra(PostDetailActivity.EXTRA_POST_ID, row.postId));
                }
        }
    }
}
