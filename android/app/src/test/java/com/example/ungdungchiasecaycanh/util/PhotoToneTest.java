package com.example.ungdungchiasecaycanh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class PhotoToneTest {
    private static final int RED = 0xFFD0202A;      // poinsettia red
    private static final int MAGENTA_RED = 0xFFC8104A;
    private static final int LEAF = 0xFF2E7D32;
    private static final int TERRACOTTA = 0xFFA0603C; // pot: orange-brown, not red enough
    private static final int DARK_RED = 0xFF3A0A0A;   // shadow: too dark
    private static final int WHITE = 0xFFFFFFFF;

    private static int[] photo(int redPixels, int otherColor, int size) {
        int[] pixels = new int[size];
        Arrays.fill(pixels, otherColor);
        Arrays.fill(pixels, 0, redPixels, RED);
        return pixels;
    }

    @Test public void onlyStrongRedCounts() {
        assertTrue(PhotoTone.isStrongRed(RED));
        assertTrue(PhotoTone.isStrongRed(MAGENTA_RED));
        assertFalse(PhotoTone.isStrongRed(LEAF));
        assertFalse(PhotoTone.isStrongRed(TERRACOTTA));
        assertFalse(PhotoTone.isStrongRed(DARK_RED));
        assertFalse(PhotoTone.isStrongRed(WHITE));
    }

    @Test public void aRedPlantIsLeftOutAGreenOneIsKept() {
        assertTrue(PhotoTone.mostlyRed(photo(40, LEAF, 100)));
        assertFalse(PhotoTone.mostlyRed(photo(5, LEAF, 100)));
        assertEquals(0.4, PhotoTone.redShare(photo(40, WHITE, 100)), 1e-9);
        assertEquals(0, PhotoTone.redShare(new int[0]), 0);
    }
}
