package com.example.ungdungchiasecaycanh.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;

/** Dark and blurry photos are flagged before Plant Doctor spends an AI request on them. */
public class PhotoQualityTest {
    private static final int SIDE = 32;

    @Test public void darkPhotoIsTooDark() {
        int[] luma = new int[SIDE * SIDE];
        Arrays.fill(luma, 15);
        assertEquals(PhotoQuality.Result.TOO_DARK, PhotoQuality.assess(luma, SIDE, SIDE));
    }

    @Test public void evenGreyWithNoEdgesIsBlurry() {
        int[] luma = new int[SIDE * SIDE];
        Arrays.fill(luma, 128);
        assertEquals(PhotoQuality.Result.BLURRY, PhotoQuality.assess(luma, SIDE, SIDE));
    }

    @Test public void sharpDetailIsOk() {
        int[] luma = new int[SIDE * SIDE];
        for (int y = 0; y < SIDE; y++) for (int x = 0; x < SIDE; x++) luma[y * SIDE + x] = (x + y) % 2 == 0 ? 40 : 220;
        assertEquals(PhotoQuality.Result.OK, PhotoQuality.assess(luma, SIDE, SIDE));
    }
}
