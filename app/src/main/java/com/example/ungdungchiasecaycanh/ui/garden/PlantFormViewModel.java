package com.example.ungdungchiasecaycanh.ui.garden;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;

import java.util.ArrayList;
import java.util.List;

public class PlantFormViewModel extends AndroidViewModel {
    private final GardenRepository repository;
    private final MutableLiveData<PlantEntity> plant = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleted = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> deleteFailed = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final LiveData<List<PlantEntity>> plants;

    public PlantFormViewModel(@NonNull Application application) {
        super(application);
        repository = new GardenRepository(application);
        plants = repository.observeCurrentUserPlants();
    }

    /** The member's plants, for suggesting areas they already use. */
    public LiveData<List<PlantEntity>> getPlants() { return plants; }

    public LiveData<PlantEntity> getPlant() { return plant; }
    public LiveData<Boolean> getBusy() { return busy; }
    public LiveData<Boolean> getSaved() { return saved; }
    public LiveData<Boolean> getDeleted() { return deleted; }
    public LiveData<Boolean> getDeleteFailed() { return deleteFailed; }

    public void delete(long plantId) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        repository.deletePlant(plantId, new GardenRepository.Callback<Void>() {
            @Override public void onSuccess(Void value) { busy.setValue(false); deleted.setValue(true); }
            @Override public void onError(Exception cause) { busy.setValue(false); deleteFailed.setValue(true); }
        });
    }

    public void deleteErrorShown() { deleteFailed.setValue(false); }
    public LiveData<String> getError() { return error; }

    /** Daily reminder times being edited, minutes after midnight (kept across rotation). */
    final List<Integer> times = new ArrayList<>();

    public void load(long plantId) {
        busy.setValue(true);
        repository.getPlant(plantId, new GardenRepository.Callback<PlantEntity>() {
            @Override public void onSuccess(PlantEntity value) {
                busy.setValue(false);
                if (value == null) error.setValue("Plant unavailable"); else plant.setValue(value);
            }
            @Override public void onError(Exception cause) {
                busy.setValue(false); error.setValue("Unable to load plant");
            }
        });
    }

    public void add(String nickname, String species, String imageUri,
                    boolean reminderEnabled, int intervalDays, String wateringTimes, String room, String light) {
        busy.setValue(true);
        repository.addPlant(nickname, species, imageUri, reminderEnabled, intervalDays, wateringTimes, room, light,
                new GardenRepository.Callback<Long>() {
                    @Override public void onSuccess(Long value) { busy.setValue(false); saved.setValue(true); }
                    @Override public void onError(Exception cause) { busy.setValue(false); error.setValue(cause.getMessage()); }
                });
    }

    public void update(long plantId, String nickname, String species, String imageUri,
                       boolean reminderEnabled, int intervalDays, String wateringTimes, String room, String light) {
        busy.setValue(true);
        repository.updatePlant(plantId, nickname, species, imageUri, reminderEnabled, intervalDays, wateringTimes,
                room, light,
                new GardenRepository.Callback<PlantEntity>() {
                    @Override public void onSuccess(PlantEntity value) {
                        busy.setValue(false); plant.setValue(value); saved.setValue(true);
                    }
                    @Override public void onError(Exception cause) { busy.setValue(false); error.setValue(cause.getMessage()); }
                });
    }
}
