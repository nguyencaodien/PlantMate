package com.example.ungdungchiasecaycanh.data.remote;

import android.os.Handler;
import android.os.Looper;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SnapshotMetadata;

/**
 * Real-time updates through Firestore's addSnapshotListener: an edit made elsewhere (another
 * phone, the Firebase Console) calls {@code onChange} on the main thread, so the screen reloads
 * itself without a refresh. The first server snapshot (the data the screen just loaded), cached
 * snapshots and this device's own pending writes are skipped; bursts of changes collapse into one
 * call after {@link #DEBOUNCE_MS}.
 */
public final class FirestoreWatch {
    static final long DEBOUNCE_MS = 400;

    private FirestoreWatch() { }

    public static ListenerRegistration watch(Query query, Runnable onChange) {
        Debounce debounce = new Debounce(onChange);
        ListenerRegistration registration = query.addSnapshotListener((snapshot, error) -> {
            if (error != null) android.util.Log.w("FirestoreWatch", "Live updates stopped", error);
            if (snapshot != null) debounce.offer(snapshot.getMetadata());
        });
        return debounce.wrap(registration);
    }

    public static ListenerRegistration watch(DocumentReference document, Runnable onChange) {
        Debounce debounce = new Debounce(onChange);
        ListenerRegistration registration = document.addSnapshotListener((snapshot, error) -> {
            if (error != null) android.util.Log.w("FirestoreWatch", "Live updates stopped", error);
            if (snapshot != null) debounce.offer(snapshot.getMetadata());
        });
        return debounce.wrap(registration);
    }

    private static final class Debounce {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Runnable action;
        private boolean seenServer;

        Debounce(Runnable action) { this.action = action; }

        void offer(SnapshotMetadata metadata) {
            if (metadata.isFromCache() || metadata.hasPendingWrites()) return;
            if (!seenServer) {   // the state the screen already shows
                seenServer = true;
                return;
            }
            android.util.Log.d("FirestoreWatch", "Server change, refreshing");
            handler.removeCallbacks(action);
            handler.postDelayed(action, DEBOUNCE_MS);
        }

        ListenerRegistration wrap(ListenerRegistration registration) {
            return () -> {
                registration.remove();
                handler.removeCallbacks(action);
            };
        }
    }
}
