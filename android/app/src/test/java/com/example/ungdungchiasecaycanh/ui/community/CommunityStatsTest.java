package com.example.ungdungchiasecaycanh.ui.community;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommunityStatsTest {
    private static final long NOW = 1_800_000_000_000L;
    private static final long DAY = CommunityViewModel.DAY_MS;

    private static CommunityPostItem post(String id, long ageMs, int likes, int comments) {
        long at = NOW - ageMs;
        Post p = new Post(id, "u", id, PostCategory.GENERAL, "", "",
                new Timestamp(at / 1000, (int) (at % 1000) * 1_000_000));
        return new CommunityPostItem(p, "A", false, likes, false, "", "", comments);
    }

    @Test public void weekCountsDailyBarsAndTrending() {
        List<CommunityPostItem> items = Arrays.asList(
                post("today", DAY / 3, 2, 0),
                post("yesterday", DAY + 1000, 5, 1),
                post("six", 6 * DAY + 1000, 5, 3),
                post("lastWeek", 9 * DAY, 50, 9),
                post("old", 30 * DAY, 1, 0));
        CommunityViewModel.Stats stats = CommunityViewModel.stats(items, NOW);
        assertEquals(3, stats.thisWeek);
        assertEquals(1, stats.lastWeek);
        assertArrayEquals(new int[]{1, 0, 0, 0, 0, 1, 1}, stats.daily);
        // Ties on likes go to the post with more comments; older weeks never trend.
        assertEquals("six", stats.trending.post.getPostId());
    }

    @Test public void emptyFeedHasNoTrending() {
        CommunityViewModel.Stats stats = CommunityViewModel.stats(Collections.emptyList(), NOW);
        assertEquals(0, stats.thisWeek);
        assertNull(stats.trending);
    }

    @Test public void mostLikedSortKeepsNewestOrderOnTies() {
        List<CommunityPostItem> in = Arrays.asList(post("a", 1, 1, 0), post("b", 2, 3, 0), post("c", 3, 1, 0));
        List<CommunityPostItem> out = CommunityViewModel.sorted(in, CommunityViewModel.Sort.MOST_LIKED);
        assertEquals("b", out.get(0).post.getPostId());
        assertEquals("a", out.get(1).post.getPostId());
        assertEquals("c", out.get(2).post.getPostId());
        assertEquals("a", CommunityViewModel.sorted(in, CommunityViewModel.Sort.NEWEST).get(0).post.getPostId());
    }

    /** The next page goes after the loaded posts and never repeats one already shown. */
    @Test public void nextPageAppendsWithoutDuplicates() {
        List<CommunityPostItem> current = Arrays.asList(post("a", 1, 0, 0), post("b", 2, 0, 0));
        List<CommunityPostItem> out = CommunityViewModel.appendNew(current,
                Arrays.asList(post("b", 2, 0, 0), post("c", 3, 0, 0)));
        assertEquals(3, out.size());
        assertEquals("c", out.get(2).post.getPostId());
        assertEquals(2, current.size());
    }
}
