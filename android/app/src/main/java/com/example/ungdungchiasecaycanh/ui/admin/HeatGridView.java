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

/**
 * One column of squares per day (MoonRow canvas): from the bottom, a square per new listing, then one
 * per new post, four squares high; date labels under the first, middle and last day.
 */
public class HeatGridView extends View {
    private static final int ROWS = 4;
    private final Paint cell = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int[] listings = new int[0], posts = new int[0];
    private int listingColor, postColor;
    private String[] labels = new String[0];

    public HeatGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        label.setTextSize(10 * getResources().getDisplayMetrics().density);
        label.setColor(ContextCompat.getColor(context, R.color.color_text_secondary));
    }

    /** labels: one per day; only the first, middle and last are drawn. */
    public void setData(int[] listings, int[] posts, int listingColor, int postColor, String[] labels) {
        this.listings = listings;
        this.posts = posts;
        this.listingColor = listingColor;
        this.postColor = postColor;
        this.labels = labels;
        requestLayout();
        invalidate();
    }

    private float gap() { return 3 * getResources().getDisplayMetrics().density; }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int days = Math.max(1, listings.length);
        float size = (w - gap() * (days - 1)) / days;
        int h = Math.round(size * ROWS + gap() * (ROWS - 1) + label.getTextSize() * 1.8f);
        setMeasuredDimension(w, h);
    }

    @Override protected void onDraw(Canvas canvas) {
        int days = listings.length;
        if (days == 0) return;
        float gap = gap();
        float size = (getWidth() - gap * (days - 1)) / days;
        float radius = 3 * getResources().getDisplayMetrics().density;
        int empty = ContextCompat.getColor(getContext(), R.color.color_panel_line);
        for (int d = 0; d < days; d++) {
            float x = d * (size + gap);
            // ponytail: four squares cap a day; past that the column just stays full.
            for (int r = 0; r < ROWS; r++) {
                cell.setColor(r < listings[d] ? listingColor : r < listings[d] + posts[d] ? postColor : empty);
                float top = (ROWS - 1 - r) * (size + gap);
                rect.set(x, top, x + size, top + size);
                canvas.drawRoundRect(rect, radius, radius, cell);
            }
        }
        float baseline = getHeight() - label.getTextSize() * 0.3f;
        int[] at = {0, days / 2, days - 1};
        Paint.Align[] align = {Paint.Align.LEFT, Paint.Align.CENTER, Paint.Align.RIGHT};
        float[] xs = {0, (days / 2) * (size + gap) + size / 2, getWidth()};
        for (int i = 0; i < at.length; i++) {
            if (at[i] >= labels.length) continue;
            label.setTextAlign(align[i]);
            canvas.drawText(labels[at[i]], xs[i], baseline, label);
        }
    }
}
