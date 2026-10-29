package com.example.ungdungchiasecaycanh.data.repository;

import android.content.Context;
import android.net.Uri;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseImageDataSource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Uploads member photos (compressed on a background thread) and returns their image refs. */
public class ImageRepository {
    private static final Executor ENCODER = Executors.newSingleThreadExecutor();

    private final Context context;
    private final FirebaseImageDataSource data = new FirebaseImageDataSource();
    private final FirebaseAuthDataSource auth = new FirebaseAuthDataSource();

    public ImageRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    /** One photo → {@code pmimg://{id}}. */
    public Task<String> upload(String localUri) {
        return upload(localUri, false);
    }

    /** A story photo: only the owner, their friends and admins can read it. */
    public Task<String> uploadForFriends(String localUri) {
        return upload(localUri, true);
    }

    private Task<String> upload(String localUri, boolean friendsOnly) {
        String uid = auth.getCurrentUserId();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        return Tasks.call(ENCODER, () -> FirebaseImageDataSource.encode(context, Uri.parse(localUri)))
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return data.save(uid, task.getResult(), friendsOnly);
                });
    }

    /** Several photos, uploaded one after another in order; fails if any one fails. */
    public Task<List<String>> uploadAll(List<String> localUris) {
        List<String> refs = new ArrayList<>();
        Task<Void> chain = Tasks.forResult(null);
        for (String uri : localUris) {
            chain = chain.continueWithTask(previous -> {
                if (!previous.isSuccessful()) throw previous.getException();
                return upload(uri).continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    refs.add(task.getResult());
                    return null;
                });
            });
        }
        return chain.continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return refs;
        });
    }
}
