package com.example.ungdungchiasecaycanh.ui.garden;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.text.format.DateFormat;

/**
 * Presentation-only watering state derived from the Room schedule (reminderEnabled,
 * wateringIntervalDays, nextWateringAt). It never invents health data.
 */
public final class WateringStatus {
    public enum Kind { OFF, NO_SCHEDULE, DUE, UPCOMING }

    public final Kind kind;
    /** Whole days until the next watering; 0 when due or overdue. */
    public final int daysUntil;
    /** Elapsed share of the current interval, 0..100. */
    public final int progressPercent;
    /** Watered at fixed times every day (see {@link PlantEntity#getWateringTimes()}). */
    public final boolean daily;
    /** The next planned watering, 0 without a schedule. */
    public final long nextAt;

    private WateringStatus(Kind kind, int daysUntil, int progressPercent, boolean daily, long nextAt) {
        this.kind = kind;
        this.daysUntil = daysUntil;
        this.progressPercent = progressPercent;
        this.daily = daily;
        this.nextAt = nextAt;
    }

    public static WateringStatus of(PlantEntity plant, long now) {
        if (plant == null || !plant.isReminderEnabled()) return new WateringStatus(Kind.OFF, 0, 0, false, 0);
        boolean daily = !plant.getWateringTimes().isEmpty();
        Long next = plant.getNextWateringAt();
        int interval = plant.getWateringIntervalDays();
        if (next == null || interval < 1) return new WateringStatus(Kind.NO_SCHEDULE, 0, 0, daily, 0);
        long remaining = next - now;
        if (remaining <= 0) return new WateringStatus(Kind.DUE, 0, 100, daily, next);
        // Daily plans count calendar days (0 = later today); every-N-days plans round up.
        int days = daily ? Math.max(0, CareLogAdapter.calendarDaysBetween(now, next))
                : (int) Math.ceil(remaining / (double) TimeUnit.DAYS.toMillis(1));
        long intervalMillis = TimeUnit.DAYS.toMillis(interval);
        long elapsed = Math.max(0, intervalMillis - remaining);
        int progress = (int) Math.min(100, Math.round(elapsed * 100.0 / intervalMillis));
        return new WateringStatus(Kind.UPCOMING, days, progress, daily, next);
    }

    /** "At 17:00" later today, or "Tomorrow, 07:00", for a daily plan's next watering. */
    public static String dailyLabel(Context context, long nextAt, long now) {
        String time = DateFormat.getTimeFormat(context).format(new Date(nextAt));
        return context.getString(CareLogAdapter.calendarDaysBetween(now, nextAt) <= 0
                ? R.string.water_at_today : R.string.water_at_tomorrow, time);
    }

    /** "2 times a day". */
    public static String timesPerDay(Context context, PlantEntity plant) {
        int count = GardenRepository.waterPerDay(plant);
        return context.getResources().getQuantityString(R.plurals.garden_times_per_day, count, count);
    }

    /** Why a care log was refused; a generic message for anything but the watering limit. */
    public static String careError(Context context, Exception error) {
        if (!(error instanceof GardenRepository.CareLimitException)) return context.getString(R.string.care_action_error);
        GardenRepository.CareLimitException limit = (GardenRepository.CareLimitException) error;
        if (limit.dailyLimit && !limit.planned) return context.getString(R.string.care_limit_daily_unplanned);
        if (limit.dailyLimit) {
            return context.getResources().getQuantityString(R.plurals.care_limit_daily, limit.perDay, limit.perDay);
        }
        return context.getString(R.string.care_limit_soon,
                DateFormat.getTimeFormat(context).format(new Date(limit.allowedAt)));
    }

    public static int dueCount(List<PlantEntity> plants, long now) {
        if (plants == null) return 0;
        int count = 0;
        for (PlantEntity plant : plants) {
            if (of(plant, now).kind == Kind.DUE) count++;
        }
        return count;
    }
}
