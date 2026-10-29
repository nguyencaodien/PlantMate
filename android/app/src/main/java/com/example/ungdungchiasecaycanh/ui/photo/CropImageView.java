package com.example.ungdungchiasecaycanh.ui.photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Shows a photo behind a fixed crop frame. Drag to move it, pinch (or double-tap) to zoom; the
 * photo always covers the frame, so every crop is a full picture with no empty edges.
 */
public class CropImageView extends View {
    private static final float MAX_ZOOM = 5f;

    private final Matrix matrix = new Matrix();
    private final RectF frame = new RectF();
    private final Paint photoPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint shade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path outside = new Path();
    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;
    private Bitmap bitmap;
    private float aspect = 1f;
    private float minScale = 1f;

    public CropImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        shade.setColor(0xA6000000);
        outside.setFillType(Path.FillType.EVEN_ODD);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(2 * density);
        border.setColor(Color.WHITE);
        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(density);
        grid.setColor(0x66FFFFFF);
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(@NonNull ScaleGestureDetector detector) {
                zoomBy(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
                return true;
            }
        });
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(@NonNull MotionEvent e) {
                return true;
            }

            @Override public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float dx, float dy) {
                matrix.postTranslate(-dx, -dy);
                keepFrameCovered();
                invalidate();
                return true;
            }

            @Override public boolean onDoubleTap(@NonNull MotionEvent e) {
                zoomBy(currentScale() > minScale * 1.5f ? minScale / currentScale() : 2f, e.getX(), e.getY());
                return true;
            }
        });
    }

    /** Frame shape as width / height, e.g. 1 for square, 1.5 for 3:2. */
    public void setAspect(float widthOverHeight) {
        aspect = widthOverHeight;
        layoutFrame();
    }

    public void setBitmap(Bitmap value) {
        bitmap = value;
        reset();
    }

    /** Back to the whole frame filled with the middle of the photo. */
    public void reset() {
        layoutFrame();
    }

    /** The photo inside the frame, at most {@code maxSide} pixels on its longer side. */
    @Nullable
    public Bitmap crop(int maxSide) {
        if (bitmap == null || frame.isEmpty()) return null;
        Matrix inverse = new Matrix();
        matrix.invert(inverse);
        RectF source = new RectF(frame);
        inverse.mapRect(source);
        int left = Math.max(0, Math.round(source.left));
        int top = Math.max(0, Math.round(source.top));
        int width = Math.min(bitmap.getWidth() - left, Math.round(source.width()));
        int height = Math.min(bitmap.getHeight() - top, Math.round(source.height()));
        if (width <= 0 || height <= 0) return null;
        Bitmap cut = Bitmap.createBitmap(bitmap, left, top, width, height);
        float shrink = Math.min(1f, maxSide / (float) Math.max(width, height));
        if (shrink >= 1f) return cut;
        return Bitmap.createScaledBitmap(cut, Math.round(width * shrink), Math.round(height * shrink), true);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layoutFrame();
    }

    /** Centres the largest frame of the chosen shape, then fits the photo to cover it. */
    private void layoutFrame() {
        if (getWidth() == 0 || getHeight() == 0) return;
        float margin = 20 * getResources().getDisplayMetrics().density;
        float availableW = getWidth() - 2 * margin, availableH = getHeight() - 2 * margin;
        float w = availableW, h = w / aspect;
        if (h > availableH) {
            h = availableH;
            w = h * aspect;
        }
        frame.set((getWidth() - w) / 2f, (getHeight() - h) / 2f, (getWidth() + w) / 2f, (getHeight() + h) / 2f);
        outside.reset();
        outside.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        outside.addRect(frame, Path.Direction.CW);
        if (bitmap != null) {
            minScale = Math.max(frame.width() / bitmap.getWidth(), frame.height() / bitmap.getHeight());
            matrix.setScale(minScale, minScale);
            matrix.postTranslate(frame.centerX() - bitmap.getWidth() * minScale / 2f,
                    frame.centerY() - bitmap.getHeight() * minScale / 2f);
        }
        invalidate();
    }

    private float currentScale() {
        float[] values = new float[9];
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    private void zoomBy(float factor, float focusX, float focusY) {
        if (bitmap == null) return;
        float target = Math.max(minScale, Math.min(minScale * MAX_ZOOM, currentScale() * factor));
        float applied = target / currentScale();
        matrix.postScale(applied, applied, focusX, focusY);
        keepFrameCovered();
        invalidate();
    }

    /** Slides the photo back so no edge of it shows inside the frame. */
    private void keepFrameCovered() {
        if (bitmap == null) return;
        RectF photo = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        matrix.mapRect(photo);
        float dx = 0, dy = 0;
        if (photo.left > frame.left) dx = frame.left - photo.left;
        else if (photo.right < frame.right) dx = frame.right - photo.right;
        if (photo.top > frame.top) dy = frame.top - photo.top;
        else if (photo.bottom < frame.bottom) dy = frame.bottom - photo.bottom;
        matrix.postTranslate(dx, dy);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        boolean handled = scaleDetector.onTouchEvent(event);
        handled = gestureDetector.onTouchEvent(event) || handled;
        if (event.getActionMasked() == MotionEvent.ACTION_UP) performClick();
        return handled || super.onTouchEvent(event);
    }

    @Override public boolean performClick() {
        return super.performClick();
    }

    @Override protected void onDraw(@NonNull Canvas canvas) {
        if (bitmap != null) canvas.drawBitmap(bitmap, matrix, photoPaint);
        canvas.drawPath(outside, shade);
        for (int i = 1; i <= 2; i++) {
            float x = frame.left + frame.width() * i / 3f;
            float y = frame.top + frame.height() * i / 3f;
            canvas.drawLine(x, frame.top, x, frame.bottom, grid);
            canvas.drawLine(frame.left, y, frame.right, y, grid);
        }
        canvas.drawRect(frame, border);
    }
}
