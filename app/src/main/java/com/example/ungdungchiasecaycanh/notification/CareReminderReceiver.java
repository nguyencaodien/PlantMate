package com.example.ungdungchiasecaycanh.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.example.ungdungchiasecaycanh.data.local.db.PlantMateDatabase;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import android.Manifest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CareReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long plantId = intent == null ? -1 : intent.getLongExtra(CareReminderScheduler.EXTRA_PLANT_ID, -1);
        if (plantId <= 0) return;
        PendingResult pending = goAsync();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String uid = new FirebaseAuthDataSource().getCurrentUserId();
                if (uid == null || uid.trim().isEmpty()) return;
                PlantEntity plant = PlantMateDatabase.getInstance(context).plantDao()
                        .getByIdForOwner(plantId, uid);
                boolean enabled = new SettingsRepository(context)
                        .isCareNotificationsEnabledForOwner(uid);
                boolean permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                        || ContextCompat.checkSelfPermission(context,
                        Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
                if (permissionGranted && CareReminderScheduler.shouldDeliver(
                        plant, uid, System.currentTimeMillis(), enabled)) {
                    NotificationHelper.showWateringReminder(context, plant);
                    // Daily plans ring again at the next reminder time while the watering is still open.
                    new CareReminderScheduler(context).scheduleOrCancel(plant);
                }
            } catch (RuntimeException ignored) {
                // Room remains authoritative; Garden reconciliation can recover future alarms.
            } finally {
                pending.finish();
                executor.shutdown();
            }
        });
    }
}
