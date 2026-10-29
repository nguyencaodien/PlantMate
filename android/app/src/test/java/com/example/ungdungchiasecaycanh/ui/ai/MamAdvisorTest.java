package com.example.ungdungchiasecaycanh.ui.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Locale;

public class MamAdvisorTest {
    @Test public void englishAndVietnameseKeywordsPickTheSameGuidance() {
        String yellow = MamAdvisor.forSymptom(MamAdvisor.Symptom.YELLOW_LEAVES).title;
        assertEquals(yellow, MamAdvisor.reply("Why are my leaves turning yellow?").title);
        assertEquals(yellow, MamAdvisor.reply("Lá cây bị vàng").title);
        assertEquals(MamAdvisor.forSymptom(MamAdvisor.Symptom.PESTS).title, MamAdvisor.reply("có rệp trên lá").title);
    }

    @Test public void wateringAndOrchidQuestionsOfferGarden() {
        assertEquals(MamAdvisor.Action.OPEN_GARDEN, MamAdvisor.reply("How often should I water?").action);
        assertEquals(MamAdvisor.Action.OPEN_GARDEN, MamAdvisor.reply("bao lâu thì tưới cây").action);
        assertTrue(MamAdvisor.reply("How often to water an orchid?").title.startsWith("Orchids"));
    }

    @Test public void everySuggestionHasASpecificAnswer() {
        for (String q : MamAdvisor.SUGGESTIONS) {
            MamAdvisor.Guidance g = MamAdvisor.reply(q);
            assertFalse(q, g == MamAdvisor.FALLBACK);
        }
    }

    @Test public void unknownOrEmptyQuestionsFallBackHonestly() {
        assertSame(MamAdvisor.FALLBACK, MamAdvisor.reply("what is the capital of France"));
        assertSame(MamAdvisor.FALLBACK, MamAdvisor.reply("   "));
        assertSame(MamAdvisor.FALLBACK, MamAdvisor.reply(null));
    }

    @Test public void vietnameseLocaleGetsVietnameseAnswersAndSuggestions() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("vi", "VN"));
            assertTrue(MamAdvisor.reply("Lá cây bị vàng").title.startsWith("Lá vàng"));
            assertEquals("Lá vàng", MamAdvisor.symptomLabel(MamAdvisor.Symptom.YELLOW_LEAVES));
            assertSame(MamAdvisor.FALLBACK_VI, MamAdvisor.reply("thủ đô của Pháp"));
            for (String q : MamAdvisor.suggestions()) {
                assertFalse(q, MamAdvisor.reply(q) == MamAdvisor.FALLBACK_VI);
            }
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test public void everySymptomHasStepsAndALabel() {
        for (MamAdvisor.Symptom s : MamAdvisor.Symptom.values()) {
            MamAdvisor.Guidance g = MamAdvisor.forSymptom(s);
            assertEquals(3, g.steps.size());
            assertFalse(g.title.isEmpty());
            assertFalse(g.watchFor.isEmpty());
            assertFalse(MamAdvisor.symptomLabel(s).isEmpty());
        }
    }
}
