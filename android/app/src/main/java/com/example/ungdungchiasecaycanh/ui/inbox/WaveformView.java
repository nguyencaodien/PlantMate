package com.example.ungdungchiasecaycanh.ui.inbox;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Voice-note waveform: rounded bars whose heights come from a seed (the message id), so the
 * same note always looks the same. Bars before the playback position use the played colour.
 */
public class WaveformView extends View {
    private static final int BARS = 26;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float[] heights = new float[BARS];
    private final float dp;
    private int playedColor = 0xFFFFFFFF, idleColor = 0x80FFFFFF;
    private float progress;

    public WaveformView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        setSeed("");
    }

    public void setSeed(String seed) {
        long h = seed == null ? 7 : seed.hashCode();
        for (int i = 0; i < BARS; i++) {
            h = h * 6364136223846793005L + 1442695040888963407L;
            float r = ((h >>> 33) % 1000) / 1000f;
            float envelope = 0.45f + 0.55f * (float) Math.sin(Math.PI * (i + 0.5) / BARS);
            heights[i] = Math.max(0.18f, Math.min(1f, (0.25f + 0.75f * r) * envelope));
        }
        invalidate();
    }

    public void setColors(int played, int idle) {
        playedColor = played;
        idleColor = idle;
        invalidate();
    }

    public void setProgress(float value) {
        progress = Math.max(0f, Math.min(1f, value));
        invalidate();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(resolveSize((int) (132 * dp), widthMeasureSpec), resolveSize((int) (26 * dp), heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float slot = w / BARS, bar = Math.max(2 * dp, slot * 0.55f);
        for (int i = 0; i < BARS; i++) {
            float bh = h * heights[i];
            float left = i * slot + (slot - bar) / 2f;
            rect.set(left, (h - bh) / 2f, left + bar, (h + bh) / 2f);
            paint.setColor((i + 0.5f) / BARS <= progress ? playedColor : idleColor);
            canvas.drawRoundRect(rect, bar / 2f, bar / 2f, paint);
        }
    }
}
