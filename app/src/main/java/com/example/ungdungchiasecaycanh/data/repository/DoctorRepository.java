package com.example.ungdungchiasecaycanh.data.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.local.dao.CareLogDao;
import com.example.ungdungchiasecaycanh.data.local.dao.DoctorHistoryDao;
import com.example.ungdungchiasecaycanh.data.local.dao.PlantDao;
import com.example.ungdungchiasecaycanh.data.local.db.PlantMateDatabase;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseDoctorFeedbackDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseImageDataSource;
import com.example.ungdungchiasecaycanh.data.remote.GeminiDoctorAnalyzer;
import com.example.ungdungchiasecaycanh.model.PlantDiagnosisResult;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DoctorRepository {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception error);
    }

    public static class HistoryDetail {
        public final DoctorHistoryEntity history;
        public final PlantEntity plant;
        HistoryDetail(DoctorHistoryEntity history, PlantEntity plant) {
            this.history = history;
            this.plant = plant;
        }
    }

    private final DoctorHistoryDao historyDao;
    private final PlantDao plantDao;
    private final CareLogDao careLogDao;
    private final Context context;
    private final FirebaseDoctorFeedbackDataSource feedback = new FirebaseDoctorFeedbackDataSource();
    private final FirebaseAuthDataSource auth;
    private final GeminiDoctorAnalyzer analyzer;
    private final ExecutorService executor;
    private final Handler mainHandler;

    public DoctorRepository(Context context) {
        PlantMateDatabase database = PlantMateDatabase.getInstance(context);
        this.historyDao = database.doctorHistoryDao();
        this.plantDao = database.plantDao();
        this.careLogDao = database.careLogDao();
        this.context = context.getApplicationContext();
        this.auth = new FirebaseAuthDataSource();
        this.analyzer = new GeminiDoctorAnalyzer(context);
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public boolean hasAuthenticatedUser() { return currentUid() != null; }

    public LiveData<List<PlantEntity>> observeCurrentUserPlants() {
        String uid = currentUid();
        return uid == null ? new MutableLiveData<>(Collections.emptyList())
                : plantDao.observeForOwner(uid);
    }

    public LiveData<List<DoctorHistoryEntity>> observeCurrentUserHistory() {
        String uid = currentUid();
        return uid == null ? new MutableLiveData<>(Collections.emptyList())
                : historyDao.observeHistoryForOwner(uid);
    }

    public void getHistoryDetail(long historyId, Callback<HistoryDetail> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        executor.execute(() -> {
            try {
                DoctorHistoryEntity history = historyDao.getHistoryByIdForOwner(historyId, uid);
                if (history == null) { postSuccess(callback, null); return; }
                PlantEntity plant = history.getPlantId() == null ? null
                        : plantDao.getByIdForOwner(history.getPlantId(), uid);
                postSuccess(callback, new HistoryDetail(history, plant));
            } catch (Exception error) { postError(callback, error); }
        });
    }

    /** {@code symptom}: what the owner said they noticed, or null. */
    public void analyze(String imageUri, Long plantId, String symptom, Callback<Long> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        final String normalizedImage;
        try { normalizedImage = normalizeImageUri(imageUri); }
        catch (Exception error) { callback.onError(error); return; }

        executor.execute(() -> {
            try {
                PlantEntity plant = null;
                List<CareLogEntity> recent = null;
                if (plantId != null) {
                    plant = plantDao.getByIdForOwner(plantId, uid);
                    validatePlantLink(plant, uid);
                    recent = careLogDao.getRecent(plantId, RECENT_CARE);
                }
                String records = describe(plant, recent, symptom, System.currentTimeMillis());
                analyzer.analyzePlantImage(normalizedImage, records, new GeminiDoctorAnalyzer.Callback() {
                    @Override public void onSuccess(PlantDiagnosisResult result) {
                        executor.execute(() -> persistResult(uid, plantId, normalizedImage, result, callback));
                    }
                    @Override public void onError(Exception error) { postError(callback, error); }
                });
            } catch (Exception error) { postError(callback, error); }
        });
    }

    /** How many recent care entries go into the prompt. */
    static final int RECENT_CARE = 6;

    /**
     * The owner's own records for the AI prompt: the plant, its watering plan, recent care and the
     * symptom they noticed. Empty when nothing is known.
     */
    public static String describe(PlantEntity plant, List<CareLogEntity> recentCare, String symptom, long now) {
        StringBuilder out = new StringBuilder();
        if (plant != null) {
            out.append("- Plant: ").append(plant.getNickname());
            if (!plant.getSpecies().isEmpty()) out.append(" (").append(plant.getSpecies()).append(')');
            out.append('\n');
            if (plant.isReminderEnabled()) {
                int perDay = GardenRepository.waterPerDay(plant);
                out.append("- Watering plan: ").append(plant.getWateringTimes().isEmpty()
                        ? "every " + plant.getWateringIntervalDays() + " days" : perDay + " times a day").append('\n');
            }
        }
        if (recentCare != null && !recentCare.isEmpty()) {
            out.append("- Recent care, newest first:");
            for (CareLogEntity log : recentCare) {
                long days = TimeUnit.MILLISECONDS.toDays(Math.max(0, now - log.getCreatedAt()));
                out.append("\n  * ").append(log.getType().name().toLowerCase(Locale.ROOT)).append(", ")
                        .append(days == 0 ? "today" : days + " days ago");
                String note = log.getNote().trim();
                if (!note.isEmpty()) out.append(": ").append(note.length() > 120 ? note.substring(0, 120) : note);
            }
            out.append('\n');
        }
        if (symptom != null && !symptom.trim().isEmpty()) {
            out.append("- Symptom the owner noticed: ").append(symptom.trim()).append('\n');
        }
        return out.toString();
    }

    /**
     * Sends "was this right?" for a saved diagnosis (and the photo, only when the member agreed)
     * so Plant Doctor can be measured and trained; then remembers that it was answered.
     */
    public void submitFeedback(long historyId, boolean correct, String correction, boolean sharePhoto,
                               Callback<Void> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onError(new IllegalStateException("Authentication required")); return; }
        executor.execute(() -> {
            try {
                DoctorHistoryEntity history = historyDao.getHistoryByIdForOwner(historyId, uid);
                if (history == null) throw new IllegalStateException("Diagnosis unavailable");
                FirebaseImageDataSource.Encoded photo = sharePhoto
                        ? FirebaseImageDataSource.encode(context, Uri.parse(history.getImageUri())) : null;
                Tasks.await(feedback.submit(uid, feedbackFields(history, correct, correction), photo));
                historyDao.setFeedback(historyId, uid, correct);
                postSuccess(callback, null);
            } catch (Exception error) { postError(callback, error); }
        });
    }

    /** The answer as stored in doctorFeedback (without owner, photo and time, which the data source adds). */
    public static Map<String, Object> feedbackFields(DoctorHistoryEntity history, boolean correct, String correction) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("correct", correct);
        fields.put("diagnosis", history.getDiagnosis());
        if (history.getPlantName() != null) fields.put("plantName", history.getPlantName());
        if (history.getScientificName() != null) fields.put("scientificName", history.getScientificName());
        if (history.getConfidence() != null) fields.put("confidence", history.getConfidence());
        if (history.getHealthy() != null) fields.put("healthy", history.getHealthy());
        String note = correct || correction == null ? "" : correction.trim();
        fields.put("correction", note.length() > 300 ? note.substring(0, 300) : note);
        return fields;
    }

    private void persistResult(String uid, Long plantId, String imageUri,
                               PlantDiagnosisResult result, Callback<Long> callback) {
        try {
            if (!uid.equals(currentUid())) throw new SecurityException("Account changed");
            if (plantId != null) validatePlantLink(plantDao.getByIdForOwner(plantId, uid), uid);
            PlantDiagnosisResult normalized = normalizeResult(result);
            DoctorHistoryEntity entity = createHistory(uid, plantId, imageUri,
                    normalized, System.currentTimeMillis());
            long id = historyDao.insertHistory(entity);
            mainHandler.post(() -> callback.onSuccess(id));
        } catch (Exception error) { postError(callback, error); }
    }

    public static PlantDiagnosisResult normalizeResult(PlantDiagnosisResult result) {
        if (result == null) throw new IllegalArgumentException("Diagnosis result is required");
        String diagnosis = require(result.getDiagnosis(), com.example.ungdungchiasecaycanh.data.remote.DoctorAiParser.MAX_DIAGNOSIS, "Diagnosis is required");
        String guidance = require(result.getGuidance(), 2000, "Guidance is required");
        Double confidence = result.getConfidence();
        if (confidence != null && (Double.isNaN(confidence) || confidence < 0d || confidence > 1d))
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        return new PlantDiagnosisResult(diagnosis, confidence, guidance,
                optional(result.getPlantName(), 100), optional(result.getScientificName(), 120),
                result.getHealthy());
    }

    public static DoctorHistoryEntity createHistory(String ownerUid, Long plantId,
                                                     String imageUri, PlantDiagnosisResult result,
                                                     long createdAt) {
        String owner = require(ownerUid, Integer.MAX_VALUE, "Owner is required");
        String image = normalizeImageUri(imageUri);
        PlantDiagnosisResult normalized = normalizeResult(result);
        return new DoctorHistoryEntity(0, owner, plantId, image,
                normalized.getDiagnosis(), normalized.getConfidence(),
                normalized.getGuidance(), createdAt, normalized.getPlantName(),
                normalized.getScientificName(), normalized.getHealthy());
    }

    public static void validatePlantLink(PlantEntity plant, String currentUid) {
        if (plant == null || currentUid == null || !currentUid.equals(plant.getOwnerUid()))
            throw new SecurityException("Plant unavailable");
    }

    public static String normalizeImageUri(String imageUri) {
        String normalized = require(imageUri, Integer.MAX_VALUE, "Image is required");
        if (!normalized.startsWith("content://"))
            throw new IllegalArgumentException("A local content image is required");
        return normalized;
    }

    /** Trimmed optional text, cut to {@code max} characters; blank becomes null. */
    private static String optional(String value, int max) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return null;
        return normalized.length() > max ? normalized.substring(0, max).trim() : normalized;
    }

    private static String require(String value, int max, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > max) throw new IllegalArgumentException(message);
        return normalized;
    }

    private String currentUid() {
        String uid = auth.getCurrentUserId();
        return uid == null || uid.trim().isEmpty() ? null : uid;
    }

    private <T> void postSuccess(Callback<T> callback, T value) {
        mainHandler.post(() -> callback.onSuccess(value));
    }
    private <T> void postError(Callback<T> callback, Exception error) {
        mainHandler.post(() -> callback.onError(error));
    }
}
