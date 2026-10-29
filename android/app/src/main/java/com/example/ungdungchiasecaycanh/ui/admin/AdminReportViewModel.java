package com.example.ungdungchiasecaycanh.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.AdminRepository;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.Member;
import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReview;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin view of one report: details, the reported content (reloaded) and the review decision. */
public class AdminReportViewModel extends ViewModel {

    public static final class Data {
        public final Report report;
        public final ReportReview review;
        public final String reporterName;
        public final String reviewerName;
        public final String targetTitle;   // null when the target no longer exists

        Data(Report report, ReportReview review, String reporterName, String reviewerName, String targetTitle) {
            this.report = report;
            this.review = review;
            this.reporterName = reporterName;
            this.reviewerName = reviewerName;
            this.targetTitle = targetTitle;
        }
    }

    private final AdminRepository repository = new AdminRepository();
    private final ListingRepository listings = new ListingRepository();
    private final MutableLiveData<AdminEditState<Data>> state = new MutableLiveData<>(AdminEditState.loading());
    private String reportId;

    public LiveData<AdminEditState<Data>> getState() { return state; }

    public void load(String reportId) {
        this.reportId = reportId;
        if (current().data == null) state.setValue(AdminEditState.loading());
        Task<List<Report>> reports = repository.getReports();
        Task<Map<String, ReportReview>> reviews = repository.getReportReviews();
        Task<List<Member>> members = repository.getMembers();
        Tasks.whenAllSuccess(reports, reviews, members).addOnSuccessListener(ignored -> {
            Report report = null;
            for (Report r : reports.getResult()) if (r.getReportId().equals(reportId)) report = r;
            if (report == null) {
                state.postValue(current().withStatus(AdminEditState.Status.NOT_FOUND, null));
                return;
            }
            Map<String, String> names = new HashMap<>();
            for (Member m : members.getResult()) names.put(m.getUid(), m.getDisplayName());
            Report found = report;
            ReportReview review = reviews.getResult().get(reportId);
            targetTitle(found, names).addOnCompleteListener(title -> state.postValue(current().withStatus(
                    AdminEditState.Status.CONTENT, new Data(found, review,
                    orEmpty(names.get(found.getReporterId())),
                    review == null ? "" : orEmpty(names.get(review.getAdminId())),
                    title.isSuccessful() ? title.getResult() : null))));
        }).addOnFailureListener(e -> state.postValue(current().withStatus(AdminEditState.Status.ERROR, null)));
    }

    public void review(String status, String note) {
        state.setValue(current().busy());
        repository.reviewReport(reportId, status, note)
                .addOnSuccessListener(ignored -> { state.postValue(current().saved(null)); load(reportId); })
                .addOnFailureListener(e -> state.postValue(current().failed()));
    }

    private Task<String> targetTitle(Report r, Map<String, String> names) {
        switch (r.getTargetType()) {
            case USER:
                return names.containsKey(r.getTargetId()) ? Tasks.forResult(orEmpty(names.get(r.getTargetId())))
                        : Tasks.forException(new IllegalStateException("missing"));
            case LISTING:
                return listings.getListing(r.getTargetId()).continueWith(t -> t.getResult().getNickname());
            case POST:
                return repository.getPost(r.getTargetId()).continueWith(t -> excerpt(t.getResult().getText()));
            default:
                return repository.getComments(r.getParentId()).continueWith(t -> {
                    for (Comment c : t.getResult()) if (c.getCommentId().equals(r.getTargetId())) return excerpt(c.getText());
                    throw new IllegalStateException("missing");
                });
        }
    }

    private static String excerpt(String text) {
        if (text == null) return "";
        String t = text.trim();
        return t.length() > 120 ? t.substring(0, 120) + "…" : t;
    }

    private static String orEmpty(String value) { return value == null ? "" : value; }

    private AdminEditState<Data> current() {
        AdminEditState<Data> value = state.getValue();
        return value == null ? AdminEditState.loading() : value;
    }
}
