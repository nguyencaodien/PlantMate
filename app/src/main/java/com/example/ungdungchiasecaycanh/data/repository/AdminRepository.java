package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAdminDataSource;
import com.example.ungdungchiasecaycanh.model.AdminAction;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.Member;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReview;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Admin operations for the signed-in admin. The UI hides the admin area from members, but the
 * real boundary is firestore.rules: every call below is denied unless admins/{uid} exists.
 */
public class AdminRepository {
    private final AuthRepository auth = new AuthRepository();
    private final FirebaseAdminDataSource remote = new FirebaseAdminDataSource();

    public String getCurrentUserId() {
        return auth.getCurrentUserId();
    }

    public Task<Boolean> isCurrentUserAdmin() {
        return remote.isAdmin(auth.getCurrentUserId());
    }

    public Task<Long> count(String collection) { return remote.count(collection); }

    public Task<List<Listing>> getAllListings() { return remote.getAllListings(); }
    public Task<Void> updateListing(String listingId, Map<String, Object> fields, String note) {
        return guarded(() -> remote.updateListing(admin(), listingId, fields, note));
    }
    public Task<Void> deleteListing(String listingId, String note) {
        return guarded(() -> remote.deleteListing(admin(), listingId, note));
    }

    public Task<List<Post>> getAllPosts() { return remote.getAllPosts(); }
    public Task<Post> getPost(String postId) { return remote.getPost(postId); }
    public Task<List<Comment>> getComments(String postId) { return remote.getComments(postId); }
    public Task<Void> updatePost(String postId, String text, PostCategory category, String imageUrl, String note) {
        return guarded(() -> remote.updatePost(admin(), postId, text, category, imageUrl, note));
    }
    public Task<Void> deletePost(String postId, String note) {
        return guarded(() -> remote.deletePost(admin(), postId, note));
    }
    public Task<Void> deleteComment(String postId, String commentId, String note) {
        return guarded(() -> remote.deleteComment(admin(), postId, commentId, note));
    }

    public Task<List<Member>> getMembers() { return remote.getMembers(); }
    public Task<Member> getMember(String uid) { return remote.getMember(uid); }
    public Task<Void> updateMember(String uid, String displayName, String locationText, String bio, String note) {
        return guarded(() -> remote.updateMember(admin(), uid, displayName, locationText, bio, note));
    }
    public Task<Void> setSuspended(String uid, boolean suspended, String note) {
        if (uid != null && uid.equals(admin())) {
            return Tasks.forException(new IllegalArgumentException("Admins cannot suspend themselves"));
        }
        return guarded(() -> remote.setSuspended(admin(), uid, suspended, note));
    }

    public Task<Void> setAdmin(String uid, boolean grant, String note) {
        if (uid != null && uid.equals(admin())) {
            return Tasks.forException(new IllegalArgumentException("Admins cannot change their own role"));
        }
        return guarded(() -> remote.setAdmin(admin(), uid, grant, note));
    }

    public Task<List<Report>> getReports() { return remote.getReports(); }
    public Task<Map<String, ReportReview>> getReportReviews() { return remote.getReportReviews(); }
    public Task<Void> reviewReport(String reportId, String status, String note) {
        return guarded(() -> remote.reviewReport(admin(), reportId, status, note));
    }

    public Task<List<AdminAction>> getActions() { return remote.getActions(); }

    private String admin() {
        return auth.getCurrentUserId();
    }

    private Task<Void> guarded(Supplier<Task<Void>> call) {
        if (admin() == null) return Tasks.forException(new IllegalStateException("Not signed in"));
        return call.get();
    }
}
