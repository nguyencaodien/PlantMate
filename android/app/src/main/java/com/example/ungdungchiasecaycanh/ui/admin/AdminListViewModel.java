package com.example.ungdungchiasecaycanh.ui.admin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.AdminRepository;
import com.example.ungdungchiasecaycanh.model.AdminAction;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.Member;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReview;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads one admin list (listings, posts, members, reports or the log) and filters it locally. */
public class AdminListViewModel extends AndroidViewModel {

    public enum Kind { LISTINGS, POSTS, MEMBERS, REPORTS, LOG }
    public enum Status { LOADING, CONTENT, EMPTY, ERROR }

    public static final String FILTER_OPEN = "OPEN";
    public static final String FILTER_ALL = "ALL";

    public static final class State {
        public final Status status;
        public final List<AdminRow> rows;
        State(Status status, List<AdminRow> rows) { this.status = status; this.rows = rows; }
    }

    private final AdminRepository repository = new AdminRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.LOADING, new ArrayList<>()));
    private List<AdminRow> all = new ArrayList<>();
    private Kind kind = Kind.LISTINGS;
    private String query = "";
    private String filter = FILTER_ALL;

    public AdminListViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<State> getState() { return state; }

    public void load(Kind kind) {
        this.kind = kind;
        state.setValue(new State(Status.LOADING, new ArrayList<>()));
        Task<List<Member>> members = repository.getMembers();
        Task<List<AdminRow>> rows;
        switch (kind) {
            case POSTS:
                Task<List<Post>> posts = repository.getAllPosts();
                rows = Tasks.whenAllSuccess(members, posts).continueWith(t -> postRows(posts.getResult(), names(members.getResult())));
                break;
            case MEMBERS:
                rows = members.continueWith(t -> memberRows(members.getResult()));
                break;
            case REPORTS:
                Task<List<Report>> reports = repository.getReports();
                Task<Map<String, ReportReview>> reviews = repository.getReportReviews();
                rows = Tasks.whenAllSuccess(members, reports, reviews).continueWith(t ->
                        reportRows(reports.getResult(), reviews.getResult(), names(members.getResult())));
                break;
            case LOG:
                Task<List<AdminAction>> actions = repository.getActions();
                rows = Tasks.whenAllSuccess(members, actions).continueWith(t -> logRows(actions.getResult(), names(members.getResult())));
                break;
            default:
                Task<List<Listing>> listings = repository.getAllListings();
                rows = Tasks.whenAllSuccess(members, listings).continueWith(t -> listingRows(listings.getResult(), names(members.getResult())));
        }
        rows.addOnSuccessListener(result -> { all = result; publish(); })
                .addOnFailureListener(error -> state.postValue(new State(Status.ERROR, new ArrayList<>())));
    }

    public void setQuery(String query) {
        this.query = query == null ? "" : query;
        publish();
    }

    public void setFilter(String filter) {
        this.filter = filter;
        publish();
    }

    private void publish() {
        List<AdminRow> visible = new ArrayList<>();
        for (AdminRow row : all) {
            if (!FILTER_ALL.equals(filter) && !filter.equals(row.filterKey)) continue;
            if (!AdminValidation.matches(query, row.searchText)) continue;
            visible.add(row);
        }
        state.postValue(new State(visible.isEmpty() ? Status.EMPTY : Status.CONTENT, visible));
    }

    // ---------------- row builders ----------------

    private List<AdminRow> listingRows(List<Listing> listings, Map<String, String> names) {
        List<AdminRow> rows = new ArrayList<>();
        for (Listing l : listings) {
            String subtitle = join(l.getSpecies(), AdminLabels.type(ctx(), l.getType()));
            String meta = join(name(names, l.getOwnerId()), l.getLocationText());
            rows.add(new AdminRow(l.getListingId(), "", l.getNickname(), subtitle, meta, millis(l.getCreatedAt()),
                    l.getCoverImageUrl(), "", R.drawable.ic_leaf, "", 0, 0, l.getStatus(),
                    l.getStatus() == null ? "" : l.getStatus().name()));
        }
        return sorted(rows);
    }

    private List<AdminRow> postRows(List<Post> posts, Map<String, String> names) {
        List<AdminRow> rows = new ArrayList<>();
        for (Post p : posts) {
            String author = name(names, p.getAuthorId());
            rows.add(new AdminRow(p.getPostId(), "", p.getText(), author, AdminLabels.category(ctx(), p.getCategory()),
                    millis(p.getCreatedAt()), p.getImageUrl(), SecondaryChrome.initials(author), 0,
                    "", 0, 0, null, p.getCategory() == null ? "" : p.getCategory().name()));
        }
        return sorted(rows);
    }

    private List<AdminRow> memberRows(List<Member> members) {
        List<AdminRow> rows = new ArrayList<>();
        for (Member m : members) {
            String name = m.getDisplayName().isEmpty() ? ctx().getString(R.string.detail_member_fallback) : m.getDisplayName();
            boolean self = m.getUid().equals(repository.getCurrentUserId());
            String pill = m.isSuspended() ? ctx().getString(R.string.admin_member_suspended)
                    : m.isAdmin() ? ctx().getString(R.string.admin_role_tag)
                    : self ? ctx().getString(R.string.admin_you) : "";
            int background = m.isSuspended() ? R.drawable.bg_status_reserved
                    : m.isAdmin() ? R.drawable.bg_status_admin : R.drawable.bg_status_rehomed;
            int ink = m.isSuspended() ? R.color.status_reserved_ink
                    : m.isAdmin() ? R.color.nav_doctor_icon : R.color.status_rehomed_ink;
            rows.add(new AdminRow(m.getUid(), "", name, m.getProfile() == null ? "" : m.getProfile().getLocationText(),
                    shortId(m.getUid()), 0, "", SecondaryChrome.initials(name), 0, pill, background, ink, null, ""));
        }
        Collections.sort(rows, (a, b) -> AdminValidation.fold(a.title).compareTo(AdminValidation.fold(b.title)));
        return rows;
    }

    private List<AdminRow> reportRows(List<Report> reports, Map<String, ReportReview> reviews, Map<String, String> names) {
        List<AdminRow> rows = new ArrayList<>();
        for (Report r : reports) {
            ReportReview review = reviews.get(r.getReportId());
            int[] pill = AdminLabels.reviewPill(review);
            String title = AdminLabels.reason(ctx(), r.getReason()) + " · " + AdminLabels.target(ctx(), r.getTargetType());
            String details = r.getDetails() == null || r.getDetails().trim().isEmpty()
                    ? ctx().getString(R.string.admin_no_details) : r.getDetails();
            String key = review == null ? FILTER_OPEN : review.getStatus();
            rows.add(new AdminRow(r.getReportId(), "", title, details,
                    ctx().getString(R.string.admin_reported_by, name(names, r.getReporterId())), millis(r.getCreatedAt()),
                    "", "", R.drawable.ic_flag, AdminLabels.reviewStatus(ctx(), review), pill[0], pill[1], null, key));
        }
        return sorted(rows);
    }

    private List<AdminRow> logRows(List<AdminAction> actions, Map<String, String> names) {
        List<AdminRow> rows = new ArrayList<>();
        for (AdminAction a : actions) {
            rows.add(new AdminRow(a.getActionId(), "", AdminLabels.action(ctx(), a), a.getNote(),
                    name(names, a.getAdminId()), millis(a.getCreatedAt()), "", "", R.drawable.ic_history_list,
                    "", 0, 0, null, ""));
        }
        return sorted(rows);
    }

    // ---------------- helpers ----------------

    private Application ctx() { return getApplication(); }

    private Map<String, String> names(List<Member> members) {
        Map<String, String> names = new HashMap<>();
        for (Member m : members) names.put(m.getUid(), m.getDisplayName());
        return names;
    }

    private String name(Map<String, String> names, String uid) {
        String name = uid == null ? null : names.get(uid);
        return name == null || name.trim().isEmpty() ? ctx().getString(R.string.detail_member_fallback) : name;
    }

    private static String join(String a, String b) {
        boolean hasA = a != null && !a.trim().isEmpty(), hasB = b != null && !b.trim().isEmpty();
        if (hasA && hasB) return a.trim() + " · " + b.trim();
        return hasA ? a.trim() : hasB ? b.trim() : "";
    }

    private static String shortId(String uid) {
        return uid == null ? "" : uid.length() > 10 ? uid.substring(0, 10) + "…" : uid;
    }

    private static long millis(Timestamp t) {
        return t == null ? 0 : t.toDate().getTime();
    }

    private static List<AdminRow> sorted(List<AdminRow> rows) {
        Collections.sort(rows, (a, b) -> Long.compare(b.timeMillis, a.timeMillis));
        return rows;
    }
}
