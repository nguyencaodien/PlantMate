package com.example.ungdungchiasecaycanh.ui.settings;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** About PlantMate: the app's intent, what to expect, and licences for bundled fonts and libraries. */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        SecondaryChrome.apply(this, findViewById(R.id.aboutRoot), findViewById(R.id.aboutHero));
        Motion.start(findViewById(R.id.aboutMam));
        Motion.rise(findViewById(R.id.aboutMission), 180);
        Motion.rise(findViewById(R.id.aboutSections), 280);

        TextView version = findViewById(R.id.aboutVersion);
        try {
            String name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            version.setText(getString(R.string.about_version, name == null ? "" : name));
        } catch (PackageManager.NameNotFoundException ignored) {
            version.setText("");
        }

        findViewById(R.id.aboutBack).setOnClickListener(v -> finish());
        findViewById(R.id.aboutLicenceAndroidx).setOnClickListener(v ->
                showLicence(R.string.about_lib_androidx, getString(R.string.about_licence_apache_text)));
        findViewById(R.id.aboutLicenceFirebase).setOnClickListener(v ->
                showLicence(R.string.about_lib_firebase, getString(R.string.about_licence_apache_text)));
    }

    private void showLicence(@StringRes int title, String text) {
        TextView body = new TextView(this);
        body.setText(text);
        body.setTextAppearance(R.style.TextAppearance_PlantMate_Meta);
        body.setTextIsSelectable(true);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        body.setPadding(pad, pad / 2, pad, pad / 2);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private String readRaw(@RawRes int id) {
        StringBuilder out = new StringBuilder();
        try (InputStream in = getResources().openRawResource(id);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
        } catch (IOException e) {
            return "";
        }
        return out.toString().trim();
    }
}
