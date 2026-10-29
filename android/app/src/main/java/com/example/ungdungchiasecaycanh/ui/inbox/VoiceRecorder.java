package com.example.ungdungchiasecaycanh.ui.inbox;

import android.content.Context;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.File;
import java.util.Locale;

/**
 * Records one voice note as AAC in an MP4 container (about 32 kbps mono, so 60 s is roughly
 * 240 KB). Recording stops by itself at {@link #MAX_MS}.
 */
public final class VoiceRecorder {
    public static final long MAX_MS = 60_000;
    public static final long MIN_MS = 500;

    private final Context context;
    private MediaRecorder recorder;
    private File file;
    private long startedAt;
    private volatile boolean autoStopped;

    public VoiceRecorder(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isRecording() { return recorder != null; }

    public long elapsedMs() { return recorder == null ? 0 : SystemClock.elapsedRealtime() - startedAt; }

    /** Starts recording; {@code onMaxReached} runs on the main thread when 60 s are reached. */
    public void start(Runnable onMaxReached) throws Exception {
        stopQuietly();
        file = new File(context.getCacheDir(), "voice_" + System.currentTimeMillis() + ".m4a");
        MediaRecorder r = Build.VERSION.SDK_INT >= 31 ? new MediaRecorder(context) : new MediaRecorder();
        r.setAudioSource(MediaRecorder.AudioSource.MIC);
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        r.setAudioChannels(1);
        r.setAudioSamplingRate(22_050);
        r.setAudioEncodingBitRate(32_000);
        // The screen stops the note at MAX_MS; this is only a safety net slightly above it.
        r.setMaxDuration((int) MAX_MS + 1_000);
        autoStopped = false;
        r.setOutputFile(file.getAbsolutePath());
        r.setOnInfoListener((mr, what, extra) -> {
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED && onMaxReached != null) {
                autoStopped = true;
                new Handler(Looper.getMainLooper()).post(onMaxReached);
            }
        });
        r.prepare();
        r.start();
        recorder = r;
        startedAt = SystemClock.elapsedRealtime();
    }

    /** Stops and returns the file, or null when the note is too short or recording failed. */
    public Result stop() {
        if (recorder == null) return null;
        long duration = Math.min(MAX_MS, elapsedMs());
        boolean ok = true;
        try {
            recorder.stop();
        } catch (RuntimeException error) {
            // After the max-duration safety net the recorder has already finalised the file.
            ok = autoStopped && file != null && file.length() > 0;
        }
        recorder.release();
        recorder = null;
        File out = file;
        file = null;
        if (!ok || duration < MIN_MS) {
            if (out != null) out.delete();
            return null;
        }
        return new Result(out, duration);
    }

    /** Cancels a recording and deletes its file. */
    public void stopQuietly() {
        if (recorder != null) {
            try { recorder.stop(); } catch (RuntimeException ignored) { }
            recorder.release();
            recorder = null;
        }
        if (file != null) file.delete();
        file = null;
    }

    public static final class Result {
        public final File file;
        public final long durationMs;
        Result(File file, long durationMs) { this.file = file; this.durationMs = durationMs; }
    }

    /** "0:07" style duration label. */
    public static String format(long ms) {
        long seconds = Math.max(0, Math.round(ms / 1000.0));
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
