package com.example.ungdungchiasecaycanh.ui.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SecondaryChrome.apply(this, findViewById(R.id.settingsRoot), findViewById(R.id.settingsHero));
        findViewById(R.id.settingsBack).setOnClickListener(v -> finish());
        if (savedInstanceState == null) getSupportFragmentManager().beginTransaction()
                .replace(R.id.settingsContainer, new SettingsFragment()).commit();
    }
}
