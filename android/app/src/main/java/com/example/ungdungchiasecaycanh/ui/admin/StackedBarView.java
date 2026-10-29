package com.example.ungdungchiasecaycanh.ui.admin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ungdungchiasecaycanh.R;

/** One rounded 100% bar split by share, a 2dp gap between parts. */
public class StackedBarView extends View {
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clip = new Path();
    private int[] values = new int[0];
    private int[] colors = new int[0];

    public StackedBarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setData(int[] values, int[] colors) {
        this.values = values;
        this.colors = colors;
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), Math.round(14 * getResources().getDisplayMetrics().density));
    }

    @Override protected void onDraw(Canvas canvas) {
        float dp = getResources().getDisplayMetrics().density;
        float h = getHeight();
        clip.reset();
        rect.set(0, 0, getWidth(), h);
        clip.addRoundRect(rect, h / 2, h / 2, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        int total = 0;
        for (int v : values) total += v;
        if (total == 0) {
            fill.setColor(ContextCompat.getColor(getContext(), R.color.color_panel_line));
            canvas.drawRect(rect, fill);
        } else {
            float x = 0;
            for (int i = 0; i < values.length; i++) {
                if (values[i] <= 0) continue;
                float w = getWidth() * values[i] / (float) total;
                fill.setColor(colors[i % colors.length]);
                canvas.drawRect(x, 0, Math.max(x, x + w - 2 * dp), h, fill);
                x += w;
            }
        }
        canvas.restore();
    }
}
