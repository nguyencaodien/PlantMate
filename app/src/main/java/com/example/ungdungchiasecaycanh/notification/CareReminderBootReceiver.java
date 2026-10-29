package com.example.ungdungchiasecaycanh.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;

public class CareReminderBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        PendingResult pending = goAsync();
        new GardenRepository(context).reconcileCurrentUserReminders(new GardenRepository.Callback<Integer>() {
            @Override public void onSuccess(Integer value) { pending.finish(); }
            @Override public void onError(Exception error) { pending.finish(); }
        });
    }
}
