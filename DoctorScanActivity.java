package com.example.ungdungchiasecaycanh.ui.doctor;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.PhotoQuality;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plant Doctor's in-app camera (canvas "DoctorScan"). Shows the live preview in a framing window, checks
 * light and sharpness on the frames as they come, and returns the photo (or one picked from the
 * library) as the result data URI. Without the camera permission the library stays available.
 */
public class DoctorScanActivity extends AppCompatActivity {
    /** How often a preview frame is checked for light and sharpness. */
    private static final long CHECK_EVERY_MS = 400;
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ProcessCameraProvider provider;
    private ExecutorService analysisExecutor;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean flashOn;
    private boolean capturing;
    private long lastCheck;
    private boolean askedPermission;
    private final List<ObjectAnimator> dotAnimators = new ArrayList<>();

    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startCamera(); else showBlocked(R.string.doctor_scan_no_permission, true);
            });
    private final ActivityResultLauncher<PickVisualMediaRequest> library = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Still readable for this session; the Doctor screen reports it if not.
                }
                finishWith(uri);
            });

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_scan);
        previewView = findViewById(R.id.scanPreview);
        analysisExecutor = Executors.newSingleThreadExecutor();
        View header = findViewById(R.id.scanHeader);
        View bar = findViewById(R.id.scanBar);
        int barBottom = bar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scanRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            header.setPadding(header.getPaddingLeft(), bars.top, header.getPaddingRight(), 0);
            bar.setPadding(bar.getPaddingLeft(), bar.getPaddingTop(), bar.getPaddingRight(), barBottom + bars.bottom);
            return insets;
        });

        findViewById(R.id.scanClose).setOnClickListener(v -> finish());
        findViewById(R.id.scanLibrary).setOnClickListener(v -> library.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        findViewById(R.id.scanCapture).setOnClickListener(v -> capture());
        findViewById(R.id.scanFlash).setOnClickListener(v -> toggleFlash());
        findViewById(R.id.scanFlip).setOnClickListener(v -> {
            lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK
                    ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            bind();
        });
        findViewById(R.id.scanAllow).setOnClickListener(v -> askAgain());
        showChecks(PhotoQuality.Result.OK);
        pulseDots();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            askedPermission = true;
            permission.launch(Manifest.permission.CAMERA);
        }
    }

    /** Asks again while Android allows it; once the member chose "don't ask again", opens the app's settings. */
    private void askAgain() {
        if (!askedPermission || shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            askedPermission = true;
            permission.launch(Manifest.permission.CAMERA);
        } else {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null)));
        }
    }

    @Override protected void onResume() {
        super.onResume();
        // Back from the app settings with the camera now allowed.
        if (provider == null && findViewById(R.id.scanBlocked).getVisibility() == View.VISIBLE
                && ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    private void startCamera() {
        findViewById(R.id.scanBlocked).setVisibility(View.GONE);
        setCameraControlsVisible(true);
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                provider = future.get();
                bind();
            } catch (Exception error) {
                showBlocked(R.string.doctor_scan_unavailable, false);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /** (Re)binds preview, capture and frame checks to the chosen lens. */
    private void bind() {
        if (provider == null || isFinishing()) return;
        try {
            CameraSelector selector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
            if (!provider.hasCamera(selector)) {
                lensFacing = CameraSelector.LENS_FACING_BACK;
                selector = CameraSelector.DEFAULT_BACK_CAMERA;
            }
            Preview preview = new Preview.Builder().build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                    .build();
            ImageAnalysis analysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            analysis.setAnalyzer(analysisExecutor, this::check);
            provider.unbindAll();
            provider.bindToLifecycle(this, selector, preview, imageCapture, analysis);
            boolean both = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                    && provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
            findViewById(R.id.scanFlip).setVisibility(both ? View.VISIBLE : View.INVISIBLE);
        } catch (Exception error) {
            showBlocked(R.string.doctor_scan_unavailable, false);
        }
    }

    /** Runs on the analysis thread: luma of the centre of the frame, a few times a second. */
    private void check(@NonNull ImageProxy image) {
        try {
            long now = SystemClock.elapsedRealtime();
            if (now - lastCheck < CHECK_EVERY_MS) return;
            lastCheck = now;
            ImageProxy.PlaneProxy y = image.getPlanes()[0];
            PhotoQuality.Result result = PhotoQuality.checkLumaPlane(y.getBuffer(), y.getRowStride(),
                    y.getPixelStride(), image.getWidth(), image.getHeight());
            runOnUiThread(() -> showChecks(result));
        } finally {
            image.close();
        }
    }

    /** Green check chips when the frame is bright and sharp; dark chips say what to fix. */
    private void showChecks(PhotoQuality.Result result) {
        boolean bright = result != PhotoQuality.Result.TOO_DARK;
        boolean sharp = result == PhotoQuality.Result.OK;
        chip(findViewById(R.id.scanLight), bright, bright ? R.string.doctor_scan_bright : R.string.doctor_scan_dark);
        chip(findViewById(R.id.scanSharp), sharp, sharp ? R.string.doctor_scan_sharp : R.string.doctor_scan_blurry);
    }

    private void chip(TextView chip, boolean ok, int label) {
        chip.setText(label);
        chip.setBackgroundResource(ok ? R.drawable.bg_badge_give_away : R.drawable.bg_scan_check_dark);
        chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        chip.setCompoundDrawablesRelativeWithIntrinsicBounds(ok ? R.drawable.ic_check_14 : 0, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(chip, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
    }

    private void capture() {
        if (imageCapture == null || capturing) return;
        File dir = new File(getFilesDir(), "doctor_photos");
        if (!dir.exists() && !dir.mkdirs()) { failCapture(); return; }
        File file = new File(dir, "scan_" + System.currentTimeMillis() + ".jpg");
        capturing = true;
        Motion.pop(findViewById(R.id.scanCapture), 260, 0);
        imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
                    @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        finishWith(FileProvider.getUriForFile(DoctorScanActivity.this,
                                getPackageName() + ".fileprovider", file));
                    }

                    @Override public void onError(@NonNull ImageCaptureException error) {
                        capturing = false;
                        failCapture();
                    }
                });
    }

    private void failCapture() {
        Toast.makeText(this, R.string.doctor_scan_failed, Toast.LENGTH_SHORT).show();
    }

    private void toggleFlash() {
        flashOn = !flashOn;
        if (imageCapture != null) imageCapture.setFlashMode(flashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
        ImageButton flash = findViewById(R.id.scanFlash);
        flash.setImageResource(flashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        flash.setContentDescription(getString(flashOn ? R.string.doctor_scan_flash_on : R.string.doctor_scan_flash_off));
    }

    /** No camera: Mầm explains, the library stays usable; "Allow" shows only when asking again helps. */
    private void showBlocked(int message, boolean canAsk) {
        findViewById(R.id.scanBlocked).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.scanBlockedText)).setText(message);
        findViewById(R.id.scanAllow).setVisibility(canAsk ? View.VISIBLE : View.GONE);
        setCameraControlsVisible(false);
    }

    private void setCameraControlsVisible(boolean visible) {
        int v = visible ? View.VISIBLE : View.INVISIBLE;
        findViewById(R.id.scanOverlay).setVisibility(v);
        findViewById(R.id.scanHint).setVisibility(v);
        findViewById(R.id.scanChecks).setVisibility(v);
        findViewById(R.id.scanCapture).setVisibility(v);
        findViewById(R.id.scanFlash).setVisibility(v);
        findViewById(R.id.scanFlip).setVisibility(v);
        findViewById(R.id.scanCaption).setVisibility(v);
    }

    /** The three "watching" dots fade in turn. */
    private void pulseDots() {
        if (!Motion.enabled(this)) return;
        ViewGroup dots = findViewById(R.id.scanDots);
        for (int i = 0; i < dots.getChildCount(); i++) {
            ObjectAnimator a = ObjectAnimator.ofFloat(dots.getChildAt(i), View.ALPHA, 0.25f, 1f);
            a.setDuration(600);
            a.setStartDelay(180L * i);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.start();
            dotAnimators.add(a);
        }
    }

    private void finishWith(Uri uri) {
        setResult(RESULT_OK, new Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        finish();
    }

    @Override protected void onDestroy() {
        for (ObjectAnimator a : dotAnimators) a.cancel();
        analysisExecutor.shutdown();
        super.onDestroy();
    }
}
