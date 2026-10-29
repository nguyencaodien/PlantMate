package com.example.ungdungchiasecaycanh.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Home filter sheet: pick-up areas and "with photos" only (the type chips and the sort menu on Home
 * own the rest). Edits a draft with a live match count; "Show N plants" applies it and closes.
 * The draft survives rotation.
 */
public class HomeFiltersBottomSheet extends BottomSheetDialogFragment {
    public static final String TAG = "home_filters";
    private static final String KEY_AREAS = "areas";
    private static final String KEY_PHOTOS = "photos";

    private HomeViewModel viewModel;
    private final Set<String> areas = new LinkedHashSet<>();
    private boolean photos;
    private boolean binding;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                 @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.bottom_sheet_home_filters, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        // Keep the apply button above the gesture / navigation bar.
        View content = ((ViewGroup) view).getChildAt(0);
        int basePadding = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(),
                    basePadding + bottom);
            return insets;
        });
        viewModel = new ViewModelProvider(requireParentFragment()).get(HomeViewModel.class);
        HomeViewModel.Refine current = viewModel.getRefine().getValue();
        if (state != null) {
            ArrayList<String> savedAreas = state.getStringArrayList(KEY_AREAS);
            if (savedAreas != null) areas.addAll(savedAreas);
            photos = state.getBoolean(KEY_PHOTOS);
        } else if (current != null) {
            areas.addAll(current.areas);
            photos = current.withPhotos;
        }

        buildAreaChips(view.findViewById(R.id.filtersArea), viewModel.availableAreas());
        ((MaterialSwitch) view.findViewById(R.id.filtersPhotos)).setOnCheckedChangeListener((b, checked) -> {
            if (binding) return;
            photos = checked;
            updateCount(view);
        });
        view.findViewById(R.id.filtersReset).setOnClickListener(v -> {
            areas.clear();
            photos = false;
            bind(view);
        });
        view.findViewById(R.id.filtersApply).setOnClickListener(v -> {
            viewModel.setRefine(new HomeViewModel.Refine(areas, currentSort(), photos));
            getParentFragmentManager().setFragmentResult(TAG, new Bundle());
            dismiss();
        });
        bind(view);
    }

    /** Opens fully so the apply button is always reachable without dragging. */
    @Override public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) getDialog()).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(KEY_AREAS, new ArrayList<>(areas));
        outState.putBoolean(KEY_PHOTOS, photos);
    }

    /** The sort chosen on Home stays as it is. */
    private HomeViewModel.Sort currentSort() {
        HomeViewModel.Refine current = viewModel.getRefine().getValue();
        return current == null ? HomeViewModel.Sort.NEWEST : current.sort;
    }

    private void buildAreaChips(ChipGroup group, List<String> available) {
        group.addView(areaChip(group, getString(R.string.home_filters_any_area), ""));
        for (String value : available) group.addView(areaChip(group, value, value));
    }

    private Chip areaChip(ChipGroup group, String label, String value) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_filter_chip, group, false);
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(value);
        chip.setOnClickListener(v -> {
            if (value.isEmpty()) {
                areas.clear();
            } else if (!removeArea(value)) {
                areas.add(value);
            }
            bind(requireView());
        });
        return chip;
    }

    /** Removes {@code value} however it is spelled ("Quận 7" = "quan 7"); true when it was chosen. */
    private boolean removeArea(String value) {
        String key = HomeViewModel.normalize(value);
        for (String a : areas) {
            if (HomeViewModel.normalize(a).equals(key)) {
                areas.remove(a);
                return true;
            }
        }
        return false;
    }

    /** Checks the chips from the draft: "Any area" when no area is chosen. */
    private void bind(View view) {
        binding = true;
        Set<String> chosen = new LinkedHashSet<>();
        for (String a : areas) chosen.add(HomeViewModel.normalize(a));
        ChipGroup areaGroup = view.findViewById(R.id.filtersArea);
        for (int i = 0; i < areaGroup.getChildCount(); i++) {
            Chip chip = (Chip) areaGroup.getChildAt(i);
            String value = (String) chip.getTag();
            chip.setChecked(value.isEmpty() ? areas.isEmpty() : chosen.contains(HomeViewModel.normalize(value)));
        }
        ((MaterialSwitch) view.findViewById(R.id.filtersPhotos)).setChecked(photos);
        binding = false;
        updateCount(view);
    }

    private void updateCount(View view) {
        int count = viewModel.countFor(viewModel.getSelectedTypes(), new HomeViewModel.Refine(areas, currentSort(), photos));
        MaterialButton apply = view.findViewById(R.id.filtersApply);
        apply.setText(getResources().getQuantityString(R.plurals.home_filters_apply, count, count));
    }
}
