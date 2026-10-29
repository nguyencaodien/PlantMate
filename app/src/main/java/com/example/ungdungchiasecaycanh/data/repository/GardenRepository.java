package com.example.ungdungchiasecaycanh.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.local.dao.PlantDao;
import com.example.ungdungchiasecaycanh.data.local.dao.CareLogDao;
import com.example.ungdungchiasecaycanh.data.local.db.PlantMateDatabase;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantPhotoEntity;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.notification.CareReminderScheduler;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GardenRepository {
    private static final String TAG = "GardenRepository";
    /** A daily plan has 1 to this many reminder times. */
    public static final int MAX_TIMES_PER_DAY = 4;
    /** Two waterings of one plant must be at least this far apart (stops double taps and spam). */
    public static final long MIN_WATER_GAP_MS = TimeUnit.HOURS.toMillis(1);

    /** A WATER log refused because today's plan is used up, or the last one was too recent. */
    public static final class CareLimitException extends IllegalStateException {
        public final boolean dailyLimit;
        public final int perDay;
        /** When the next watering can be logged. */
        public final long allowedAt;
        /** The plant has a watering plan (reminders on); otherwise the limit is the default once a day. */
        public final boolean planned;

        CareLimitException(boolean dailyLimit, int perDay, long allowedAt, boolean planned) {
            super(dailyLimit ? "Today's waterings are already logged" : "Watered too recently");
            this.dailyLimit = dailyLimit;
            this.perDay = perDay;
            this.allowedAt = allowedAt;
            this.planned = planned;
        }
    }
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception error);
    }

    private final PlantDao plantDao;
    private final CareLogDao careLogDao;
    private final PlantMateDatabase database;
    private final FirebaseAuthDataSource authDataSource;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final CareReminderScheduler reminderScheduler;
    private final Context appContext;

    public GardenRepository(Context context) {
        appContext = context.getApplicationContext();
        database = PlantMateDatabase.getInstance(context);
        plantDao = database.plantDao();
        careLogDao = database.careLogDao();
        authDataSource = new FirebaseAuthDataSource();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        reminderScheduler = new CareReminderScheduler(context);
    }

    public boolean hasAuthenticatedUser() { return currentUid() != null; }
    public LiveData<List<PlantEntity>> observeCurrentUserPlants() {
        String uid = currentUid();
        if (uid == null) return new MutableLiveData<>(Collections.emptyList());
        return plantDao.observeForOwner(uid);
    }

    /** The signed-in member's care logs since {@code since}; empty when signed out. */
    public LiveData<List<CareLogEntity>> observeCurrentUserCareSince(long since) {
        String uid = currentUid();
        if (uid == null) return new MutableLiveData<>(Collections.emptyList());
        return careLogDao.observeForOwnerSince(uid, since);
    }

    /** The plant's photos over time, newest first; empty when signed out or not the owner's plant. */
    public LiveData<List<PlantPhotoEntity>> observePlantPhotos(long plantId) {
        String uid = currentUid();
        if (uid == null) return new MutableLiveData<>(Collections.emptyList());
        return database.plantPhotoDao().observeForPlant(plantId, uid);
    }

    /** Latest Plant Doctor verdict for the plant (true = healthy), or null. */
    public LiveData<Boolean> observeLatestHealth(long plantId) {
        String uid = currentUid();
        if (uid == null) return new MutableLiveData<>(null);
        return database.doctorHistoryDao().observeLatestHealth(plantId, uid);
    }

    public LiveData<DoctorHistoryEntity> observeLatestCheck(long plantId) {
        String uid = currentUid();
        if (uid == null) return new MutableLiveData<>(null);
        return database.doctorHistoryDao().observeLatestCheck(plantId, uid);
    }

    /** Adds a cropped photo (app-private content URI) to one of the member's plants. */
    public void addPlantPhoto(long plantId, String imageUri, Callback<Long> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        final String uri;
        try {
            uri = normalizeImageUri(imageUri);
            if (uri.isEmpty()) throw new IllegalArgumentException("Photo is required");
        } catch (Exception error) { callback.onError(error); return; }
        executor.execute(() -> {
            try {
                if (!isOwnedBy(plantDao.getByIdForOwner(plantId, uid), uid)) throw new SecurityException("Plant unavailable");
                long id = database.plantPhotoDao().insert(new PlantPhotoEntity(0, plantId, uri, System.currentTimeMillis()));
                mainHandler.post(() -> callback.onSuccess(id));
            } catch (Exception error) { mainHandler.post(() -> callback.onError(error)); }
        });
    }

    public void getPlant(long plantId, Callback<PlantEntity> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        executor.execute(() -> {
            try {
                PlantEntity plant = plantDao.getByIdForOwner(plantId, uid);
                mainHandler.post(() -> callback.onSuccess(plant));
            } catch (Exception error) { mainHandler.post(() -> callback.onError(error)); }
        });
    }

    public LiveData<PlantEntity> observePlant(long plantId) {
        String uid = currentUid();
        if (uid == null) return new MutableLiveData<>(null);
        return plantDao.observeByIdForOwner(plantId, uid);
    }

    public void observeCareLogsForOwnedPlant(long plantId,
                                             Callback<LiveData<List<CareLogEntity>>> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        executor.execute(() -> {
            try {
                PlantEntity plant = plantDao.getByIdForOwner(plantId, uid);
                if (!isOwnedBy(plant, uid)) throw new SecurityException("Plant unavailable");
                LiveData<List<CareLogEntity>> history = careLogDao.observeHistory(plantId); // newest first in SQL
                mainHandler.post(() -> callback.onSuccess(history));
            } catch (Exception error) { mainHandler.post(() -> callback.onError(error)); }
        });
    }

    public void recordCare(long plantId, CareType careType, String note,
                           Callback<CareLogEntity> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        final String normalizedNote;
        try { normalizedNote = normalizeCareNote(careType, note); }
        catch (Exception error) { callback.onError(error); return; }
        executor.execute(() -> {
            try {
                final CareLogEntity[] result = new CareLogEntity[1];
                database.runInTransaction(() -> {
                    PlantEntity plant = plantDao.getByIdForOwner(plantId, uid);
                    if (!isOwnedBy(plant, uid)) throw new SecurityException("Plant unavailable");
                    long actionTime = System.currentTimeMillis();
                    if (careType == CareType.WATER) {
                        long since = Math.min(startOfDay(actionTime), actionTime - MIN_WATER_GAP_MS);
                        checkWaterLimit(careLogDao.waterTimesSince(plantId, since), waterPerDay(plant), actionTime,
                                plant.isReminderEnabled());
                    }
                    CareLogEntity log = new CareLogEntity(0, plantId, careType, normalizedNote, actionTime);
                    long insertedId = careLogDao.insert(log);
                    log.setId(insertedId);
                    if (shouldRescheduleAfterCare(careType, plant.isReminderEnabled())) {
                        Long nextWateringAt = nextWateringAtAfterWater(plant, actionTime);
                        if (plantDao.updateWateringScheduleForOwner(plantId, uid,
                                nextWateringAt, actionTime) != 1)
                            throw new IllegalStateException("Plant schedule update failed");
                    }
                    result[0] = log;
                });
                CareLogEntity savedLog = result[0];
                if (careType == CareType.WATER) {
                    try {
                        PlantEntity updatedPlant = plantDao.getByIdForOwner(plantId, uid);
                        reminderScheduler.scheduleOrCancel(updatedPlant);
                    } catch (RuntimeException schedulingError) {
                        Log.w(TAG, "Watering reminder scheduling will be retried during reconciliation");
                    }
                }
                mainHandler.post(() -> callback.onSuccess(savedLog));
            } catch (Exception error) { mainHandler.post(() -> callback.onError(error)); }
        });
    }

    public static String normalizeCareNote(CareType type, String note) {
        if (type == null) throw new IllegalArgumentException("Care type is required");
        String normalized = note == null ? "" : note.trim();
        if (normalized.length() > 500) throw new IllegalArgumentException("Note must be 500 characters or fewer");
        if (type == CareType.NOTE && normalized.isEmpty())
            throw new IllegalArgumentException("A note is required");
        return normalized;
    }

    public static Long nextWateringAtAfterWater(PlantEntity plant, long careTime) {
        if (plant == null) throw new IllegalArgumentException("Plant is required");
        if (!plant.isReminderEnabled()) return null;
        List<Integer> times = parseTimes(plant.getWateringTimes());
        // Watered early or on time: the next reminder is the first slot at least an hour later.
        if (!times.isEmpty()) return nextSlotAfter(times, careTime + MIN_WATER_GAP_MS);
        int interval = plant.getWateringIntervalDays();
        if (interval < 1 || interval > 365)
            throw new IllegalStateException("Plant watering interval is invalid");
        return addCalendarDays(careTime, interval);
    }

    public static boolean shouldRescheduleAfterCare(CareType type, boolean reminderEnabled) {
        return type == CareType.WATER && reminderEnabled;
    }

    public void addPlant(String nickname, String species, String imageUri,
                         boolean reminderEnabled, int intervalDays, String wateringTimes,
                         String room, String light, Callback<Long> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        final PlantEntity plant;
        try {
            plant = normalizeForCreate(uid, nickname, species, imageUri,
                    reminderEnabled, intervalDays, wateringTimes, room, light, System.currentTimeMillis());
        } catch (Exception error) { callback.onError(error); return; }
        executor.execute(() -> {
            try {
                long plantId = plantDao.insert(plant);
                plant.setId(plantId);
                try { reminderScheduler.scheduleOrCancel(plant); }
                catch (RuntimeException schedulingError) {
                    Log.w(TAG, "Plant reminder scheduling will be retried during reconciliation");
                }
                mainHandler.post(() -> callback.onSuccess(plantId));
            } catch (Exception error) { mainHandler.post(() -> callback.onError(error)); }
        });
    }

    public void updatePlant(long plantId, String nickname, String species, String imageUri,
                            boolean reminderEnabled, int intervalDays, String wateringTimes,
                            String room, String light, Callback<PlantEntity> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        executor.execute(() -> {
            try {
                PlantEntity existing = plantDao.getByIdForOwner(plantId, uid);
                if (existing == null) throw new IllegalStateException("Plant unavailable");
                PlantEntity updated = normalizeForUpdate(existing, uid, nickname, species,
                        imageUri, reminderEnabled, intervalDays, wateringTimes, room, light, System.currentTimeMillis());
                if (plantDao.update(updated) != 1) throw new IllegalStateException("Plant update failed");
                try { reminderScheduler.scheduleOrCancel(updated); }
                catch (RuntimeException schedulingError) {
                    Log.w(TAG, "Plant reminder scheduling will be retried during reconciliation");
                }
                mainHandler.post(() -> callback.onSuccess(updated));
            } catch (Exception error) {
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    /**
     * Deletes the signed-in member's plant with its care logs, photos and reminder. App-private photo files
     * it alone used are removed too; Doctor history keeps its entries without the plant link.
     */
    public void deletePlant(long plantId, Callback<Void> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        executor.execute(() -> {
            try {
                PlantEntity plant = plantDao.getByIdForOwner(plantId, uid);
                if (plant == null) throw new IllegalStateException("Plant unavailable");
                List<String> images = new ArrayList<>(database.plantPhotoDao().getImageUrisForPlant(plantId, uid));
                if (plant.getImageUri() != null && !plant.getImageUri().isEmpty()) images.add(plant.getImageUri());
                List<String> orphaned = new ArrayList<>();
                for (String uri : images) {
                    if (plantDao.countOtherPlantsUsingImage(uri, plantId) == 0
                            && database.plantPhotoDao().countOtherPlantsUsingImage(uri, plantId) == 0
                            && database.doctorHistoryDao().countUsingImage(uri) == 0) orphaned.add(uri);
                }
                try { reminderScheduler.cancel(plantId); }
                catch (RuntimeException ignored) { /* reconciliation drops reminders of missing plants */ }
                if (plantDao.deleteForOwner(plantId, uid) != 1) throw new IllegalStateException("Plant delete failed");
                for (String uri : orphaned) deletePrivatePhoto(uri);
                mainHandler.post(() -> callback.onSuccess(null));
            } catch (Exception error) {
                mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    /**
     * Account deletion, on the calling (background) thread: cancels the account's reminders and
     * deletes its plants, care logs, plant photos, Doctor history, listing draft and seen-story
     * marks, then the photo files no other account on the device still uses.
     */
    public void deleteAllForOwnerBlocking(String uid) {
        List<String> images = new ArrayList<>(plantDao.getImageUrisForOwner(uid));
        images.addAll(database.plantPhotoDao().getImageUrisForOwner(uid));
        images.addAll(database.doctorHistoryDao().getImageUrisForOwner(uid));
        for (Long plantId : plantDao.getIdsForOwner(uid)) {
            try { reminderScheduler.cancel(plantId); }
            catch (RuntimeException ignored) { /* reconciliation drops reminders of missing plants */ }
        }
        database.runInTransaction(() -> {
            plantDao.deleteAllForOwner(uid);
            database.doctorHistoryDao().deleteAllForOwner(uid);
            database.listingDraftDao().deleteForOwner(uid);
            database.storyViewDao().deleteAllForOwner(uid);
        });
        for (String uri : new java.util.LinkedHashSet<>(images)) {
            if (uri == null || uri.isEmpty()) continue;
            if (plantDao.countOtherPlantsUsingImage(uri, -1) == 0
                    && database.plantPhotoDao().countOtherPlantsUsingImage(uri, -1) == 0
                    && database.doctorHistoryDao().countUsingImage(uri) == 0) deletePrivatePhoto(uri);
        }
    }

    /** Deletes a cropped photo the app saved under files/photos (FileProvider URI); other URIs are left alone. */
    private void deletePrivatePhoto(String uri) {
        try {
            android.net.Uri parsed = android.net.Uri.parse(uri);
            if (!"content".equals(parsed.getScheme())
                    || !(appContext.getPackageName() + ".fileprovider").equals(parsed.getAuthority())) return;
            String name = parsed.getLastPathSegment();
            if (name == null || name.contains("/") || name.contains("..")) return;
            java.io.File file = new java.io.File(new java.io.File(appContext.getFilesDir(), "photos"), name);
            if (file.isFile() && !file.delete()) Log.w(TAG, "Could not delete an unused plant photo");
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not delete an unused plant photo");
        }
    }

    public void reconcileCurrentUserReminders(Callback<Integer> callback) {
        String uid = currentUid();
        if (uid == null) {
            if (callback != null) callback.onSuccess(0);
            return;
        }
        executor.execute(() -> {
            try {
                List<PlantEntity> plants = plantDao.getReminderPlantsForOwner(uid);
                reminderScheduler.reconcile(plants);
                int count = plants == null ? 0 : plants.size();
                if (callback != null) mainHandler.post(() -> callback.onSuccess(count));
            } catch (Exception error) {
                if (callback != null) mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    public void cancelCurrentUserReminders(Callback<Integer> callback) {
        String uid = currentUid();
        if (uid == null) {
            if (callback != null) callback.onSuccess(0);
            return;
        }
        executor.execute(() -> {
            try {
                List<PlantEntity> plants = plantDao.getReminderPlantsForOwner(uid);
                if (plants != null) {
                    for (PlantEntity plant : plants) reminderScheduler.cancel(plant.getId());
                }
                int count = plants == null ? 0 : plants.size();
                if (callback != null) mainHandler.post(() -> callback.onSuccess(count));
            } catch (Exception error) {
                if (callback != null) mainHandler.post(() -> callback.onError(error));
            }
        });
    }

    private String currentUid() {
        String uid = authDataSource.getCurrentUserId();
        return uid == null || uid.trim().isEmpty() ? null : uid;
    }

    public static PlantEntity normalizeForCreate(String ownerUid, String nickname,
                                                   String species, String imageUri,
                                                   boolean reminderEnabled,
                                                   int intervalDays, String wateringTimes, long now) {
        return normalizeForCreate(ownerUid, nickname, species, imageUri, reminderEnabled, intervalDays,
                wateringTimes, "", "", now);
    }

    public static PlantEntity normalizeForCreate(String ownerUid, String nickname,
                                                   String species, String imageUri,
                                                   boolean reminderEnabled, int intervalDays,
                                                   String wateringTimes, String room, String light, long now) {
        String owner = require(ownerUid, "Owner is required", Integer.MAX_VALUE);
        String normalizedNickname = require(nickname, "Nickname is required", 60);
        String normalizedSpecies = optional(species, 100, "Species is too long");
        String normalizedImage = normalizeImageUri(imageUri);
        String times = normalizeTimes(reminderEnabled, wateringTimes);
        int normalizedInterval = times.isEmpty() ? validateReminder(reminderEnabled, intervalDays) : 1;
        Long next = firstWatering(reminderEnabled, normalizedInterval, times, now);
        return new PlantEntity(0, owner, normalizedNickname, normalizedSpecies,
                normalizedImage, reminderEnabled, normalizedInterval, next, now, now, times,
                normalizeRoom(room), normalizeLight(light));
    }

    /** Keeps the plant's area and light. */
    public static PlantEntity normalizeForUpdate(PlantEntity existing, String currentUid,
                                                   String nickname, String species,
                                                   String imageUri, boolean reminderEnabled,
                                                   int intervalDays, String wateringTimes, long now) {
        return normalizeForUpdate(existing, currentUid, nickname, species, imageUri, reminderEnabled,
                intervalDays, wateringTimes, existing == null ? "" : existing.getRoom(),
                existing == null ? "" : existing.getLight(), now);
    }

    public static PlantEntity normalizeForUpdate(PlantEntity existing, String currentUid,
                                                   String nickname, String species,
                                                   String imageUri, boolean reminderEnabled,
                                                   int intervalDays, String wateringTimes,
                                                   String room, String light, long now) {
        if (!isOwnedBy(existing, currentUid)) throw new SecurityException("Plant is owned by another account");
        String normalizedNickname = require(nickname, "Nickname is required", 60);
        String normalizedSpecies = optional(species, 100, "Species is too long");
        String normalizedImage = normalizeImageUri(imageUri);
        String times = normalizeTimes(reminderEnabled, wateringTimes);
        int normalizedInterval = times.isEmpty() ? validateReminder(reminderEnabled, intervalDays) : 1;
        Long next = null;
        if (reminderEnabled) {
            boolean unchanged = existing.isReminderEnabled()
                    && existing.getWateringIntervalDays() == normalizedInterval
                    && existing.getWateringTimes().equals(times)
                    && existing.getNextWateringAt() != null
                    && existing.getNextWateringAt() > 0;
            next = unchanged ? existing.getNextWateringAt() : firstWatering(true, normalizedInterval, times, now);
        }
        return new PlantEntity(existing.getId(), existing.getOwnerUid(), normalizedNickname,
                normalizedSpecies, normalizedImage, reminderEnabled, normalizedInterval,
                next, existing.getCreatedAt(), now, times, normalizeRoom(room), normalizeLight(light));
    }

    static final int MAX_ROOM_LENGTH = 40;

    /** Trimmed area with single spaces ("  Ban   công " → "Ban công"); "" when not set. */
    static String normalizeRoom(String room) {
        String value = room == null ? "" : room.trim().replaceAll("\\s+", " ");
        if (value.length() > MAX_ROOM_LENGTH) throw new IllegalArgumentException("Area is too long");
        return value;
    }

    /** A {@link LightLevel} name or ""; anything else is refused. */
    static String normalizeLight(String light) {
        String value = light == null ? "" : light.trim();
        if (value.isEmpty()) return "";
        if (LightLevel.fromName(value) == null) throw new IllegalArgumentException("Unknown light level");
        return value;
    }

    public static boolean isOwnedBy(PlantEntity plant, String uid) {
        return plant != null && uid != null && uid.equals(plant.getOwnerUid());
    }

    public static long addCalendarDays(long baseMillis, int days) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(baseMillis);
        calendar.add(Calendar.DAY_OF_YEAR, days);
        return calendar.getTimeInMillis();
    }

    private static Long firstWatering(boolean enabled, int intervalDays, String times, long now) {
        if (!enabled) return null;
        return times.isEmpty() ? addCalendarDays(now, intervalDays) : nextSlotAfter(parseTimes(times), now);
    }

    /** Daily reminder times, sorted ("420,1020" → [420, 1020]); empty for every-N-days plants. */
    public static List<Integer> parseTimes(String value) {
        List<Integer> times = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) return times;
        for (String part : value.split(",")) times.add(Integer.parseInt(part.trim()));
        Collections.sort(times);
        return times;
    }

    public static String formatTimes(List<Integer> times) {
        return times.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    static String normalizeTimes(boolean enabled, String value) {
        if (!enabled) return "";
        List<Integer> times;
        try { times = parseTimes(value); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Watering times are invalid"); }
        if (times.size() > MAX_TIMES_PER_DAY || new HashSet<>(times).size() != times.size())
            throw new IllegalArgumentException("Use 1 to 4 different watering times");
        for (int minute : times)
            if (minute < 0 || minute >= 24 * 60) throw new IllegalArgumentException("Watering times are invalid");
        return formatTimes(times);
    }

    /** The first reminder time strictly after {@code after}, today or tomorrow (local time). */
    public static long nextSlotAfter(List<Integer> times, long after) {
        if (times.isEmpty()) throw new IllegalArgumentException("No watering times");
        Calendar slot = Calendar.getInstance();
        for (int day = 0; day <= 1; day++) {
            for (int minute : times) {
                slot.setTimeInMillis(after);
                slot.add(Calendar.DAY_OF_YEAR, day);
                slot.set(Calendar.HOUR_OF_DAY, minute / 60);
                slot.set(Calendar.MINUTE, minute % 60);
                slot.set(Calendar.SECOND, 0);
                slot.set(Calendar.MILLISECOND, 0);
                if (slot.getTimeInMillis() > after) return slot.getTimeInMillis();
            }
        }
        throw new IllegalStateException("No watering time found");
    }

    /** Waterings the plan expects per day: the daily times, otherwise one. */
    public static int waterPerDay(PlantEntity plant) {
        List<Integer> times = parseTimes(plant.getWateringTimes());
        return times.isEmpty() ? 1 : times.size();
    }

    /**
     * Refuses a WATER log when today already has {@code perDay} of them, or the last one was less
     * than {@link #MIN_WATER_GAP_MS} ago. {@code recent} holds WATER log times from the last day.
     */
    public static void checkWaterLimit(List<Long> recent, int perDay, long now) {
        checkWaterLimit(recent, perDay, now, true);
    }

    public static void checkWaterLimit(List<Long> recent, int perDay, long now, boolean planned) {
        long dayStart = startOfDay(now);
        int today = 0;
        long latest = Long.MIN_VALUE;
        if (recent != null) for (long time : recent) {
            if (time >= dayStart) today++;
            latest = Math.max(latest, time);
        }
        if (today >= perDay) throw new CareLimitException(true, perDay, addCalendarDays(dayStart, 1), planned);
        if (latest != Long.MIN_VALUE && now - latest < MIN_WATER_GAP_MS)
            throw new CareLimitException(false, perDay, latest + MIN_WATER_GAP_MS, planned);
    }

    public static long startOfDay(long time) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(time);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day.getTimeInMillis();
    }

    private static int validateReminder(boolean enabled, int intervalDays) {
        if (!enabled) return 0;
        if (intervalDays < 1 || intervalDays > 365)
            throw new IllegalArgumentException("Watering interval must be 1 to 365 days");
        return intervalDays;
    }

    private static String normalizeImageUri(String value) {
        String normalized = value == null ? "" : value.trim();
        if (!normalized.isEmpty() && !normalized.startsWith("content://"))
            throw new IllegalArgumentException("Plant image must be a local content URI");
        return normalized;
    }

    private static String require(String value, String message, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(message);
        if (normalized.length() > max) throw new IllegalArgumentException(message);
        return normalized;
    }

    private static String optional(String value, int max, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > max) throw new IllegalArgumentException(message);
        return normalized;
    }
}
