package com.example.ungdungchiasecaycanh.ui.admin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ungdungchiasecaycanh.R;

/** Donut with the total in the middle; segments in the given colours, a thin gap between them. */
public class DonutChartView extends View {
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private int[] values = new int[0];
    private int[] colors = new int[0];
    private String center = "", caption = "";
    /** Half ring (a gauge) with the total under its middle, as on the admin overview. */
    private boolean half;

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float dp = getResources().getDisplayMetrics().density;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(12 * dp);
        big.setTextAlign(Paint.Align.CENTER);
        big.setTextSize(22 * dp);
        big.setFakeBoldText(true);
        big.setColor(ContextCompat.getColor(context, R.color.color_text_primary));
        small.setTextAlign(Paint.Align.CENTER);
        small.setTextSize(11 * dp);
        small.setColor(ContextCompat.getColor(context, R.color.color_text_secondary));
    }

    public void setData(int[] values, int[] colors, String center, String caption) {
        this.values = values;
        this.colors = colors;
        this.center = center;
        this.caption = caption;
        invalidate();
    }

    public void setHalf(boolean half) {
        this.half = half;
        ring.setStrokeWidth((half ? 20 : 12) * getResources().getDisplayMetrics().density);
        big.setTextSize((half ? 26 : 22) * getResources().getDisplayMetrics().density);
        requestLayout();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        if (half) {
            int w = Math.min(MeasureSpec.getSize(widthSpec), Math.round(190 * getResources().getDisplayMetrics().density));
            setMeasuredDimension(w, Math.round(w * 0.58f));
            return;
        }
        int size = Math.min(MeasureSpec.getSize(widthSpec), Math.round(120 * getResources().getDisplayMetrics().density));
        setMeasuredDimension(size, size);
    }

    @Override protected void onDraw(Canvas canvas) {
        if (half) {
            drawHalf(canvas);
            return;
        }
        float stroke = ring.getStrokeWidth();
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f;
        oval.set(left + stroke / 2, stroke / 2, left + size - stroke / 2, size - stroke / 2);
        int total = 0;
        for (int v : values) total += v;
        if (total == 0) {
            ring.setColor(ContextCompat.getColor(getContext(), R.color.color_panel_line));
            canvas.drawArc(oval, 0, 360, false, ring);
        } else {
            float gap = values.length > 1 ? 3f : 0f;
            float start = -90;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                float sweep = 360f * values[i] / total;
                ring.setColor(colors[i % colors.length]);
                canvas.drawArc(oval, start + gap / 2, Math.max(1f, sweep - gap), false, ring);
                start += sweep;
            }
        }
        float cy = size / 2f;
        canvas.drawText(center, getWidth() / 2f, cy + big.getTextSize() * 0.2f, big);
        canvas.drawText(caption, getWidth() / 2f, cy + big.getTextSize() * 0.2f + small.getTextSize() * 1.4f, small);
    }

    private void drawHalf(Canvas canvas) {
        float stroke = ring.getStrokeWidth();
        float w = getWidth();
        oval.set(stroke / 2, stroke / 2, w - stroke / 2, w - stroke / 2);
        int total = 0;
        for (int v : values) total += v;
        if (total == 0) {
            ring.setColor(ContextCompat.getColor(getContext(), R.color.color_panel_line));
            canvas.drawArc(oval, 180, 180, false, ring);
        } else {
            float start = 180;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                float sweep = 180f * values[i] / total;
                ring.setColor(colors[i % colors.length]);
                canvas.drawArc(oval, start, Math.max(1f, sweep - 2f), false, ring);
                start += sweep;
            }
        }
        float base = getHeight() - small.getTextSize() * 0.4f;
        canvas.drawText(caption, w / 2f, base, small);
        canvas.drawText(center, w / 2f, base - small.getTextSize() * 1.5f, big);
    }
}
