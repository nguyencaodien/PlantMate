package com.example.ungdungchiasecaycanh.data.local.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** A v3 database opens as the current version with its plants, care log and Doctor history intact. */
@RunWith(AndroidJUnit4.class)
public class PlantMateMigrationTest {
    private static final String NAME = "migration-test.db";
    private final Context context = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() {
        context.deleteDatabase(NAME);
    }

    @Test
    public void version3PlantsBecomeEveryNDaysPlans() {
        context.deleteDatabase(NAME);
        try (SQLiteDatabase v3 = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(NAME), null)) {
            v3.execSQL("CREATE TABLE `plants` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ownerUid` TEXT NOT NULL, `nickname` TEXT NOT NULL, `species` TEXT NOT NULL, `imageUri` TEXT NOT NULL, `reminderEnabled` INTEGER NOT NULL, `wateringIntervalDays` INTEGER NOT NULL, `nextWateringAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
            v3.execSQL("CREATE TABLE `care_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plantId` INTEGER NOT NULL, `type` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            v3.execSQL("CREATE INDEX `index_care_logs_plantId` ON `care_logs` (`plantId`)");
            v3.execSQL("CREATE TABLE `doctor_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ownerUid` TEXT NOT NULL, `plantId` INTEGER, `imageUri` TEXT NOT NULL, `diagnosis` TEXT NOT NULL, `confidence` REAL, `guidance` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `plantName` TEXT, `scientificName` TEXT, `healthy` INTEGER, FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
            v3.execSQL("CREATE INDEX `index_doctor_history_plantId` ON `doctor_history` (`plantId`)");
            v3.execSQL("INSERT INTO plants VALUES (7, 'uid-a', 'Mochi', 'Monstera', '', 1, 5, 9000, 100, 200)");
            v3.execSQL("INSERT INTO care_logs VALUES (1, 7, 'WATER', '', 5000)");
            v3.execSQL("INSERT INTO care_logs VALUES (2, 7, 'NOTE', 'new leaf', 6000)");
            v3.execSQL("INSERT INTO doctor_history VALUES (3, 'uid-a', 7, 'content://x', 'Overwatering', 0.8, 'Let it dry', 7000, 'Monstera', NULL, 0)");
            v3.setVersion(3);
        }

        PlantMateDatabase db = Room.databaseBuilder(context, PlantMateDatabase.class, NAME)
                .addMigrations(PlantMateDatabase.MIGRATION_1_2, PlantMateDatabase.MIGRATION_2_3,
                        PlantMateDatabase.MIGRATION_3_4, PlantMateDatabase.MIGRATION_4_5,
                        PlantMateDatabase.MIGRATION_5_6, PlantMateDatabase.MIGRATION_6_7,
                        PlantMateDatabase.MIGRATION_7_8, PlantMateDatabase.MIGRATION_8_9,
                        PlantMateDatabase.MIGRATION_9_10)
                .build();
        try {
            PlantEntity plant = db.plantDao().getByIdForOwner(7, "uid-a");
            assertEquals("Mochi", plant.getNickname());
            assertEquals(5, plant.getWateringIntervalDays());
            assertEquals(9000L, plant.getNextWateringAt().longValue());
            assertEquals("", plant.getWateringTimes());

            List<Long> water = db.careLogDao().waterTimesSince(7, 0);
            assertEquals(Collections.singletonList(5000L), water);
            assertEquals(Collections.emptyList(), db.careLogDao().waterTimesSince(7, 5001));

            assertEquals(null, db.doctorHistoryDao().getHistoryByIdForOwner(3, "uid-a").getFeedback());
            assertEquals(1, db.doctorHistoryDao().setFeedback(3, "uid-a", false));
            assertEquals(Boolean.FALSE, db.doctorHistoryDao().getHistoryByIdForOwner(3, "uid-a").getFeedback());
        } finally {
            db.close();
        }
    }
}
