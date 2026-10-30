package com.example.ungdungchiasecaycanh.data.local.db;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ungdungchiasecaycanh.data.local.entity.ListingDraftEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** One unpublished listing per account: saving replaces it, and accounts never see each other's. */
@RunWith(AndroidJUnit4.class)
public class ListingDraftDaoTest {
    private PlantMateDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, PlantMateDatabase.class).allowMainThreadQueries().build();
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static ListingDraftEntity draft(String owner, String nickname) {
        return new ListingDraftEntity(owner, "GIVE_AWAY", nickname, "Monstera", "", 0, "", "Quận 7",
                "", "", 0, "", "", 100, "", 0, "", false);
    }

    @Test
    public void aSecondSaveReplacesTheAccountsDraft() {
        db.listingDraftDao().save(draft("uid-a", "First"));
        db.listingDraftDao().save(draft("uid-a", "Second"));
        assertEquals("Second", db.listingDraftDao().getForOwner("uid-a").getNickname());
    }

    @Test
    public void anotherAccountDoesNotSeeTheDraft() {
        db.listingDraftDao().save(draft("uid-a", "Mine"));
        assertNull(db.listingDraftDao().getForOwner("uid-b"));
    }

    @Test
    public void deletingADraftKeepsOtherAccountsDrafts() {
        db.listingDraftDao().save(draft("uid-a", "Mine"));
        db.listingDraftDao().save(draft("uid-b", "Theirs"));

        db.listingDraftDao().deleteForOwner("uid-a");

        assertNull(db.listingDraftDao().getForOwner("uid-a"));
        assertNotNull(db.listingDraftDao().getForOwner("uid-b"));
    }
}
