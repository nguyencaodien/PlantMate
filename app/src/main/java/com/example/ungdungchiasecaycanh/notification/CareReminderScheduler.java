package com.example.ungdungchiasecaycanh.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;

import java.util.List;

public class CareReminderScheduler {
    public static final String EXTRA_PLANT_ID = "extra_plant_id";
    private static final String ALARM_SCHEME = "plantmate";
    private static final String ALARM_AUTHORITY = "care-reminder";

    private final Context context;
    private final AlarmManager alarmManager;
    private final SettingsRepository settingsRepository;

    public CareReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
        settingsRepository = new SettingsRepository(this.context);
    }

    public void scheduleOrCancel(PlantEntity plant) {
        if (plant == null) return;
        long now = System.currentTimeMillis();
        boolean globallyEnabled = settingsRepository.isCareNotificationsEnabledForOwner(
                plant.getOwnerUid());
        Long at = globallyEnabled ? alarmTime(plant, now) : null;
        if (at == null) {
            cancel(plant.getId());
            return;
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                at, alarmPendingIntent(plant.getId(), PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public void cancel(long plantId) {
        PendingIntent pendingIntent = alarmPendingIntent(plantId, PendingIntent.FLAG_NO_CREATE);
        if (pendingIntent != null) alarmManager.cancel(pendingIntent);
    }

    public void reconcile(List<PlantEntity> plants) {
        if (plants == null) return;
        for (PlantEntity plant : plants) scheduleOrCancel(plant);
    }

    private PendingIntent alarmPendingIntent(long plantId, int lookupFlag) {
        Intent intent = new Intent(context, CareReminderReceiver.class)
                .setData(Uri.parse(alarmIdentity(plantId)))
                .putExtra(EXTRA_PLANT_ID, plantId);
        return PendingIntent.getBroadcast(context, 0, intent,
                lookupFlag | PendingIntent.FLAG_IMMUTABLE);
    }

    public static boolean shouldSchedule(PlantEntity plant, long now) {
        return shouldSchedule(plant, now, true);
    }

    public static boolean shouldSchedule(PlantEntity plant, long now, boolean globallyEnabled) {
        return globallyEnabled && alarmTime(plant, now) != null;
    }

    /**
     * When the reminder should ring: the planned watering, or for a daily plan that is already due
     * (a missed watering) the next reminder time of the day. Every-N-days plants ring once.
     */
    static Long alarmTime(PlantEntity plant, long now) {
        if (plant == null || !plant.isReminderEnabled()
                || plant.getWateringIntervalDays() < 1 || plant.getWateringIntervalDays() > 365
                || plant.getNextWateringAt() == null) return null;
        if (plant.getNextWateringAt() > now) return plant.getNextWateringAt();
        List<Integer> times = GardenRepository.parseTimes(plant.getWateringTimes());
        return times.isEmpty() ? null : GardenRepository.nextSlotAfter(times, now);
    }

    public static boolean shouldDeliver(PlantEntity plant, String currentUid, long now) {
        return shouldDeliver(plant, currentUid, now, true);
    }

    public static boolean shouldDeliver(PlantEntity plant, String currentUid, long now,
                                        boolean globallyEnabled) {
        return plant != null
                && globallyEnabled
                && currentUid != null
                && currentUid.equals(plant.getOwnerUid())
                && plant.isReminderEnabled()
                && plant.getWateringIntervalDays() >= 1
                && plant.getWateringIntervalDays() <= 365
                && plant.getNextWateringAt() != null
                && plant.getNextWateringAt() <= now;
    }

    public static String alarmIdentity(long plantId) {
        return ALARM_SCHEME + "://" + ALARM_AUTHORITY + "/" + plantId;
    }
}
