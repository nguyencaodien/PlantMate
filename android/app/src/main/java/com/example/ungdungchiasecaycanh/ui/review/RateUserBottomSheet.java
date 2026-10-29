package com.example.ungdungchiasecaycanh.ui.review;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.ungdungchiasecaycanh.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RateUserBottomSheet extends BottomSheetDialogFragment {
    public interface Listener { void onSubmitReview(int rating, String comment); }
    private static final String ARG_NAME = "peerName";
    private MaterialButton submit;

    public static RateUserBottomSheet newInstance(String peerName) {
        RateUserBottomSheet sheet = new RateUserBottomSheet();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_NAME, peerName);
        sheet.setArguments(arguments);
        return sheet;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                   @Nullable ViewGroup container,
                                                   @Nullable Bundle state) {
        return inflater.inflate(R.layout.bottom_sheet_rate_user, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        String peerName = getArguments() == null ? null : getArguments().getString(ARG_NAME);
        ((TextView) view.findViewById(R.id.rate_member_name)).setText(
                peerName == null ? getString(R.string.detail_member_fallback) : peerName);
        RatingBar rating = view.findViewById(R.id.rate_rating);
        TextInputLayout commentLayout = view.findViewById(R.id.rate_comment_layout);
        TextInputEditText comment = view.findViewById(R.id.rate_comment);
        submit = view.findViewById(R.id.rate_submit);
        view.findViewById(R.id.rate_cancel).setOnClickListener(button -> dismiss());
        submit.setOnClickListener(button -> {
            String text = comment.getText() == null ? "" : comment.getText().toString().trim();
            if (rating.getRating() < 1) {
                commentLayout.setError(getString(R.string.review_rating_required));
                return;
            }
            if (text.length() > 500) {
                commentLayout.setError(getString(R.string.review_comment_too_long));
                return;
            }
            commentLayout.setError(null);
            if (requireActivity() instanceof Listener) {
                ((Listener) requireActivity()).onSubmitReview((int) rating.getRating(), text);
            }
        });
    }

    public void setSubmitting(boolean submitting) {
        if (submit != null) submit.setEnabled(!submitting);
        setCancelable(!submitting);
    }
}
