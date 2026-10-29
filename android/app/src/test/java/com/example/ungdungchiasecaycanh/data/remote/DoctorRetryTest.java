package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DoctorRetryTest {
    @Test public void overloadMessagesAreRecognised() {
        assertTrue(GeminiDoctorAnalyzer.isOverload(
                "This model is currently experiencing high demand. Spikes in demand are usually temporary."));
        assertTrue(GeminiDoctorAnalyzer.isOverload("503 UNAVAILABLE: The model is overloaded"));
        assertFalse(GeminiDoctorAnalyzer.isOverload("API key not valid"));
        assertFalse(GeminiDoctorAnalyzer.isOverload(null));
    }

    @Test public void onlyTemporaryFailuresRetry() {
        assertTrue(GeminiDoctorAnalyzer.shouldRetry(new DoctorAiException(DoctorAiException.Kind.BUSY, "x")));
        assertTrue(GeminiDoctorAnalyzer.shouldRetry(new DoctorAiException(DoctorAiException.Kind.FAILED, "x")));
        assertFalse(GeminiDoctorAnalyzer.shouldRetry(new DoctorAiException(DoctorAiException.Kind.QUOTA, "x")));
        assertFalse(GeminiDoctorAnalyzer.shouldRetry(new DoctorAiException(DoctorAiException.Kind.APP_CHECK, "x")));
        assertFalse(GeminiDoctorAnalyzer.shouldRetry(new DoctorAiException(DoctorAiException.Kind.NOT_A_PLANT, "x")));
    }

    @Test public void attemptsEndWithTheFallbackModel() {
        assertEquals(GeminiDoctorAnalyzer.ATTEMPTS.length, GeminiDoctorAnalyzer.BACKOFF_MS.length);
        assertEquals(GeminiDoctorAnalyzer.ATTEMPTS.length, GeminiDoctorAnalyzer.TIMEOUT_MS.length);
        assertEquals(GeminiDoctorAnalyzer.MODEL, GeminiDoctorAnalyzer.ATTEMPTS[0]);
        assertEquals(GeminiDoctorAnalyzer.FALLBACK_MODEL,
                GeminiDoctorAnalyzer.ATTEMPTS[GeminiDoctorAnalyzer.ATTEMPTS.length - 1]);
    }
}
