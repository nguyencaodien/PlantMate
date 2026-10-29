package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.repository.FriendRepository.Relation;
import com.example.ungdungchiasecaycanh.model.Story;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.firebase.Timestamp;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/** Friendship resolution, story expiry, story input rules and the strip order. */
public class FriendStoryRulesTest {
    private static final long NOW = 1_800_000_000_000L;

    @Test public void relation_selfFriendsIncomingOutgoingNone() {
        assertEquals(Relation.SELF, FriendRepository.resolve("a", "a", true, true, true));
        assertEquals(Relation.FRIENDS, FriendRepository.resolve("a", "b", true, true, true));
        assertEquals(Relation.INCOMING, FriendRepository.resolve("a", "b", false, true, true));
        assertEquals(Relation.OUTGOING, FriendRepository.resolve("a", "b", false, true, false));
        assertEquals(Relation.NONE, FriendRepository.resolve("a", "b", false, false, false));
    }

    @Test public void friends_sortedByVietnameseName() {
        List<FriendRepository.Person> people = new ArrayList<>(Arrays.asList(person("Ý"), person("An"), person("Đức"), person("Bảo")));
        FriendRepository.sortByName(people);
        assertEquals(Arrays.asList("An", "Bảo", "Đức", "Ý"), names(people));
    }

    @Test public void story_activeFor24Hours() {
        assertTrue(story("s", NOW - 60_000).isActive(NOW));
        assertTrue(story("s", NOW - Story.LIFETIME_MS + 1).isActive(NOW));
        assertFalse(story("s", NOW - Story.LIFETIME_MS).isActive(NOW));
        assertTrue("a pending server time counts as now", new Story("s", "a", "t", "", "pmimg://x", null).isActive(NOW));
    }

    @Test public void storyInput_trimmedTitleAndPhotoRequired() {
        String[] ok = StoryRepository.normalize("  Khoe lộc non ", " lá mới ", "content://photo");
        assertEquals("Khoe lộc non", ok[0]);
        assertEquals("lá mới", ok[1]);
        assertRejected("", "", "content://photo");
        assertRejected("Lá", "", null);
        assertRejected("1234567890123456789012345", "", "content://photo");
        assertRejected("Lá", new String(new char[201]).replace('\0', 'x'), "content://photo");
    }

    @Test public void strip_ownFirstThenUnseenThenNewest() {
        StoryRepository.Item seenNew = item("seenNew", NOW - 1_000, false, true);
        StoryRepository.Item unseenOld = item("unseenOld", NOW - 9_000, false, false);
        StoryRepository.Item unseenNew = item("unseenNew", NOW - 2_000, false, false);
        StoryRepository.Item mineOld = item("mineOld", NOW - 8_000, true, true);
        StoryRepository.Item mineNew = item("mineNew", NOW - 3_000, true, true);
        List<StoryRepository.Item> ordered = StoryRepository.order(
                Arrays.asList(seenNew, unseenOld, mineOld, unseenNew, mineNew));
        List<String> ids = new ArrayList<>();
        for (StoryRepository.Item i : ordered) ids.add(i.story.getStoryId());
        assertEquals(Arrays.asList("mineNew", "mineOld", "unseenNew", "unseenOld", "seenNew"), ids);
    }

    private static void assertRejected(String title, String caption, String photo) {
        try {
            StoryRepository.normalize(title, caption, photo);
            throw new AssertionError("accepted " + title);
        } catch (IllegalArgumentException expected) { /* rejected */ }
    }

    private static Story story(String id, long createdMs) {
        return new Story(id, "a", "t", "", "pmimg://x", new Timestamp(new Date(createdMs)));
    }

    private static StoryRepository.Item item(String id, long createdMs, boolean mine, boolean seen) {
        return new StoryRepository.Item(story(id, createdMs), null, mine, seen);
    }

    private static FriendRepository.Person person(String name) {
        return new FriendRepository.Person(name, new User(name, "Q7", "", ""));
    }

    private static List<String> names(List<FriendRepository.Person> people) {
        List<String> out = new ArrayList<>();
        for (FriendRepository.Person p : people) out.add(p.name());
        return out;
    }
}
