package com.example.ungdungchiasecaycanh;

import android.app.Application;

import com.example.ungdungchiasecaycanh.data.remote.OfflineCounts;
import com.example.ungdungchiasecaycanh.util.AppCheckSetup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.PersistentCacheSettings;

/**
 * App entry point: installs Firebase App Check before any Firebase AI Logic request, and keeps
 * Firestore's on-device cache (SQLite) so recently loaded listings, posts, profiles and chats still
 * show offline. Personal data (garden, care logs, drafts) lives in Room and never needs the network.
 */
public class PlantMateApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppCheckSetup.install(this);
        // Must run before the first Firestore call. 100 MB keeps plenty of recent content for offline use.
        FirebaseFirestore.getInstance().setFirestoreSettings(new FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().setSizeBytes(100L * 1024 * 1024).build())
                .build());
        OfflineCounts.init(this);
        followConnectivity();
        // Each account keeps its own light/dark choice; signed out follows the system.
        com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository settings =
                new com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository(this);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(settings.getNightMode());
        com.google.firebase.auth.FirebaseAuth.getInstance().addAuthStateListener(auth ->
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(settings.getNightMode()));
    }

    /**
     * Without this, the first read after the network drops waits ~10 s for the server before using the
     * cache. Telling Firestore at once makes offline screens fill from the cache immediately; queued
     * writes go out when the network returns.
     */
    private void followConnectivity() {
        android.net.ConnectivityManager cm = getSystemService(android.net.ConnectivityManager.class);
        if (cm == null) return;
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        android.net.Network active = cm.getActiveNetwork();
        if (active == null) firestore.disableNetwork();
        cm.registerDefaultNetworkCallback(new android.net.ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(@androidx.annotation.NonNull android.net.Network network) {
                firestore.enableNetwork();
            }

            @Override public void onLost(@androidx.annotation.NonNull android.net.Network network) {
                firestore.disableNetwork();
            }
        });
    }
}
