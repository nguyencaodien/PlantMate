package com.example.ungdungchiasecaycanh.ui.admin;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class AdminStatsTest {
    private static final TimeZone VN = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    private static long at(int year, int month, int day, int hour) {
        Calendar c = Calendar.getInstance(VN);
        c.clear();
        c.set(year, month - 1, day, hour, 0);
        return c.getTimeInMillis();
    }

    @Test public void dailyCountsBucketByLocalDayOldestFirst() {
        long now = at(2026, 9, 26, 14);
        List<Long> times = Arrays.asList(
                at(2026, 9, 26, 9), at(2026, 9, 26, 0),   // today ×2
                at(2026, 9, 25, 23),                      // yesterday
                at(2026, 9, 20, 12),                      // 6 days ago
                at(2026, 9, 10, 12),                      // outside a 7-day window
                0L, null);
        int[] counts = AdminStats.dailyCounts(times, now, 7, VN);
        assertArrayEquals(new int[]{1, 0, 0, 0, 0, 1, 2}, counts);
        assertEquals(4, AdminStats.sum(counts));
    }

    @Test public void futureTimesAreIgnored() {
        long now = at(2026, 9, 26, 14);
        int[] counts = AdminStats.dailyCounts(Arrays.asList(at(2026, 9, 27, 8)), now, 3, VN);
        assertArrayEquals(new int[]{0, 0, 0}, counts);
    }

    @Test public void dayStartsCoverTheWindow() {
        long now = at(2026, 9, 26, 14);
        long[] starts = AdminStats.dayStarts(now, 3, VN);
        assertEquals(at(2026, 9, 24, 0), starts[0]);
        assertEquals(at(2026, 9, 26, 0), starts[2]);
    }

    @Test public void countByIncludesEveryValue() {
        Map<ListingType, Integer> counts = AdminStats.countBy(
                Arrays.asList(ListingType.GIVE_AWAY, ListingType.GIVE_AWAY, ListingType.SELL),
                ListingType.class, t -> t);
        assertEquals(Integer.valueOf(2), counts.get(ListingType.GIVE_AWAY));
        assertEquals(Integer.valueOf(0), counts.get(ListingType.TRADE));
        assertEquals(Integer.valueOf(1), counts.get(ListingType.SELL));
    }

    @Test public void percentRoundsAndHandlesZero() {
        assertEquals(33, AdminStats.percent(1, 3));
        assertEquals(0, AdminStats.percent(3, 0));
    }
}
