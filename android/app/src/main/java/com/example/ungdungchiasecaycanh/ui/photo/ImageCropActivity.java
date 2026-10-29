package com.example.ungdungchiasecaycanh.ui.photo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.example.ungdungchiasecaycanh.util.Bitmaps;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Step between picking a photo and using it: the member moves and zooms the photo inside a fixed
 * frame (square for plants and listings, 3:2 for posts). The result is a JPEG in app storage,
 * returned as a content URI, so later screens never depend on the picker's temporary access.
 */
public class ImageCropActivity extends AppCompatActivity {
    private static final String EXTRA_ASPECT = "aspect";
    private static final String EXTRA_STEP = "step";
    private static final String EXTRA_STEPS = "steps";
    /** Big enough for a sharp full-width photo; the uploaders compress further. */
    private static final int MAX_DECODE_SIDE = 2048;
    private static final int MAX_OUTPUT_SIDE = 1600;

    /** Photo to crop, frame shape (width / height) and, for several photos, "2 of 3". */
    public static final class Request {
        final Uri source;
        final float aspect;
        final int step, steps;

        public Request(@NonNull Uri source, float aspect, int step, int steps) {
            this.source = source;
            this.aspect = aspect;
            this.step = step;
            this.steps = steps;
        }
    }

    /** Result: the cropped photo's URI, or null when the member closed the editor. */
    public static final class Crop extends ActivityResultContract<Request, Uri> {
        @NonNull @Override public Intent createIntent(@NonNull Context context, Request request) {
            return new Intent(context, ImageCropActivity.class)
                    .setData(request.source)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(EXTRA_ASPECT, request.aspect)
                    .putExtra(EXTRA_STEP, request.step)
                    .putExtra(EXTRA_STEPS, request.steps);
        }

        @Override public Uri parseResult(int resultCode, @Nullable Intent intent) {
            return resultCode == Activity.RESULT_OK && intent != null ? intent.getData() : null;
        }
    }

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private CropImageView cropView;
    private MaterialButton useButton;
    private View progress;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_crop);
        SecondaryChrome.applyPlain(this, findViewById(R.id.cropRoot));
        // The editor is dark in both themes, so the status and navigation bar icons are light.
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(false);
        bars.setAppearanceLightNavigationBars(false);
        cropView = findViewById(R.id.cropView);
        useButton = findViewById(R.id.cropUse);
        progress = findViewById(R.id.cropProgress);
        cropView.setAspect(getIntent().getFloatExtra(EXTRA_ASPECT, 1f));
        int steps = getIntent().getIntExtra(EXTRA_STEPS, 1);
        TextView title = findViewById(R.id.cropTitle);
        title.setText(steps > 1 ? getString(R.string.crop_title_step,
                getIntent().getIntExtra(EXTRA_STEP, 1), steps) : getString(R.string.crop_title));
        findViewById(R.id.cropClose).setOnClickListener(v -> finish());
        findViewById(R.id.cropReset).setOnClickListener(v -> cropView.reset());
        useButton.setOnClickListener(v -> save());
        load(getIntent().getData());
    }

    private void load(Uri source) {
        busy(true);
        io.execute(() -> {
            Bitmap bitmap = null;
            try {
                bitmap = Bitmaps.decode(getContentResolver(), source, MAX_DECODE_SIDE);
            } catch (IOException | RuntimeException ignored) {
                // Handled below: an unreadable photo closes the editor with a message.
            }
            Bitmap decoded = bitmap;
            main.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                busy(false);
                if (decoded == null) {
                    Toast.makeText(this, R.string.crop_open_error, Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    cropView.setBitmap(decoded);
                }
            });
        });
    }

    private void save() {
        Bitmap cropped = cropView.crop(MAX_OUTPUT_SIDE);
        if (cropped == null) return;
        busy(true);
        io.execute(() -> {
            Uri result = null;
            try {
                // ponytail: photos replaced or removed later are not deleted; sweep photos/ if storage grows.
                File dir = new File(getFilesDir(), "photos");
                if (!dir.isDirectory() && !dir.mkdirs()) throw new IOException("No photo folder");
                File file = new File(dir, UUID.randomUUID() + ".jpg");
                try (FileOutputStream out = new FileOutputStream(file)) {
                    cropped.compress(Bitmap.CompressFormat.JPEG, 90, out);
                }
                result = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            } catch (IOException | RuntimeException ignored) {
                // Reported below.
            }
            Uri saved = result;
            main.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                busy(false);
                if (saved == null) {
                    Toast.makeText(this, R.string.crop_save_error, Toast.LENGTH_LONG).show();
                    return;
                }
                setResult(RESULT_OK, new Intent().setData(saved).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
                finish();
            });
        });
    }

    private void busy(boolean value) {
        progress.setVisibility(value ? View.VISIBLE : View.GONE);
        useButton.setEnabled(!value);
    }

    @Override protected void onDestroy() {
        io.shutdown();
        super.onDestroy();
    }
}
