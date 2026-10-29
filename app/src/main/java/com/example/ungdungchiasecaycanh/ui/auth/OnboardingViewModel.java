package com.example.ungdungchiasecaycanh.ui.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.User;

import java.util.LinkedHashSet;
import java.util.Set;

/** First run: area, what the member wants to do, care reminders. Step and choices survive rotation. */
public class OnboardingViewModel extends AndroidViewModel {
    public static final int STEPS = 3;

    private final UserRepository users = new UserRepository();
    private final SettingsRepository settings;
    private final MutableLiveData<Integer> step = new MutableLiveData<>(0);
    private final MutableLiveData<String> area = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveFailed = new MutableLiveData<>(false);
    private final Set<String> intents = new LinkedHashSet<>();
    private User profile;

    public OnboardingViewModel(@NonNull Application application) {
        super(application);
        settings = new SettingsRepository(application);
        users.getCurrentProfile().addOnSuccessListener(user -> {
            profile = user;
            if (user != null && area.getValue() == null) area.setValue(user.getLocationText());
        });
    }

    public LiveData<Integer> getStep() { return step; }
    /** The area from the member's profile, to prefill step 1 (null until loaded). */
    public LiveData<String> getArea() { return area; }
    public LiveData<Boolean> getSaveFailed() { return saveFailed; }
    public Set<String> getIntents() { return intents; }

    public void toggleIntent(String intent, boolean on) {
        if (on) intents.add(intent); else intents.remove(intent);
    }

    /** Step 1 → 2: keeps the area on the profile when it changed; the next step never waits on it. */
    public void saveArea(String value) {
        String next = value == null ? "" : value.trim();
        area.setValue(next);
        if (profile != null && !next.isEmpty() && !next.equals(profile.getLocationText())) {
            users.updateCurrentUserProfile(profile.getDisplayName(), next, profile.getBio())
                    .addOnFailureListener(e -> saveFailed.setValue(true));
        }
        step.setValue(1);
    }

    public void next() {
        Integer current = step.getValue();
        step.setValue(Math.min(STEPS - 1, (current == null ? 0 : current) + 1));
    }

    public void back() {
        Integer current = step.getValue();
        step.setValue(Math.max(0, (current == null ? 0 : current) - 1));
    }

    /** Done (or skipped): never shown again for this account; Home reads the choices. */
    public void finish(boolean remindersOn) {
        settings.completeOnboarding(intents);
        if (!remindersOn) settings.setCareNotificationsEnabled(false);
    }
}
