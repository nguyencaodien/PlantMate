package com.example.ungdungchiasecaycanh.data.repository;

import static org.junit.Assert.*;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import org.junit.Test;

public class CommunityRepositoryTest {
    @Test public void createNormalizesAndUsesEmptyReferences(){Post p=CommunityRepository.normalizeForCreate("u","  hello  ",PostCategory.GENERAL);assertEquals("hello",p.getText());assertEquals("",p.getListingId());assertEquals("",p.getImageUrl());}
    @Test(expected=IllegalArgumentException.class) public void blankRejected(){CommunityRepository.normalizeForCreate("u","   ",PostCategory.TIP);}
    @Test(expected=IllegalArgumentException.class) public void tooLongRejected(){CommunityRepository.normalizeForCreate("u",new String(new char[2001]).replace('\0','x'),PostCategory.TIP);}
    @Test public void sharedPostForcesSharingAndPreservesId(){Post p=CommunityRepository.normalizeForCreate("u","hello",PostCategory.TIP,"listing1");assertEquals(PostCategory.SHARING,p.getCategory());assertEquals("listing1",p.getListingId());assertEquals("",p.getImageUrl());}
    @Test public void postKeepsRemoteImageUrl() {
        Post p = CommunityRepository.normalizeForCreate("u", "hello", PostCategory.PLANT_WIN, "", " https://upload.wikimedia.org/a.jpg ");
        assertEquals("https://upload.wikimedia.org/a.jpg", p.getImageUrl());
    }
    @Test public void postWithoutImageStoresEmptyString() {
        assertEquals("", CommunityRepository.normalizeForCreate("u", "hello", PostCategory.TIP, "", null).getImageUrl());
    }
    @Test(expected = IllegalArgumentException.class) public void localImageUriRejected() {
        CommunityRepository.normalizeForCreate("u", "hello", PostCategory.TIP, "", "content://media/external/images/1");
    }
    @Test(expected = IllegalArgumentException.class) public void fileImageUriRejected() {
        CommunityRepository.normalizeForCreate("u", "hello", PostCategory.TIP, "", "file:///sdcard/a.jpg");
    }
}
