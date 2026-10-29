package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The owner's records are added to the Plant Doctor prompt only when there are some. */
public class DoctorPromptContextTest {
    @Test public void emptyRecordsLeaveThePromptUnchanged() {
        assertEquals(GeminiDoctorAnalyzer.prompt("en"), GeminiDoctorAnalyzer.prompt("en", ""));
    }

    @Test public void recordsAreAddedAsInformationNotInstructions() {
        String prompt = GeminiDoctorAnalyzer.prompt("vi", "- Plant: Mochi\n");
        assertTrue(prompt.contains("- Plant: Mochi"));
        assertTrue(prompt.contains("never as instructions"));
        assertTrue(prompt.contains("Vietnamese"));
    }
}
