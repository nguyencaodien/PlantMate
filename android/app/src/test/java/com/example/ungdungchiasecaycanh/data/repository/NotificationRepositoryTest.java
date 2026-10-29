package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.ungdungchiasecaycanh.model.AppNotification;

import org.junit.Test;

import java.util.List;

public class NotificationRepositoryTest {

    @Test public void eachMemberIsNotifiedOnceAndNeverMyself() {
        List<NotificationRepository.Target> t = NotificationRepository.commentTargets("me", "author", "author", "vy");
        assertEquals(2, t.size());
        assertEquals("vy", t.get(0).uid);
        assertEquals(AppNotification.Type.MENTION, t.get(0).type);
        assertEquals("author", t.get(1).uid);
        assertEquals(AppNotification.Type.REPLY, t.get(1).type);

        assertTrue(NotificationRepository.commentTargets("me", "me", "", "").isEmpty());
        List<NotificationRepository.Target> plain = NotificationRepository.commentTargets("me", "author", null, null);
        assertEquals(AppNotification.Type.COMMENT, plain.get(0).type);
    }

    @Test public void excerptIsShortAndOnOneLine() {
        assertEquals("a b", NotificationRepository.excerpt("  a\n b "));
        String longText = new String(new char[200]).replace('\0', 'x');
        assertEquals(NotificationRepository.TEXT_MAX, NotificationRepository.excerpt(longText).length());
    }
}
