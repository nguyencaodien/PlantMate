package com.example.ungdungchiasecaycanh.ui.community;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.repository.StoryRepository;
import com.example.ungdungchiasecaycanh.model.Story;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** One story card per author; the viewer plays that author's stories in a row. */
public class StoryGroupingTest {
    private static StoryRepository.Item item(String id, String author, boolean seen) {
        return new StoryRepository.Item(new Story(id, author, "t", "", "", null), null, false, seen);
    }

    private static List<String> ids(List<StoryRepository.Item> items) {
        List<String> out = new ArrayList<>();
        for (StoryRepository.Item i : items) out.add(i.story.getStoryId());
        return out;
    }

    @Test public void storiesOfOneAuthorSitTogetherInFirstSeenOrder() {
        List<StoryRepository.Item> grouped = StoryStripViewModel.groupedByAuthor(Arrays.asList(
                item("p1", "phuc", false), item("v1", "vy", false), item("p2", "phuc", true), item("p3", "phuc", false)));
        assertEquals(Arrays.asList("p1", "p2", "p3", "v1"), ids(grouped));
    }

    @Test public void eachAuthorGetsOneCardStartingAtTheirFirstStory() {
        List<StoryStripAdapter.Group> groups = StoryStripAdapter.groups(StoryStripViewModel.groupedByAuthor(Arrays.asList(
                item("p1", "phuc", true), item("v1", "vy", true), item("p2", "phuc", false))));
        assertEquals(2, groups.size());
        assertEquals("p1", groups.get(0).lead.story.getStoryId());
        assertEquals(0, groups.get(0).start);
        assertFalse(groups.get(0).allSeen); // p2 is unseen: the card keeps its green frame
        assertEquals(2, groups.get(1).start);
        assertTrue(groups.get(1).allSeen);
    }
}
