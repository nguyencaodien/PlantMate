package com.example.ungdungchiasecaycanh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.Test;

public class CareReminderSchedulerTest {
    private static final long NOW = 1_700_000_000_000L;

    @Test public void schedulingRequiresEnabledValidFutureSchedule() {
        assertFalse(CareReminderScheduler.shouldSchedule(plant("uid-a", false, 0, null), NOW));
        assertTrue(CareReminderScheduler.shouldSchedule(plant("uid-a", true, 7, NOW + 1), NOW));
        assertFalse(CareReminderScheduler.shouldSchedule(plant("uid-a", true, 7, null), NOW));
        assertFalse(CareReminderScheduler.shouldSchedule(plant("uid-a", true, 7, NOW), NOW));
        assertFalse(CareReminderScheduler.shouldSchedule(plant("uid-a", true, 7, NOW - 1), NOW));
        assertFalse(CareReminderScheduler.shouldSchedule(plant("uid-a", true, 0, NOW + 1), NOW));
        assertFalse(CareReminderScheduler.shouldSchedule(plant("uid-a", true, 366, NOW + 1), NOW));
        assertFalse(CareReminderScheduler.shouldSchedule(
                plant("uid-a", true, 7, NOW + 1), NOW, false));
    }

    @Test public void deliveryRequiresCurrentOwnerAndDueCanonicalSchedule() {
        PlantEntity due = plant("uid-a", true, 7, NOW);
        assertTrue(CareReminderScheduler.shouldDeliver(due, "uid-a", NOW));
        assertFalse(CareReminderScheduler.shouldDeliver(due, "uid-b", NOW));
        assertFalse(CareReminderScheduler.shouldDeliver(due, null, NOW));
        assertFalse(CareReminderScheduler.shouldDeliver(plant("uid-a", true, 7, NOW + 1), "uid-a", NOW));
        assertFalse(CareReminderScheduler.shouldDeliver(plant("uid-a", false, 0, null), "uid-a", NOW));
        assertFalse(CareReminderScheduler.shouldDeliver(due, "uid-a", NOW, false));
    }

    @Test public void alarmIdentityIsStableAndDistinct() {
        assertEquals(CareReminderScheduler.alarmIdentity(42), CareReminderScheduler.alarmIdentity(42));
        assertNotEquals(CareReminderScheduler.alarmIdentity(42), CareReminderScheduler.alarmIdentity(43));
        assertEquals("plantmate://care-reminder/42", CareReminderScheduler.alarmIdentity(42));
    }

    @Test public void notificationIdIsStableForRepresentativeIds() {
        assertEquals(NotificationHelper.notificationId(42), NotificationHelper.notificationId(42));
        assertNotEquals(NotificationHelper.notificationId(42), NotificationHelper.notificationId(43));
        assertNotEquals(NotificationHelper.notificationId(1), NotificationHelper.notificationId(Long.MAX_VALUE));
    }

    private static PlantEntity plant(String ownerUid, boolean enabled, int interval, Long next) {
        return new PlantEntity(42, ownerUid, "Mochi", "Monstera", "", enabled,
                interval, next, NOW - 1000, NOW - 500);
    }
}
