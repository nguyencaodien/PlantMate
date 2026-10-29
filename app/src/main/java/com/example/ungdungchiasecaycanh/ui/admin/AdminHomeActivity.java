package com.example.ungdungchiasecaycanh.ui.admin;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Admin overview (MoonRow canvas), opened from Settings for allowlisted admins only: totals with mini
 * bars, the chosen period's activity against the period before, listings by type, 14 days of activity
 * and reports by reason. The bottom tabs open the lists to manage.
 */
public class AdminHomeActivity extends AppCompatActivity {

    private static final int[] PERIOD_VIEWS = {R.id.adminPeriodToday, R.id.adminPeriodWeek, R.id.adminPeriodMonth, R.id.adminPeriodAll};

    private AdminHomeViewModel viewModel;
    private BottomNavigationView tabs;
    private boolean animated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);
        SecondaryChrome.applyPlain(this, findViewById(R.id.adminHome));
        viewModel = new ViewModelProvider(this).get(AdminHomeViewModel.class);

        findViewById(R.id.adminHomeRetry).setOnClickListener(v -> viewModel.load());
        findViewById(R.id.adminHomeRefresh).setOnClickListener(v -> viewModel.load());
        findViewById(R.id.adminOpenLog).setOnClickListener(v -> openList(AdminListViewModel.Kind.LOG));
        findViewById(R.id.adminBackToApp).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, com.example.ungdungchiasecaycanh.MainActivity.class)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(com.example.ungdungchiasecaycanh.MainActivity.EXTRA_SELECT_TAB, com.example.ungdungchiasecaycanh.MainActivity.TAB_HOME));
            finish();
        });
        findViewById(R.id.adminStatListings).setOnClickListener(v -> openList(AdminListViewModel.Kind.LISTINGS));
        findViewById(R.id.adminStatPosts).setOnClickListener(v -> openList(AdminListViewModel.Kind.POSTS));
        findViewById(R.id.adminStatMembers).setOnClickListener(v -> openList(AdminListViewModel.Kind.MEMBERS));
        findViewById(R.id.adminReasonsMore).setOnClickListener(v -> openList(AdminListViewModel.Kind.REPORTS));
        AdminHomeViewModel.Period[] periods = AdminHomeViewModel.Period.values();
        for (int i = 0; i < PERIOD_VIEWS.length; i++) {
            AdminHomeViewModel.Period p = periods[i];
            findViewById(PERIOD_VIEWS[i]).setOnClickListener(v -> viewModel.setPeriod(p));
        }
        tabs = findViewById(R.id.adminTabs);
        AdminTabs.bind(this, tabs, null, 0);

        View scroll = findViewById(R.id.adminHomeScroll);
        View loading = findViewById(R.id.adminHomeLoading);
        View error = findViewById(R.id.adminHomeError);
        viewModel.getState().observe(this, state -> {
            if (state.status == AdminHomeViewModel.Status.NOT_ADMIN) {
                Toast.makeText(this, R.string.admin_not_admin, Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            boolean content = state.status == AdminHomeViewModel.Status.CONTENT && state.data != null;
            loading.setVisibility(state.status == AdminHomeViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
            error.setVisibility(state.status == AdminHomeViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
            scroll.setVisibility(content ? View.VISIBLE : View.GONE);
            if (!content) return;
            bind(state.data);
            if (!animated) {
                animated = true;
                Motion.rise(findViewById(R.id.adminHomeGrid), 80);
            }
        });
        viewModel.getPeriod().observe(this, p -> {
            for (int i = 0; i < PERIOD_VIEWS.length; i++) findViewById(PERIOD_VIEWS[i]).setSelected(periods[i] == p);
            AdminHomeViewModel.State s = viewModel.getState().getValue();
            if (s != null && s.data != null) bindPeriod(s.data, p);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tabs.setSelectedItemId(R.id.admin_tab_overview);
        viewModel.load();
    }

    private void openList(AdminListViewModel.Kind kind) {
        startActivity(AdminListActivity.intent(this, kind));
    }

    private void bind(AdminHomeViewModel.Dashboard d) {
        ((TextView) findViewById(R.id.adminHomeMembers)).setText(
                getResources().getQuantityString(R.plurals.admin_members_sub, d.members, d.members));
        AdminTabs.badge(tabs, d.openReports);
        bindPeriod(d, viewModel.getPeriod().getValue());
        bindHeat(d);
        bindReasons(d);
    }

    // ---------------- Everything that follows the period switch ----------------

    private void bindPeriod(AdminHomeViewModel.Dashboard d, @Nullable AdminHomeViewModel.Period period) {
        if (period == null) period = AdminHomeViewModel.Period.WEEK;
        long now = System.currentTimeMillis();
        AdminHomeViewModel.Window w = AdminHomeViewModel.window(d, period, now, TimeZone.getDefault());
        NumberFormat number = NumberFormat.getIntegerInstance();
        int forest = ContextCompat.getColor(this, R.color.color_success);
        int lime = ContextCompat.getColor(this, R.color.brand_lime);
        int amber = ContextCompat.getColor(this, R.color.admin_amber);
        int ink = ContextCompat.getColor(this, R.color.color_text_primary);
        int grey = ContextCompat.getColor(this, R.color.color_panel_line);

        // Totals; the bars show the type mix, then how much of each total is new in the period.
        int give = count(d.byType, ListingType.GIVE_AWAY), trade = count(d.byType, ListingType.TRADE), sell = count(d.byType, ListingType.SELL);
        ((TextView) findViewById(R.id.adminCountListings)).setText(number.format(d.listings));
        ((StackedBarView) findViewById(R.id.adminBarListings)).setData(new int[]{give, trade, sell}, new int[]{forest, lime, amber});
        ((TextView) findViewById(R.id.adminCountPosts)).setText(number.format(d.posts));
        ((StackedBarView) findViewById(R.id.adminBarPosts)).setData(new int[]{w.posts, Math.max(0, d.posts - w.posts)}, new int[]{ink, grey});
        ((TextView) findViewById(R.id.adminCountMembers)).setText(number.format(d.members));
        ((StackedBarView) findViewById(R.id.adminBarMembers)).setData(new int[]{w.members, Math.max(0, d.members - w.members)}, new int[]{forest, grey});

        // Ink highlight
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dM");
        String range = period == AdminHomeViewModel.Period.ALL ? getString(R.string.admin_range_all)
                : period == AdminHomeViewModel.Period.TODAY ? getString(R.string.admin_period_today)
                : getString(R.string.admin_range, DateFormat.format(pattern, w.start), DateFormat.format(pattern, now));
        ((TextView) findViewById(R.id.adminHighlightRange)).setText(range);
        int activity = w.activity();
        ((TextView) findViewById(R.id.adminHighlightCount)).setText(number.format(activity));
        ((TextView) findViewById(R.id.adminHighlightLabel)).setText(
                getResources().getQuantityString(R.plurals.admin_new_activity, activity));
        TextView delta = findViewById(R.id.adminHighlightDelta);
        delta.setVisibility(period == AdminHomeViewModel.Period.ALL ? View.GONE : View.VISIBLE);
        int change = activity - w.previousActivity;
        String signed = (change > 0 ? "+" : change < 0 ? "−" : "±") + number.format(Math.abs(change));
        delta.setText(getString(period == AdminHomeViewModel.Period.TODAY ? R.string.admin_vs_yesterday
                : period == AdminHomeViewModel.Period.MONTH ? R.string.admin_vs_last_month : R.string.admin_vs_last_week, signed));
        int white = ContextCompat.getColor(this, R.color.white);
        ((StackedBarView) findViewById(R.id.adminHighlightBar)).setData(new int[]{w.listings, w.posts}, new int[]{lime, white});
        LinearLayout legend = findViewById(R.id.adminHighlightLegend);
        legend.removeAllViews();
        int onInk = ContextCompat.getColor(this, R.color.member_hero_text);
        legend.addView(legendItem(lime, getResources().getQuantityString(R.plurals.admin_headline_listings, w.listings, w.listings), onInk));
        legend.addView(legendItem(white, getResources().getQuantityString(R.plurals.admin_headline_posts, w.posts, w.posts), onInk));

        // Listings by type in the period
        ((TextView) findViewById(R.id.adminGaugePeriod)).setText(getString(new int[]{R.string.admin_period_today,
                R.string.admin_period_week, R.string.admin_period_month, R.string.admin_period_all}[period.ordinal()]));
        int[] typeColors = {forest, lime, amber};
        int[] typeValues = {w.give, w.trade, w.sell};
        DonutChartView gauge = findViewById(R.id.adminTypeChart);
        gauge.setHalf(true);
        gauge.setData(typeValues, typeColors, number.format(w.listings), getString(R.string.admin_gauge_caption));
        fillLegend(findViewById(R.id.adminTypeLegend), typeColors,
                new String[]{typeLabel(ListingType.GIVE_AWAY), typeLabel(ListingType.TRADE), typeLabel(ListingType.SELL)}, typeValues);
    }

    // ---------------- 14 days ----------------

    private void bindHeat(AdminHomeViewModel.Dashboard d) {
        int listingColor = ContextCompat.getColor(this, R.color.color_text_primary);
        int postColor = ContextCompat.getColor(this, isNight() ? R.color.brand_lime : R.color.color_success);
        LinearLayout legend = findViewById(R.id.adminHeatLegend);
        legend.removeAllViews();
        int text = ContextCompat.getColor(this, R.color.color_text_secondary);
        legend.addView(legendItem(listingColor, getString(R.string.admin_series_listings), text));
        legend.addView(legendItem(postColor, getString(R.string.admin_series_posts), text));
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dM");
        String[] labels = new String[d.dayStarts.length];
        for (int i = 0; i < labels.length; i++) labels[i] = DateFormat.format(pattern, d.dayStarts[i]).toString();
        HeatGridView heat = findViewById(R.id.adminHeat);
        heat.setContentDescription(getString(R.string.admin_chart_activity_desc,
                AdminStats.sum(d.newListingsPerDay), AdminStats.sum(d.newPostsPerDay)));
        heat.setData(d.newListingsPerDay, d.newPostsPerDay, listingColor, postColor, labels);
    }

    // ---------------- Reports by reason ----------------

    private void bindReasons(AdminHomeViewModel.Dashboard d) {
        LinearLayout table = findViewById(R.id.adminReasonTable);
        table.removeAllViews();
        table.addView(tableRow(getString(R.string.admin_col_reason), getString(R.string.admin_col_share),
                getString(R.string.admin_col_count), true));
        int total = d.openReports + d.resolvedReports + d.dismissedReports;
        NumberFormat number = NumberFormat.getIntegerInstance();
        for (ReportReason reason : ReportReason.values()) {
            Integer n = d.byReason.get(reason);
            int value = n == null ? 0 : n;
            table.addView(tableRow(AdminLabels.reason(this, reason),
                    getString(R.string.admin_percent, AdminStats.percent(value, total)), number.format(value), false));
        }
    }

    private View tableRow(String name, String share, String count, boolean header) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(header ? 32 : 40));
        if (!header) row.setBackgroundResource(R.drawable.bg_top_hairline_panel);
        String[] cells = {name, share, count};
        float[] weights = {2f, 1f, 0.7f};
        for (int i = 0; i < cells.length; i++) {
            TextView cell = new TextView(this);
            cell.setText(cells[i]);
            cell.setTextAppearance(header ? R.style.TextAppearance_PlantMate_Meta : R.style.TextAppearance_PlantMate_Body);
            cell.setTextColor(ContextCompat.getColor(this, header ? R.color.color_text_secondary : R.color.color_text_primary));
            if (!header) cell.setTextSize(14);
            cell.setFontFeatureSettings("tnum");
            cell.setGravity(i == 0 ? Gravity.START : Gravity.END);
            row.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weights[i]));
        }
        return row;
    }

    // ---------------- Shared pieces ----------------

    /** Rows of: colour dot, name, and the count on the right. */
    private void fillLegend(LinearLayout legend, int[] colors, String[] names, int[] counts) {
        legend.removeAllViews();
        for (int i = 0; i < names.length; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(5), 0, dp(5));
            View dot = new View(this);
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(colors[i]);
            dot.setBackground(circle);
            row.addView(dot, new LinearLayout.LayoutParams(dp(8), dp(8)));
            TextView name = new TextView(this);
            name.setText(names[i]);
            name.setTextAppearance(R.style.TextAppearance_PlantMate_Body);
            name.setTextSize(14);
            name.setTextColor(ContextCompat.getColor(this, R.color.color_text_primary));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            p.setMarginStart(dp(8));
            row.addView(name, p);
            TextView value = new TextView(this);
            value.setText(NumberFormat.getIntegerInstance().format(counts[i]));
            value.setTextAppearance(R.style.TextAppearance_PlantMate_Label);
            value.setTextColor(ContextCompat.getColor(this, R.color.color_text_primary));
            value.setFontFeatureSettings("tnum");
            row.addView(value);
            legend.addView(row);
        }
    }

    /** Legend entry: a small square in the series colour and its name. */
    private View legendItem(int color, String text, int textColor) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextAppearance(R.style.TextAppearance_PlantMate_Meta);
        label.setTextSize(12);
        label.setTextColor(textColor);
        label.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable square = new GradientDrawable();
        square.setColor(color);
        square.setCornerRadius(dp(2));
        square.setSize(dp(8), dp(8));
        label.setCompoundDrawablesRelativeWithIntrinsicBounds(square, null, null, null);
        label.setCompoundDrawablePadding(dp(4));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(dp(10));
        label.setLayoutParams(params);
        return label;
    }

    private static int count(Map<ListingType, Integer> map, ListingType type) {
        Integer n = map.get(type);
        return n == null ? 0 : n;
    }

    private String typeLabel(ListingType type) {
        return getString(type == ListingType.TRADE ? R.string.home_filter_trade
                : type == ListingType.SELL ? R.string.home_filter_sell : R.string.home_filter_give_away);
    }

    private boolean isNight() {
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
