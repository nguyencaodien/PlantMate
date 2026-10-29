package com.example.ungdungchiasecaycanh.ui.community;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class PostHashtagsTest {
    @Test public void distinctTagsInOrderUpToThree() {
        assertEquals(Arrays.asList("#Monstera", "#KiểngLá", "#tip_1"),
                PostAdapter.hashtags("Lá mới! #Monstera #KiểngLá #monstera, #tip_1 #extra"));
    }

    @Test public void noTagsOrNoText() {
        assertEquals(Collections.emptyList(), PostAdapter.hashtags("Không có thẻ # nào"));
        assertEquals(Collections.emptyList(), PostAdapter.hashtags(null));
    }
}
