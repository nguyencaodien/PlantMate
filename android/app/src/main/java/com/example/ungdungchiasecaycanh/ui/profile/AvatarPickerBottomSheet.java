package com.example.ungdungchiasecaycanh.ui.profile;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Avatar picker: a photo from the device, one of the plant illustrations, or the initials.
 * The choice goes to EditProfileViewModel and is saved with the profile.
 */
public class AvatarPickerBottomSheet extends BottomSheetDialogFragment {
    public static final String TAG = "avatar_picker";

    private EditProfileViewModel viewModel;
    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) return;
                viewModel.setAvatar(uri.toString());
                dismiss();
            });

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_avatar_picker, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(EditProfileViewModel.class);
        View content = ((ViewGroup) view).getChildAt(0);
        int basePadding = content.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(), content.getPaddingRight(), basePadding + bottom);
            return insets;
        });

        view.findViewById(R.id.avatarPickPhoto).setOnClickListener(v -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        view.findViewById(R.id.avatarUseInitials).setOnClickListener(v -> { viewModel.setAvatar(""); dismiss(); });

        LinearLayout grid = view.findViewById(R.id.avatarPresetGrid);
        LinearLayout row = null;
        String current = viewModel.getAvatar().getValue();
        float dp = getResources().getDisplayMetrics().density;
        int size = (int) (64 * dp);
        for (int i = 0; i < AvatarBinder.PRESETS.length; i++) {
            AvatarBinder.Preset preset = AvatarBinder.PRESETS[i];
            boolean selected = preset.ref().equals(current);
            FrameLayout cell = new FrameLayout(requireContext());
            int pad = (int) (4 * dp);
            cell.setPadding(pad, pad, pad, pad);
            cell.setBackgroundResource(selected ? R.drawable.bg_avatar_selected_ring : 0);
            View avatar = new View(requireContext());
            avatar.setBackground(AvatarBinder.presetDrawable(requireContext(), preset));
            cell.addView(avatar, new FrameLayout.LayoutParams(size, size));
            cell.setClickable(true);
            cell.setFocusable(true);
            cell.setForeground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_ripple_circle));
            cell.setContentDescription(getString(R.string.avatar_preset_desc, i + 1, AvatarBinder.PRESETS.length));
            cell.setSelected(selected);
            cell.setOnClickListener(v -> { viewModel.setAvatar(preset.ref()); dismiss(); });
            if (i % 4 == 0) {
                row = new LinearLayout(requireContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = (int) (8 * dp);
                grid.addView(row, rowParams);
            }
            FrameLayout slot = new FrameLayout(requireContext());
            FrameLayout.LayoutParams cellParams = new FrameLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            slot.addView(cell, cellParams);
            row.addView(slot, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
    }

    @Override public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) getDialog()).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
}
