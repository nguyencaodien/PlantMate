package com.example.ungdungchiasecaycanh.ui.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.example.ungdungchiasecaycanh.model.User;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/** Friend suggestions: people you chatted with, neighbours in your area first. */
public class FriendSuggestionTest {
    private static FriendRepository.Person person(String uid, String area) {
        return new FriendRepository.Person(uid, new User(uid, area, "", ""));
    }

    @Test public void sameAreaComesFirstIgnoringAccentsAndCase() {
        List<FriendsViewModel.Suggestion> out = FriendsViewModel.suggest(Arrays.asList(
                person("far", "Quận 1, TP.HCM"), person("near", "quan 7, tp.hcm")), "Quận 7, TP.HCM", 5);
        assertEquals("near", out.get(0).person.uid);
        assertTrue(out.get(0).sameArea);
        assertFalse(out.get(1).sameArea);
    }

    @Test public void listIsCapped() {
        assertEquals(2, FriendsViewModel.suggest(Arrays.asList(person("a", ""), person("b", ""), person("c", "")),
                "", 2).size());
    }
}
