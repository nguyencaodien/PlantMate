package com.example.ungdungchiasecaycanh.ui.garden;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantPhotoEntity;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;

import java.util.List;

public class PlantDetailViewModel extends AndroidViewModel {
    private final GardenRepository repository;
    private final MediatorLiveData<PlantEntity> plant = new MediatorLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> unavailable = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> careSubmitting = new MutableLiveData<>(false);
    private final MutableLiveData<String> careError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> careSaved = new MutableLiveData<>(false);
    private final MediatorLiveData<List<CareLogEntity>> careLogs = new MediatorLiveData<>();
    private LiveData<PlantEntity> observedPlant;
    private LiveData<List<CareLogEntity>> observedLogs;
    private long plantId;
    private final MediatorLiveData<List<PlantPhotoEntity>> photos = new MediatorLiveData<>();
    private final MediatorLiveData<Boolean> health = new MediatorLiveData<>();
    private final MediatorLiveData<DoctorHistoryEntity> latestCheck = new MediatorLiveData<>();
    private LiveData<List<PlantPhotoEntity>> observedPhotos;
    private LiveData<Boolean> observedHealth;
    private final MutableLiveData<Integer> photoErrors = new MutableLiveData<>(0);

    public PlantDetailViewModel(@NonNull Application application) {
        super(application);
        repository = new GardenRepository(application);
    }

    public LiveData<PlantEntity> getPlant() { return plant; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getUnavailable() { return unavailable; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getCareSubmitting() { return careSubmitting; }
    public LiveData<String> getCareError() { return careError; }
    public LiveData<Boolean> getCareSaved() { return careSaved; }
    public LiveData<List<CareLogEntity>> getCareLogs() { return careLogs; }
    public LiveData<List<PlantPhotoEntity>> getPhotos() { return photos; }
    /** Latest Plant Doctor verdict (true = healthy), or null when there is none. */
    public LiveData<Boolean> getHealth() { return health; }
    /** Latest Plant Doctor check of this plant, or null. */
    public LiveData<DoctorHistoryEntity> getLatestCheck() { return latestCheck; }
    public LiveData<Integer> getPhotoErrors() { return photoErrors; }

    public void addPhoto(String uri) {
        repository.addPlantPhoto(plantId, uri, new GardenRepository.Callback<Long>() {
            @Override public void onSuccess(Long value) { }
            @Override public void onError(Exception cause) {
                Integer n = photoErrors.getValue();
                photoErrors.setValue(n == null ? 1 : n + 1);
            }
        });
    }

    public void load(long id) {
        plantId = id;
        loading.setValue(true); unavailable.setValue(false); error.setValue(null);
        repository.getPlant(id, new GardenRepository.Callback<PlantEntity>() {
            @Override public void onSuccess(PlantEntity value) {
                loading.setValue(false);
                if (value == null) unavailable.setValue(true); else {
                    plant.setValue(value);
                    observeOwnedPlant(id);
                    observeHistory(id);
                    observeExtras(id);
                }
            }
            @Override public void onError(Exception cause) {
                loading.setValue(false); error.setValue("Unable to load plant");
            }
        });
    }

    public void retry() { if (plantId > 0) load(plantId); }

    public void recordCare(CareType type, String note) {
        if (Boolean.TRUE.equals(careSubmitting.getValue())) return;
        careSubmitting.setValue(true);
        careError.setValue(null);
        careSaved.setValue(false);
        repository.recordCare(plantId, type, note, new GardenRepository.Callback<CareLogEntity>() {
            @Override public void onSuccess(CareLogEntity value) {
                careSubmitting.setValue(false);
                careSaved.setValue(true);
            }
            @Override public void onError(Exception cause) {
                careSubmitting.setValue(false);
                careError.setValue(WateringStatus.careError(getApplication(), cause));
            }
        });
    }

    private void observeOwnedPlant(long id) {
        if (observedPlant != null) return;
        observedPlant = repository.observePlant(id);
        plant.addSource(observedPlant, value -> {
            if (value == null) unavailable.setValue(true); else plant.setValue(value);
        });
    }

    private void observeExtras(long id) {
        if (observedPhotos != null) return;
        observedPhotos = repository.observePlantPhotos(id);
        photos.addSource(observedPhotos, photos::setValue);
        observedHealth = repository.observeLatestHealth(id);
        health.addSource(observedHealth, health::setValue);
        latestCheck.addSource(repository.observeLatestCheck(id), latestCheck::setValue);
    }

    private void observeHistory(long id) {
        repository.observeCareLogsForOwnedPlant(id,
                new GardenRepository.Callback<LiveData<List<CareLogEntity>>>() {
            @Override public void onSuccess(LiveData<List<CareLogEntity>> value) {
                if (observedLogs != null) careLogs.removeSource(observedLogs);
                observedLogs = value;
                careLogs.addSource(value, careLogs::setValue);
            }
            @Override public void onError(Exception cause) {
                error.setValue("Unable to load care history");
            }
        });
    }
}
