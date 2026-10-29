package com.example.ungdungchiasecaycanh.ui.garden;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.example.ungdungchiasecaycanh.util.LocalImageLoader;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Garden (canvas B "Vườn của tôi"): the week strip with a sliding pill, the chosen day's care (today's by
 * default) with liquid-fill water buttons and a rolling counter, Mầm's tip from the care log, area chips over
 * the plant grid, and recent activity. Everything is computed by {@link GardenOverview} and
 * {@link GardenTips} from the member's Room plants and care logs.
 */
public class GardenFragment extends Fragment {
    /** Rows shown on the day panel; the rest are counted. */
    static final int MAX_TASKS = 6;
    static final int MAX_ACTIVITY = 3;
    /** How long the green fill takes to rise in a water button before the care is logged. */
    private static final long FILL_MS = 560;
    private GardenViewModel viewModel;
    private int shownWaterErrors;
    private GardenShelfAdapter shelf;
    private List<PlantEntity> plants;
    private List<CareLogEntity> care = Collections.emptyList();
    private GardenOverview overview;
    /** Month calendar sheet (canvas B): the month on screen (its 1st) and the selected day (midnight). */
    private BottomSheetDialog monthSheet;
    private Calendar monthShown;
    private long monthSelected;
    private boolean sortByName;
    private String lastCount;

    public GardenFragment() { super(R.layout.fragment_garden); }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View loading = view.findViewById(R.id.gardenLoading);
        TextView error = view.findViewById(R.id.gardenError);
        View add = view.findViewById(R.id.gardenAdd); // beside the title, not floating (BUG-009)
        Motion.rise(view.findViewById(R.id.gardenHero), 40);
        Motion.rise(view.findViewById(R.id.gardenWeek), 90);
        Motion.rise(view.findViewById(R.id.gardenTasksCard), 140);

        viewModel = new ViewModelProvider(this).get(GardenViewModel.class);
        View.OnClickListener openAdd = v -> startActivity(new Intent(requireContext(), AddPlantActivity.class));
        add.setOnClickListener(openAdd);
        view.findViewById(R.id.gardenEmptyAction).setOnClickListener(openAdd);
        shelf = new GardenShelfAdapter(this::openPlant);
        RecyclerView grid = view.findViewById(R.id.gardenShelf);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        grid.setAdapter(shelf);

        NestedScrollView scroll = view.findViewById(R.id.gardenScroll);
        view.findViewById(R.id.gardenBigDate).setOnClickListener(v -> openMonth());
        View searchBar = view.findViewById(R.id.gardenSearchBar);
        EditText search = view.findViewById(R.id.gardenSearch);
        searchBar.setVisibility(viewModel.query.isEmpty() ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.gardenSearchToggle).setOnClickListener(v -> {
            boolean open = searchBar.getVisibility() != View.VISIBLE;
            searchBar.setVisibility(open ? View.VISIBLE : View.GONE);
            if (open) search.requestFocus(); else search.setText("");
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                viewModel.query = s.toString().trim();
                bindShelf();
            }
        });
        TextView sort = view.findViewById(R.id.gardenSort);
        sort.setText(R.string.garden_sort_due);
        sort.setOnClickListener(v -> {
            sortByName = !sortByName;
            sort.setText(sortByName ? R.string.garden_sort_name : R.string.garden_sort_due);
            bindShelf();
        });

        if (!viewModel.hasAuthenticatedUser()) {
            loading.setVisibility(View.GONE);
            error.setVisibility(View.VISIBLE);
            error.setText(R.string.garden_auth_required);
            return;
        }
        viewModel.getWaterErrors().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > shownWaterErrors) {
                shownWaterErrors = count;
                Snackbar.make(view, viewModel.lastWaterError(), Snackbar.LENGTH_LONG).show();
            }
        });
        viewModel.getCare().observe(getViewLifecycleOwner(), logs -> {
            care = logs == null ? Collections.emptyList() : logs;
            if (plants != null) bind(view);
        });
        viewModel.getPlants().observe(getViewLifecycleOwner(), values -> {
            plants = values == null ? Collections.emptyList() : values;
            loading.setVisibility(View.GONE);
            error.setVisibility(View.GONE);
            bind(view);
        });
    }

    private void bind(View view) {
        long now = System.currentTimeMillis();
        boolean empty = plants.isEmpty();
        view.findViewById(R.id.gardenEmpty).setVisibility(empty ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.gardenContent).setVisibility(empty ? View.GONE : View.VISIBLE);
        view.findViewById(R.id.gardenShelf).setVisibility(empty ? View.GONE : View.VISIBLE);
        overview = GardenOverview.of(plants, care, now);
        String count = getResources().getQuantityString(R.plurals.garden_plants_short, plants.size(), plants.size());
        ((TextView) view.findViewById(R.id.gardenSubtitle)).setText(empty ? getString(R.string.garden_subtitle)
                : overview.namedAreas == 0 ? getResources().getQuantityString(R.plurals.garden_plant_count, plants.size(), plants.size())
                : getString(R.string.garden_subtitle_areas, count,
                        getResources().getQuantityString(R.plurals.garden_areas_short, overview.namedAreas, overview.namedAreas)));
        if (empty) {
            view.findViewById(R.id.gardenActivitySection).setVisibility(View.GONE);
            return;
        }
        if (viewModel.selectedDay < 0) viewModel.selectedDay = overview.todayIndex;
        bindWeek(view);
        bindDay(view, false);
        bindTip(view, now);
        bindZones(view);
        bindShelf();
        bindActivity(view);
        if (monthSheet != null && monthSheet.isShowing()) bindMonth();
    }

    // ---------------- Week strip ----------------

    private void bindWeek(View view) {
        LinearLayout days = view.findViewById(R.id.gardenWeekDays);
        days.removeAllViews();
        String[] names = getResources().getStringArray(R.array.garden_week_days);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < GardenOverview.DAYS; i++) {
            View day = inflater.inflate(R.layout.item_week_day, days, false);
            cal.setTimeInMillis(overview.weekDays[i]);
            ((TextView) day.findViewById(R.id.weekDayName)).setText(names[i]);
            final int index = i;
            day.setOnClickListener(v -> selectDay(view, index));
            day.setContentDescription(names[i] + " " + cal.get(Calendar.DAY_OF_MONTH));
            days.addView(day);
        }
        bindWeekState(view);
    }

    /** The big date shows the selected day; each dot says whether that day has care and whether it is done. */
    private void bindWeekState(View view) {
        LinearLayout days = view.findViewById(R.id.gardenWeekDays);
        for (int i = 0; i < days.getChildCount(); i++) {
            View day = days.getChildAt(i);
            boolean selected = i == viewModel.selectedDay;
            TextView name = day.findViewById(R.id.weekDayName);
            name.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            name.setTextColor(ContextCompat.getColor(requireContext(), selected ? R.color.color_text_primary : R.color.color_text_secondary));
            day.setSelected(selected);
            day.setAlpha(i < overview.todayIndex && !selected ? 0.6f : 1f);
            boolean planned = overview.weekDots[i] > 0;
            boolean done = planned && i <= overview.todayIndex
                    && (i == overview.todayIndex ? overview.pending == 0 : overview.weekCare[i] >= overview.weekDots[i]);
            day.findViewById(R.id.weekDayDot).setBackground(dot(planned, done));
        }
        Date date = new Date(overview.weekDays[viewModel.selectedDay]);
        ((TextView) view.findViewById(R.id.gardenBigDay)).setText(new SimpleDateFormat("d", Locale.getDefault()).format(date));
        ((TextView) view.findViewById(R.id.gardenBigWeekday)).setText(capitalized(new SimpleDateFormat("EEEE", Locale.getDefault()).format(date)));
        ((TextView) view.findViewById(R.id.gardenBigMonth)).setText(capitalized(new SimpleDateFormat("LLLL, yyyy", Locale.getDefault()).format(date)));
    }

    /** Hollow grey: no care. Solid green: care planned. Green ring: all of that day's care done. */
    private android.graphics.drawable.GradientDrawable dot(boolean planned, boolean done) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        int green = ContextCompat.getColor(requireContext(), R.color.color_primary);
        if (done) {
            d.setColor(ContextCompat.getColor(requireContext(), R.color.color_background));
            d.setStroke(dp(2), green);
        } else if (planned) {
            d.setColor(green);
        } else {
            d.setColor(android.graphics.Color.TRANSPARENT);
            d.setStroke(dp(2), ContextCompat.getColor(requireContext(), R.color.color_border));
        }
        return d;
    }

    private static String capitalized(String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.getDefault()) + text.substring(1);
    }

    // ---------------- Month sheet ----------------

    private void openMonth() {
        if (overview == null) return;
        monthSelected = overview.weekDays[viewModel.selectedDay];
        monthShown = Calendar.getInstance();
        monthShown.setTimeInMillis(monthSelected);
        monthShown.set(Calendar.DAY_OF_MONTH, 1);
        monthSheet = new BottomSheetDialog(requireContext());
        monthSheet.setContentView(R.layout.bottom_sheet_garden_month);
        monthSheet.getBehavior().setSkipCollapsed(true);
        monthSheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        monthSheet.findViewById(R.id.monthPrev).setOnClickListener(v -> { monthShown.add(Calendar.MONTH, -1); bindMonth(); });
        monthSheet.findViewById(R.id.monthNext).setOnClickListener(v -> { monthShown.add(Calendar.MONTH, 1); bindMonth(); });

        LinearLayout header = monthSheet.findViewById(R.id.monthWeekdays);
        for (String name : getResources().getStringArray(R.array.garden_week_days)) {
            TextView label = new TextView(requireContext());
            label.setText(name);
            label.setGravity(android.view.Gravity.CENTER);
            label.setTextAppearance(R.style.TextAppearance_PlantMate_Meta);
            label.setTextSize(12);
            header.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        LinearLayout legend = monthSheet.findViewById(R.id.monthLegend);
        legend.addView(legendItem(R.string.garden_month_legend_planned, true, false));
        legend.addView(legendItem(R.string.garden_month_legend_done, true, true));
        legend.addView(legendItem(R.string.garden_month_legend_none, false, false));
        bindMonth();
        monthSheet.show();
    }

    private TextView legendItem(int label, boolean planned, boolean done) {
        TextView item = new TextView(requireContext());
        item.setText(label);
        item.setTextAppearance(R.style.TextAppearance_PlantMate_Meta);
        item.setTextSize(12);
        GradientDrawable d = dot(planned, done);
        d.setSize(dp(8), dp(8));
        item.setCompoundDrawablesRelativeWithIntrinsicBounds(d, null, null, null);
        item.setCompoundDrawablePadding(dp(6));
        item.setPaddingRelative(0, 0, dp(16), 0);
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return item;
    }

    /** Grid of the shown month (Monday first) and the care of the selected day below it. */
    private void bindMonth() {
        BottomSheetDialog sheet = monthSheet;
        if (sheet == null || overview == null || !isAdded()) return;
        long now = System.currentTimeMillis();
        ((TextView) sheet.findViewById(R.id.monthTitle)).setText(
                capitalized(new SimpleDateFormat("LLLL, yyyy", Locale.getDefault()).format(monthShown.getTime())));
        int[] states = overview.monthStates(monthShown.getTimeInMillis(), plants, care, now);
        long first = GardenOverview.startOfDay(monthShown.getTimeInMillis());
        long todayStart = overview.weekDays[overview.todayIndex];
        int lead = (monthShown.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        LinearLayout grid = sheet.findViewById(R.id.monthGrid);
        grid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int ink = ContextCompat.getColor(requireContext(), R.color.color_text_primary);
        int paper = ContextCompat.getColor(requireContext(), R.color.color_background);
        // Always six rows so the sheet keeps its height (and the arrows their place) from month to month.
        for (int start = 0; start < 42; start += 7) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 7; c++) {
                View cell = inflater.inflate(R.layout.item_month_day, row, false);
                row.addView(cell);
                int index = start + c - lead;
                if (index < 0 || index >= states.length) {
                    cell.setVisibility(View.INVISIBLE);
                    continue;
                }
                long day = GardenRepository.addCalendarDays(first, index);
                boolean selected = day == monthSelected;
                TextView number = cell.findViewById(R.id.monthDayNumber);
                number.setText(String.valueOf(index + 1));
                number.setTextColor(selected ? paper : ink);
                number.setTypeface(null, day == todayStart || selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
                cell.findViewById(R.id.monthDayDot).setBackground(
                        dot(states[index] != GardenOverview.DAY_NONE, states[index] == GardenOverview.DAY_DONE));
                if (selected) {
                    GradientDrawable fill = new GradientDrawable();
                    fill.setColor(ink);
                    fill.setCornerRadius(dp(12));
                    cell.setBackground(fill);
                } else {
                    cell.setBackgroundResource(R.drawable.bg_month_day);
                }
                cell.setAlpha(day < todayStart && !selected ? 0.7f : 1f);
                cell.setContentDescription(DateUtils.formatDateTime(requireContext(), day,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_WEEKDAY));
                cell.setOnClickListener(v -> { monthSelected = day; bindMonth(); });
            }
            grid.addView(row);
        }

        List<GardenOverview.Task> tasks = overview.tasksForDay(monthSelected, plants, care, now);
        String date = capitalized(new SimpleDateFormat("EEEE d/M", Locale.getDefault()).format(new Date(monthSelected)));
        ((TextView) sheet.findViewById(R.id.monthDayTitle)).setText(tasks.isEmpty() ? date
                : getString(R.string.garden_month_day_title, date, tasks.size()));
        LinearLayout list = sheet.findViewById(R.id.monthDayTasks);
        list.removeAllViews();
        for (GardenOverview.Task task : tasks) list.addView(taskRow(inflater, list, task));
        sheet.findViewById(R.id.monthDayEmpty).setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override public void onDestroyView() {
        if (monthSheet != null) monthSheet.dismiss();
        monthSheet = null;
        super.onDestroyView();
    }

    /** Tapping a day moves the big date to it; the day's care fades in. */
    private void selectDay(View view, int day) {
        if (overview == null || day == viewModel.selectedDay) return;
        viewModel.selectedDay = day;
        bindWeekState(view);
        bindDay(view, true);
    }

    // ---------------- Day panel ----------------

    private void bindDay(View view, boolean fade) {
        long now = System.currentTimeMillis();
        boolean today = viewModel.selectedDay == overview.todayIndex;
        List<GardenOverview.Task> tasks = overview.tasksOn(viewModel.selectedDay, plants, care, now);
        TextView title = view.findViewById(R.id.gardenTodayTitle);
        LinearProgressIndicator progress = view.findViewById(R.id.gardenTodayProgress);
        int total = today ? overview.pending + overview.done : tasks.size();
        if (today) {
            title.setText(overview.pending > 0
                    ? getResources().getQuantityString(R.plurals.garden_today_left, overview.pending, overview.pending)
                    : getString(total > 0 ? R.string.garden_today_all_done : R.string.garden_today_none));
            setCount(view, total > 0 ? getString(R.string.garden_count_done, overview.done, total) : "");
            progress.setProgressCompat(total == 0 ? 0 : overview.done * 100 / total, true);
        } else {
            title.setText(new SimpleDateFormat("EEEE d/M", Locale.getDefault()).format(new Date(overview.weekDays[viewModel.selectedDay])));
            setCount(view, tasks.isEmpty() ? "" : getResources().getQuantityString(R.plurals.garden_day_tasks, tasks.size(), tasks.size()));
            progress.setProgressCompat(viewModel.selectedDay < overview.todayIndex && !tasks.isEmpty() ? 100 : 0, true);
        }
        progress.setVisibility(total > 0 ? View.VISIBLE : View.GONE);

        LinearLayout list = view.findViewById(R.id.gardenTaskList);
        Runnable fill = () -> {
            list.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            String group = null;
            int shown = Math.min(MAX_TASKS, tasks.size());
            for (int i = 0; i < shown; i++) {
                GardenOverview.Task task = tasks.get(i);
                String header = groupOf(task, today);
                if (!header.equals(group)) {
                    group = header;
                    list.addView(groupHeader(inflater, list, header));
                }
                list.addView(taskRow(inflater, list, task));
            }
            int extra = tasks.size() - shown;
            TextView more = view.findViewById(R.id.gardenTasksMore);
            more.setVisibility(extra > 0 ? View.VISIBLE : View.GONE);
            if (extra > 0) more.setText(getString(R.string.garden_tasks_more, extra));
            view.findViewById(R.id.gardenTasksDone).setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
        };
        if (fade && Motion.enabled(requireContext())) {
            list.animate().alpha(0f).setDuration(140).withEndAction(() -> {
                fill.run();
                list.animate().alpha(1f).setDuration(180).start();
            }).start();
        } else {
            fill.run();
        }
    }

    /** "Due now" first, then "Morning · 07:30", "Afternoon · 17:00"… later today; "Done" / "Planned" on other days. */
    private String groupOf(GardenOverview.Task task, boolean today) {
        if (task.state == GardenOverview.TaskState.DONE) return getString(R.string.garden_group_done);
        if (!today) return getString(R.string.garden_group_planned);
        if (task.state == GardenOverview.TaskState.DUE) return getString(R.string.garden_group_now);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(task.at);
        String time = DateFormat.getTimeFormat(requireContext()).format(new Date(task.at));
        int hour = c.get(Calendar.HOUR_OF_DAY);
        return getString(hour < 12 ? R.string.garden_group_morning : hour < 18 ? R.string.garden_group_afternoon
                : R.string.garden_group_evening, time);
    }

    private View groupHeader(LayoutInflater inflater, ViewGroup parent, String text) {
        TextView header = new TextView(requireContext());
        header.setText(text);
        header.setTextAppearance(R.style.TextAppearance_PlantMate_Meta);
        header.setTextSize(13);
        header.setPadding(0, dp(12), 0, dp(2));
        return header;
    }

    /** The counter rolls up into place when it changes. */
    private void setCount(View view, String text) {
        TextView counter = view.findViewById(R.id.gardenTodayCount);
        boolean changed = lastCount != null && !lastCount.equals(text) && !text.isEmpty();
        lastCount = text;
        counter.setText(text);
        if (changed && Motion.enabled(requireContext())) {
            counter.setTranslationY(counter.getHeight() * 0.7f);
            counter.setAlpha(0f);
            counter.animate().translationY(0).alpha(1f).setDuration(420).setInterpolator(new OvershootInterpolator(1.6f)).start();
        }
    }

    private View taskRow(LayoutInflater inflater, ViewGroup parent, GardenOverview.Task task) {
        Context c = requireContext();
        View row = inflater.inflate(R.layout.item_garden_task, parent, false);
        PlantEntity plant = task.plant;
        boolean done = task.state == GardenOverview.TaskState.DONE;
        ImageView photo = row.findViewById(R.id.taskPhoto);
        LocalImageLoader.load(photo, plant.getImageUri(), dp(48), () -> photo.setVisibility(View.VISIBLE));
        ((TextView) row.findViewById(R.id.taskTitle)).setText(plant.getNickname());
        TextView sub = row.findViewById(R.id.taskSub);
        String time = DateFormat.getTimeFormat(c).format(new Date(task.at));
        String room = plant.getRoom().trim();
        if (done) {
            sub.setText(getString(task.type == CareType.WATER ? R.string.garden_watered_at : R.string.garden_task_done_at, time));
            sub.setTextColor(ContextCompat.getColor(c, R.color.color_primary));
            row.findViewById(R.id.taskDone).setVisibility(View.VISIBLE);
        } else {
            String what = getString(taskName(task.type), "").trim();
            String when = task.lateDays > 0
                    ? getResources().getQuantityString(R.plurals.garden_task_late, task.lateDays, task.lateDays)
                    : task.slot > 0 ? getString(R.string.garden_task_slot, task.slot, task.slots) : what;
            sub.setText(room.isEmpty() ? when : getString(R.string.garden_task_meta, when, room));
            if (task.lateDays > 0) sub.setTextColor(ContextCompat.getColor(c, R.color.color_danger));
            if (task.state == GardenOverview.TaskState.DUE && viewModel.selectedDay == overview.todayIndex) {
                TextView water = row.findViewById(R.id.taskWater);
                water.setVisibility(View.VISIBLE);
                water.setContentDescription(getString(R.string.garden_task_done_desc, plant.getNickname()));
                water.setOnClickListener(v -> fillAndWater(water, plant.getId()));
            } else {
                TextView chip = row.findViewById(R.id.taskTime);
                chip.setVisibility(View.VISIBLE);
                chip.setText(viewModel.selectedDay == overview.todayIndex ? time : getString(R.string.garden_reminder_at, time));
            }
        }
        row.setOnClickListener(v -> openPlant(plant.getId()));
        return row;
    }

    /** Green rises inside the button, the label turns white, then the watering is logged. */
    private void fillAndWater(TextView button, long plantId) {
        button.setEnabled(false);
        LayerDrawable layers = (LayerDrawable) button.getBackground().mutate();
        ClipDrawable fill = (ClipDrawable) layers.findDrawableByLayerId(R.id.water_fill);
        int white = ContextCompat.getColor(requireContext(), R.color.color_on_primary);
        if (!Motion.enabled(requireContext())) {
            viewModel.water(plantId);
            return;
        }
        ValueAnimator rise = ValueAnimator.ofInt(0, 10_000);
        rise.setDuration(FILL_MS);
        rise.setInterpolator(new android.view.animation.PathInterpolator(0.5f, 0f, 0.2f, 1f));
        rise.addUpdateListener(a -> {
            int level = (int) a.getAnimatedValue();
            fill.setLevel(level);
            if (level > 5_000) {
                button.setTextColor(white);
                androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(button,
                        android.content.res.ColorStateList.valueOf(white));
            }
        });
        rise.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (isAdded()) viewModel.water(plantId);
            }
        });
        rise.start();
    }

    private static int taskName(CareType type) {
        switch (type) {
            case FERTILIZE: return R.string.garden_task_fertilize_name;
            case REPOT: return R.string.garden_task_repot_name;
            case PRUNE: return R.string.garden_task_prune_name;
            default: return R.string.garden_task_water_name;
        }
    }

    // ---------------- Mầm's tip ----------------

    private void bindTip(View view, long now) {
        GardenTips.EarlyWatering tip = GardenTips.earlyWatering(plants, care, now);
        View card = view.findViewById(R.id.gardenTip);
        card.setVisibility(tip == null ? View.GONE : View.VISIBLE);
        if (tip == null) return;
        ((TextView) view.findViewById(R.id.gardenTipTitle)).setText(
                getString(R.string.garden_tip_early_title, tip.plant.getNickname(), tip.times));
        view.findViewById(R.id.gardenTipAction).setOnClickListener(v -> startActivity(
                new Intent(requireContext(), EditPlantActivity.class).putExtra(EditPlantActivity.EXTRA_PLANT_ID, tip.plant.getId())));
    }

    // ---------------- Area chips and grid ----------------

    private void bindZones(View view) {
        ChipGroup zones = view.findViewById(R.id.gardenZones);
        zones.setOnCheckedStateChangeListener(null);
        zones.removeAllViews();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (GardenOverview.Area area : overview.areas) counts.put(area.room, area.plants.size());
        addZone(zones, getString(R.string.garden_zone_all), plants.size(), null);
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            addZone(zones, e.getKey().isEmpty() ? getString(R.string.garden_area_unset) : e.getKey(), e.getValue(), e.getKey());
        }
        zones.setOnCheckedStateChangeListener((group, ids) -> {
            Chip chip = ids.isEmpty() ? null : group.findViewById(ids.get(0));
            viewModel.areaFilter = chip == null ? null : (String) chip.getTag();
            bindShelf();
        });
    }

    private void addZone(ChipGroup zones, String label, int count, @Nullable String key) {
        Chip chip = (Chip) LayoutInflater.from(requireContext()).inflate(R.layout.item_choice_chip, zones, false);
        chip.setId(View.generateViewId());
        chip.setText(getString(R.string.garden_zone_label, label, count));
        chip.setTag(key);
        chip.setChecked(key == null ? viewModel.areaFilter == null : key.equalsIgnoreCase(String.valueOf(viewModel.areaFilter)));
        zones.addView(chip);
    }

    /** All plants, or those in the chosen area / matching the search; due first, or by name. */
    private void bindShelf() {
        if (plants == null) return;
        long now = System.currentTimeMillis();
        String q = viewModel.query.toLowerCase(Locale.ROOT);
        List<PlantEntity> shown = new ArrayList<>();
        for (PlantEntity p : plants) {
            if (viewModel.areaFilter != null && !p.getRoom().trim().equalsIgnoreCase(viewModel.areaFilter)) continue;
            if (!q.isEmpty() && !(p.getNickname() + " " + p.getSpecies() + " " + p.getRoom())
                    .toLowerCase(Locale.ROOT).contains(q)) continue;
            shown.add(p);
        }
        if (sortByName) {
            shown.sort((a, b) -> a.getNickname().compareToIgnoreCase(b.getNickname()));
        } else {
            shown.sort((a, b) -> Long.compare(nextAt(a, now), nextAt(b, now)));
        }
        shelf.submit(shown, now);
    }

    private static long nextAt(PlantEntity p, long now) {
        WateringStatus s = WateringStatus.of(p, now);
        return s.kind == WateringStatus.Kind.DUE || s.kind == WateringStatus.Kind.UPCOMING ? s.nextAt : Long.MAX_VALUE;
    }

    // ---------------- Recent activity ----------------

    private void bindActivity(View view) {
        LinearLayout list = view.findViewById(R.id.gardenActivity);
        list.removeAllViews();
        Map<Long, PlantEntity> byId = new LinkedHashMap<>();
        for (PlantEntity p : plants) byId.put(p.getId(), p);
        List<CareLogEntity> recent = new ArrayList<>();
        for (CareLogEntity log : care) if (byId.containsKey(log.getPlantId())) recent.add(log);
        recent.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        view.findViewById(R.id.gardenActivitySection).setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int count = Math.min(MAX_ACTIVITY, recent.size());
        for (int i = 0; i < count; i++) {
            CareLogEntity log = recent.get(i);
            View row = inflater.inflate(R.layout.item_garden_activity, list, false);
            row.findViewById(R.id.activityDot).setActivated(i == 0);
            row.findViewById(R.id.activityLine).setVisibility(i == count - 1 ? View.INVISIBLE : View.VISIBLE);
            PlantEntity plant = byId.get(log.getPlantId());
            String name = plant == null ? "" : plant.getNickname();
            ((TextView) row.findViewById(R.id.activityTitle)).setText(log.getType() == CareType.NOTE
                    ? getString(R.string.garden_activity_note, name) : getString(taskName(log.getType()), name));
            ((TextView) row.findViewById(R.id.activityWhen)).setText(DateUtils.getRelativeTimeSpanString(
                    log.getCreatedAt(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            row.setOnClickListener(v -> { if (plant != null) openPlant(plant.getId()); });
            list.addView(row);
        }
    }

    // ---------------- helpers ----------------

    private void openPlant(long id) {
        startActivity(new Intent(requireContext(), PlantDetailActivity.class)
                .putExtra(PlantDetailActivity.EXTRA_PLANT_ID, id));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onResume() {
        super.onResume();
        if (viewModel != null && viewModel.hasAuthenticatedUser()) viewModel.reconcileReminders();
    }
}
