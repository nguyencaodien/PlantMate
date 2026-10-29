package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

/** Daily watering plans (several times a day) and the watering log limit. */
public class WateringPlanTest {
    private static final long HOUR = 3_600_000L;

    private static long at(int day, int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.MARCH, day, hour, minute, 0);
        return c.getTimeInMillis();
    }

    @Test public void timesAreParsedSortedAndFormattedBack() {
        assertEquals(Arrays.asList(420, 1020), GardenRepository.parseTimes("1020, 420"));
        assertEquals("420,1020", GardenRepository.formatTimes(GardenRepository.parseTimes("1020,420")));
        assertTrue(GardenRepository.parseTimes("").isEmpty());
    }

    @Test public void nextSlotIsLaterTodayOrFirstTomorrow() {
        List<Integer> times = Arrays.asList(420, 1020);
        assertEquals(at(10, 7, 0), GardenRepository.nextSlotAfter(times, at(10, 6, 0)));
        assertEquals(at(10, 17, 0), GardenRepository.nextSlotAfter(times, at(10, 7, 0)));
        assertEquals(at(11, 7, 0), GardenRepository.nextSlotAfter(times, at(10, 18, 0)));
    }

    @Test public void wateringOnTimeOrEarlyMovesToTheNextSlot() {
        PlantEntity plant = daily("420,1020");
        assertEquals(at(10, 17, 0), GardenRepository.nextWateringAtAfterWater(plant, at(10, 7, 5)).longValue());
        assertEquals(at(10, 17, 0), GardenRepository.nextWateringAtAfterWater(plant, at(10, 6, 30)).longValue());
        assertEquals(at(11, 7, 0), GardenRepository.nextWateringAtAfterWater(plant, at(10, 16, 30)).longValue());
    }

    @Test public void dailyPlanStartsAtTheNextTimeAndUsesOneDayCycle() {
        PlantEntity plant = GardenRepository.normalizeForCreate("uid", "Basil", "", "", true, 0, "1020,420", at(10, 8, 0));
        assertEquals("420,1020", plant.getWateringTimes());
        assertEquals(1, plant.getWateringIntervalDays());
        assertEquals(at(10, 17, 0), plant.getNextWateringAt().longValue());
        assertEquals(2, GardenRepository.waterPerDay(plant));
    }

    @Test public void remindersOffClearTheTimes() {
        PlantEntity plant = GardenRepository.normalizeForCreate("uid", "Basil", "", "", false, 0, "420", at(10, 8, 0));
        assertEquals("", plant.getWateringTimes());
        assertEquals(1, GardenRepository.waterPerDay(plant));
    }

    @Test public void badTimesAreRejected() {
        for (String bad : new String[]{"420,420", "420,600,900,1200,1300", "1440", "-1", "7:00"}) {
            try {
                GardenRepository.normalizeForCreate("uid", "Basil", "", "", true, 0, bad, at(10, 8, 0));
                fail("accepted " + bad);
            } catch (IllegalArgumentException expected) { }
        }
    }

    @Test public void changingTimesReschedules() {
        PlantEntity existing = GardenRepository.normalizeForCreate("uid", "Basil", "", "", true, 0, "420", at(10, 8, 0));
        PlantEntity same = GardenRepository.normalizeForUpdate(existing, "uid", "Basil", "", "", true, 0, "420", at(10, 9, 0));
        assertEquals(existing.getNextWateringAt(), same.getNextWateringAt());
        PlantEntity changed = GardenRepository.normalizeForUpdate(existing, "uid", "Basil", "", "", true, 0, "420,1020", at(10, 9, 0));
        assertEquals(at(10, 17, 0), changed.getNextWateringAt().longValue());
    }

    @Test public void limitAllowsThePlannedCountThenRefuses() {
        long now = at(10, 18, 0);
        GardenRepository.checkWaterLimit(Collections.singletonList(at(10, 7, 0)), 2, now);
        try {
            GardenRepository.checkWaterLimit(Arrays.asList(at(10, 7, 0), at(10, 12, 0)), 2, now);
            fail();
        } catch (GardenRepository.CareLimitException limit) {
            assertTrue(limit.dailyLimit);
            assertEquals(at(11, 0, 0), limit.allowedAt);
        }
    }

    /** A plant without a plan still gets one watering a day, but the refusal knows it is not "as planned" (B4). */
    @Test public void limitTellsWhetherThePlantHasAPlan() {
        long now = at(10, 18, 0);
        try {
            GardenRepository.checkWaterLimit(Collections.singletonList(at(10, 9, 0)), 1, now, false);
            fail();
        } catch (GardenRepository.CareLimitException limit) {
            assertTrue(limit.dailyLimit);
            assertFalse(limit.planned);
        }
        try {
            GardenRepository.checkWaterLimit(Collections.singletonList(at(10, 9, 0)), 1, now);
            fail();
        } catch (GardenRepository.CareLimitException limit) {
            assertTrue(limit.planned);
        }
    }

    @Test public void yesterdayDoesNotCountButTheGapStillApplies() {
        long now = at(11, 0, 20);
        try {
            GardenRepository.checkWaterLimit(Collections.singletonList(at(10, 23, 50)), 1, now);
            fail();
        } catch (GardenRepository.CareLimitException limit) {
            assertFalse(limit.dailyLimit);
            assertEquals(at(10, 23, 50) + HOUR, limit.allowedAt);
        }
        GardenRepository.checkWaterLimit(Collections.singletonList(at(10, 22, 0)), 1, now);
    }

    private static PlantEntity daily(String times) {
        return new PlantEntity(1, "uid", "Basil", "", "", true, 1, at(10, 7, 0), 0, 0, times);
    }
}
