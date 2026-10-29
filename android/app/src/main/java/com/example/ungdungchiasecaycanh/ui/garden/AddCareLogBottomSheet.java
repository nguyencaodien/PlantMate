package com.example.ungdungchiasecaycanh.ui.garden;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class AddCareLogBottomSheet extends BottomSheetDialogFragment {
    public static final String RESULT_KEY = "garden_care_request";
    public static final String RESULT_TYPE = "care_type";
    public static final String RESULT_NOTE = "care_note";
    private static final String ARG_TYPE = "care_type";
    private TextInputLayout noteLayout;
    private MaterialButton saveButton;
    private TextView errorView;

    public static AddCareLogBottomSheet newInstance(CareType type) {
        AddCareLogBottomSheet sheet = new AddCareLogBottomSheet();
        Bundle args = new Bundle(); args.putString(ARG_TYPE, type.name()); sheet.setArguments(args);
        return sheet;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_care_log, container, false);
        CareType type = getCareType();
        view.<TextView>findViewById(R.id.careSheetTitle).setText(label(type));
        noteLayout = view.findViewById(R.id.careNoteLayout);
        errorView = view.findViewById(R.id.careSheetError);
        TextInputEditText noteInput = view.findViewById(R.id.careNoteInput);
        noteLayout.setHint(getString(type == CareType.NOTE ? R.string.care_note_required : R.string.care_note_optional));
        saveButton = view.findViewById(R.id.careSaveButton);
        view.findViewById(R.id.careCancelButton).setOnClickListener(v -> dismiss());
        saveButton.setOnClickListener(v -> {
            String note = noteInput.getText() == null ? "" : noteInput.getText().toString();
            noteLayout.setError(null);
            try { GardenRepository.normalizeCareNote(type, note); }
            catch (IllegalArgumentException error) {
                noteLayout.setError(type == CareType.NOTE ? getString(R.string.care_note_required)
                        : getString(R.string.care_note_too_long));
                return;
            }
            Bundle result = new Bundle();
            result.putString(RESULT_TYPE, type.name());
            result.putString(RESULT_NOTE, note);
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        });
        return view;
    }

    public void setSubmitting(boolean submitting) { if (saveButton != null) saveButton.setEnabled(!submitting); }
    public void showError(String message) { if (errorView != null) { errorView.setText(message); errorView.setVisibility(View.VISIBLE); } }

    private CareType getCareType() {
        String value = getArguments() == null ? CareType.NOTE.name() : getArguments().getString(ARG_TYPE);
        try { return CareType.valueOf(value); } catch (Exception ignored) { return CareType.NOTE; }
    }

    private int label(CareType type) {
        switch (type) {
            case WATER: return R.string.care_water;
            case FERTILIZE: return R.string.care_fertilize;
            case REPOT: return R.string.care_repot;
            case PRUNE: return R.string.care_prune;
            default: return R.string.care_note;
        }
    }
}
