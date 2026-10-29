package com.example.ungdungchiasecaycanh.ui.settings;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.ui.admin.AdminHomeActivity;
import com.example.ungdungchiasecaycanh.ui.safety.BlockedUsersActivity;
import com.example.ungdungchiasecaycanh.util.Motion;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

public class SettingsFragment extends PreferenceFragmentCompat {

    /** Cards sit on the paper background with the design's 20dp gutters; no list dividers. */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDivider(null);
        float density = getResources().getDisplayMetrics().density;
        RecyclerView list = getListView();
        list.setPadding((int) (20 * density), 0, (int) (20 * density), (int) (24 * density));
        list.setClipToPadding(false);
        Motion.staggerRows(list, 160, 40);
    }

    private SettingsViewModel viewModel;
    private SwitchPreferenceCompat careNotifications;
    private int shownErrorVersion;

    private static final String[] LANGUAGE_TAGS = {
            SettingsRepository.LANGUAGE_SYSTEM,
            SettingsRepository.LANGUAGE_ENGLISH,
            SettingsRepository.LANGUAGE_VIETNAMESE};

    /** Language row: shows the current choice and opens a single-choice dialog. */
    private void bindLanguage(Preference language) {
        if (language == null) return;
        String[] labels = {getString(R.string.settings_language_system),
                getString(R.string.settings_language_english), getString(R.string.settings_language_vietnamese)};
        int current = Arrays.asList(LANGUAGE_TAGS).indexOf(viewModel.getLanguage());
        int selected = Math.max(0, current);
        language.setSummary(labels[selected]);
        language.setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.settings_language)
                    .setSingleChoiceItems(labels, selected, (dialog, which) -> {
                        dialog.dismiss();
                        viewModel.setLanguage(LANGUAGE_TAGS[which]);
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return true;
        });
    }

    @Override public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_settings, rootKey);
        careNotifications = findPreference("care_notifications_enabled_ui");
        if (careNotifications != null) {
            careNotifications.setPersistent(false);
            careNotifications.setOnPreferenceChangeListener((preference, newValue) -> {
                viewModel.setCareNotificationsEnabled(Boolean.TRUE.equals(newValue));
                return false;
            });
        }
        Preference systemSettings = findPreference("notification_settings_action");
        Preference deleteAccount = findPreference("delete_account_action");
        if (deleteAccount != null) deleteAccount.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(), DeleteAccountActivity.class)); return true;
        });
        Preference blockedUsers = findPreference("blocked_users_action");
        if (blockedUsers != null) blockedUsers.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(), BlockedUsersActivity.class)); return true;
        });
        if (systemSettings != null) systemSettings.setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
            startActivity(intent); return true;
        });
        // Admin tools appear only for allowlisted admins (admins/{uid}); the rules enforce it anyway.
        Preference adminCategory = findPreference("admin_category");
        Preference adminTools = findPreference("admin_tools_action");
        if (adminTools != null) adminTools.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(),
                    AdminHomeActivity.class)); return true;
        });
        Preference about = findPreference("about_plantmate");
        if (about != null) about.setOnPreferenceClickListener(preference -> {
            startActivity(new Intent(requireContext(), AboutActivity.class)); return true;
        });
        Preference version = findPreference("app_version_info");
        if (version != null) {
            try {
                version.setSummary(requireContext().getPackageManager()
                        .getPackageInfo(requireContext().getPackageName(), 0).versionName);
            } catch (PackageManager.NameNotFoundException ignored) {
                version.setSummary("");
            }
        }
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        bindLanguage(findPreference("app_language"));
        viewModel.isAdmin().observe(this, isAdmin -> {
            if (adminCategory != null) adminCategory.setVisible(Boolean.TRUE.equals(isAdmin));
        });
        viewModel.getState().observe(this, state -> {
            if (!state.authenticated) { requireActivity().finish(); return; }
            if (careNotifications != null) {
                careNotifications.setChecked(state.careNotificationsEnabled);
                careNotifications.setEnabled(!state.saving);
            }
            if (state.errorVersion > shownErrorVersion) {
                shownErrorVersion = state.errorVersion;
                Toast.makeText(requireContext(), R.string.settings_update_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
