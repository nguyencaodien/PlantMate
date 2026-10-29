package com.example.ungdungchiasecaycanh.ui.admin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.ui.community.CreatePostActivity;
import com.example.ungdungchiasecaycanh.ui.listing.CreateListingActivity;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

/** One admin list; the kind comes from the Intent (no models are passed between screens). */
public class AdminListActivity extends AppCompatActivity {

    public static final String EXTRA_KIND = "kind";

    public static Intent intent(Context context, AdminListViewModel.Kind kind) {
        return new Intent(context, AdminListActivity.class).putExtra(EXTRA_KIND, kind.name());
    }

    private AdminListViewModel viewModel;
    private AdminListViewModel.Kind kind;
    private boolean rowsAnimated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_list);
        SecondaryChrome.apply(this, findViewById(R.id.adminListRoot), findViewById(R.id.adminListHero));
        Motion.rise(findViewById(R.id.adminListSearchBar), 200);
        Motion.start(findViewById(R.id.adminListMam));

        try {
            kind = AdminListViewModel.Kind.valueOf(getIntent().getStringExtra(EXTRA_KIND));
        } catch (RuntimeException e) {
            kind = AdminListViewModel.Kind.LISTINGS;
        }
        bindHeader();
        com.google.android.material.bottomnavigation.BottomNavigationView tabs = findViewById(R.id.adminListTabs);
        if (kind == AdminListViewModel.Kind.LOG) tabs.setVisibility(View.GONE);
        else AdminTabs.bind(this, tabs, kind, getIntent().getIntExtra(AdminTabs.EXTRA_OPEN_REPORTS, 0));

        RecyclerView list = findViewById(R.id.adminListRecycler);
        list.setLayoutManager(new LinearLayoutManager(this));
        AdminRowAdapter adapter = new AdminRowAdapter(kind == AdminListViewModel.Kind.LOG ? null : this::open);
        list.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AdminListViewModel.class);
        findViewById(R.id.adminListBack).setOnClickListener(v -> finish());
        findViewById(R.id.adminListRetry).setOnClickListener(v -> viewModel.load(kind));
        ((EditText) findViewById(R.id.adminListSearch)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { viewModel.setQuery(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });

        bindFilters();

        View loading = findViewById(R.id.adminListLoading);
        View empty = findViewById(R.id.adminListEmpty);
        View error = findViewById(R.id.adminListError);
        viewModel.getState().observe(this, state -> {
            loading.setVisibility(state.status == AdminListViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
            list.setVisibility(state.status == AdminListViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
            empty.setVisibility(state.status == AdminListViewModel.Status.EMPTY ? View.VISIBLE : View.GONE);
            error.setVisibility(state.status == AdminListViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
            adapter.submit(state.rows);
            if (!rowsAnimated && !state.rows.isEmpty()) {
                rowsAnimated = true;
                Motion.staggerRows(list, 200, 50);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.load(kind);   // reload after an edit screen returns
    }

    private void bindHeader() {
        TextView title = findViewById(R.id.adminListTitle);
        TextView subtitle = findViewById(R.id.adminListSubtitle);
        ExtendedFloatingActionButton fab = findViewById(R.id.adminListFab);
        switch (kind) {
            case POSTS:
                title.setText(R.string.admin_posts);
                subtitle.setText(R.string.admin_posts_supporting);
                fab.setVisibility(View.VISIBLE);
                fab.setText(R.string.community_create_post);
                fab.setOnClickListener(v -> startActivity(new Intent(this, CreatePostActivity.class)));
                break;
            case MEMBERS:
                title.setText(R.string.admin_members);
                subtitle.setText(R.string.admin_members_supporting);
                break;
            case REPORTS:
                title.setText(R.string.admin_reports);
                subtitle.setText(R.string.admin_reports_supporting);
                break;
            case LOG:
                title.setText(R.string.admin_log_title);
                subtitle.setText(R.string.admin_log_list_supporting);
                findViewById(R.id.adminListSearchBar).setVisibility(View.GONE);
                break;
            default:
                title.setText(R.string.admin_listings);
                subtitle.setText(R.string.admin_listings_supporting);
                fab.setVisibility(View.VISIBLE);
                fab.setText(R.string.my_listings_share_plant);
                fab.setOnClickListener(v -> startActivity(new Intent(this, CreateListingActivity.class)));
        }
        if (fab.getVisibility() == View.VISIBLE) Motion.pop(fab, 480, 400);
    }

    /** Quick filters: report review state, listing status or post category. */
    private void bindFilters() {
        String[][] filters;
        switch (kind) {
            case REPORTS:
                filters = new String[][]{{AdminListViewModel.FILTER_OPEN, getString(R.string.admin_filter_open)},
                        {"RESOLVED", getString(R.string.admin_filter_resolved)},
                        {"DISMISSED", getString(R.string.admin_filter_dismissed)},
                        {AdminListViewModel.FILTER_ALL, getString(R.string.admin_filter_all)}};
                break;
            case LISTINGS:
                filters = new String[][]{{AdminListViewModel.FILTER_ALL, getString(R.string.admin_filter_all)},
                        {"ACTIVE", getString(R.string.listing_status_active)},
                        {"RESERVED", getString(R.string.listing_status_reserved)},
                        {"REHOMED", getString(R.string.listing_status_rehomed)},
                        {"UNAVAILABLE", getString(R.string.listing_status_unavailable)}};
                break;
            case POSTS:
                PostCategory[] categories =
                        PostCategory.values();
                filters = new String[categories.length + 1][];
                filters[0] = new String[]{AdminListViewModel.FILTER_ALL, getString(R.string.admin_filter_all)};
                for (int i = 0; i < categories.length; i++) {
                    filters[i + 1] = new String[]{categories[i].name(), AdminLabels.category(this, categories[i])};
                }
                break;
            default:
                return;
        }
        findViewById(R.id.adminListFilterScroll).setVisibility(View.VISIBLE);
        ChipGroup group = findViewById(R.id.adminListFilters);
        group.removeAllViews();
        for (int i = 0; i < filters.length; i++) {
            Chip chip = new Chip(this, null,
                    com.google.android.material.R.attr.chipStyle);
            chip.setId(View.generateViewId());
            chip.setText(filters[i][1]);
            chip.setTag(filters[i][0]);
            chip.setCheckable(true);
            group.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
        viewModel.setFilter(filters[0][0]);
        group.setOnCheckedStateChangeListener((g, ids) -> {
            if (ids.isEmpty()) return;
            Object key = g.findViewById(ids.get(0)).getTag();
            viewModel.setFilter(key == null ? AdminListViewModel.FILTER_ALL : key.toString());
        });
    }

    private void open(AdminRow row) {
        switch (kind) {
            case POSTS:
                startActivity(new Intent(this, AdminPostEditActivity.class).putExtra(AdminPostEditActivity.EXTRA_POST_ID, row.id));
                break;
            case MEMBERS:
                startActivity(new Intent(this, AdminMemberActivity.class).putExtra(AdminMemberActivity.EXTRA_UID, row.id));
                break;
            case REPORTS:
                startActivity(new Intent(this, AdminReportActivity.class).putExtra(AdminReportActivity.EXTRA_REPORT_ID, row.id));
                break;
            case LISTINGS:
                startActivity(new Intent(this, AdminListingEditActivity.class).putExtra(AdminListingEditActivity.EXTRA_LISTING_ID, row.id));
                break;
            default:
                break;
        }
    }
}
