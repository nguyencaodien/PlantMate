package com.example.ungdungchiasecaycanh.data.local.db;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantPhotoEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** Deleting a plant: only the owner's, with its care logs and photos, keeping Doctor history unlinked. */
@RunWith(AndroidJUnit4.class)
public class PlantDeleteTest {
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

    private long plant(String owner, String image) {
        return db.plantDao().insert(new PlantEntity(0, owner, "Mochi", "Monstera", image, false, 7, null, 100, 100));
    }

    @Test
    public void ownerDeleteRemovesCareLogsAndPhotosButKeepsDoctorHistory() {
        long id = plant("uid-a", "content://photo/a.jpg");
        db.careLogDao().insert(new CareLogEntity(0, id, CareType.WATER, "", 200));
        db.plantPhotoDao().insert(new PlantPhotoEntity(0, id, "content://photo/b.jpg", 300));
        long history = db.doctorHistoryDao().insertHistory(
                new DoctorHistoryEntity(0, "uid-a", id, "content://doctor.jpg", "Healthy", 0.9, "Keep going", 400));

        assertEquals(1, db.plantDao().deleteForOwner(id, "uid-a"));

        assertNull(db.plantDao().getByIdForOwner(id, "uid-a"));
        assertEquals(0, db.careLogDao().getRecent(id, 10).size());
        assertEquals(0, db.plantPhotoDao().getImageUrisForPlant(id, "uid-a").size());
        DoctorHistoryEntity kept = db.doctorHistoryDao().getHistoryByIdForOwner(history, "uid-a");
        assertNotNull(kept);
        assertNull(kept.getPlantId());
    }

    @Test
    public void anotherAccountCannotDeleteThePlant() {
        long id = plant("uid-a", "");
        assertEquals(0, db.plantDao().deleteForOwner(id, "uid-b"));
        assertNotNull(db.plantDao().getByIdForOwner(id, "uid-a"));
    }

    @Test
    public void sharedPhotosAreCountedSoTheyAreNotDeleted() {
        long first = plant("uid-a", "content://photo/shared.jpg");
        long second = plant("uid-a", "content://photo/shared.jpg");
        db.plantPhotoDao().insert(new PlantPhotoEntity(0, second, "content://photo/timeline.jpg", 300));

        assertEquals(1, db.plantDao().countOtherPlantsUsingImage("content://photo/shared.jpg", first));
        assertEquals(0, db.plantDao().countOtherPlantsUsingImage("content://photo/only.jpg", first));
        assertEquals(1, db.plantPhotoDao().countOtherPlantsUsingImage("content://photo/timeline.jpg", first));
        assertEquals(0, db.plantPhotoDao().countOtherPlantsUsingImage("content://photo/timeline.jpg", second));
    }
}
