package com.example.ungdungchiasecaycanh.ui.admin;

import android.app.Activity;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ungdungchiasecaycanh.R;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Admin bottom tabs (MoonRow canvas). The overview is AdminHomeActivity; each other tab is an
 * AdminListActivity that replaces the previous list, so Back from any list returns to the overview.
 */
final class AdminTabs {
    static final String EXTRA_OPEN_REPORTS = "open_reports";

    private AdminTabs() { }

    /** current: the list shown, or null on the overview. */
    static void bind(Activity activity, BottomNavigationView tabs, @Nullable AdminListViewModel.Kind current, int openReports) {
        tabs.setSelectedItemId(idFor(current));
        badge(tabs, openReports);
        tabs.setOnItemReselectedListener(item -> { });
        tabs.setOnItemSelectedListener(item -> {
            AdminListViewModel.Kind target = kindFor(item.getItemId());
            BadgeDrawable badge = tabs.getBadge(R.id.admin_tab_reports);
            int open = badge == null || !badge.isVisible() ? 0 : badge.getNumber();
            if (target != null) {
                activity.startActivity(AdminListActivity.intent(activity, target).putExtra(EXTRA_OPEN_REPORTS, open));
            }
            if (current != null) activity.finish();
            activity.overridePendingTransition(0, 0);
            return false;   // the screen that opens shows its own tab
        });
    }

    /** Red count of reports still waiting on the Reports tab. */
    static void badge(BottomNavigationView tabs, int openReports) {
        if (openReports <= 0) {
            tabs.removeBadge(R.id.admin_tab_reports);
            return;
        }
        BadgeDrawable badge = tabs.getOrCreateBadge(R.id.admin_tab_reports);
        badge.setNumber(openReports);
        badge.setBackgroundColor(ContextCompat.getColor(tabs.getContext(), R.color.color_danger));
        badge.setBadgeTextColor(ContextCompat.getColor(tabs.getContext(), R.color.white));
    }

    private static int idFor(@Nullable AdminListViewModel.Kind kind) {
        if (kind == null) return R.id.admin_tab_overview;
        switch (kind) {
            case POSTS: return R.id.admin_tab_posts;
            case MEMBERS: return R.id.admin_tab_members;
            case REPORTS: return R.id.admin_tab_reports;
            default: return R.id.admin_tab_listings;
        }
    }

    @Nullable
    private static AdminListViewModel.Kind kindFor(int id) {
        if (id == R.id.admin_tab_listings) return AdminListViewModel.Kind.LISTINGS;
        if (id == R.id.admin_tab_posts) return AdminListViewModel.Kind.POSTS;
        if (id == R.id.admin_tab_members) return AdminListViewModel.Kind.MEMBERS;
        if (id == R.id.admin_tab_reports) return AdminListViewModel.Kind.REPORTS;
        return null;
    }
}
