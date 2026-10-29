package com.example.ungdungchiasecaycanh.ui.garden;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;

import java.util.List;

/**
 * The last {@link #DAYS} days of care as two rows of rounded squares, oldest first, today ringed:
 * blue with a drop watered, orange with a bag fertilised, split diagonally when both happened that day,
 * mint any other care, grey nothing logged. Tapping a day reports its index.
 */
public class CareHeatmapView extends View {
    public static final int DAYS = 30;
    public static final int NONE = 0, WATER = 1, FERTILIZE = 2, OTHER = 3, BOTH = 4;

    public interface OnDayClickListener { void onDayClick(int index); }

    private OnDayClickListener dayClick;
    private final Path half = new Path();
    private static final int COLUMNS = 15;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cell = new RectF();
    private int[] days = new int[DAYS];
    private final Drawable drop, bag;

    public CareHeatmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(2 * getResources().getDisplayMetrics().density);
        ring.setColor(ContextCompat.getColor(context, R.color.color_primary));
        drop = ContextCompat.getDrawable(context, R.drawable.ic_care_drop);
        bag = ContextCompat.getDrawable(context, R.drawable.ic_care_bag);
    }

    public void setDays(int[] values) {
        days = values == null || values.length != DAYS ? new int[DAYS] : values.clone();
        invalidate();
    }

    public void setOnDayClickListener(@Nullable OnDayClickListener listener) {
        dayClick = listener;
    }

    /**
     * Kind of care per day (index 0 = 29 days ago, 29 = today): watering and fertilising on the same day
     * is BOTH, else the one that happened; other care only shows when neither did. Notes do not count.
     */
    public static int[] fromLogs(@Nullable List<CareLogEntity> logs, long now) {
        int[] out = new int[DAYS];
        if (logs == null) return out;
        for (CareLogEntity log : logs) {
            if (log.getType() == CareType.NOTE) continue;
            int ago = CareLogAdapter.calendarDaysBetween(log.getCreatedAt(), now);
            if (ago < 0 || ago >= DAYS) continue;
            int i = DAYS - 1 - ago;
            int kind = log.getType() == CareType.FERTILIZE ? FERTILIZE : log.getType() == CareType.WATER ? WATER : OTHER;
            out[i] = combine(out[i], kind);
        }
        return out;
    }

    static int combine(int current, int kind) {
        if (current == BOTH || current == kind) return current;
        if ((current == WATER && kind == FERTILIZE) || (current == FERTILIZE && kind == WATER)) return BOTH;
        if (kind == OTHER) return current == NONE ? OTHER : current;
        return kind; // water or fertilise replaces "other"
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (dayClick == null) return super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float gap = 4 * getResources().getDisplayMetrics().density;
            float size = (getWidth() - gap * (COLUMNS - 1)) / COLUMNS;
            int column = Math.min(COLUMNS - 1, Math.max(0, (int) (event.getX() / (size + gap))));
            int row = event.getY() < size + gap / 2 ? 0 : 1;
            dayClick.onDayClick(row * COLUMNS + column);
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        float gap = 4 * getResources().getDisplayMetrics().density;
        float size = (width - gap * (COLUMNS - 1)) / COLUMNS;
        setMeasuredDimension(width, (int) Math.ceil(size * 2 + gap));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float density = getResources().getDisplayMetrics().density;
        float gap = 4 * density;
        float size = (getWidth() - gap * (COLUMNS - 1)) / COLUMNS;
        float radius = 5 * density;
        float inset = ring.getStrokeWidth();
        for (int i = 0; i < DAYS; i++) {
            float left = (i % COLUMNS) * (size + gap);
            float top = (i / COLUMNS) * (size + gap);
            cell.set(left, top, left + size, top + size);
            fill.setColor(ContextCompat.getColor(getContext(), colorFor(days[i])));
            if (i == DAYS - 1) {
                canvas.drawRoundRect(cell.left + inset / 2, cell.top + inset / 2, cell.right - inset / 2, cell.bottom - inset / 2, radius, radius, ring);
                cell.inset(inset * 1.6f, inset * 1.6f);
            }
            if (days[i] == BOTH) {
                drawBoth(canvas, radius);
                continue;
            }
            canvas.drawRoundRect(cell, radius, radius, fill);
            Drawable icon = days[i] == WATER ? drop : days[i] == FERTILIZE ? bag : null;
            if (icon != null) {
                float pad = cell.width() * 0.17f;
                icon.setBounds(Math.round(cell.left + pad), Math.round(cell.top + pad),
                        Math.round(cell.right - pad), Math.round(cell.bottom - pad));
                icon.draw(canvas);
            }
        }
    }

    /** Watering in the top-left half (drop), fertilising in the bottom-right half (bag). */
    private void drawBoth(Canvas canvas, float radius) {
        int save = canvas.save();
        half.reset();
        half.moveTo(cell.left, cell.top);
        half.lineTo(cell.right, cell.top);
        half.lineTo(cell.left, cell.bottom);
        half.close();
        canvas.clipPath(half);
        fill.setColor(ContextCompat.getColor(getContext(), R.color.care_water));
        canvas.drawRoundRect(cell, radius, radius, fill);
        canvas.restoreToCount(save);
        save = canvas.save();
        half.reset();
        half.moveTo(cell.right, cell.top);
        half.lineTo(cell.right, cell.bottom);
        half.lineTo(cell.left, cell.bottom);
        half.close();
        canvas.clipPath(half);
        fill.setColor(ContextCompat.getColor(getContext(), R.color.care_feed));
        canvas.drawRoundRect(cell, radius, radius, fill);
        canvas.restoreToCount(save);
        float s = cell.width() * 0.48f, pad = cell.width() * 0.06f;
        drop.setBounds(Math.round(cell.left + pad), Math.round(cell.top + pad),
                Math.round(cell.left + pad + s), Math.round(cell.top + pad + s));
        drop.draw(canvas);
        bag.setBounds(Math.round(cell.right - pad - s), Math.round(cell.bottom - pad - s),
                Math.round(cell.right - pad), Math.round(cell.bottom - pad));
        bag.draw(canvas);
    }

    private static int colorFor(int kind) {
        switch (kind) {
            case WATER: return R.color.care_water;
            case FERTILIZE: return R.color.care_feed;
            case OTHER: return R.color.scan_line;
            default: return R.color.color_panel_line;
        }
    }
}
