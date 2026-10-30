package com.example.ungdungchiasecaycanh.data.local.db;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Care logs reach an account only through its own plants; the Garden week and repeat check read them. */
@RunWith(AndroidJUnit4.class)
public class CareLogDaoOwnerTest {
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

    private long plant(String owner) {
        return db.plantDao().insert(new PlantEntity(0, owner, "Mochi", "Monstera", "", false, 7, null, 100, 100));
    }

    private long log(long plantId, CareType type, long createdAt) {
        return db.careLogDao().insert(new CareLogEntity(0, plantId, type, "", createdAt));
    }

    private static <T> T firstValue(LiveData<T> live) throws InterruptedException {
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        Observer<T> observer = next -> {
            value.set(next);
            received.countDown();
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> live.observeForever(observer));
        assertTrue(received.await(5, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> live.removeObserver(observer));
        return value.get();
    }

    @Test
    public void ownerSeesOnlyTheirLogsSinceTheDateNewestFirst() throws InterruptedException {
        long mine = plant("uid-a");
        long theirs = plant("uid-b");
        log(mine, CareType.WATER, 100);
        long older = log(mine, CareType.WATER, 200);
        long newer = log(mine, CareType.WATER, 300);
        log(theirs, CareType.WATER, 400);

        List<CareLogEntity> logs = firstValue(db.careLogDao().observeForOwnerSince("uid-a", 200));

        assertEquals(2, logs.size());
        assertEquals(newer, logs.get(0).getId());
        assertEquals(older, logs.get(1).getId());
    }

    @Test
    public void recentLogsAreLimitedAndNewestFirst() {
        long id = plant("uid-a");
        log(id, CareType.WATER, 100);
        log(id, CareType.WATER, 200);
        long newest = log(id, CareType.WATER, 300);

        List<CareLogEntity> logs = db.careLogDao().getRecent(id, 2);

        assertEquals(2, logs.size());
        assertEquals(newest, logs.get(0).getId());
    }

    @Test
    public void repeatCheckCountsOnlyRecentWateringOfThatPlant() {
        long id = plant("uid-a");
        long other = plant("uid-a");
        log(id, CareType.WATER, 100);
        log(id, CareType.WATER, 300);
        log(other, CareType.WATER, 300);

        List<Long> times = db.careLogDao().waterTimesSince(id, 200);

        assertEquals(1, times.size());
        assertEquals(Long.valueOf(300), times.get(0));
    }
}
