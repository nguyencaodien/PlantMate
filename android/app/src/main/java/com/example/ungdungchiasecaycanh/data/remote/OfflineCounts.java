package com.example.ungdungchiasecaycanh.data.remote;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.Query;

/**
 * Server counts (likes, comments, unread...) that still show something offline: each successful count
 * is remembered on the device for the signed-in account and returned when the server cannot be reached.
 * Firestore's own offline cache does not hold aggregate results, hence this small store.
 */
public final class OfflineCounts {
    private static SharedPreferences store;

    private OfflineCounts() { }

    /** Called once from the Application. */
    public static void init(Context context) {
        store = context.getApplicationContext().getSharedPreferences("plantmate_offline_counts", Context.MODE_PRIVATE);
    }

    /** The count of {@code query}; offline, the last known value under {@code key} (0 if never counted). */
    public static Task<Long> count(Query query, String key) {
        String k = scoped(key);
        return query.count().get(AggregateSource.SERVER).continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                long value = task.getResult().getCount();
                if (store != null && k != null) store.edit().putLong(k, value).apply();
                return Tasks.forResult(value);
            }
            long remembered = store == null || k == null ? 0 : store.getLong(k, 0);
            return Tasks.forResult(remembered);
        });
    }

    /** Counts are per account so one member never sees another's remembered numbers. */
    private static String scoped(String key) {
        String uid = FirebaseAuth.getInstance().getUid();
        return uid == null ? null : uid + ":" + key;
    }
}
