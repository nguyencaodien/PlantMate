package com.example.ungdungchiasecaycanh.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.example.ungdungchiasecaycanh.R;

public final class NotificationChannels {
    public static final String PLANT_CARE_REMINDERS = "plant_care_reminders";

    private NotificationChannels() { }

    public static void ensurePlantCareChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(PLANT_CARE_REMINDERS,
                context.getString(R.string.notification_channel_care_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notification_channel_care_description));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
