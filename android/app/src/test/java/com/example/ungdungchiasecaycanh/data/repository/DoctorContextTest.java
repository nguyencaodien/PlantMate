package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

/** What Plant Doctor is told about the plant, and what a "was this right?" answer stores. */
public class DoctorContextTest {
    private static final long DAY = 86_400_000L;
    private static final long NOW = 100 * DAY;

    @Test public void nothingKnownGivesNoContext() {
        assertEquals("", DoctorRepository.describe(null, null, null, NOW));
    }

    @Test public void plantPlanCareAndSymptomAreDescribed() {
        PlantEntity plant = new PlantEntity(1, "uid", "Mochi", "Monstera", "", true, 7, NOW + DAY, 0, 0);
        String text = DoctorRepository.describe(plant, Arrays.asList(
                new CareLogEntity(2, 1, CareType.WATER, "", NOW - 2 * DAY),
                new CareLogEntity(1, 1, CareType.NOTE, "new leaf is yellow", NOW)), "Lá vàng", NOW);
        assertTrue(text.contains("Mochi (Monstera)"));
        assertTrue(text.contains("every 7 days"));
        assertTrue(text.contains("water, 2 days ago"));
        assertTrue(text.contains("note, today: new leaf is yellow"));
        assertTrue(text.contains("Symptom the owner noticed: Lá vàng"));
    }

    @Test public void dailyPlanIsDescribedAsTimesPerDay() {
        PlantEntity plant = new PlantEntity(1, "uid", "Basil", "", "", true, 1, NOW, 0, 0, "420,1020");
        assertTrue(DoctorRepository.describe(plant, null, null, NOW).contains("2 times a day"));
    }

    @Test public void feedbackKeepsCorrectionOnlyWhenWrong() {
        DoctorHistoryEntity history = new DoctorHistoryEntity(5, "uid", null, "content://x", "Overwatering",
                0.8, "Let it dry", 1, "Pothos", "Epipremnum aureum", false);
        Map<String, Object> right = DoctorRepository.feedbackFields(history, true, "ignored");
        assertEquals(true, right.get("correct"));
        assertEquals("", right.get("correction"));
        assertEquals("Epipremnum aureum", right.get("scientificName"));
        Map<String, Object> wrong = DoctorRepository.feedbackFields(history, false, "  spider mites ");
        assertEquals("spider mites", wrong.get("correction"));
        assertEquals(0.8, wrong.get("confidence"));
        assertFalse(wrong.containsKey("ownerId"));
    }
}
