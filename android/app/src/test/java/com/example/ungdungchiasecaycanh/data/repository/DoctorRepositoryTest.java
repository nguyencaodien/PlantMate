package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.model.PlantDiagnosisResult;

import org.junit.Test;

public class DoctorRepositoryTest {
    @Test public void resultIsTrimmedAndConfidenceMayBeNull() {
        PlantDiagnosisResult result = DoctorRepository.normalizeResult(
                new PlantDiagnosisResult("  issue  ", null, "  guidance  "));
        assertEquals("issue", result.getDiagnosis());
        assertEquals("guidance", result.getGuidance());
        assertNull(result.getConfidence());
    }

    @Test public void diagnosisBoundaryAccepted() {
        assertEquals(200, DoctorRepository.normalizeResult(new PlantDiagnosisResult(
                repeat('d', 200), 0d, "care")).getDiagnosis().length());
    }
    @Test(expected = IllegalArgumentException.class) public void blankDiagnosisRejected() {
        DoctorRepository.normalizeResult(new PlantDiagnosisResult(" ", null, "care"));
    }
    @Test(expected = IllegalArgumentException.class) public void longDiagnosisRejected() {
        DoctorRepository.normalizeResult(new PlantDiagnosisResult(repeat('d', com.example.ungdungchiasecaycanh.data.remote.DoctorAiParser.MAX_DIAGNOSIS + 1), null, "care"));
    }
    @Test public void guidanceBoundaryAccepted() {
        assertEquals(2000, DoctorRepository.normalizeResult(new PlantDiagnosisResult(
                "issue", 1d, repeat('g', 2000))).getGuidance().length());
    }
    @Test(expected = IllegalArgumentException.class) public void blankGuidanceRejected() {
        DoctorRepository.normalizeResult(new PlantDiagnosisResult("issue", null, " "));
    }
    @Test(expected = IllegalArgumentException.class) public void longGuidanceRejected() {
        DoctorRepository.normalizeResult(new PlantDiagnosisResult("issue", null, repeat('g', 2001)));
    }
    @Test public void confidenceBoundariesAccepted() {
        assertEquals(0d, DoctorRepository.normalizeResult(new PlantDiagnosisResult("i", 0d, "g")).getConfidence(), 0d);
        assertEquals(1d, DoctorRepository.normalizeResult(new PlantDiagnosisResult("i", 1d, "g")).getConfidence(), 0d);
    }
    @Test(expected = IllegalArgumentException.class) public void negativeConfidenceRejected() {
        DoctorRepository.normalizeResult(new PlantDiagnosisResult("i", -0.1d, "g"));
    }
    @Test(expected = IllegalArgumentException.class) public void highConfidenceRejected() {
        DoctorRepository.normalizeResult(new PlantDiagnosisResult("i", 1.1d, "g"));
    }

    @Test public void historyFactoryUsesExactCanonicalValues() {
        DoctorHistoryEntity history = DoctorRepository.createHistory(" user-a ", 9L,
                "content://doctor/image", new PlantDiagnosisResult(" issue ", 0.82, " care "), 42L);
        assertEquals(0, history.getId()); assertEquals("user-a", history.getOwnerUid());
        assertEquals(Long.valueOf(9), history.getPlantId());
        assertEquals("content://doctor/image", history.getImageUri());
        assertEquals("issue", history.getDiagnosis()); assertEquals(0.82, history.getConfidence(), 0d);
        assertEquals("care", history.getGuidance()); assertEquals(42L, history.getCreatedAt());
    }

    @Test public void historyKeepsTrimmedIdentificationAndHealth() {
        DoctorHistoryEntity history = DoctorRepository.createHistory("user-a", null, "content://doctor/image",
                new PlantDiagnosisResult("issue", 0.5, "care", "  Trầu bà ", " ", false), 1L);
        assertEquals("Trầu bà", history.getPlantName());
        assertEquals(null, history.getScientificName());
        assertEquals(Boolean.FALSE, history.getHealthy());
        assertEquals(100, DoctorRepository.normalizeResult(new PlantDiagnosisResult("i", null, "g",
                repeat('n', 150), null, null)).getPlantName().length());
    }

    @Test public void sameOwnerPlantAccepted() {
        DoctorRepository.validatePlantLink(plant("user-a"), "user-a");
    }
    @Test(expected = SecurityException.class) public void crossOwnerPlantRejected() {
        DoctorRepository.validatePlantLink(plant("user-a"), "user-b");
    }
    @Test(expected = SecurityException.class) public void missingAuthenticationRejected() {
        DoctorRepository.validatePlantLink(plant("user-a"), null);
    }

    private static PlantEntity plant(String owner) {
        return new PlantEntity(9, owner, "Plant", "", "", false, 0, null, 1, 1);
    }
    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }
}
