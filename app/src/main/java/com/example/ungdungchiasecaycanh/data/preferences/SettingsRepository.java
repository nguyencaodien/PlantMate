package com.example.ungdungchiasecaycanh.data.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SettingsRepository {
    public static final String KEY_CARE_NOTIFICATIONS_ENABLED = "care_notifications_enabled";
    public static final String KEY_VNEID_DEMO_DONE = "vneid_demo_done";
    public static final String KEY_ONBOARDING_DONE = "onboarding_done";
    public static final String KEY_NIGHT_MODE = "night_mode";
    /** Local midnight of the day the Notifications screen was last opened (the watering row counts once a day). */
    public static final String KEY_NOTIFICATIONS_SEEN_DAY = "notifications_seen_day";
    public static final String KEY_INTENTS = "onboarding_intents";
    /** What the member said they came for on the first run. */
    public static final String INTENT_RECEIVE = "RECEIVE", INTENT_GIVE = "GIVE", INTENT_TRADE = "TRADE",
            INTENT_CARE = "CARE";
    private static final String PREFERENCES_PREFIX = "plantmate_settings_";

    private final Context context;
    private final FirebaseAuthDataSource authDataSource;

    public SettingsRepository(Context context) {
        this.context = context.getApplicationContext();
        this.authDataSource = new FirebaseAuthDataSource();
    }

    public boolean hasAuthenticatedUser() { return currentUid() != null; }

    public boolean isCareNotificationsEnabled() {
        return preferencesForCurrentUser().getBoolean(KEY_CARE_NOTIFICATIONS_ENABLED,
                defaultCareNotificationsEnabled());
    }

    public boolean isCareNotificationsEnabledForOwner(String ownerUid) {
        String current = currentUid();
        return current != null && current.equals(ownerUid)
                && preferences(current).getBoolean(KEY_CARE_NOTIFICATIONS_ENABLED,
                defaultCareNotificationsEnabled());
    }

    public void setCareNotificationsEnabled(boolean enabled) {
        if (!preferencesForCurrentUser().edit()
                .putBoolean(KEY_CARE_NOTIFICATIONS_ENABLED, enabled).commit()) {
            throw new IllegalStateException("Unable to save notification preference");
        }
    }

    /** This account went through the VNeID step. It is a demo: no real identity check happens. */
    public boolean isVneidDemoDone() {
        String uid = currentUid();
        return uid != null && preferences(uid).getBoolean(KEY_VNEID_DEMO_DONE, false);
    }

    public void setVneidDemoDone() {
        preferencesForCurrentUser().edit().putBoolean(KEY_VNEID_DEMO_DONE, true).apply();
    }

    private static final String CHAT_SEEN_PREFIX = "chat_seen_", KEY_CHAT_SEEN_SINCE = "chat_seen_since";

    /** Conversation ID → when it was last open here. */
    public java.util.Map<String, Long> getChatSeen() {
        java.util.Map<String, Long> out = new java.util.HashMap<>();
        String uid = currentUid();
        if (uid == null) return out;
        for (java.util.Map.Entry<String, ?> e : preferences(uid).getAll().entrySet()) {
            if (e.getKey().startsWith(CHAT_SEEN_PREFIX) && !e.getKey().equals(KEY_CHAT_SEEN_SINCE) && e.getValue() instanceof Long) {
                out.put(e.getKey().substring(CHAT_SEEN_PREFIX.length()), (Long) e.getValue());
            }
        }
        return out;
    }

    /** Chats untouched since this feature arrived count as read: this is the first time it was asked. */
    public long getChatSeenSince() {
        String uid = currentUid();
        if (uid == null) return Long.MAX_VALUE;
        SharedPreferences p = preferences(uid);
        if (!p.contains(KEY_CHAT_SEEN_SINCE)) p.edit().putLong(KEY_CHAT_SEEN_SINCE, System.currentTimeMillis()).apply();
        return p.getLong(KEY_CHAT_SEEN_SINCE, System.currentTimeMillis());
    }

    public void setChatSeen(String conversationId) {
        if (currentUid() == null || conversationId == null || conversationId.isEmpty()) return;
        preferencesForCurrentUser().edit().putLong(CHAT_SEEN_PREFIX + conversationId, System.currentTimeMillis()).apply();
    }

    public long getNotificationsSeenDay() {
        String uid = currentUid();
        return uid == null ? 0 : preferences(uid).getLong(KEY_NOTIFICATIONS_SEEN_DAY, 0);
    }

    public void setNotificationsSeenDay(long dayStart) {
        if (currentUid() != null) preferencesForCurrentUser().edit().putLong(KEY_NOTIFICATIONS_SEEN_DAY, dayStart).apply();
    }

    /** The first-run steps were finished or skipped by this account (on this device). */
    public boolean isOnboardingDone() {
        String uid = currentUid();
        return uid == null || preferences(uid).getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void completeOnboarding(Set<String> intents) {
        preferencesForCurrentUser().edit().putBoolean(KEY_ONBOARDING_DONE, true)
                .putStringSet(KEY_INTENTS, new HashSet<>(intents)).apply();
    }

    public Set<String> getIntents() {
        String uid = currentUid();
        return uid == null ? Collections.emptySet()
                : new HashSet<>(preferences(uid).getStringSet(KEY_INTENTS, Collections.emptySet()));
    }

    /** AppCompat night mode for the signed-in member; follows the system until they choose. */
    public int getNightMode() {
        String uid = currentUid();
        return uid == null ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                : preferences(uid).getInt(KEY_NIGHT_MODE, androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setNightMode(int mode) {
        if (currentUid() != null) preferencesForCurrentUser().edit().putInt(KEY_NIGHT_MODE, mode).apply();
    }

    /** Home shows trade requests before give-aways for members who came to trade, not to receive. */
    public static boolean tradesFirst(Set<String> intents) {
        return intents.contains(INTENT_TRADE) && !intents.contains(INTENT_RECEIVE);
    }

    private SharedPreferences preferencesForCurrentUser() {
        return preferences(requireUid(currentUid()));
    }

    private SharedPreferences preferences(String uid) {
        return context.getSharedPreferences(namespaceForUid(uid), Context.MODE_PRIVATE);
    }

    private String currentUid() {
        String uid = authDataSource.getCurrentUserId();
        return uid == null || uid.trim().isEmpty() ? null : uid;
    }

    public static String namespaceForUid(String uid) {
        return PREFERENCES_PREFIX + requireUid(uid);
    }

    public static boolean defaultCareNotificationsEnabled() { return true; }

    /** Language tags offered in Settings; "" follows the device language. */
    public static final String LANGUAGE_SYSTEM = "";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_VIETNAMESE = "vi";

    /** The in-app language tag ("" when following the device). AppCompat stores it per app. */
    public String getAppLanguage() {
        LocaleListCompat locales = AppCompatDelegate.getApplicationLocales();
        return locales.isEmpty() || locales.get(0) == null ? LANGUAGE_SYSTEM : locales.get(0).getLanguage();
    }

    /** Applies a language to the whole app; visible activities are recreated in the new language. */
    public void setAppLanguage(String tag) {
        AppCompatDelegate.setApplicationLocales(tag == null || tag.isEmpty()
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(tag));
    }

    private static String requireUid(String uid) {
        String normalized = uid == null ? "" : uid.trim();
        if (normalized.isEmpty()) throw new IllegalStateException("Authentication required");
        return normalized;
    }
}
