package com.example.ungdungchiasecaycanh.ui.garden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GardenDayAndTipTest {
    private static final long DAY = TimeUnit.DAYS.toMillis(1);
    private static final long HOUR = TimeUnit.HOURS.toMillis(1);

    /** Wednesday 7 Oct 2026, 10:00 local time (week index 2). */
    private static long now() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 7, 10, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static PlantEntity plant(long id, Long next, int interval) {
        return new PlantEntity(id, "uid", "p" + id, "", "", next != null, interval, next, 0, 0, "", "", "");
    }

    private static CareLogEntity log(long plantId, CareType type, long at) {
        return new CareLogEntity(0, plantId, type, "", at);
    }

    @Test public void pastDayListsWhatWasLogged() {
        long now = now();
        List<PlantEntity> plants = Collections.singletonList(plant(1, now + 2 * DAY, 3));
        List<CareLogEntity> logs = Arrays.asList(
                log(1, CareType.WATER, now - DAY - HOUR),    // Tuesday
                log(1, CareType.NOTE, now - DAY),             // notes are not care
                log(1, CareType.FERTILIZE, now - 2 * DAY));   // Monday
        GardenOverview o = GardenOverview.of(plants, logs, now);
        List<GardenOverview.Task> tuesday = o.tasksOn(1, plants, logs, now);
        assertEquals(1, tuesday.size());
        assertEquals(CareType.WATER, tuesday.get(0).type);
        assertEquals(GardenOverview.TaskState.DONE, tuesday.get(0).state);
        assertEquals(1, o.tasksOn(0, plants, logs, now).size());
    }

    @Test public void laterDayListsPlannedWaterings() {
        long now = now();
        List<PlantEntity> plants = Arrays.asList(plant(1, now + 2 * DAY, 3), plant(2, null, 3));
        GardenOverview o = GardenOverview.of(plants, Collections.emptyList(), now);
        assertEquals(1, o.tasksOn(4, plants, Collections.emptyList(), now).size()); // Friday
        assertEquals(0, o.tasksOn(3, plants, Collections.emptyList(), now).size()); // Thursday
        assertSame(o.today, o.tasksOn(2, plants, Collections.emptyList(), now));
    }

    @Test public void earlyWateringNeedsTwoEarlyGaps() {
        long now = now();
        PlantEntity p = plant(1, now + DAY, 10);
        List<CareLogEntity> twice = Arrays.asList(
                log(1, CareType.WATER, now - 20 * DAY),
                log(1, CareType.WATER, now - 15 * DAY),   // 5 days after: early for a 10-day plan
                log(1, CareType.WATER, now - 10 * DAY));  // 5 days after again
        GardenTips.EarlyWatering tip = GardenTips.earlyWatering(Collections.singletonList(p), twice, now);
        assertNotNull(tip);
        assertEquals(2, tip.times);
        List<CareLogEntity> once = Arrays.asList(log(1, CareType.WATER, now - 20 * DAY), log(1, CareType.WATER, now - 15 * DAY));
        assertNull(GardenTips.earlyWatering(Collections.singletonList(p), once, now));
    }

    @Test public void shortPlansAndOldLogsGiveNoTip() {
        long now = now();
        PlantEntity shortPlan = plant(1, now + DAY, 2);
        List<CareLogEntity> logs = Arrays.asList(log(1, CareType.WATER, now - 3 * DAY), log(1, CareType.WATER, now - 2 * DAY),
                log(1, CareType.WATER, now - DAY));
        assertNull(GardenTips.earlyWatering(Collections.singletonList(shortPlan), logs, now));
        PlantEntity longPlan = plant(2, now + DAY, 10);
        List<CareLogEntity> old = Arrays.asList(log(2, CareType.WATER, now - 50 * DAY), log(2, CareType.WATER, now - 45 * DAY),
                log(2, CareType.WATER, now - 40 * DAY));
        assertNull(GardenTips.earlyWatering(Collections.singletonList(longPlan), old, now));
    }
}
