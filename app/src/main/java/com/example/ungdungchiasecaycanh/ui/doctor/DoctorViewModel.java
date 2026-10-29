package com.example.ungdungchiasecaycanh.ui.doctor;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.remote.DoctorAiException;
import com.example.ungdungchiasecaycanh.data.repository.DoctorRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.ui.ai.MamAdvisor;
import com.example.ungdungchiasecaycanh.util.PhotoQuality;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DoctorViewModel extends AndroidViewModel {
    /** Treatment steps ticked on the result screen (by position); kept across rotation. */
    final java.util.Set<Integer> doneSteps = new java.util.HashSet<>();
    public enum AnalysisStatus { IDLE, READY, ANALYZING, RESULT, ERROR, QUALITY_WARNING }
    public enum FeedbackStatus { IDLE, SENDING, SENT, ERROR }

    public static class AnalysisState {
        public final AnalysisStatus status;
        public final String imageUri;
        public final Long plantId;
        public final Long historyId;
        /** Why the last analysis failed (ERROR only); null for unexpected errors. */
        public final DoctorAiException.Kind errorKind;
        /** QUALITY_WARNING only: why the photo may give a poor answer. */
        public final PhotoQuality.Result quality;
        AnalysisState(AnalysisStatus status, String imageUri, Long plantId, Long historyId) {
            this(status, imageUri, plantId, historyId, null);
        }
        AnalysisState(AnalysisStatus status, String imageUri, Long plantId, Long historyId,
                      DoctorAiException.Kind errorKind) {
            this(status, imageUri, plantId, historyId, errorKind, null);
        }
        AnalysisState(AnalysisStatus status, String imageUri, Long plantId, Long historyId,
                      DoctorAiException.Kind errorKind, PhotoQuality.Result quality) {
            this.status = status; this.imageUri = imageUri; this.plantId = plantId; this.historyId = historyId;
            this.errorKind = errorKind;
            this.quality = quality;
        }
    }

    private final DoctorRepository repository;
    private final LiveData<List<PlantEntity>> plants;
    private final LiveData<List<DoctorHistoryEntity>> history;
    private final MutableLiveData<AnalysisState> analysis = new MutableLiveData<>(
            new AnalysisState(AnalysisStatus.IDLE, null, null, null));
    private final MutableLiveData<DoctorRepository.HistoryDetail> detail = new MutableLiveData<>();
    private final MutableLiveData<Boolean> detailLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> detailMissing = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> detailError = new MutableLiveData<>(false);
    private long requestedHistoryId = -1;

    public DoctorViewModel(@NonNull Application application) {
        super(application);
        repository = new DoctorRepository(application);
        plants = repository.observeCurrentUserPlants();
        history = repository.observeCurrentUserHistory();
    }

    private final MutableLiveData<MamAdvisor.Symptom> symptom =
            new MutableLiveData<>(MamAdvisor.Symptom.DROOPING);

    /** Symptom picked in the guidance grid; survives rotation. */
    public LiveData<MamAdvisor.Symptom> getSymptom() { return symptom; }
    public void selectSymptom(MamAdvisor.Symptom value) {
        symptom.setValue(value);
        symptomChosen = true;
    }

    /** The grid starts on a default symptom; only one the member tapped is told to the AI. */
    private boolean symptomChosen;
    private static final Executor QUALITY = Executors.newSingleThreadExecutor();
    private final MutableLiveData<FeedbackStatus> feedback = new MutableLiveData<>(FeedbackStatus.IDLE);

    public LiveData<FeedbackStatus> getFeedback() { return feedback; }

    public boolean hasAuthenticatedUser() { return repository.hasAuthenticatedUser(); }
    public LiveData<List<PlantEntity>> getPlants() { return plants; }
    public LiveData<List<DoctorHistoryEntity>> getHistory() { return history; }
    public LiveData<AnalysisState> getAnalysis() { return analysis; }
    public LiveData<DoctorRepository.HistoryDetail> getDetail() { return detail; }

    private final MutableLiveData<List<Listing>> givers = new MutableLiveData<>();
    private final ListingRepository listings = new ListingRepository();
    private String giversFor;

    /** Open give-away and trade listings of the diagnosed plant (by name or scientific name). */
    public LiveData<List<Listing>> getGivers() { return givers; }

    public void loadGivers(String name, String scientific) {
        String key = name + "|" + scientific;
        if (key.equals(giversFor)) return;
        giversFor = key;
        listings.getActiveListings().addOnSuccessListener(all -> givers.postValue(matching(all, name, scientific)));
    }

    static List<Listing> matching(List<Listing> all, String name, String scientific) {
        List<Listing> out = new ArrayList<>();
        String n = fold(name), sc = fold(scientific);
        if (all == null || (n.isEmpty() && sc.isEmpty())) return out;
        for (Listing l : all) {
            if (l == null || l.getType() == ListingType.SELL || l.getStatus() != ListingStatus.ACTIVE) continue;
            String nick = fold(l.getNickname()), species = fold(l.getSpecies());
            boolean hit = (!n.isEmpty() && (nick.contains(n) || n.contains(nick) && !nick.isEmpty()))
                    || (!sc.isEmpty() && !species.isEmpty() && (species.contains(sc) || sc.contains(species)));
            if (hit) out.add(l);
        }
        return out;
    }

    /** Lower case, no accents, single spaces ("Cây Lưỡi Hổ" → "cay luoi ho"). */
    static String fold(String text) {
        if (text == null) return "";
        String lower = text.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replaceAll("\\s+", " ")
                .replaceFirst("^cay ", "");
    }
    public LiveData<Boolean> getDetailLoading() { return detailLoading; }
    public LiveData<Boolean> getDetailMissing() { return detailMissing; }
    public LiveData<Boolean> getDetailError() { return detailError; }

    public void selectImage(String uri) {
        AnalysisState current = analysis.getValue();
        Long plantId = current == null ? null : current.plantId;
        if (uri == null || uri.trim().isEmpty())
            analysis.setValue(new AnalysisState(AnalysisStatus.IDLE, null, plantId, null));
        else analysis.setValue(new AnalysisState(AnalysisStatus.READY, uri, plantId, null));
    }

    public void selectPlant(Long plantId) {
        AnalysisState current = analysis.getValue();
        String image = current == null ? null : current.imageUri;
        AnalysisStatus status = image == null ? AnalysisStatus.IDLE : AnalysisStatus.READY;
        analysis.setValue(new AnalysisState(status, image, plantId, null));
    }

    public void analyze() {
        analyze(false);
    }

    /** Checks the photo first: a dark or blurry one asks before an AI request is spent on it. */
    public void analyze(boolean skipQualityCheck) {
        AnalysisState current = analysis.getValue();
        if (current == null || current.imageUri == null || current.status == AnalysisStatus.ANALYZING) return;
        analysis.setValue(new AnalysisState(AnalysisStatus.ANALYZING,
                current.imageUri, current.plantId, null));
        if (skipQualityCheck) {
            send(current);
            return;
        }
        QUALITY.execute(() -> {
            PhotoQuality.Result quality = PhotoQuality.check(
                    getApplication().getContentResolver(), Uri.parse(current.imageUri));
            if (quality == PhotoQuality.Result.OK) send(current);
            else analysis.postValue(new AnalysisState(AnalysisStatus.QUALITY_WARNING,
                    current.imageUri, current.plantId, null, null, quality));
        });
    }

    /** The warning dialog was shown; the form is ready again (Retake or Analyse anyway follows). */
    public void qualityWarningShown() {
        AnalysisState current = analysis.getValue();
        if (current != null && current.status == AnalysisStatus.QUALITY_WARNING) {
            analysis.setValue(new AnalysisState(AnalysisStatus.READY, current.imageUri, current.plantId, null));
        }
    }

    private void send(AnalysisState current) {
        String told = symptomChosen && symptom.getValue() != null ? MamAdvisor.symptomLabel(symptom.getValue()) : null;
        repository.analyze(current.imageUri, current.plantId, told, new DoctorRepository.Callback<Long>() {
            @Override public void onSuccess(Long historyId) {
                analysis.setValue(new AnalysisState(AnalysisStatus.RESULT,
                        current.imageUri, current.plantId, historyId));
            }
            @Override public void onError(Exception error) {
                analysis.setValue(new AnalysisState(AnalysisStatus.ERROR, current.imageUri, current.plantId, null,
                        error instanceof DoctorAiException
                                ? ((DoctorAiException) error).getKind()
                                : null));
            }
        });
    }

    /** After the result screen opened, the form returns to READY so Back does not reopen it. */
    public void resultShown() {
        AnalysisState current = analysis.getValue();
        if (current != null && current.status == AnalysisStatus.RESULT) {
            analysis.setValue(new AnalysisState(AnalysisStatus.READY, current.imageUri, current.plantId, null));
        }
    }

    public void loadHistory(long historyId) {
        requestedHistoryId = historyId;
        detailLoading.setValue(true); detailMissing.setValue(false); detailError.setValue(false);
        repository.getHistoryDetail(historyId, new DoctorRepository.Callback<DoctorRepository.HistoryDetail>() {
            @Override public void onSuccess(DoctorRepository.HistoryDetail value) {
                detailLoading.setValue(false);
                if (value == null) detailMissing.setValue(true); else detail.setValue(value);
            }
            @Override public void onError(Exception error) {
                detailLoading.setValue(false); detailError.setValue(true);
            }
        });
    }

    public void retryHistory() { if (requestedHistoryId > 0) loadHistory(requestedHistoryId); }

    /** "Was this right?" for the diagnosis on screen. */
    public void submitFeedback(boolean correct, String correction, boolean sharePhoto) {
        if (requestedHistoryId <= 0 || feedback.getValue() == FeedbackStatus.SENDING) return;
        feedback.setValue(FeedbackStatus.SENDING);
        repository.submitFeedback(requestedHistoryId, correct, correction, sharePhoto,
                new DoctorRepository.Callback<Void>() {
                    @Override public void onSuccess(Void value) { feedback.setValue(FeedbackStatus.SENT); }
                    @Override public void onError(Exception error) { feedback.setValue(FeedbackStatus.ERROR); }
                });
    }
}
