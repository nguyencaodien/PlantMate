package com.example.ungdungchiasecaycanh.ui.garden;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;

import org.junit.Test;

import java.util.Arrays;

public class CareHeatmapTest {
    private static final long DAY = 24L * 60 * 60 * 1000;

    @Test public void waterAndFertiliseSameDayIsBoth() {
        long now = 1_760_000_000_000L;
        int[] days = CareHeatmapView.fromLogs(Arrays.asList(
                new CareLogEntity(1, 1, CareType.PRUNE, "", now),
                new CareLogEntity(2, 1, CareType.FERTILIZE, "", now),
                new CareLogEntity(3, 1, CareType.WATER, "", now),
                new CareLogEntity(4, 1, CareType.PRUNE, "", now - DAY),
                new CareLogEntity(5, 1, CareType.NOTE, "", now - 2 * DAY)), now);
        assertEquals(CareHeatmapView.BOTH, days[29]);
        assertEquals(CareHeatmapView.OTHER, days[28]);
        assertEquals(CareHeatmapView.NONE, days[27]);
    }

    @Test public void daysMapOldestFirst() {
        long now = 1_760_000_000_000L;
        int[] days = CareHeatmapView.fromLogs(Arrays.asList(
                new CareLogEntity(2, 1, CareType.FERTILIZE, "", now),
                new CareLogEntity(3, 1, CareType.WATER, "", now - 3 * DAY),
                new CareLogEntity(4, 1, CareType.PRUNE, "", now - 29 * DAY),
                new CareLogEntity(5, 1, CareType.WATER, "", now - 31 * DAY)), now);
        assertEquals(CareHeatmapView.FERTILIZE, days[29]);
        assertEquals(CareHeatmapView.WATER, days[26]);
        assertEquals(CareHeatmapView.OTHER, days[0]);
        assertEquals(CareHeatmapView.NONE, days[10]);
    }
}
