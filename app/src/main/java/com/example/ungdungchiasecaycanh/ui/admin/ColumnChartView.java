package com.example.ungdungchiasecaycanh.ui.admin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ungdungchiasecaycanh.R;

/**
 * Grouped daily columns (one or two series) on a single axis. Thin columns with 4dp rounded tops
 * anchored to the baseline, 2dp gaps, a recessive grid, and tap-to-inspect (the selected day is
 * reported through {@link Listener}, and its values are drawn above the columns).
 */
public class ColumnChartView extends View {

    public interface Listener { void onDaySelected(int index); }

    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axisText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valueText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint highlight = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final float dp;

    private int[][] series = new int[0][];
    private int[] colors = new int[0];
    private String[] xLabels = new String[0];
    private int selected = -1;
    private Listener listener;
    private final int touchSlop;
    private float downX, downY;
    private boolean dragging;

    public ColumnChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        grid.setColor(ContextCompat.getColor(context, R.color.color_border));
        grid.setStrokeWidth(dp);
        axisText.setColor(ContextCompat.getColor(context, R.color.color_text_secondary));
        axisText.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        valueText.setColor(ContextCompat.getColor(context, R.color.color_text_primary));
        valueText.setTextSize(11 * getResources().getDisplayMetrics().scaledDensity);
        valueText.setFakeBoldText(true);
        valueText.setTextAlign(Paint.Align.CENTER);
        highlight.setColor(ContextCompat.getColor(context, R.color.color_primary_tint));
    }

    public void setListener(Listener listener) { this.listener = listener; }

    /** {@code series[s][day]}; colours follow the series, labels follow the days (oldest first). */
    public void setData(int[][] series, int[] colors, String[] xLabels) {
        this.series = series;
        this.colors = colors;
        this.xLabels = xLabels;
        this.selected = days() - 1;   // today is inspected by default
        invalidate();
        if (listener != null && selected >= 0) listener.onDaySelected(selected);
    }

    private int days() { return series.length == 0 ? 0 : series[0].length; }

    private int max() {
        int max = 0;
        for (int[] s : series) for (int v : s) max = Math.max(max, v);
        return max;
    }

    /** Axis maximum rounded up to an even number (at least 2) so the midline is a whole number. */
    private int axisMax() {
        int m = Math.max(2, max());
        return m % 2 == 0 ? m : m + 1;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, resolveSize((int) (180 * dp), heightMeasureSpec));
    }

    private float left() { return 28 * dp; }
    private float top() { return 18 * dp; }
    private float bottom() { return getHeight() - 22 * dp; }

    @Override
    protected void onDraw(Canvas canvas) {
        int days = days();
        if (days == 0) return;
        float left = left(), top = top(), bottom = bottom(), right = getWidth();
        int axisMax = axisMax();

        // Recessive grid: baseline, midline and top line with whole-number labels.
        axisText.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 2; i++) {
            float y = bottom - (bottom - top) * i / 2f;
            canvas.drawLine(left, y, right, y, grid);
            canvas.drawText(String.valueOf(axisMax * i / 2), left - 6 * dp, y + 4 * dp, axisText);
        }

        float slot = (right - left) / days;
        int count = series.length;
        // Narrow charts (a half-width card) shrink the gap and use most of each day's slot.
        float gap = Math.min(2 * dp, slot * 0.1f);
        float barWidth = Math.max(dp, Math.min(10 * dp, (slot * 0.8f - gap * (count - 1)) / count));
        float groupWidth = barWidth * count + gap * (count - 1);
        float radius = Math.min(4 * dp, barWidth / 2f);

        for (int d = 0; d < days; d++) {
            float slotLeft = left + slot * d;
            if (d == selected) {
                rect.set(slotLeft + dp, top - 14 * dp, slotLeft + slot - dp, bottom);
                canvas.drawRoundRect(rect, 6 * dp, 6 * dp, highlight);
            }
            float x = slotLeft + (slot - groupWidth) / 2f;
            for (int s = 0; s < count; s++) {
                int v = series[s][d];
                float barLeft = x + s * (barWidth + gap);
                if (v > 0) {
                    float h = (bottom - top) * v / (float) axisMax;
                    roundedTop(barLeft, bottom - h, barLeft + barWidth, bottom, radius);
                    bar.setColor(colors[s]);
                    canvas.drawPath(path, bar);
                }
                if (d == selected && barWidth >= 6 * dp) { // too thin to label: the line below the chart says it
                    float h = (bottom - top) * v / (float) axisMax;
                    canvas.drawText(String.valueOf(v), barLeft + barWidth / 2f, bottom - h - 4 * dp, valueText);
                }
            }
        }

        // Sparse day labels: first (left-aligned), middle and last (right-aligned) so none is clipped.
        float labelY = getHeight() - 6 * dp;
        if (xLabels.length > 0) {
            axisText.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(xLabels[0], left, labelY, axisText);
        }
        if (days / 2 < xLabels.length) {
            axisText.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(xLabels[days / 2], left + slot * (days / 2) + slot / 2f, labelY, axisText);
        }
        if (days - 1 < xLabels.length) {
            axisText.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(xLabels[days - 1], right, labelY, axisText);
        }
    }

    private void roundedTop(float l, float t, float r, float b, float radius) {
        path.reset();
        float rr = Math.min(radius, (b - t));
        path.moveTo(l, b);
        path.lineTo(l, t + rr);
        path.quadTo(l, t, l + rr, t);
        path.lineTo(r - rr, t);
        path.quadTo(r, t, r, t + rr);
        path.lineTo(r, b);
        path.close();
    }

    /**
     * A tap or a mostly-horizontal drag selects a day; a vertical swipe is left to the parent
     * ScrollView so the dashboard still scrolls when the gesture starts on the chart.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (days() == 0) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(event.getX() - downX), dy = Math.abs(event.getY() - downY);
                if (!dragging && dx > touchSlop && dx > dy) {
                    dragging = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (dragging) select(event.getX());
                return true;
            case MotionEvent.ACTION_UP:
                if (!dragging) {
                    select(event.getX());
                    performClick();
                }
                dragging = false;
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return true;
        }
    }

    private void select(float x) {
        int days = days();
        float slot = (getWidth() - left()) / days;
        int index = Math.max(0, Math.min(days - 1, (int) ((x - left()) / slot)));
        if (index != selected) {
            selected = index;
            invalidate();
            if (listener != null) listener.onDaySelected(index);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
