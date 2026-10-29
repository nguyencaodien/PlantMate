package com.example.ungdungchiasecaycanh.ui.photo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/** A wide photo cropped in the square editor comes back as a square JPEG in app storage. */
@RunWith(AndroidJUnit4.class)
public class ImageCropActivityTest {

    @Test
    public void usePhotoReturnsASquareCrop() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        File dir = new File(context.getFilesDir(), "photos");
        dir.mkdirs();
        File source = new File(dir, "crop-test-source.png");
        Bitmap wide = Bitmap.createBitmap(1200, 600, Bitmap.Config.ARGB_8888);
        wide.eraseColor(Color.rgb(52, 83, 59));
        try (FileOutputStream out = new FileOutputStream(source)) {
            wide.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", source);

        ImageCropActivity.Crop contract = new ImageCropActivity.Crop();
        try (ActivityScenario<ImageCropActivity> scenario = ActivityScenario.launchActivityForResult(
                contract.createIntent(context, new ImageCropActivity.Request(uri, 1f, 1, 1)))) {
            waitUntilEnabled();
            onView(withId(R.id.cropUse)).perform(click());

            Uri result = contract.parseResult(scenario.getResult().getResultCode(), scenario.getResult().getResultData());
            assertEquals(Activity.RESULT_OK, scenario.getResult().getResultCode());
            assertNotNull(result);
            try (InputStream in = context.getContentResolver().openInputStream(result)) {
                Bitmap cropped = BitmapFactory.decodeStream(in);
                assertEquals(cropped.getWidth(), cropped.getHeight());
            }
        } finally {
            source.delete();
        }
    }

    /** The photo decodes off the main thread; Use photo is enabled once it is shown. */
    private static void waitUntilEnabled() throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            try {
                onView(withId(R.id.cropUse)).check(matches(isEnabled()));
                Thread.sleep(300);  // the bitmap is set just after the button enables
                return;
            } catch (AssertionError notYet) {
                Thread.sleep(100);
            }
        }
    }
}
