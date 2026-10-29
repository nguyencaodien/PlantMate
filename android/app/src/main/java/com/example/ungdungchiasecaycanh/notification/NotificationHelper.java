package com.example.ungdungchiasecaycanh.notification;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.ui.garden.PlantDetailActivity;

public final class NotificationHelper {
    private NotificationHelper() { }

    public static boolean showWateringReminder(Context context, PlantEntity plant) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return false;

        NotificationChannels.ensurePlantCareChannel(context);
        Intent detailIntent = new Intent(context, PlantDetailActivity.class)
                .setData(Uri.parse("plantmate://plant/" + plant.getId()))
                .putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plant.getId())
                // CLEAR_TOP without SINGLE_TOP: an open plant screen is replaced, so it never shows another plant.
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String nickname = plant.getNickname() == null ? "" : plant.getNickname().trim();
        String title = nickname.isEmpty()
                ? context.getString(R.string.notification_water_title_fallback)
                : context.getString(R.string.notification_water_title, nickname);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context,
                NotificationChannels.PLANT_CARE_REMINDERS)
                .setSmallIcon(R.drawable.ic_nav_garden)
                .setContentTitle(title)
                .setContentText(context.getString(R.string.notification_water_body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(notificationId(plant.getId()), builder.build());
        return true;
    }

    /** Removes every reminder already shown (they name the member's plants) when the member leaves. */
    public static void clearAll(Context context) {
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    }

    public static int notificationId(long plantId) {
        long mixed = plantId ^ (plantId >>> 33) ^ (plantId << 11);
        return (int) (mixed ^ (mixed >>> 32));
    }
}
