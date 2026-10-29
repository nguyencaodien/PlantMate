package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.example.ungdungchiasecaycanh.model.PlantDiagnosisResult;

import org.junit.Test;

public class DoctorAiParserTest {

    private static final String FULL = "{\"isPlant\":\"true\",\"commonName\":\"Trầu bà\","
            + "\"scientificName\":\"Epipremnum aureum\",\"healthy\":\"false\","
            + "\"diagnosis\":\"Overwatering\",\"confidence\":0.82,"
            + "\"careSteps\":[\"Let the soil dry\",\"  Empty the saucer \",\"\",\"Move to bright light\"]}";

    @Test public void parsesIdentificationHealthAndSteps() throws Exception {
        PlantDiagnosisResult r = DoctorAiParser.parse(FULL);
        assertEquals("Trầu bà", r.getPlantName());
        assertEquals("Epipremnum aureum", r.getScientificName());
        assertFalse(r.getHealthy());
        assertEquals("Overwatering", r.getDiagnosis());
        assertEquals(0.82, r.getConfidence(), 1e-9);
        assertEquals("Let the soil dry\nEmpty the saucer\nMove to bright light", r.getGuidance());
    }

    @Test public void acceptsCodeFencesRealBooleansAndPercentConfidence() throws Exception {
        PlantDiagnosisResult r = DoctorAiParser.parse("```json\n{\"isPlant\":true,\"healthy\":true,"
                + "\"diagnosis\":\"Looks healthy\",\"confidence\":90,\"careSteps\":[\"Keep it up\"]}\n```");
        assertTrue(r.getHealthy());
        assertEquals(0.9, r.getConfidence(), 1e-9);
        assertNull(r.getPlantName());
    }

    @Test public void outOfRangeConfidenceIsDropped() throws Exception {
        assertNull(DoctorAiParser.parse("{\"diagnosis\":\"x\",\"confidence\":250,\"careSteps\":[\"a\"]}").getConfidence());
        assertNull(DoctorAiParser.parse("{\"diagnosis\":\"x\",\"confidence\":-1,\"careSteps\":[\"a\"]}").getConfidence());
    }

    @Test public void notAPlantIsReportedAsSuch() {
        expect(DoctorAiException.Kind.NOT_A_PLANT, "{\"isPlant\":\"false\",\"diagnosis\":\"A cat\",\"careSteps\":[\"-\"]}");
    }

    @Test public void missingDiagnosisStepsOrJsonFail() {
        expect(DoctorAiException.Kind.FAILED, "{\"isPlant\":true,\"careSteps\":[\"a\"]}");
        expect(DoctorAiException.Kind.FAILED, "{\"isPlant\":true,\"diagnosis\":\"x\",\"careSteps\":[]}");
        expect(DoctorAiException.Kind.FAILED, "not json");
    }

    @Test public void longDiagnosisIsCutAndStepsCapped() throws Exception {
        StringBuilder steps = new StringBuilder("[");
        for (int i = 0; i < 10; i++) steps.append(i == 0 ? "" : ",").append("\"step ").append(i).append('"');
        steps.append(']');
        PlantDiagnosisResult r = DoctorAiParser.parse("{\"diagnosis\":\"" + repeat('d', DoctorAiParser.MAX_DIAGNOSIS + 200)
                + "\",\"careSteps\":" + steps + "}");
        assertEquals(DoctorAiParser.MAX_DIAGNOSIS, r.getDiagnosis().length());
        assertEquals(DoctorAiParser.MAX_STEPS, r.getGuidance().split("\n").length);
    }

    @Test public void promptFollowsTheAppLanguage() {
        assertTrue(GeminiDoctorAnalyzer.prompt("vi").endsWith("in Vietnamese."));
        assertTrue(GeminiDoctorAnalyzer.prompt("en").endsWith("in English."));
    }

    private static void expect(DoctorAiException.Kind kind, String json) {
        try {
            DoctorAiParser.parse(json);
            fail("Expected " + kind);
        } catch (DoctorAiException error) {
            assertEquals(kind, error.getKind());
        }
    }

    private static String repeat(char c, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(c);
        return b.toString();
    }
}
