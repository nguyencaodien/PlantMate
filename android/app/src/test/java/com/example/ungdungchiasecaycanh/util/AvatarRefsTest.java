package com.example.ungdungchiasecaycanh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class AvatarRefsTest {
    @Test public void presetsResolveByRefAndKeysAreUniqueAndRuleSafe() {
        Set<String> keys = new HashSet<>();
        for (AvatarBinder.Preset p : AvatarBinder.PRESETS) {
            assertTrue(keys.add(p.key));
            assertTrue(p.key.matches("[a-z0-9_]{1,24}"));   // same pattern as validAvatar() in the rules
            assertEquals(p, AvatarBinder.preset(p.ref()));
        }
        assertNull(AvatarBinder.preset("preset:unknown"));
        assertNull(AvatarBinder.preset(""));
        assertNull(AvatarBinder.preset(null));
    }

    @Test public void photosAreUploadedRefsOrHttpsNotPresets() {
        assertTrue(AvatarBinder.isPhoto("pmimg://ghHwxUQTcRiEOu44tLMh"));
        assertTrue(AvatarBinder.isPhoto("https://example.com/a.jpg"));
        assertFalse(AvatarBinder.isPhoto("preset:mam"));
        assertFalse(AvatarBinder.isPhoto(""));
        assertNotNull(ImageRefs.idOf("pmimg://ghHwxUQTcRiEOu44tLMh"));
        assertNull(ImageRefs.idOf("pmimg://bad id"));
    }
}
