package com.example.ungdungchiasecaycanh.util;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/** Release builds: App Check with Play Integrity, so only the genuine app can call Firebase AI Logic. */
public final class AppCheckSetup {
    private AppCheckSetup() { }

    public static void install(Context context) {
        FirebaseApp.initializeApp(context);
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance());
    }
}
