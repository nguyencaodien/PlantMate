package com.example.ungdungchiasecaycanh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RemoteImageLoaderTest {
    @Test public void onlyHttpUrlsAreRemote() {
        assertTrue(RemoteImageLoader.isRemote("https://upload.wikimedia.org/a.jpg"));
        assertTrue(RemoteImageLoader.isRemote(" HTTP://example.org/b.png"));
        assertFalse(RemoteImageLoader.isRemote("content://media/external/images/1"));
        assertFalse(RemoteImageLoader.isRemote("file:///sdcard/a.jpg"));
        assertFalse(RemoteImageLoader.isRemote(""));
        assertFalse(RemoteImageLoader.isRemote(null));
    }

    @Test public void sampleSizeHalvesUntilCloseToTarget() {
        assertEquals(1, RemoteImageLoader.sampleSize(960, 540));
        assertEquals(2, RemoteImageLoader.sampleSize(2000, 540));
        assertEquals(4, RemoteImageLoader.sampleSize(4000, 540));
        assertEquals(1, RemoteImageLoader.sampleSize(0, 540));
    }

    /** Cards of uploaded photos use the thumbnail; full-width views and web images do not. */
    @Test public void smallUploadedPhotosUseTheThumbnail() {
        String ref = "pmimg://abcdefghij1234";
        assertEquals(ref + "#thumb", RemoteImageLoader.cacheKey(ref, 400));
        assertEquals(ref, RemoteImageLoader.cacheKey(ref, 1080));
        assertEquals("https://a.org/x.jpg", RemoteImageLoader.cacheKey("https://a.org/x.jpg", 200));
    }
}
