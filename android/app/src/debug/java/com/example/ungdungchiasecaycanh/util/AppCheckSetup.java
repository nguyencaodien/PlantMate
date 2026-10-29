package com.example.ungdungchiasecaycanh.util;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;

/**
 * Debug builds: App Check debug provider. On first run it logs a debug secret (tag
 * DebugAppCheckProvider) that the project owner adds under Firebase Console, App Check,
 * Manage debug tokens. Never ship this provider in release builds.
 */
public final class AppCheckSetup {
    private AppCheckSetup() { }

    public static void install(Context context) {
        FirebaseApp.initializeApp(context);
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance());
    }
}
