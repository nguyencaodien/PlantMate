package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.*;
import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Path;

public class FirestoreSafetyRulesTest {
    private static String rules() throws Exception {
        Path cwd=Path.of(System.getProperty("user.dir"));
        Path file=cwd.resolve("firestore.rules");
        if(!Files.exists(file))file=cwd.resolve("../firestore.rules");
        if(!Files.exists(file))file=cwd.resolve("../../firestore.rules");
        return Files.readString(file.normalize()).replace("\r\n", "\n");
    }
    @Test public void reportIsCreateOnlyWithCanonicalValidation() throws Exception {
        String rules=rules();
        String section=rules.substring(rules.indexOf("match /reports/{reportId}"),rules.indexOf("match /reviews/{reviewId}"));
        // Members cannot read reports; only allowlisted admins can (docs/ADMIN_FLOW_DESIGN.md).
        assertTrue(section.contains("allow read: if isAdmin()"));
        assertFalse(section.contains("allow read: if request.auth != null"));
        assertTrue(section.contains("allow create: if request.auth != null"));
        assertTrue(section.contains("keys().hasOnly(reportFields())"));
        assertTrue(section.contains("keys().hasAll(reportFields())"));
        assertTrue(section.contains("'USER', 'LISTING', 'POST', 'COMMENT'"));
        assertTrue(section.contains("'HARASSMENT', 'SPAM', 'SCAM', 'INAPPROPRIATE', 'OTHER'"));
        assertTrue(section.contains("details.size() <= 500"));
        assertTrue(section.contains("reason != 'OTHER'"));
        assertTrue(section.contains("createdAt == request.time"));
        assertTrue(section.contains("validReportTarget(request.resource.data)"));
        assertTrue(section.contains("allow update, delete: if false"));
        assertTrue(rules.contains("d.targetType == 'USER'")&&rules.contains("d.targetType == 'LISTING'")
                &&rules.contains("d.targetType == 'POST'")&&rules.contains("d.targetType == 'COMMENT'"));
        assertTrue(rules.contains("d.targetId != request.auth.uid"));
        assertTrue(rules.contains(".data.ownerId")&&rules.contains(".data.authorId"));
    }
    @Test public void blockRelationIsPrivateAndMessagingChecksBothDirections() throws Exception {
        String rules=rules();
        String block=rules.substring(rules.indexOf("match /blockedUsers/{blockedUid}"),rules.indexOf("match /listings/{listingId}"));
        assertTrue(block.contains("allow read: if request.auth != null && request.auth.uid == uid"));
        assertTrue(block.contains("blockedUid != uid"));
        assertTrue(block.contains("documents/users/$(blockedUid)"));
        assertTrue(block.contains("keys().hasOnly(['createdAt'])"));
        assertTrue(block.contains("createdAt == request.time"));
        assertTrue(block.contains("allow update: if false"));
        assertTrue(block.contains("allow delete: if request.auth != null && request.auth.uid == uid"));
        assertTrue(rules.contains("documents/users/$(uidA)/blockedUsers/$(uidB)"));
        assertTrue(rules.contains("documents/users/$(uidB)/blockedUsers/$(uidA)"));
        String conversation=rules.substring(rules.indexOf("match /conversations/{conversationId}"),rules.indexOf("match /reports/{reportId}"));
        assertTrue(conversation.contains("!isBlockedEitherDirection(request.resource.data.ownerId"));
        assertTrue(conversation.contains("!isBlockedEitherDirection(\n            get("));
        assertTrue(conversation.contains("allow read: if request.auth != null"));
        assertTrue(conversation.contains("allow read: if isConversationParticipant(conversationId)"));
        assertTrue(conversation.contains("allow update, delete: if false"));
    }
    @Test public void blockedUsersSettingIsNavigationOnly() throws Exception {
        Path cwd=Path.of(System.getProperty("user.dir"));
        Path xml=cwd.resolve("src/main/res/xml/preferences_settings.xml");
        if(!Files.exists(xml))xml=cwd.resolve("app/src/main/res/xml/preferences_settings.xml");
        String preferences=Files.readString(xml.normalize());
        assertTrue(preferences.contains("app:key=\"blocked_users_action\""));
        assertTrue(preferences.contains("app:persistent=\"false\""));
        assertEquals(1,preferences.split("SwitchPreferenceCompat",-1).length-1);
    }
}
