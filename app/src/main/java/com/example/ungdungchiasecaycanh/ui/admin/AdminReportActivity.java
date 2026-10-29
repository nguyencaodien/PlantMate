package com.example.ungdungchiasecaycanh.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReview;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Admin: review one report and open the reported content in its admin editor. */
public class AdminReportActivity extends AppCompatActivity {

    public static final String EXTRA_REPORT_ID = "reportId";

    private AdminReportViewModel viewModel;
    private String reportId;
    private int shownSaved, shownError;
    private TextInputLayout noteLayout;
    private TextInputEditText note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report);
        SecondaryChrome.apply(this, findViewById(R.id.adminReport), findViewById(R.id.adminReportHero));
        reportId = getIntent().getStringExtra(EXTRA_REPORT_ID);
        ((TextView) findViewById(R.id.adminReportTitle)).setText(R.string.admin_report_title);
        noteLayout = findViewById(R.id.adminReportNoteLayout);
        note = findViewById(R.id.adminReportNote);

        MaterialButton resolve = findViewById(R.id.adminReportResolve);
        MaterialButton dismiss = findViewById(R.id.adminReportDismiss);
        findViewById(R.id.adminReportBack).setOnClickListener(v -> finish());
        findViewById(R.id.adminReportRetry).setOnClickListener(v -> viewModel.load(reportId));
        resolve.setOnClickListener(v -> review(ReportReview.RESOLVED));
        dismiss.setOnClickListener(v -> review(ReportReview.DISMISSED));

        View scroll = findViewById(R.id.adminReportScroll);
        View loading = findViewById(R.id.adminReportLoading);
        View error = findViewById(R.id.adminReportError);
        viewModel = new ViewModelProvider(this).get(AdminReportViewModel.class);
        viewModel.getState().observe(this, state -> {
            boolean content = state.status == AdminEditState.Status.CONTENT;
            loading.setVisibility(state.status == AdminEditState.Status.LOADING ? View.VISIBLE : View.GONE);
            error.setVisibility(state.status == AdminEditState.Status.ERROR
                    || state.status == AdminEditState.Status.NOT_FOUND ? View.VISIBLE : View.GONE);
            scroll.setVisibility(content ? View.VISIBLE : View.GONE);
            resolve.setEnabled(!state.busy);
            dismiss.setEnabled(!state.busy);
            if (content && state.data != null) bind(state.data);
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
        viewModel.load(reportId);
    }

    private void review(String status) {
        String n = AdminDialogs.note(this, noteLayout, note);
        if (n != null) viewModel.review(status, n);
    }

    private void bind(AdminReportViewModel.Data data) {
        Report r = data.report;
        ((TextView) findViewById(R.id.adminReportSubtitle)).setText(AdminLabels.target(this, r.getTargetType()));
        ((TextView) findViewById(R.id.adminReportReason)).setText(AdminLabels.reason(this, r.getReason()));
        ((TextView) findViewById(R.id.adminReportTime)).setText(r.getCreatedAt() == null ? "" : String.valueOf(
                DateUtils.getRelativeTimeSpanString(r.getCreatedAt().toDate().getTime(), System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS)));
        ((TextView) findViewById(R.id.adminReportDetails)).setText(r.getDetails() == null || r.getDetails().trim().isEmpty()
                ? getString(R.string.admin_no_details) : r.getDetails());
        ((TextView) findViewById(R.id.adminReportReporter)).setText(getString(R.string.admin_reported_by,
                data.reporterName.isEmpty() ? getString(R.string.detail_member_fallback) : data.reporterName));

        ((TextView) findViewById(R.id.adminReportTargetType)).setText(AdminLabels.target(this, r.getTargetType()));
        ((TextView) findViewById(R.id.adminReportTargetTitle)).setText(
                data.targetTitle == null ? getString(R.string.admin_target_missing) : data.targetTitle);
        View target = findViewById(R.id.adminReportTarget);
        target.setEnabled(data.targetTitle != null);
        target.setOnClickListener(v -> openTarget(r));

        TextView reviewView = findViewById(R.id.adminReportReview);
        if (data.review == null) {
            reviewView.setVisibility(View.GONE);
        } else {
            reviewView.setVisibility(View.VISIBLE);
            int[] pill = AdminLabels.reviewPill(data.review);
            reviewView.setBackgroundResource(pill[0]);
            reviewView.setTextColor(ContextCompat.getColor(this, pill[1]));
            String who = data.reviewerName.isEmpty() ? getString(R.string.detail_member_fallback) : data.reviewerName;
            String when = data.review.getReviewedAt() == null ? "" : String.valueOf(DateUtils.getRelativeTimeSpanString(
                    data.review.getReviewedAt().toDate().getTime(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            String text = getString(R.string.admin_review_state, AdminLabels.reviewStatus(this, data.review), who, when);
            if (!data.review.getNote().isEmpty()) text += "\n" + data.review.getNote();
            reviewView.setText(text);
        }
    }

    private void openTarget(Report r) {
        switch (r.getTargetType()) {
            case USER:
                startActivity(new Intent(this, AdminMemberActivity.class).putExtra(AdminMemberActivity.EXTRA_UID, r.getTargetId()));
                break;
            case LISTING:
                startActivity(new Intent(this, AdminListingEditActivity.class).putExtra(AdminListingEditActivity.EXTRA_LISTING_ID, r.getTargetId()));
                break;
            case POST:
                startActivity(new Intent(this, AdminPostEditActivity.class).putExtra(AdminPostEditActivity.EXTRA_POST_ID, r.getTargetId()));
                break;
            default:   // a comment is managed from its post
                startActivity(new Intent(this, AdminPostEditActivity.class).putExtra(AdminPostEditActivity.EXTRA_POST_ID, r.getParentId()));
        }
    }
}
