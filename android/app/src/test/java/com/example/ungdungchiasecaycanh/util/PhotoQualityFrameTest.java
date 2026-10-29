package com.example.ungdungchiasecaycanh.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.ByteBuffer;

public class PhotoQualityFrameTest {
    /** A 640x480 luma plane with padded rows, like a camera frame. */
    private static ByteBuffer plane(int rowStride, java.util.function.IntBinaryOperator luma) {
        ByteBuffer b = ByteBuffer.allocate(rowStride * 480);
        for (int y = 0; y < 480; y++) for (int x = 0; x < 640; x++) b.put(y * rowStride + x, (byte) luma.applyAsInt(x, y));
        return b;
    }

    @Test public void darkFrameIsTooDark() {
        assertEquals(PhotoQuality.Result.TOO_DARK,
                PhotoQuality.checkLumaPlane(plane(704, (x, y) -> 10), 704, 1, 640, 480));
    }

    @Test public void flatBrightFrameIsBlurry() {
        assertEquals(PhotoQuality.Result.BLURRY,
                PhotoQuality.checkLumaPlane(plane(704, (x, y) -> 160), 704, 1, 640, 480));
    }

    @Test public void detailedBrightFrameIsOk() {
        assertEquals(PhotoQuality.Result.OK, PhotoQuality.checkLumaPlane(
                plane(704, (x, y) -> ((x / 6 + y / 6) % 2 == 0) ? 230 : 60), 704, 1, 640, 480));
    }
}
