package com.example.ungdungchiasecaycanh.ui.garden;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;

import java.util.List;

public class GardenViewModel extends AndroidViewModel {
    /** Care history read for the week view and the streak. */
    static final int HISTORY_DAYS = 60;
    private final GardenRepository repository;
    private final LiveData<List<PlantEntity>> plants;
    private final LiveData<List<CareLogEntity>> care;
    private final MutableLiveData<Integer> waterErrors = new MutableLiveData<>(0);
    /** Day of the week shown on the panel (Monday = 0); -1 follows today. Kept here so rotation keeps it. */
    int selectedDay = -1;
    private String lastWaterError = "";
    /** Shelf filters: the area tapped (null = all) and the search text. */
    String areaFilter;
    String query = "";

    public GardenViewModel(@NonNull Application application) {
        super(application);
        repository = new GardenRepository(application);
        plants = repository.observeCurrentUserPlants();
        care = repository.observeCurrentUserCareSince(GardenRepository.addCalendarDays(
                GardenOverview.startOfDay(System.currentTimeMillis()), -HISTORY_DAYS));
    }

    public LiveData<List<PlantEntity>> getPlants() { return plants; }
    public LiveData<List<CareLogEntity>> getCare() { return care; }
    public LiveData<Integer> getWaterErrors() { return waterErrors; }
    /** Text for the latest failed watering (e.g. today's waterings are already logged). */
    public String lastWaterError() { return lastWaterError; }
    public boolean hasAuthenticatedUser() { return repository.hasAuthenticatedUser(); }
    public void reconcileReminders() { repository.reconcileCurrentUserReminders(null); }

    /** Logs a WATER care entry; the repository reschedules the reminder and Room refreshes the views. */
    public void water(long plantId) {
        repository.recordCare(plantId, CareType.WATER, "", new GardenRepository.Callback<CareLogEntity>() {
            @Override public void onSuccess(CareLogEntity value) { }
            @Override public void onError(Exception error) {
                lastWaterError = WateringStatus.careError(getApplication(), error);
                Integer current = waterErrors.getValue();
                waterErrors.postValue(current == null ? 1 : current + 1);
            }
        });
    }
}
