package com.example.ungdungchiasecaycanh.data.local.db;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Every plant query stays inside the signed-in account; another account sees and changes nothing. */
@RunWith(AndroidJUnit4.class)
public class PlantDaoOwnerTest {
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

    private long plant(String owner, String image, boolean reminder, Long nextWateringAt) {
        return db.plantDao().insert(new PlantEntity(0, owner, "Mochi", "Monstera", image,
                reminder, 7, nextWateringAt, 100, 100));
    }

    @Test
    public void anotherAccountCannotReadThePlant() {
        long id = plant("uid-a", "", false, null);
        assertNotNull(db.plantDao().getByIdForOwner(id, "uid-a"));
        assertNull(db.plantDao().getByIdForOwner(id, "uid-b"));
    }

    @Test
    public void anotherAccountCannotChangeTheWateringSchedule() {
        long id = plant("uid-a", "", true, 500L);
        assertEquals(0, db.plantDao().updateWateringScheduleForOwner(id, "uid-b", 900L, 200));
        assertEquals(Long.valueOf(500), db.plantDao().getByIdForOwner(id, "uid-a").getNextWateringAt());

        assertEquals(1, db.plantDao().updateWateringScheduleForOwner(id, "uid-a", 900L, 200));
        assertEquals(Long.valueOf(900), db.plantDao().getByIdForOwner(id, "uid-a").getNextWateringAt());
    }

    @Test
    public void idsAndImagesListOnlyTheOwnersPlants() {
        long mine = plant("uid-a", "content://photo/a.jpg", false, null);
        long noImage = plant("uid-a", "", false, null);
        plant("uid-b", "content://photo/b.jpg", false, null);

        List<Long> ids = db.plantDao().getIdsForOwner("uid-a");
        assertEquals(2, ids.size());
        assertTrue(ids.containsAll(Arrays.asList(mine, noImage)));
        assertEquals(Collections.singletonList("content://photo/a.jpg"),
                db.plantDao().getImageUrisForOwner("uid-a"));
    }

    @Test
    public void remindersAreTheOwnersEnabledPlantsSoonestFirst() {
        long later = plant("uid-a", "", true, 900L);
        long sooner = plant("uid-a", "", true, 300L);
        plant("uid-a", "", false, 100L);
        plant("uid-b", "", true, 200L);

        List<PlantEntity> reminders = db.plantDao().getReminderPlantsForOwner("uid-a");
        assertEquals(2, reminders.size());
        assertEquals(sooner, reminders.get(0).getId());
        assertEquals(later, reminders.get(1).getId());
    }

    @Test
    public void accountDeletionLeavesOtherAccountsAlone() {
        plant("uid-a", "", false, null);
        plant("uid-a", "", false, null);
        long other = plant("uid-b", "", false, null);

        assertEquals(2, db.plantDao().deleteAllForOwner("uid-a"));

        assertEquals(0, db.plantDao().getIdsForOwner("uid-a").size());
        assertNotNull(db.plantDao().getByIdForOwner(other, "uid-b"));
    }
}
