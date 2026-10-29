package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.Test;

public class GardenRepositoryTest {
    private static final long NOW = 1_700_000_000_000L;

    @Test public void createNormalizesValuesAndAcceptsContentUri() {
        PlantEntity plant = GardenRepository.normalizeForCreate("user-a", "  Mochi  ",
                "  Monstera  ", "content://plants/1", false, 200, "", NOW);
        assertEquals("Mochi", plant.getNickname());
        assertEquals("Monstera", plant.getSpecies());
        assertEquals("content://plants/1", plant.getImageUri());
        assertFalse(plant.isReminderEnabled());
        assertEquals(0, plant.getWateringIntervalDays());
        assertNull(plant.getNextWateringAt());
    }

    @Test(expected = IllegalArgumentException.class)
    public void blankNicknameRejected() {
        GardenRepository.normalizeForCreate("user-a", "  ", "", "", false, 0, "", NOW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void longSpeciesRejected() {
        GardenRepository.normalizeForCreate("user-a", "Mochi", repeat('s', 101), "", false, 0, "", NOW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void remoteImageUriRejected() {
        GardenRepository.normalizeForCreate("user-a", "Mochi", "", "https://example.test/a.jpg", false, 0, "", NOW);
    }

    @Test public void enabledReminderCalculatesNextDate() {
        PlantEntity plant = GardenRepository.normalizeForCreate("user-a", "Mochi", "", "",
                true, 7, "", NOW);
        assertTrue(plant.isReminderEnabled());
        assertEquals(7, plant.getWateringIntervalDays());
        assertEquals(GardenRepository.addCalendarDays(NOW, 7), plant.getNextWateringAt().longValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void reminderIntervalBelowRangeRejected() {
        GardenRepository.normalizeForCreate("user-a", "Mochi", "", "", true, 0, "", NOW);
    }

    @Test(expected = IllegalArgumentException.class)
    public void reminderIntervalAboveRangeRejected() {
        GardenRepository.normalizeForCreate("user-a", "Mochi", "", "", true, 366, "", NOW);
    }

    @Test public void editPreservesIdentityAndUnchangedSchedule() {
        PlantEntity existing = plant("user-a", true, 7, NOW + 12345);
        PlantEntity updated = GardenRepository.normalizeForUpdate(existing, "user-a",
                "New name", "", "", true, 7, "", NOW + 5000);
        assertEquals(42, updated.getId());
        assertEquals("user-a", updated.getOwnerUid());
        assertEquals(NOW - 1000, updated.getCreatedAt());
        assertEquals(NOW + 5000, updated.getUpdatedAt());
        assertEquals(NOW + 12345, updated.getNextWateringAt().longValue());
    }

    @Test public void changedIntervalRecalculatesSchedule() {
        PlantEntity existing = plant("user-a", true, 7, NOW + 12345);
        PlantEntity updated = GardenRepository.normalizeForUpdate(existing, "user-a",
                "Mochi", "", "", true, 10, "", NOW);
        assertEquals(GardenRepository.addCalendarDays(NOW, 10), updated.getNextWateringAt().longValue());
    }

    @Test public void disablingReminderClearsSchedule() {
        PlantEntity updated = GardenRepository.normalizeForUpdate(plant("user-a", true, 7, NOW + 1),
                "user-a", "Mochi", "", "", false, 7, "", NOW);
        assertFalse(updated.isReminderEnabled());
        assertEquals(0, updated.getWateringIntervalDays());
        assertNull(updated.getNextWateringAt());
    }

    @Test(expected = SecurityException.class)
    public void wrongOwnerCannotNormalizeUpdate() {
        GardenRepository.normalizeForUpdate(plant("user-a", false, 0, null), "user-b",
                "Mochi", "", "", false, 0, "", NOW);
    }

    @Test public void ownershipCheckIsAccountScoped() {
        PlantEntity plant = plant("user-a", false, 0, null);
        assertTrue(GardenRepository.isOwnedBy(plant, "user-a"));
        assertFalse(GardenRepository.isOwnedBy(plant, "user-b"));
        assertFalse(GardenRepository.isOwnedBy(plant, null));
    }

    private static PlantEntity plant(String owner, boolean enabled, int interval, Long next) {
        return new PlantEntity(42, owner, "Mochi", "", "", enabled, interval,
                next, NOW - 1000, NOW - 500);
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }

    @Test public void areaAndLightAreNormalizedAndKeptOnUpdate() {
        PlantEntity plant = GardenRepository.normalizeForCreate("user-a", "Mochi", "", "", false, 0, "",
                "  Ban   công ", "SUN", NOW);
        assertEquals("Ban công", plant.getRoom());
        assertEquals("SUN", plant.getLight());
        PlantEntity renamed = GardenRepository.normalizeForUpdate(plant, "user-a", "Mochi 2", "", "",
                false, 0, "", NOW + 1);
        assertEquals("Ban công", renamed.getRoom());
        assertEquals("SUN", renamed.getLight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownLightIsRefused() {
        GardenRepository.normalizeForCreate("user-a", "Mochi", "", "", false, 0, "", "", "MOONLIGHT", NOW);
    }
}
