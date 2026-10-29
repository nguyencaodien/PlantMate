package com.example.ungdungchiasecaycanh.ui.garden;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class WateringStatusTest {
    private static final long NOW = 1_700_000_000_000L;
    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    @Test public void reminderOffHasNoSchedule() {
        WateringStatus status = WateringStatus.of(plant(false, 7, NOW + DAY), NOW);
        assertEquals(WateringStatus.Kind.OFF, status.kind);
        assertEquals(0, status.progressPercent);
    }

    @Test public void enabledWithoutNextDateIsNoSchedule() {
        assertEquals(WateringStatus.Kind.NO_SCHEDULE, WateringStatus.of(plant(true, 7, null), NOW).kind);
    }

    @Test public void dueOrOverdueIsDueAtFullProgress() {
        WateringStatus due = WateringStatus.of(plant(true, 7, NOW), NOW);
        assertEquals(WateringStatus.Kind.DUE, due.kind);
        assertEquals(100, due.progressPercent);
        assertEquals(WateringStatus.Kind.DUE, WateringStatus.of(plant(true, 7, NOW - DAY), NOW).kind);
    }

    @Test public void upcomingRoundsDaysUpAndReportsElapsedShare() {
        WateringStatus status = WateringStatus.of(plant(true, 10, NOW + 5 * DAY - 1000), NOW);
        assertEquals(WateringStatus.Kind.UPCOMING, status.kind);
        assertEquals(5, status.daysUntil);
        assertEquals(50, status.progressPercent);
    }

    @Test public void dueCountOnlyCountsDuePlants() {
        assertEquals(2, WateringStatus.dueCount(Arrays.asList(
                plant(true, 7, NOW - 1), plant(true, 7, NOW), plant(true, 7, NOW + DAY),
                plant(false, 7, NOW - 1)), NOW));
        assertEquals(0, WateringStatus.dueCount(null, NOW));
    }

    private static PlantEntity plant(boolean enabled, int interval, Long next) {
        return new PlantEntity(1, "uid-a", "Mochi", "Monstera deliciosa", "", enabled,
                interval, next, NOW - 1000, NOW - 500);
    }
}
