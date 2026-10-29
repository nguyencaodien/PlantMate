package com.example.ungdungchiasecaycanh.data.local.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

import com.example.ungdungchiasecaycanh.data.local.dao.CareLogDao;
import com.example.ungdungchiasecaycanh.data.local.dao.PlantDao;
import com.example.ungdungchiasecaycanh.data.local.dao.DoctorHistoryDao;
import com.example.ungdungchiasecaycanh.data.local.dao.ListingDraftDao;
import com.example.ungdungchiasecaycanh.data.local.dao.StoryViewDao;
import com.example.ungdungchiasecaycanh.data.local.dao.PlantPhotoDao;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.ListingDraftEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.StoryViewEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantPhotoEntity;

@Database(entities = {PlantEntity.class, CareLogEntity.class, DoctorHistoryEntity.class, PlantPhotoEntity.class,
        ListingDraftEntity.class, StoryViewEntity.class}, version = 10, exportSchema = false)
@TypeConverters(CareTypeConverter.class)
public abstract class PlantMateDatabase extends RoomDatabase {
    private static volatile PlantMateDatabase instance;

    public abstract PlantDao plantDao();
    public abstract CareLogDao careLogDao();
    public abstract DoctorHistoryDao doctorHistoryDao();
    public abstract PlantPhotoDao plantPhotoDao();
    public abstract ListingDraftDao listingDraftDao();
    public abstract StoryViewDao storyViewDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `doctor_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `ownerUid` TEXT NOT NULL, `plantId` INTEGER, `imageUri` TEXT NOT NULL, `diagnosis` TEXT NOT NULL, `confidence` REAL, `guidance` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_doctor_history_plantId` ON `doctor_history` (`plantId`)");
        }
    };

    /** v3: Plant Doctor also stores what the plant is and whether it looks healthy. */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `doctor_history` ADD COLUMN `plantName` TEXT");
            database.execSQL("ALTER TABLE `doctor_history` ADD COLUMN `scientificName` TEXT");
            database.execSQL("ALTER TABLE `doctor_history` ADD COLUMN `healthy` INTEGER");
        }
    };

    /** v4: daily watering plans keep their reminder times; existing plants stay every-N-days (""). */
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `plants` ADD COLUMN `wateringTimes` TEXT NOT NULL DEFAULT ''");
        }
    };

    /** v5: Plant Doctor remembers whether the member said a diagnosis was right. */
    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `doctor_history` ADD COLUMN `feedback` INTEGER");
        }
    };

    /** v6: plants remember their area (room) and light for the Garden areas; older plants stay unset (""). */
    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `plants` ADD COLUMN `room` TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE `plants` ADD COLUMN `light` TEXT NOT NULL DEFAULT ''");
        }
    };

    /** v7: photos over time for each plant (removed with the plant). */
    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `plant_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `plantId` INTEGER NOT NULL, `imageUri` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_plant_photos_plantId` ON `plant_photos` (`plantId`)");
        }
    };

    /** v8: one saved listing draft per account ("Save draft" on the create form). */
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `listing_drafts` (`ownerUid` TEXT NOT NULL, `type` TEXT NOT NULL, `nickname` TEXT NOT NULL, `species` TEXT NOT NULL, `lookingFor` TEXT NOT NULL, `price` INTEGER NOT NULL, `description` TEXT NOT NULL, `locationText` TEXT NOT NULL, `imageUris` TEXT NOT NULL, `conditions` TEXT NOT NULL, `heightCm` INTEGER NOT NULL, `light` TEXT NOT NULL, `pickupTimes` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`ownerUid`))");
        }
    };

    /** v9: which friends' stories this account has opened (Community "green moments"). */
    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `story_views` (`ownerUid` TEXT NOT NULL, `storyId` TEXT NOT NULL, `viewedAt` INTEGER NOT NULL, PRIMARY KEY(`ownerUid`, `storyId`))");
        }
    };

    /** Version 10: listing drafts keep the kind of plant, pot size, age and "negotiable". */
    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `listing_drafts` ADD COLUMN `category` TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE `listing_drafts` ADD COLUMN `potCm` INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `listing_drafts` ADD COLUMN `age` TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE `listing_drafts` ADD COLUMN `negotiable` INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static PlantMateDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (PlantMateDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            PlantMateDatabase.class, "plantmate.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                            .build();
                }
            }
        }
        return instance;
    }
}
