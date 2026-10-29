package com.example.ungdungchiasecaycanh.ui.garden;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.util.DiffList;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/** Plant activity log. Shows the newest {@link #COLLAPSED} entries until expanded. */
public class CareLogAdapter extends RecyclerView.Adapter<CareLogAdapter.CareLogViewHolder> {
    public static final int COLLAPSED = 4;
    private final List<CareLogEntity> all = new ArrayList<>();
    private final DiffList<CareLogEntity> items = new DiffList<>(this, CareLogEntity::getId, log -> Arrays.asList(
            log.getType(), log.getNote(), log.getCreatedAt(),
            calendarDaysBetween(log.getCreatedAt(), System.currentTimeMillis())));
    private boolean expanded;

    public void submitList(List<CareLogEntity> logs) {
        all.clear();
        if (logs != null) all.addAll(logs);
        show();
    }

    /** "See all" inserts the older entries below the first four (and removes them again). */
    public void setExpanded(boolean value) {
        expanded = value;
        show();
    }

    private void show() {
        items.submit(expanded ? all : all.subList(0, Math.min(COLLAPSED, all.size())));
    }

    public boolean isExpanded() { return expanded; }

    public int getTotalCount() { return all.size(); }

    @NonNull @Override public CareLogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CareLogViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_care_log, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull CareLogViewHolder holder, int position) {
        CareLogEntity log = items.get(position);
        Context context = holder.itemView.getContext();
        holder.type.setText(label(log.getType()));
        holder.icon.setImageResource(icon(log.getType()));
        int[] colors = colors(log.getType());
        holder.tile.setBackgroundTintList(ContextCompat.getColorStateList(context, colors[0]));
        holder.icon.setImageTintList(ContextCompat.getColorStateList(context, colors[1]));
        holder.date.setText(daysAgo(context, log.getCreatedAt(), System.currentTimeMillis()));
        holder.time.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(log.getCreatedAt())));
        holder.note.setText(log.getNote());
        holder.note.setVisibility(log.getNote().trim().isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override public int getItemCount() { return items.size(); }

    /** "Today", "Yesterday" or "3 days ago", counted in calendar days; localized by the app. */
    public static String daysAgo(Context context, long then, long now) {
        int days = calendarDaysBetween(then, now);
        if (days <= 0) return context.getString(R.string.plant_value_today);
        if (days == 1) return context.getString(R.string.plant_value_yesterday);
        return context.getResources().getQuantityString(R.plurals.plant_days_ago, days, days);
    }

    static int calendarDaysBetween(long then, long now) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(then);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(now);
        for (Calendar c : new Calendar[]{a, b}) {
            c.set(Calendar.HOUR_OF_DAY, 12);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }
        return (int) Math.round((b.getTimeInMillis() - a.getTimeInMillis()) / 86_400_000.0);
    }

    static int icon(CareType type) {
        switch (type) {
            case WATER: return R.drawable.ic_water_drop;
            case FERTILIZE: return R.drawable.ic_sparkle;
            case REPOT: return R.drawable.ic_pot;
            case PRUNE: return R.drawable.ic_scissors;
            default: return R.drawable.ic_text;
        }
    }

    /** Soft tile and ink per care type, like Leafy's coloured task icons. */
    static int[] colors(CareType type) {
        switch (type) {
            case WATER: return new int[]{R.color.color_water_due_tint, R.color.color_water_due};
            case FERTILIZE: return new int[]{R.color.type_trade_soft, R.color.type_trade_ink};
            case REPOT: return new int[]{R.color.type_sell_soft, R.color.type_sell_ink};
            case PRUNE: return new int[]{R.color.color_tile_lavender, R.color.color_tile_lavender_ink};
            default: return new int[]{R.color.color_primary_tint, R.color.color_on_primary_tint};
        }
    }

    static int label(CareType type) {
        switch (type) {
            case WATER: return R.string.care_water;
            case FERTILIZE: return R.string.care_fertilize;
            case REPOT: return R.string.care_repot;
            case PRUNE: return R.string.care_prune;
            default: return R.string.care_note;
        }
    }

    static final class CareLogViewHolder extends RecyclerView.ViewHolder {
        final TextView type, date, note, time;
        final ImageView icon;
        final View tile;
        CareLogViewHolder(@NonNull View itemView) {
            super(itemView);
            type = itemView.findViewById(R.id.careLogType);
            date = itemView.findViewById(R.id.careLogDate);
            note = itemView.findViewById(R.id.careLogNote);
            icon = itemView.findViewById(R.id.careLogIcon);
            tile = itemView.findViewById(R.id.careLogIconTile);
            time = itemView.findViewById(R.id.careLogTime);
        }
    }
}
