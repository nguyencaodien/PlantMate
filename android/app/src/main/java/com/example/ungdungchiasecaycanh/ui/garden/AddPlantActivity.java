package com.example.ungdungchiasecaycanh.ui.garden;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.example.ungdungchiasecaycanh.R;

public class AddPlantActivity extends BasePlantFormActivity {
    /** Optional prefill (e.g. from a Plant Doctor result). */
    public static final String EXTRA_NICKNAME = "extra_nickname";
    public static final String EXTRA_SPECIES = "extra_species";

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_add_plant, savedInstanceState);
        if (savedInstanceState == null) {
            String nickname = getIntent().getStringExtra(EXTRA_NICKNAME);
            String species = getIntent().getStringExtra(EXTRA_SPECIES);
            if (nickname != null) nicknameInput.setText(nickname);
            if (species != null) speciesInput.setText(species);
        }
    }

    @Override protected void submitValid(String nickname, String species, String imageUri,
                                         boolean reminders, int intervalDays, String wateringTimes,
                                         String room, String light) {
        viewModel.add(nickname, species, imageUri, reminders, intervalDays, wateringTimes, room, light);
    }
}
