package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseReviewDataSource;
import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.Review;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ReviewRepository {
    public enum Eligibility {
        ELIGIBLE, NOT_REHOMED, SELF_REVIEW, NOT_PARTICIPANT, ALREADY_REVIEWED,
        MISSING_LISTING, MISSING_CONVERSATION, NOT_AUTHENTICATED
    }

    private final FirebaseAuthDataSource authDataSource = new FirebaseAuthDataSource();
    private final FirebaseReviewDataSource reviewDataSource = new FirebaseReviewDataSource();
    private final ListingRepository listingRepository = new ListingRepository();
    private final ChatRepository chatRepository = new ChatRepository();

    public String getCurrentUserId() { return authDataSource.getCurrentUserId(); }

    public Task<Eligibility> checkEligibility(String listingId, String revieweeId) {
        String reviewerId = authDataSource.getCurrentUserId();
        if (reviewerId == null) return Tasks.forResult(Eligibility.NOT_AUTHENTICATED);
        try {
            buildReviewId(listingId, reviewerId, revieweeId);
        } catch (IllegalArgumentException error) {
            return Tasks.forResult(reviewerId.equals(trim(revieweeId))
                    ? Eligibility.SELF_REVIEW : Eligibility.NOT_PARTICIPANT);
        }
        String cleanListingId = listingId.trim();
        String cleanRevieweeId = revieweeId.trim();
        return listingRepository.getListing(cleanListingId).continueWithTask(listingTask -> {
            if (!listingTask.isSuccessful() || listingTask.getResult() == null) {
                return Tasks.forResult(Eligibility.MISSING_LISTING);
            }
            Listing listing = listingTask.getResult();
            if (listing.getStatus() != ListingStatus.REHOMED) {
                return Tasks.forResult(Eligibility.NOT_REHOMED);
            }
            String requesterId = deriveRequesterId(
                    listing.getOwnerId(), reviewerId, cleanRevieweeId);
            if (requesterId == null) return Tasks.forResult(Eligibility.NOT_PARTICIPANT);
            // The pair's one conversation; listings asked about before 2026-10-14 have their own.
            String pairId = ChatRepository.pairConversationId(listing.getOwnerId(), requesterId);
            String legacyId = ChatRepository.buildConversationId(cleanListingId, requesterId);
            return chatRepository.getConversation(pairId).continueWithTask(pairTask ->
                    pairTask.isSuccessful() && pairTask.getResult() != null ? pairTask
                            : chatRepository.getConversation(legacyId)).continueWithTask(chatTask -> {
                if (!chatTask.isSuccessful() || chatTask.getResult() == null) {
                    return Tasks.forResult(Eligibility.MISSING_CONVERSATION);
                }
                Eligibility relation = evaluateRelation(listing, chatTask.getResult(),
                        reviewerId, cleanRevieweeId, false);
                if (relation != Eligibility.ELIGIBLE) return Tasks.forResult(relation);
                String reviewId = buildReviewId(cleanListingId, reviewerId, cleanRevieweeId);
                return reviewDataSource.reviewExists(reviewId).continueWith(existsTask -> {
                    if (!existsTask.isSuccessful()) throw failure(existsTask.getException());
                    return existsTask.getResult()
                            ? Eligibility.ALREADY_REVIEWED : Eligibility.ELIGIBLE;
                });
            });
        });
    }

    public Task<Void> submitReview(String listingId, String revieweeId,
                                   int rating, String comment) {
        String reviewerId = authDataSource.getCurrentUserId();
        if (reviewerId == null) return Tasks.forException(
                new IllegalStateException("Authentication required"));
        final String normalized;
        try {
            normalized = normalizeComment(rating, comment);
            buildReviewId(listingId, reviewerId, revieweeId);
        } catch (IllegalArgumentException error) {
            return Tasks.forException(error);
        }
        String cleanListing = listingId.trim();
        String cleanReviewee = revieweeId.trim();
        return checkEligibility(cleanListing, cleanReviewee).continueWithTask(task -> {
            if (!task.isSuccessful()) throw failure(task.getException());
            if (task.getResult() != Eligibility.ELIGIBLE) {
                throw new IllegalStateException("Review is not eligible");
            }
            return reviewDataSource.createReview(
                    buildReviewId(cleanListing, reviewerId, cleanReviewee),
                    cleanListing, reviewerId, cleanReviewee, rating, normalized);
        });
    }

    public Task<List<Review>> getReviewsForUser(String revieweeId) {
        if (authDataSource.getCurrentUserId() == null) return Tasks.forException(
                new IllegalStateException("Authentication required"));
        if (revieweeId == null || revieweeId.trim().isEmpty()) return Tasks.forException(
                new IllegalArgumentException("Reviewee ID is required"));
        return reviewDataSource.getReviewsForUser(revieweeId.trim());
    }

    public static String buildReviewId(String listingId, String reviewerId, String revieweeId) {
        String listing = requireId(listingId);
        String reviewer = requireId(reviewerId);
        String reviewee = requireId(revieweeId);
        if (reviewer.equals(reviewee)) throw new IllegalArgumentException("Self review is not allowed");
        return listing + "__" + reviewer + "__" + reviewee;
    }

    public static String normalizeComment(int rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be 1 to 5");
        String value = comment == null ? "" : comment.trim();
        if (value.length() > 500) throw new IllegalArgumentException("Comment is too long");
        return value;
    }

    static Eligibility evaluateRelation(Listing listing, Conversation conversation,
                                        String reviewerId, String revieweeId,
                                        boolean alreadyReviewed) {
        if (reviewerId == null || reviewerId.equals(revieweeId)) return Eligibility.SELF_REVIEW;
        if (listing == null) return Eligibility.MISSING_LISTING;
        if (listing.getStatus() != ListingStatus.REHOMED) return Eligibility.NOT_REHOMED;
        String requesterId = deriveRequesterId(listing.getOwnerId(), reviewerId, revieweeId);
        if (requesterId == null) return Eligibility.NOT_PARTICIPANT;
        if (conversation == null) return Eligibility.MISSING_CONVERSATION;
        Set<String> participants = new HashSet<>(conversation.getParticipantIds());
        // A pair conversation proves the two talked, whichever plant it discusses now.
        boolean pair = ChatRepository.pairConversationId(listing.getOwnerId(), requesterId)
                .equals(conversation.getConversationId());
        if (pair) {
            return participants.size() == 2 && participants.contains(listing.getOwnerId())
                    && participants.contains(requesterId)
                    ? (alreadyReviewed ? Eligibility.ALREADY_REVIEWED : Eligibility.ELIGIBLE)
                    : Eligibility.NOT_PARTICIPANT;
        }
        if (!listing.getListingId().equals(conversation.getListingId())
                || !listing.getOwnerId().equals(conversation.getOwnerId())
                || !requesterId.equals(conversation.getRequesterId())
                || conversation.getParticipantIds().size() != 2
                || participants.size() != 2
                || !participants.contains(listing.getOwnerId())
                || !participants.contains(requesterId)) {
            return Eligibility.NOT_PARTICIPANT;
        }
        return alreadyReviewed ? Eligibility.ALREADY_REVIEWED : Eligibility.ELIGIBLE;
    }

    private static String deriveRequesterId(String ownerId, String reviewerId, String revieweeId) {
        if (ownerId == null) return null;
        if (ownerId.equals(revieweeId)) return reviewerId;
        if (ownerId.equals(reviewerId)) return revieweeId;
        return null;
    }

    private static String requireId(String value) {
        String result = trim(value);
        if (result.isEmpty()) throw new IllegalArgumentException("ID is required");
        return result;
    }

    private static String trim(String value) { return value == null ? "" : value.trim(); }
    private static Exception failure(Exception error) {
        return error == null ? new IllegalStateException("Review request failed") : error;
    }
}
