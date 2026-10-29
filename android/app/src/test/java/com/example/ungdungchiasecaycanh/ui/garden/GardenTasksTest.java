package com.example.ungdungchiasecaycanh.ui.garden;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.model.LightLevel;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class GardenTasksTest {
    private static final long DAY = TimeUnit.DAYS.toMillis(1);
    private static final long HOUR = TimeUnit.HOURS.toMillis(1);

    /** Wednesday 7 Oct 2026, 10:00 local time (week index 2). */
    private static long now() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.OCTOBER, 7, 10, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static PlantEntity plant(long id, Long next, String room, String light) {
        return new PlantEntity(id, "uid", "p" + id, "", "", next != null, 3, next, 0, 0, "", room, light);
    }

    private static CareLogEntity log(long plantId, CareType type, long at) {
        return new CareLogEntity(0, plantId, type, "", at);
    }

    @Test public void todayListsDueThenLaterThenDone() {
        long now = now();
        GardenOverview o = GardenOverview.of(Arrays.asList(
                plant(1, now + 5 * HOUR, "", ""),          // later today
                plant(2, now - DAY - HOUR, "", ""),        // one day late
                plant(3, now + 2 * DAY, "", ""),           // not today
                plant(4, null, "", "")),                   // no schedule
                Collections.singletonList(log(3, CareType.FERTILIZE, now - HOUR)), now);
        assertEquals(2, o.pending);
        assertEquals(1, o.done);
        assertEquals(2, o.today.get(0).plant.getId());
        assertEquals(GardenOverview.TaskState.DUE, o.today.get(0).state);
        assertEquals(1, o.today.get(0).lateDays);
        assertEquals(GardenOverview.TaskState.LATER, o.today.get(1).state);
        assertEquals(GardenOverview.TaskState.DONE, o.today.get(2).state);
        assertEquals(CareType.FERTILIZE, o.today.get(2).type);
    }

    @Test public void weekCountsLoggedCarePlannedWateringsAndStreak() {
        long now = now();
        GardenOverview o = GardenOverview.of(Collections.singletonList(plant(1, now + 2 * DAY, "", "")),
                Arrays.asList(
                        log(1, CareType.WATER, now - HOUR),           // today (Wed)
                        log(1, CareType.WATER, now - DAY),            // Tue
                        log(1, CareType.NOTE, now - 2 * DAY),         // Mon, a note: not care
                        log(1, CareType.PRUNE, now - 3 * DAY)), now); // last Sunday
        assertEquals(2, o.todayIndex);
        assertEquals(0, o.weekCare[0]);
        assertEquals(1, o.weekCare[1]);
        assertEquals(1, o.weekCare[2]);
        assertEquals(2, o.weekWatered);
        assertEquals(1, o.weekDots[4]);                 // Friday: planned watering
        assertEquals(3, o.weekWaterPlanned);            // 2 done + Friday
        assertEquals(2, o.streak);                      // Tue, Wed (Mon only had a note)
    }

    @Test public void areasGroupByRoomWithCommonLightAndUnsetLast() {
        long now = now();
        GardenOverview o = GardenOverview.of(Arrays.asList(
                plant(1, now - HOUR, "Ban công", LightLevel.SUN.name()),
                plant(2, null, "ban công", LightLevel.SUN.name()),
                plant(3, null, "Ban công", LightLevel.SHADE.name()),
                plant(4, null, "", ""),
                plant(5, null, "Phòng khách", "")), Collections.emptyList(), now);
        assertEquals(3, o.areas.size());
        assertEquals(2, o.namedAreas);
        assertEquals("Ban công", o.areas.get(0).room);
        assertEquals(3, o.areas.get(0).plants.size());
        assertEquals(LightLevel.SUN, o.areas.get(0).light);
        assertEquals(1, o.areas.get(0).due);
        assertEquals("Phòng khách", o.areas.get(1).room);
        assertNull(o.areas.get(1).light);
        assertEquals("", o.areas.get(2).room);
    }

    @Test public void dailyPlanCountsEachTimeOfAFutureDay() {
        long now = now();
        PlantEntity basil = new PlantEntity(1, "uid", "Basil", "", "", true, 1, now + 7 * HOUR, 0, 0,
                "420,1020", "", "");
        GardenOverview o = GardenOverview.of(Collections.singletonList(basil), Collections.emptyList(), now);
        assertEquals(GardenOverview.TaskState.LATER, o.today.get(0).state);
        assertEquals(2, o.today.get(0).slot);
        assertEquals(2, o.today.get(0).slots);
        assertEquals(2, o.weekDots[3]);  // Thursday: 07:00 and 17:00
    }

    @Test public void calendarDaysCountMidnights() {
        Calendar c = Calendar.getInstance();
        c.set(2026, Calendar.SEPTEMBER, 27, 0, 30, 0);
        long earlyToday = c.getTimeInMillis();
        c.set(2026, Calendar.SEPTEMBER, 26, 23, 30, 0);
        long lateYesterday = c.getTimeInMillis();
        c.set(2026, Calendar.SEPTEMBER, 27, 22, 0, 0);
        long tonight = c.getTimeInMillis();
        assertEquals(0, CareLogAdapter.calendarDaysBetween(earlyToday, tonight));
        assertEquals(1, CareLogAdapter.calendarDaysBetween(lateYesterday, earlyToday));
        assertEquals(3, CareLogAdapter.calendarDaysBetween(earlyToday - 3 * DAY, tonight));
    }

    @Test public void monthMarksPastCareTodayAndPlannedDays() {
        long now = now(); // Wed 7 Oct 2026
        GardenOverview o = GardenOverview.of(Collections.singletonList(plant(1, now + 2 * DAY, "", "")),
                Collections.singletonList(log(1, CareType.WATER, now - 3 * DAY)), now);
        int[] month = o.monthStates(now, Collections.singletonList(plant(1, now + 2 * DAY, "", "")),
                Collections.singletonList(log(1, CareType.WATER, now - 3 * DAY)), now);
        assertEquals(31, month.length);
        assertEquals(GardenOverview.DAY_DONE, month[3]);      // 4 Oct: watered
        assertEquals(GardenOverview.DAY_NONE, month[6]);      // today: nothing due or done
        assertEquals(GardenOverview.DAY_PLANNED, month[8]);   // 9 Oct: next watering
        assertEquals(GardenOverview.DAY_PLANNED, month[11]);  // every 3 days after that
        assertEquals(GardenOverview.DAY_NONE, month[9]);
    }
}
