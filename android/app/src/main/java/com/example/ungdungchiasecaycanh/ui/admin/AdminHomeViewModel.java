package com.example.ungdungchiasecaycanh.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.AdminRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.Member;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportReview;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/** Admin dashboard: checks admin access, then loads everything and aggregates it for the charts. */
public class AdminHomeViewModel extends ViewModel {

    public static final int DAYS = 14;

    public enum Status { LOADING, CONTENT, NOT_ADMIN, ERROR }

    /** Overview period; ALL has no previous period to compare with. */
    public enum Period {
        TODAY(1), WEEK(7), MONTH(30), ALL(0);
        public final int days;
        Period(int days) { this.days = days; }
    }

    /** Counts inside one period and the activity (new listings + posts) of the period before it. */
    public static final class Window {
        public final long start;
        public int give, trade, sell, listings, posts, members, previousActivity;
        Window(long start) { this.start = start; }
        public int activity() { return listings + posts; }
    }

    public static final class Dashboard {
        public final int listings, posts, members, openReports, resolvedReports, dismissedReports;
        public final int[] newListingsPerDay, newPostsPerDay;
        public final long[] dayStarts;
        public final Map<ListingType, Integer> byType;
        public final Map<ListingStatus, Integer> byStatus;
        public final Map<PostCategory, Integer> byCategory;
        public final Map<ReportReason, Integer> byReason;
        public final List<Member> newestMembers;
        /** New member profiles and new reports per day, same window as the activity chart. */
        public int[] newMembersPerDay = new int[0];
        public int[] newReportsPerDay = new int[0];
        /** Reasons of the reports still waiting, the newest two of them, and the newest listing with a photo. */
        public Map<ReportReason, Integer> openByReason = Collections.emptyMap();
        public List<Preview> openPreviews = Collections.emptyList();
        public Listing newestListing;
        public String newestOwner = "";
        /** Creation times behind the period counts (types line up with listingTimes). */
        public long[] listingTimes = new long[0], postTimes = new long[0], memberTimes = new long[0];
        public ListingType[] listingTypes = new ListingType[0];

        Dashboard(int listings, int posts, int members, int openReports, int resolvedReports, int dismissedReports,
                  int[] newListingsPerDay, int[] newPostsPerDay, long[] dayStarts,
                  Map<ListingType, Integer> byType, Map<ListingStatus, Integer> byStatus,
                  Map<PostCategory, Integer> byCategory, Map<ReportReason, Integer> byReason,
                  List<Member> newestMembers) {
            this.listings = listings;
            this.posts = posts;
            this.members = members;
            this.openReports = openReports;
            this.resolvedReports = resolvedReports;
            this.dismissedReports = dismissedReports;
            this.newListingsPerDay = newListingsPerDay;
            this.newPostsPerDay = newPostsPerDay;
            this.dayStarts = dayStarts;
            this.byType = byType;
            this.byStatus = byStatus;
            this.byCategory = byCategory;
            this.byReason = byReason;
            this.newestMembers = newestMembers;
        }
    }

    /** What a waiting report is about, with a short excerpt of it. */
    public static final class Preview {
        public final com.example.ungdungchiasecaycanh.model.ReportTargetType type;
        public final String text;

        Preview(com.example.ungdungchiasecaycanh.model.ReportTargetType type, String text) {
            this.type = type;
            this.text = text == null ? "" : text.trim();
        }
    }

    public static final class State {
        public final Status status;
        public final Dashboard data;
        State(Status status, Dashboard data) { this.status = status; this.data = data; }
    }

    private final AdminRepository repository = new AdminRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Status.LOADING, null));

    private final MutableLiveData<Period> period = new MutableLiveData<>(Period.WEEK);

    public LiveData<State> getState() { return state; }

    public LiveData<Period> getPeriod() { return period; }

    public void setPeriod(Period value) { period.setValue(value); }

    static Window window(Dashboard d, Period p, long now, TimeZone zone) {
        long start = p == Period.ALL ? Long.MIN_VALUE : AdminStats.dayStarts(now, p.days, zone)[0];
        long previous = p == Period.ALL ? start : AdminStats.dayStarts(start - 1, p.days, zone)[0];
        Window w = new Window(start);
        for (int i = 0; i < d.listingTimes.length; i++) {
            long t = d.listingTimes[i];
            if (t >= start) {
                w.listings++;
                ListingType type = d.listingTypes[i];
                if (type == ListingType.TRADE) w.trade++;
                else if (type == ListingType.SELL) w.sell++;
                else if (type == ListingType.GIVE_AWAY) w.give++;
            } else if (t >= previous) w.previousActivity++;
        }
        for (long t : d.postTimes) {
            if (t >= start) w.posts++;
            else if (t >= previous) w.previousActivity++;
        }
        for (long t : d.memberTimes) if (t >= start) w.members++;
        return w;
    }

    public void load() {
        State current = state.getValue();
        if (current == null || current.data == null) state.setValue(new State(Status.LOADING, null));
        repository.isCurrentUserAdmin().addOnCompleteListener(check -> {
            if (!check.isSuccessful() || !Boolean.TRUE.equals(check.getResult())) {
                state.postValue(new State(Status.NOT_ADMIN, null));
                return;
            }
            Task<List<Listing>> listings = repository.getAllListings();
            Task<List<Post>> posts = repository.getAllPosts();
            Task<List<Member>> members = repository.getMembers();
            Task<List<Report>> reports = repository.getReports();
            Task<Map<String, ReportReview>> reviews = repository.getReportReviews();
            Tasks.whenAllSuccess(listings, posts, members, reports, reviews)
                    .addOnSuccessListener(ignored -> state.postValue(new State(Status.CONTENT, aggregate(
                            listings.getResult(), posts.getResult(), members.getResult(),
                            reports.getResult(), reviews.getResult()))))
                    .addOnFailureListener(error -> state.postValue(new State(Status.ERROR, null)));
        });
    }

    static Dashboard aggregate(List<Listing> listings, List<Post> posts, List<Member> members,
                               List<Report> reports, Map<String, ReportReview> reviews) {
        long now = System.currentTimeMillis();
        TimeZone zone = TimeZone.getDefault();
        List<Long> listingTimes = new ArrayList<>();
        for (Listing l : listings) listingTimes.add(millis(l.getCreatedAt()));
        List<Long> postTimes = new ArrayList<>();
        for (Post p : posts) postTimes.add(millis(p.getCreatedAt()));

        int open = 0, resolved = 0, dismissed = 0;
        for (Report r : reports) {
            ReportReview review = reviews.get(r.getReportId());
            if (review == null) open++;
            else if (ReportReview.DISMISSED.equals(review.getStatus())) dismissed++;
            else resolved++;
        }

        List<Member> newest = new ArrayList<>(members);
        Collections.sort(newest, (a, b) -> Long.compare(
                millis(b.getProfile() == null ? null : b.getProfile().getCreatedAt()),
                millis(a.getProfile() == null ? null : a.getProfile().getCreatedAt())));
        if (newest.size() > 3) newest = new ArrayList<>(newest.subList(0, 3));

        List<Long> memberTimes = new ArrayList<>();
        for (Member m : members) memberTimes.add(millis(m.getProfile() == null ? null : m.getProfile().getCreatedAt()));
        List<Long> reportTimes = new ArrayList<>();
        for (Report r : reports) reportTimes.add(millis(r.getCreatedAt()));

        Dashboard dashboard = new Dashboard(listings.size(), posts.size(), members.size(), open, resolved, dismissed,
                AdminStats.dailyCounts(listingTimes, now, DAYS, zone),
                AdminStats.dailyCounts(postTimes, now, DAYS, zone),
                AdminStats.dayStarts(now, DAYS, zone),
                AdminStats.countBy(listings, ListingType.class, Listing::getType),
                AdminStats.countBy(listings, ListingStatus.class, Listing::getStatus),
                AdminStats.countBy(posts, PostCategory.class, Post::getCategory),
                AdminStats.countBy(reports, ReportReason.class, Report::getReason),
                newest);
        dashboard.newMembersPerDay = AdminStats.dailyCounts(memberTimes, now, DAYS, zone);
        dashboard.newReportsPerDay = AdminStats.dailyCounts(reportTimes, now, DAYS, zone);
        dashboard.listingTimes = toArray(listingTimes);
        dashboard.postTimes = toArray(postTimes);
        dashboard.memberTimes = toArray(memberTimes);
        dashboard.listingTypes = new ListingType[listings.size()];
        for (int i = 0; i < listings.size(); i++) dashboard.listingTypes[i] = listings.get(i).getType();

        java.util.Map<String, String> names = new java.util.HashMap<>();
        for (Member m : members) names.put(m.getUid(), m.getDisplayName());
        java.util.Map<String, String> postTexts = new java.util.HashMap<>();
        for (Post post : posts) postTexts.put(post.getPostId(), post.getText());
        java.util.Map<String, String> listingNames = new java.util.HashMap<>();
        for (Listing l : listings) listingNames.put(l.getListingId(), l.getNickname());
        List<Report> waiting = new ArrayList<>();
        for (Report r : reports) if (reviews.get(r.getReportId()) == null) waiting.add(r);
        Collections.sort(waiting, (a, b) -> Long.compare(millis(b.getCreatedAt()), millis(a.getCreatedAt())));
        dashboard.openByReason = AdminStats.countBy(waiting, ReportReason.class, Report::getReason);
        List<Preview> previews = new ArrayList<>();
        for (Report r : waiting) {
            if (previews.size() == 2) break;
            previews.add(new Preview(r.getTargetType(), excerpt(r, postTexts, listingNames, names)));
        }
        dashboard.openPreviews = previews;

        Listing newestWithPhoto = null;
        for (Listing l : listings) {
            String cover = l.getCoverImageUrl();
            if (cover == null || cover.trim().isEmpty()) continue;
            if (newestWithPhoto == null || millis(l.getCreatedAt()) > millis(newestWithPhoto.getCreatedAt())) newestWithPhoto = l;
        }
        dashboard.newestListing = newestWithPhoto;
        if (newestWithPhoto != null) {
            String owner = names.get(newestWithPhoto.getOwnerId());
            dashboard.newestOwner = owner == null ? "" : owner;
        }
        return dashboard;
    }

    /** The reported thing in a few words: the post's text, the listing's name, the member's name, else the note. */
    static String excerpt(Report r, Map<String, String> postTexts, Map<String, String> listingNames, Map<String, String> names) {
        String id = r.getTargetId();
        String text = null;
        if (r.getTargetType() == com.example.ungdungchiasecaycanh.model.ReportTargetType.POST) text = postTexts.get(id);
        else if (r.getTargetType() == com.example.ungdungchiasecaycanh.model.ReportTargetType.LISTING) text = listingNames.get(id);
        else if (r.getTargetType() == com.example.ungdungchiasecaycanh.model.ReportTargetType.USER) text = names.get(id);
        if (text == null || text.trim().isEmpty()) text = r.getDetails();
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    private static long[] toArray(List<Long> values) {
        long[] out = new long[values.size()];
        for (int i = 0; i < out.length; i++) out[i] = values.get(i);
        return out;
    }

    private static long millis(Timestamp t) {
        return t == null ? 0 : t.toDate().getTime();
    }
}
