package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ReviewRepositoryTest {
    @Test public void reviewIdIsDeterministicAndDirected() {
        String requesterToOwner = ReviewRepository.buildReviewId("L", "B", "A");
        assertEquals("L__B__A", requesterToOwner);
        assertEquals(requesterToOwner, ReviewRepository.buildReviewId("L", "B", "A"));
        assertNotEquals(requesterToOwner, ReviewRepository.buildReviewId("L", "A", "B"));
        assertThrows(IllegalArgumentException.class, () -> ReviewRepository.buildReviewId("", "B", "A"));
        assertThrows(IllegalArgumentException.class, () -> ReviewRepository.buildReviewId("L", "", "A"));
        assertThrows(IllegalArgumentException.class, () -> ReviewRepository.buildReviewId("L", "B", ""));
        assertThrows(IllegalArgumentException.class, () -> ReviewRepository.buildReviewId("L", "A", "A"));
    }

    @Test public void reviewInputIsNormalizedAndBounded() {
        assertThrows(IllegalArgumentException.class, () -> ReviewRepository.normalizeComment(0, ""));
        assertEquals("", ReviewRepository.normalizeComment(1, "   "));
        assertEquals("Good exchange", ReviewRepository.normalizeComment(5, " Good exchange "));
        assertEquals(500, ReviewRepository.normalizeComment(5, "a".repeat(500)).length());
        assertThrows(IllegalArgumentException.class,
                () -> ReviewRepository.normalizeComment(6, ""));
        assertThrows(IllegalArgumentException.class,
                () -> ReviewRepository.normalizeComment(5, "a".repeat(501)));
    }

    @Test public void requesterAndOwnerDirectionsRequireRehomedMatchingConversation() {
        Listing rehomed = listing(ListingStatus.REHOMED, "owner");
        Conversation conversation = conversation("owner", "requester");
        assertEquals(ReviewRepository.Eligibility.ELIGIBLE,
                ReviewRepository.evaluateRelation(rehomed, conversation,
                        "requester", "owner", false));
        assertEquals(ReviewRepository.Eligibility.ELIGIBLE,
                ReviewRepository.evaluateRelation(rehomed, conversation,
                        "owner", "requester", false));
        assertEquals(ReviewRepository.Eligibility.ALREADY_REVIEWED,
                ReviewRepository.evaluateRelation(rehomed, conversation,
                        "owner", "requester", true));
        assertEquals(ReviewRepository.Eligibility.NOT_REHOMED,
                ReviewRepository.evaluateRelation(listing(ListingStatus.ACTIVE, "owner"),
                        conversation, "requester", "owner", false));
        assertEquals(ReviewRepository.Eligibility.MISSING_CONVERSATION,
                ReviewRepository.evaluateRelation(rehomed, null,
                        "requester", "owner", false));
        assertEquals(ReviewRepository.Eligibility.NOT_PARTICIPANT,
                ReviewRepository.evaluateRelation(rehomed, conversation,
                        "other", "owner", false));
        assertEquals(ReviewRepository.Eligibility.NOT_PARTICIPANT,
                ReviewRepository.evaluateRelation(rehomed, conversation,
                        "requester", "other", false));
        assertEquals(ReviewRepository.Eligibility.SELF_REVIEW,
                ReviewRepository.evaluateRelation(rehomed, conversation,
                        "owner", "owner", false));
    }

    /** The pair's one conversation counts even after it moved on to another plant. */
    @Test public void pairConversationAboutAnotherPlantStillCounts() {
        Listing rehomed = listing(ListingStatus.REHOMED, "owner");
        Conversation pair = new Conversation("owner__requester", "another-listing", "owner", "requester",
                Arrays.asList("owner", "requester"), "Thanks", null, null);
        assertEquals(ReviewRepository.Eligibility.ELIGIBLE,
                ReviewRepository.evaluateRelation(rehomed, pair, "requester", "owner", false));
        assertEquals(ReviewRepository.Eligibility.NOT_PARTICIPANT,
                ReviewRepository.evaluateRelation(rehomed, pair, "other", "owner", false));
    }

    private static Listing listing(ListingStatus status, String owner) {
        return new Listing("listing", owner, "Plant", "", "Description",
                ListingType.GIVE_AWAY, 0, "", Collections.singletonList("url"),
                "Area", status, null, null);
    }

    private static Conversation conversation(String owner, String requester) {
        return new Conversation("listing__" + requester, "listing", owner, requester,
                Arrays.asList(owner, requester), "", null, null);
    }
}
