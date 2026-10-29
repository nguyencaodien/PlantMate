package com.example.ungdungchiasecaycanh.ui.doctor;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.Motion;

/**
 * Camera framing for Plant Doctor (canvas "DoctorScan"): the preview is dimmed except a rounded window
 * 56dp in from each side, 200dp down and 300dp tall, with green corner brackets and a scan line that
 * sweeps up and down inside it (still when the system asks for reduced motion).
 */
public class ScanOverlayView extends View {
    static final float INSET_DP = 56, TOP_DP = 200, HEIGHT_DP = 300, RADIUS_DP = 24, ARM_DP = 42, STROKE_DP = 4;

    private final float dp = getResources().getDisplayMetrics().density;
    private final Paint dim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint corner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF window = new RectF();
    private final Path shade = new Path();
    private final Path corners = new Path();
    private float sweep = 0.5f;
    private ValueAnimator animator;

    public ScanOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        int green = ContextCompat.getColor(context, R.color.progress_on_dark);
        dim.setColor(0x94000000);
        line.setColor(green);
        corner.setColor(green);
        corner.setStyle(Paint.Style.STROKE);
        corner.setStrokeWidth(STROKE_DP * dp);
        corner.setStrokeCap(Paint.Cap.SQUARE);
    }

    /** The framing window in this view's coordinates. */
    RectF window() { return window; }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        window.set(INSET_DP * dp, TOP_DP * dp, w - INSET_DP * dp, (TOP_DP + HEIGHT_DP) * dp);
        float r = RADIUS_DP * dp;
        shade.reset();
        shade.setFillType(Path.FillType.EVEN_ODD);
        shade.addRect(0, 0, w, h, Path.Direction.CW);
        shade.addRoundRect(window, r, r, Path.Direction.CW);

        // Brackets sit on the window edge, centred on the stroke, with 16dp rounded corners.
        float s = STROKE_DP * dp / 2, arm = ARM_DP * dp, cr = 16 * dp;
        RectF o = new RectF(window.left + s, window.top + s, window.right - s, window.bottom - s);
        corners.reset();
        bracket(o.left, o.top + arm, o.left, o.top, o.left + arm, o.top, cr);
        bracket(o.right - arm, o.top, o.right, o.top, o.right, o.top + arm, cr);
        bracket(o.right, o.bottom - arm, o.right, o.bottom, o.right - arm, o.bottom, cr);
        bracket(o.left + arm, o.bottom, o.left, o.bottom, o.left, o.bottom - arm, cr);
    }

    private void bracket(float x0, float y0, float cx, float cy, float x1, float y1, float r) {
        corners.moveTo(x0, y0);
        float ax = cx + Math.signum(x0 - cx) * r, ay = cy + Math.signum(y0 - cy) * r;
        float bx = cx + Math.signum(x1 - cx) * r, by = cy + Math.signum(y1 - cy) * r;
        corners.lineTo(ax, ay);
        corners.quadTo(cx, cy, bx, by);
        corners.lineTo(x1, y1);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!Motion.enabled(getContext())) return;
        animator = ValueAnimator.ofFloat(0.08f, 0.92f);
        animator.setDuration(1200);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(a -> { sweep = (float) a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawPath(shade, dim);
        float y = window.top + window.height() * sweep;
        float pad = 14 * dp;
        canvas.drawRoundRect(window.left + pad, y - 1.5f * dp, window.right - pad, y + 1.5f * dp, 2 * dp, 2 * dp, line);
        canvas.drawPath(corners, corner);
    }
}
