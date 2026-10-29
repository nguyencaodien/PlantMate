package com.example.ungdungchiasecaycanh.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.ungdungchiasecaycanh.data.local.dao.ListingDraftDao;
import com.example.ungdungchiasecaycanh.data.local.db.PlantMateDatabase;
import com.example.ungdungchiasecaycanh.data.local.entity.ListingDraftEntity;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The signed-in member's saved listing draft in Room (never another account's). */
public class ListingDraftRepository {
    private final ListingDraftDao dao;
    private final FirebaseAuthDataSource auth = new FirebaseAuthDataSource();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public ListingDraftRepository(Context context) {
        dao = PlantMateDatabase.getInstance(context).listingDraftDao();
    }

    public String currentUid() {
        String uid = auth.getCurrentUserId();
        return uid == null || uid.trim().isEmpty() ? null : uid;
    }

    /** The draft, or null when there is none (or nobody is signed in). */
    public void load(GardenRepository.Callback<ListingDraftEntity> callback) {
        String uid = currentUid();
        if (uid == null) { callback.onSuccess(null); return; }
        executor.execute(() -> {
            try {
                ListingDraftEntity draft = dao.getForOwner(uid);
                main.post(() -> callback.onSuccess(draft));
            } catch (Exception error) { main.post(() -> callback.onError(error)); }
        });
    }

    /** Saves the draft for its owner; refused for any account but the signed-in one. */
    public void save(ListingDraftEntity draft, GardenRepository.Callback<Void> callback) {
        String uid = currentUid();
        if (uid == null || draft == null || !uid.equals(draft.getOwnerUid())) {
            callback.onError(new IllegalStateException("Authentication required"));
            return;
        }
        executor.execute(() -> {
            try {
                dao.save(draft);
                main.post(() -> callback.onSuccess(null));
            } catch (Exception error) { main.post(() -> callback.onError(error)); }
        });
    }

    public void clear() { clear(null); }

    /** Deletes the draft; {@code done} runs on the main thread afterwards. */
    public void clear(Runnable done) {
        String uid = currentUid();
        if (uid == null) { if (done != null) done.run(); return; }
        executor.execute(() -> {
            try { dao.deleteForOwner(uid); } catch (Exception ignored) { /* nothing to discard */ }
            if (done != null) main.post(done);
        });
    }
}
