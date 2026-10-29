package com.example.ungdungchiasecaycanh.ui.inbox;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import com.example.ungdungchiasecaycanh.data.repository.ChatRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Plays one voice note at a time. Audio is downloaded once from chatMedia into the app cache
 * (cacheDir/voice/<id>.m4a) and reused. Listeners are told which message is playing and the
 * progress so the bubble can update its play icon and waveform.
 */
public final class VoicePlayer {
    public interface Listener {
        /** {@code ref} null when nothing plays; progress 0..1. */
        void onPlayback(String ref, float progress, boolean loading);
        void onError();
    }

    private static final Executor IO = Executors.newSingleThreadExecutor();
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Listener listener;
    private MediaPlayer player;
    private String current;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (player == null) return;
            int duration = Math.max(1, player.getDuration());
            listener.onPlayback(current, player.getCurrentPosition() / (float) duration, false);
            main.postDelayed(this, 100);
        }
    };

    public VoicePlayer(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    /** Plays {@code ref}, or stops when it is the one already playing. */
    public void toggle(String ref) {
        if (ref == null) return;
        if (ref.equals(current)) { stop(); return; }
        stop();
        current = ref;
        listener.onPlayback(ref, 0f, true);
        IO.execute(() -> {
            File file = cached(ref);
            main.post(() -> {
                if (!ref.equals(current)) return;
                if (file == null) { current = null; listener.onError(); listener.onPlayback(null, 0f, false); return; }
                try {
                    MediaPlayer p = new MediaPlayer();
                    // Speech on the media stream, so the media volume controls it.
                    p.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build());
                    p.setDataSource(file.getAbsolutePath());
                    p.setOnCompletionListener(mp -> stop());
                    p.prepare();
                    p.start();
                    player = p;
                    main.post(tick);
                } catch (Exception error) {
                    current = null;
                    listener.onError();
                    listener.onPlayback(null, 0f, false);
                }
            });
        });
    }

    public void stop() {
        main.removeCallbacks(tick);
        if (player != null) {
            try { player.stop(); } catch (RuntimeException ignored) { }
            player.release();
            player = null;
        }
        current = null;
        listener.onPlayback(null, 0f, false);
    }

    private File cached(String ref) {
        String id = ChatRepository.mediaIdOf(ref);
        if (id == null) return null;
        File dir = new File(context.getCacheDir(), "voice");
        if (!dir.exists() && !dir.mkdirs()) return null;
        File file = new File(dir, id + ".m4a");
        if (file.isFile() && file.length() > 0) return file;
        try {
            byte[] bytes = ChatRepository.loadMediaBlocking(ref);
            if (bytes == null) return null;
            try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); }
            return file;
        } catch (Exception error) {
            file.delete();
            return null;
        }
    }
}
