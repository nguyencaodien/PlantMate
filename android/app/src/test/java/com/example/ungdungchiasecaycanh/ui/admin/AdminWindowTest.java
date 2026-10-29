package com.example.ungdungchiasecaycanh.ui.admin;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Collections;
import java.util.TimeZone;

public class AdminWindowTest {
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Test
    public void countsThePeriodAndThePeriodBefore() {
        TimeZone utc = TimeZone.getTimeZone("UTC");
        long now = 100 * DAY + 12 * 60 * 60 * 1000;   // noon, day 100
        AdminHomeViewModel.Dashboard d = new AdminHomeViewModel.Dashboard(0, 0, 0, 0, 0, 0,
                new int[0], new int[0], new long[0], Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
        d.listingTimes = new long[]{now, 95 * DAY, 92 * DAY, 80 * DAY};
        d.listingTypes = new ListingType[]{ListingType.GIVE_AWAY, ListingType.SELL, ListingType.TRADE, ListingType.TRADE};
        d.postTimes = new long[]{94 * DAY, 93 * DAY};
        d.memberTimes = new long[]{99 * DAY, 10 * DAY};

        AdminHomeViewModel.Window week = AdminHomeViewModel.window(d, AdminHomeViewModel.Period.WEEK, now, utc);
        assertEquals(2, week.listings);   // days 94..100
        assertEquals(1, week.give);
        assertEquals(1, week.sell);
        assertEquals(1, week.posts);
        assertEquals(1, week.members);
        assertEquals(2, week.previousActivity);   // days 87..93: one listing, one post

        AdminHomeViewModel.Window today = AdminHomeViewModel.window(d, AdminHomeViewModel.Period.TODAY, now, utc);
        assertEquals(1, today.activity());

        AdminHomeViewModel.Window all = AdminHomeViewModel.window(d, AdminHomeViewModel.Period.ALL, now, utc);
        assertEquals(6, all.activity());
        assertEquals(2, all.members);
        assertEquals(0, all.previousActivity);
    }
}
