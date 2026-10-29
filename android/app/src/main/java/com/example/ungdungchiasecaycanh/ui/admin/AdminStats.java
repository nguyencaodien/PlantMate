package com.example.ungdungchiasecaycanh.ui.admin;

import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Function;

/** Pure aggregation helpers for the admin dashboard charts (no Android types, unit-tested). */
public final class AdminStats {
    private AdminStats() {
    }

    /**
     * Counts events per calendar day for the last {@code days} days, oldest first; the last
     * slot is today. Times outside the window (or 0 = unknown) are ignored.
     */
    public static int[] dailyCounts(List<Long> times, long nowMillis, int days, TimeZone zone) {
        int[] counts = new int[days];
        long todayStart = startOfDay(nowMillis, zone);
        for (Long t : times) {
            if (t == null || t <= 0 || t > nowMillis) continue;
            long dayStart = startOfDay(t, zone);
            int daysAgo = daysBetween(dayStart, todayStart, zone);
            if (daysAgo >= 0 && daysAgo < days) counts[days - 1 - daysAgo]++;
        }
        return counts;
    }

    /** Start (00:00) of each day in the window, oldest first, for axis labels. */
    public static long[] dayStarts(long nowMillis, int days, TimeZone zone) {
        long[] starts = new long[days];
        Calendar c = Calendar.getInstance(zone);
        c.setTimeInMillis(startOfDay(nowMillis, zone));
        c.add(Calendar.DAY_OF_YEAR, -(days - 1));
        for (int i = 0; i < days; i++) {
            starts[i] = c.getTimeInMillis();
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return starts;
    }

    /** Counts items per enum value; every value is present (zero when unused). */
    public static <T, E extends Enum<E>> Map<E, Integer> countBy(List<T> items, Class<E> type, Function<T, E> key) {
        Map<E, Integer> counts = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) counts.put(value, 0);
        for (T item : items) {
            E k = key.apply(item);
            if (k != null) counts.put(k, counts.get(k) + 1);
        }
        return counts;
    }

    public static int sum(int[] values) {
        int total = 0;
        for (int v : values) total += v;
        return total;
    }

    /** Whole-number percentage of {@code part} in {@code total}; 0 when total is 0. */
    public static int percent(int part, int total) {
        return total <= 0 ? 0 : Math.round(part * 100f / total);
    }

    static long startOfDay(long millis, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone);
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static int daysBetween(long fromDayStart, long toDayStart, TimeZone zone) {
        Calendar c = Calendar.getInstance(zone);
        c.setTimeInMillis(fromDayStart);
        int days = 0;
        while (c.getTimeInMillis() < toDayStart && days <= 366) {
            c.add(Calendar.DAY_OF_YEAR, 1);
            days++;
        }
        return c.getTimeInMillis() == toDayStart ? days : -1;
    }

    /** Sum of the most recent {@code days} entries (the array is oldest first). */
    public static int lastDays(int[] perDay, int days) {
        int sum = 0;
        for (int i = Math.max(0, perDay.length - days); i < perDay.length; i++) sum += perDay[i];
        return sum;
    }
}
