package com.example.ungdungchiasecaycanh.ui.garden;

import android.view.View;
import android.widget.Toast;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.example.ungdungchiasecaycanh.R;

public class EditPlantActivity extends BasePlantFormActivity {
    public static final String EXTRA_PLANT_ID = "extra_plant_id";
    /** Result code when the plant was deleted, so the plant detail closes too. */
    public static final int RESULT_PLANT_DELETED = RESULT_FIRST_USER + 1;
    private long plantId;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize(R.layout.activity_edit_plant, savedInstanceState);
        plantId = getIntent().getLongExtra(EXTRA_PLANT_ID, -1);
        if (plantId <= 0) { finish(); return; }
        viewModel.load(plantId);
        View delete = findViewById(R.id.plantDeleteButton);
        delete.setVisibility(View.VISIBLE);
        delete.setOnClickListener(v -> confirmDelete());
        viewModel.getDeleted().observe(this, done -> {
            if (!Boolean.TRUE.equals(done)) return;
            PlantEntity plant = viewModel.getPlant().getValue();
            Toast.makeText(this, getString(R.string.garden_deleted, plant == null ? "" : plant.getNickname()),
                    Toast.LENGTH_SHORT).show();
            setResult(RESULT_PLANT_DELETED);
            finish();
        });
        viewModel.getDeleteFailed().observe(this, failed -> {
            if (!Boolean.TRUE.equals(failed)) return;
            viewModel.deleteErrorShown();
            Snackbar.make(findViewById(R.id.plantFormRoot), R.string.garden_delete_error, Snackbar.LENGTH_LONG).show();
        });
    }

    private void confirmDelete() {
        PlantEntity plant = viewModel.getPlant().getValue();
        String name = plant == null ? "" : plant.getNickname();
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.garden_delete_title, name))
                .setMessage(R.string.garden_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.garden_delete_confirm, (d, w) -> viewModel.delete(plantId))
                .show();
    }

    @Override protected void submitValid(String nickname, String species, String imageUri,
                                         boolean reminders, int intervalDays, String wateringTimes,
                                         String room, String light) {
        viewModel.update(plantId, nickname, species, imageUri, reminders, intervalDays, wateringTimes, room, light);
    }
}
