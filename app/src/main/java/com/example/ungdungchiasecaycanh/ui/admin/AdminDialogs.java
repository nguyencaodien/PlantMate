package com.example.ungdungchiasecaycanh.ui.admin;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Confirmations and feedback shared by the admin screens. */
final class AdminDialogs {
    private AdminDialogs() {
    }

    /** Destructive confirmation; the positive button is labelled with {@code action}. */
    static void confirm(Activity activity, @StringRes int title, @StringRes int body, @StringRes int action, Runnable onConfirm) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(title)
                .setMessage(body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(action, (dialog, which) -> onConfirm.run())
                .show();
    }

    static void snack(View anchor, @StringRes int message) {
        Snackbar.make(anchor, message, Snackbar.LENGTH_SHORT).show();
    }

    static String text(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString().trim();
    }

    /** Shows (or clears) a field error; returns true when there was an error. */
    static boolean error(Activity activity, TextInputLayout layout, Integer message) {
        layout.setError(message == null || message == 0 ? null : activity.getString(message));
        return message != null && message != 0;
    }

    /** Validates the optional log note; returns the note or null when it is too long. */
    static String note(Activity activity, TextInputLayout layout, TextInputEditText input) {
        String note = text(input);
        int error = AdminValidation.note(note);
        layout.setError(error == 0 ? null : activity.getString(error));
        return error == 0 ? note : null;
    }
}
