package com.example.ungdungchiasecaycanh.ui.admin;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.Member;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.ui.listing.MyListingAdapter;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

/** Admin: a member's profile, their content counts, profile edit and suspension. */
public class AdminMemberActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid";

    private AdminMemberViewModel viewModel;
    private String uid;
    private boolean formBound;
    private int shownSaved, shownError;
    private TextInputLayout nameLayout, locationLayout, bioLayout, noteLayout;
    private TextInputEditText name, location, bio, note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_member);
        SecondaryChrome.apply(this, findViewById(R.id.adminMember), findViewById(R.id.adminMemberHero));
        uid = getIntent().getStringExtra(EXTRA_UID);
        nameLayout = findViewById(R.id.adminMemberNameLayout);
        locationLayout = findViewById(R.id.adminMemberLocationLayout);
        bioLayout = findViewById(R.id.adminMemberBioLayout);
        noteLayout = findViewById(R.id.adminMemberNoteLayout);
        name = findViewById(R.id.adminMemberName);
        location = findViewById(R.id.adminMemberLocation);
        bio = findViewById(R.id.adminMemberBio);
        note = findViewById(R.id.adminMemberNote);

        MaterialButton save = findViewById(R.id.adminMemberSave);
        MaterialButton suspend = findViewById(R.id.adminMemberSuspend);
        findViewById(R.id.adminMemberBack).setOnClickListener(v -> finish());
        findViewById(R.id.adminMemberRetry).setOnClickListener(v -> viewModel.load(uid));
        save.setOnClickListener(v -> {
            Map<String, Integer> errors = AdminValidation.member(AdminDialogs.text(name), AdminDialogs.text(location), AdminDialogs.text(bio));
            boolean invalid = AdminDialogs.error(this, nameLayout, errors.get(AdminValidation.DISPLAY_NAME));
            invalid |= AdminDialogs.error(this, locationLayout, errors.get(AdminValidation.LOCATION));
            invalid |= AdminDialogs.error(this, bioLayout, errors.get(AdminValidation.BIO));
            String n = AdminDialogs.note(this, noteLayout, note);
            if (!invalid && n != null) {
                viewModel.save(AdminDialogs.text(name), AdminDialogs.text(location), AdminDialogs.text(bio), n);
            }
        });

        View scroll = findViewById(R.id.adminMemberScroll);
        View loading = findViewById(R.id.adminMemberLoading);
        View error = findViewById(R.id.adminMemberError);
        viewModel = new ViewModelProvider(this).get(AdminMemberViewModel.class);
        viewModel.getState().observe(this, state -> {
            boolean content = state.status == AdminEditState.Status.CONTENT;
            loading.setVisibility(state.status == AdminEditState.Status.LOADING ? View.VISIBLE : View.GONE);
            error.setVisibility(state.status == AdminEditState.Status.ERROR
                    || state.status == AdminEditState.Status.NOT_FOUND ? View.VISIBLE : View.GONE);
            scroll.setVisibility(content ? View.VISIBLE : View.GONE);
            save.setEnabled(!state.busy);
            if (content && state.data != null) bind(state.data, suspend, state.busy);
            if (state.savedVersion > shownSaved) {
                shownSaved = state.savedVersion;
                note.setText("");
                AdminDialogs.snack(scroll, R.string.admin_saved);
            }
            if (state.errorVersion > shownError) {
                shownError = state.errorVersion;
                AdminDialogs.snack(scroll, R.string.admin_save_error);
            }
        });
        viewModel.load(uid);
    }

    /** Listings and posts by this member; each row opens its admin editor. */
    private void bindContent(AdminMemberViewModel.Data data) {
        LinearLayout listings = findViewById(R.id.adminMemberListings);
        LinearLayout posts = findViewById(R.id.adminMemberPosts);
        listings.removeAllViews();
        posts.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Listing l : data.listings) {
            View row = inflater.inflate(R.layout.item_admin_row, listings, false);
            ((TextView) row.findViewById(R.id.adminRowTitle)).setText(l.getNickname());
            ((TextView) row.findViewById(R.id.adminRowSubtitle)).setText(AdminLabels.type(this, l.getType()));
            row.findViewById(R.id.adminRowMeta).setVisibility(View.GONE);
            row.findViewById(R.id.adminRowInitials).setVisibility(View.GONE);
            ImageView icon = row.findViewById(R.id.adminRowIcon);
            icon.setVisibility(View.VISIBLE);
            icon.setImageResource(R.drawable.ic_leaf);
            RemoteImageLoader.load(row.findViewById(R.id.adminRowPhoto),
                    l.getCoverImageUrl(), (int) (56 * getResources().getDisplayMetrics().density));
            TextView pill = row.findViewById(R.id.adminRowPill);
            pill.setVisibility(View.VISIBLE);
            MyListingAdapter.bindStatusPill(pill, l.getStatus());
            pill.setCompoundDrawables(null, null, null, null);
            row.setOnClickListener(v -> startActivity(new Intent(this, AdminListingEditActivity.class)
                    .putExtra(AdminListingEditActivity.EXTRA_LISTING_ID, l.getListingId())));
            listings.addView(row);
        }
        for (Post p : data.posts) {
            View row = inflater.inflate(R.layout.item_admin_row, posts, false);
            ((TextView) row.findViewById(R.id.adminRowTitle)).setText(p.getText());
            ((TextView) row.findViewById(R.id.adminRowSubtitle)).setText(AdminLabels.category(this, p.getCategory()));
            row.findViewById(R.id.adminRowMeta).setVisibility(View.GONE);
            row.findViewById(R.id.adminRowInitials).setVisibility(View.GONE);
            ImageView icon = row.findViewById(R.id.adminRowIcon);
            icon.setVisibility(View.VISIBLE);
            icon.setImageResource(R.drawable.ic_chat);
            row.setOnClickListener(v -> startActivity(new Intent(this, AdminPostEditActivity.class)
                    .putExtra(AdminPostEditActivity.EXTRA_POST_ID, p.getPostId())));
            posts.addView(row);
        }
        if (data.listings.isEmpty()) listings.addView(emptyLine());
        if (data.posts.isEmpty()) posts.addView(emptyLine());
    }

    private TextView emptyLine() {
        TextView empty = new TextView(this);
        empty.setText(R.string.admin_member_none);
        empty.setTextAppearance(R.style.TextAppearance_PlantMate_Meta);
        return empty;
    }

    private void bind(AdminMemberViewModel.Data data, MaterialButton suspend, boolean busy) {
        Member m = data.member;
        String display = m.getDisplayName().isEmpty() ? getString(R.string.detail_member_fallback) : m.getDisplayName();
        ((TextView) findViewById(R.id.adminMemberTitle)).setText(display);
        ((TextView) findViewById(R.id.adminMemberUid)).setText(data.self ? getString(R.string.admin_you) : m.getUid());
        TextView status = findViewById(R.id.adminMemberStatus);
        status.setText(m.isSuspended() ? R.string.admin_member_suspended : R.string.admin_member_active);
        status.setBackgroundResource(m.isSuspended() ? R.drawable.bg_status_reserved : R.drawable.bg_status_active);
        status.setTextColor(ContextCompat.getColor(this, m.isSuspended() ? R.color.status_reserved_ink : R.color.status_active_ink));
        ((TextView) findViewById(R.id.adminMemberCounts)).setText(
                getString(R.string.admin_member_counts, data.listingCount, data.postCount));

        suspend.setText(m.isSuspended() ? R.string.admin_lift_suspension : R.string.admin_suspend_member);
        int color = ContextCompat.getColor(this, m.isSuspended() ? R.color.color_success : R.color.color_danger);
        suspend.setTextColor(color);
        suspend.setIconTint(ColorStateList.valueOf(color));
        suspend.setStrokeColor(ColorStateList.valueOf(color));
        suspend.setEnabled(!busy);
        // An admin cannot suspend themselves (the rules deny it too), so the action is hidden.
        suspend.setVisibility(data.self ? View.GONE : View.VISIBLE);
        suspend.setOnClickListener(v -> {
            String n = AdminDialogs.note(this, noteLayout, note);
            if (n == null) return;
            if (m.isSuspended()) viewModel.setSuspended(false, n);
            else AdminDialogs.confirm(this, R.string.admin_suspend_confirm_title, R.string.admin_suspend_confirm_body,
                    R.string.admin_suspend_member, () -> viewModel.setSuspended(true, n));
        });

        bindRole(data, display, busy);

        bindContent(data);

        if (formBound) return;
        formBound = true;
        name.setText(m.getDisplayName());
        location.setText(m.getProfile() == null ? "" : m.getProfile().getLocationText());
        bio.setText(m.getProfile() == null ? "" : m.getProfile().getBio());
    }

    /** The admin role switch: a confirmed change, never for the admin's own account (the rules refuse it too). */
    private void bindRole(AdminMemberViewModel.Data data, String display, boolean busy) {
        com.google.android.material.materialswitch.MaterialSwitch role = findViewById(R.id.adminMemberRoleSwitch);
        boolean admin = data.member.isAdmin();
        role.setOnCheckedChangeListener(null);
        role.setChecked(admin);
        role.setEnabled(!busy && !data.self);
        ((TextView) findViewById(R.id.adminMemberRoleSub)).setText(data.self ? R.string.admin_role_self : R.string.admin_role_sub);
        role.setOnCheckedChangeListener((button, checked) -> {
            button.setChecked(admin);   // only the confirmed change sticks
            String n = AdminDialogs.note(this, noteLayout, note);
            if (n == null) return;
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle(admin ? R.string.admin_revoke_title : R.string.admin_grant_title)
                    .setMessage(getString(admin ? R.string.admin_revoke_body : R.string.admin_grant_body, display))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(admin ? R.string.admin_revoke : R.string.admin_grant,
                            (dialog, which) -> viewModel.setAdmin(!admin, n))
                    .show();
        });
    }
}
