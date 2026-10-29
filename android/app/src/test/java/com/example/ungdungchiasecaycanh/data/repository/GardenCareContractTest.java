package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

public class GardenCareContractTest {
    private static final long ACTION_TIME = 1_700_000_000_000L;

    @Test public void careNotesAreTrimmedAndOptionalExceptForNoteType() {
        for (CareType type : Arrays.asList(CareType.WATER, CareType.FERTILIZE, CareType.REPOT, CareType.PRUNE)) {
            assertEquals("", GardenRepository.normalizeCareNote(type, "   "));
            assertEquals("care", GardenRepository.normalizeCareNote(type, "  care  "));
            assertEquals(500, GardenRepository.normalizeCareNote(type, repeat('x', 500)).length());
            expectInvalid(type, repeat('x', 501));
        }
        expectInvalid(CareType.NOTE, "");
        expectInvalid(CareType.NOTE, "  ");
        assertEquals("x", GardenRepository.normalizeCareNote(CareType.NOTE, " x "));
        assertEquals(500, GardenRepository.normalizeCareNote(CareType.NOTE, repeat('x', 500)).length());
        expectInvalid(CareType.NOTE, repeat('x', 501));
    }

    @Test public void careLogCreationUsesOnlyFrozenDataAndActionTime() throws Exception {
        CareLogEntity log = new CareLogEntity(0, 7, CareType.WATER,
                GardenRepository.normalizeCareNote(CareType.WATER, "  Morning  "), ACTION_TIME);
        assertEquals(0, log.getId());
        assertEquals(7, log.getPlantId());
        assertEquals(CareType.WATER, log.getType());
        assertEquals("Morning", log.getNote());
        assertEquals(ACTION_TIME, log.getCreatedAt());
        assertEquals(5, persistedFieldCount(CareLogEntity.class));
        for (Field field : CareLogEntity.class.getDeclaredFields()) assertFalse("ownerUid".equals(field.getName()));
    }

    @Test public void waterReschedulesEnabledPlantByCalendarInterval() {
        PlantEntity plant = plant(true, 7, ACTION_TIME - 1000);
        Long next = GardenRepository.nextWateringAtAfterWater(plant, ACTION_TIME);
        assertEquals(GardenRepository.addCalendarDays(ACTION_TIME, 7), next.longValue());
        assertEquals(7, plant.getWateringIntervalDays());
        assertTrue(plant.isReminderEnabled());
    }

    @Test public void waterWithReminderOffDoesNotEnableOrSchedule() {
        PlantEntity plant = plant(false, 0, null);
        assertNull(GardenRepository.nextWateringAtAfterWater(plant, ACTION_TIME));
        assertFalse(plant.isReminderEnabled());
        assertEquals(0, plant.getWateringIntervalDays());
        assertNull(plant.getNextWateringAt());
    }

    @Test(expected = IllegalStateException.class)
    public void malformedEnabledReminderDoesNotInventSchedule() {
        GardenRepository.nextWateringAtAfterWater(plant(true, 0, null), ACTION_TIME);
    }


    @Test public void persistedEntityFieldCountsRemainFrozen() {
        // v6 added room and light (Garden areas); a new column needs a migration and this count bumped.
        assertEquals(13, persistedFieldCount(PlantEntity.class));
        assertEquals(5, persistedFieldCount(CareLogEntity.class));
    }

    @Test public void onlyThePlantOwnerPassesOwnershipGuard() {
        PlantEntity plant = plant(true, 7, ACTION_TIME);
        assertTrue(GardenRepository.isOwnedBy(plant, "uid-a"));
        assertFalse(GardenRepository.isOwnedBy(plant, "uid-b"));
        assertFalse(GardenRepository.isOwnedBy(plant, null));
        assertFalse(GardenRepository.isOwnedBy(null, "uid-a"));
    }

    @Test public void nonWaterCareTypesDoNotHaveScheduleMutationPath() {
        for (CareType type : Arrays.asList(CareType.FERTILIZE, CareType.REPOT, CareType.PRUNE, CareType.NOTE)) {
            assertFalse(GardenRepository.shouldRescheduleAfterCare(type, true));
        }
        assertFalse(GardenRepository.shouldRescheduleAfterCare(CareType.WATER, false));
        assertTrue(GardenRepository.shouldRescheduleAfterCare(CareType.WATER, true));
    }

    private static PlantEntity plant(boolean enabled, int interval, Long next) {
        return new PlantEntity(22, "uid-a", "Mochi", "", "", enabled, interval,
                next, ACTION_TIME - 2000, ACTION_TIME - 1000);
    }

    private static int persistedFieldCount(Class<?> type) {
        int count = 0;
        for (Field field : type.getDeclaredFields()) if (!field.isSynthetic()) count++;
        return count;
    }

    private static void expectInvalid(CareType type, String value) {
        try { GardenRepository.normalizeCareNote(type, value); throw new AssertionError("Expected invalid note"); }
        catch (IllegalArgumentException expected) { }
    }

    private static String repeat(char value, int count) {
        char[] chars = new char[count]; Arrays.fill(chars, value); return new String(chars);
    }
}
