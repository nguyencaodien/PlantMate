package com.example.ungdungchiasecaycanh.ui.settings;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.data.repository.AdminRepository;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;

public class SettingsViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean authenticated;
        public final boolean careNotificationsEnabled;
        public final boolean saving;
        public final int errorVersion;
        State(boolean authenticated, boolean enabled, boolean saving, int errorVersion) {
            this.authenticated = authenticated; this.careNotificationsEnabled = enabled;
            this.saving = saving; this.errorVersion = errorVersion;
        }
    }

    private final SettingsRepository settings;
    private final GardenRepository garden;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private int errorVersion;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        settings = new SettingsRepository(application);
        garden = new GardenRepository(application);
        load();
    }

    public LiveData<State> getState() { return state; }

    /** Current in-app language tag: "" (device), "en" or "vi". */
    public String getLanguage() { return settings.getAppLanguage(); }

    public void setLanguage(String tag) {
        if (!tag.equals(getLanguage())) settings.setAppLanguage(tag);
    }

    private final MutableLiveData<Boolean> admin = new MutableLiveData<>(false);

    /** True when the signed-in account is on the admin allowlist (admins/{uid}). */
    public LiveData<Boolean> isAdmin() {
        new AdminRepository().isCurrentUserAdmin()
                .addOnSuccessListener(value -> admin.postValue(Boolean.TRUE.equals(value)));
        return admin;
    }

    private void load() {
        if (!settings.hasAuthenticatedUser()) {
            state.setValue(new State(false, true, false, errorVersion)); return;
        }
        try {
            state.setValue(new State(true, settings.isCareNotificationsEnabled(), false, errorVersion));
        } catch (RuntimeException error) {
            state.setValue(new State(false, true, false, ++errorVersion));
        }
    }

    public void setCareNotificationsEnabled(boolean enabled) {
        State current = state.getValue();
        if (current == null || !current.authenticated || current.saving
                || current.careNotificationsEnabled == enabled) return;
        state.setValue(new State(true, current.careNotificationsEnabled, true, errorVersion));
        try {
            settings.setCareNotificationsEnabled(enabled);
            state.setValue(new State(true, enabled, true, errorVersion));
        } catch (RuntimeException error) {
            state.setValue(new State(true, current.careNotificationsEnabled, false, ++errorVersion));
            return;
        }
        GardenRepository.Callback<Integer> callback = new GardenRepository.Callback<Integer>() {
            @Override public void onSuccess(Integer value) {
                state.setValue(new State(true, enabled, false, errorVersion));
            }
            @Override public void onError(Exception error) {
                state.setValue(new State(true, enabled, false, ++errorVersion));
            }
        };
        if (enabled) garden.reconcileCurrentUserReminders(callback);
        else garden.cancelCurrentUserReminders(callback);
    }
}
