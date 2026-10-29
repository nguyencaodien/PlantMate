package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Text checks for the admin contract in firestore.rules (see docs/ADMIN_FLOW_DESIGN.md). */
public class FirestoreAdminRulesTest {
    private static String rules() throws Exception {
        Path cwd = Path.of(System.getProperty("user.dir"));
        Path file = cwd.resolve("firestore.rules");
        if (!Files.exists(file)) file = cwd.resolve("../firestore.rules");
        return Files.readString(file.normalize());
    }

    private static String section(String rules, String start, String end) {
        int from = rules.indexOf(start);
        int to = end == null ? rules.length() : rules.indexOf(end, from);
        return rules.substring(from, to);
    }

    @Test public void onlyAdminsGrantTheRoleAndNeverChangeTheirOwn() throws Exception {
        String rules = rules();
        assertTrue(rules.contains("exists(/databases/$(database)/documents/admins/$(request.auth.uid))"));
        String admins = section(rules, "match /admins/{uid}", "match /reportReviews/{reportId}");
        assertTrue(admins.contains("allow read: if request.auth != null && (request.auth.uid == uid || isAdmin())"));
        assertTrue(admins.contains("allow create: if isAdmin()"));
        assertTrue(admins.contains("&& uid != request.auth.uid"));
        assertTrue(admins.contains("allow update: if false"));
        assertTrue(admins.contains("allow delete: if isAdmin() && uid != request.auth.uid"));
    }

    @Test public void adminListingEditsKeepOwnerAndCreationTime() throws Exception {
        String listings = section(rules(), "match /listings/{listingId}", "match /conversations/{conversationId}");
        assertTrue(listings.contains("allow update: if isAdmin()"));
        assertTrue(listings.contains("request.resource.data.ownerId == resource.data.ownerId"));
        assertTrue(listings.contains("allow delete: if isAdmin()"));
        assertFalse(listings.contains("allow delete: if request.auth != null"));
        // Member updates stay status-only.
        assertTrue(listings.contains(".hasOnly(['status', 'updatedAt'])"));
    }

    @Test public void adminPostEditsAreTextCategoryAndImageOnly() throws Exception {
        String posts = section(rules(), "match /posts/{postId}", "match /likes/{uid}");
        assertTrue(posts.contains("affectedKeys().hasOnly(['text', 'category', 'imageUrl'])"));
        // A replacement image must still be a remote http(s) URL (no local file URIs).
        assertTrue(posts.contains("validRemoteImage(request.resource.data.imageUrl)"));
        assertTrue(posts.contains("allow delete: if isAdmin()"));
    }

    @Test public void suspendedMembersCannotCreateContent() throws Exception {
        String rules = rules();
        for (String start : new String[]{"match /listings/{listingId}", "match /conversations/{conversationId}",
                "match /messages/{messageId}", "match /posts/{postId}", "match /comments/{commentId}"}) {
            String part = rules.substring(rules.indexOf(start), Math.min(rules.length(), rules.indexOf(start) + 900));
            assertTrue(start, part.contains("!isSuspended(request.auth.uid)"));
        }
        String suspended = section(rules, "match /suspendedUsers/{uid}", "match /adminActions/{actionId}");
        assertTrue(suspended.contains("uid != request.auth.uid"));
        assertTrue(suspended.contains("allow update: if false"));
    }

    @Test public void auditLogIsAppendOnly() throws Exception {
        String log = section(rules(), "match /adminActions/{actionId}", null);
        assertTrue(log.contains("allow create: if isAdmin()"));
        assertTrue(log.contains("request.resource.data.adminId == request.auth.uid"));
        assertTrue(log.contains("allow update, delete: if false"));
    }
}
