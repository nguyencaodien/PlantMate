package com.example.ungdungchiasecaycanh.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.AdminRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.Member;
import com.example.ungdungchiasecaycanh.model.Post;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

/** Admin view of one member: profile edit, content counts, suspension and the admin role. */
public class AdminMemberViewModel extends ViewModel {

    public static final class Data {
        public final Member member;
        public final int listingCount;
        public final int postCount;
        public final boolean self;
        public final List<Listing> listings;
        public final List<Post> posts;

        Data(Member member, int listingCount, int postCount, boolean self, List<Listing> listings, List<Post> posts) {
            this.listings = listings;
            this.posts = posts;
            this.member = member;
            this.listingCount = listingCount;
            this.postCount = postCount;
            this.self = self;
        }
    }

    private final AdminRepository repository = new AdminRepository();
    private final MutableLiveData<AdminEditState<Data>> state = new MutableLiveData<>(AdminEditState.loading());
    private String uid;

    public LiveData<AdminEditState<Data>> getState() { return state; }

    public void load(String uid) {
        this.uid = uid;
        if (current().data == null) state.setValue(AdminEditState.loading());
        Task<Member> member = repository.getMember(uid);
        Task<List<Listing>> listings = repository.getAllListings();
        Task<List<Post>> posts = repository.getAllPosts();
        Tasks.whenAllSuccess(member, listings, posts)
                .addOnSuccessListener(ignored -> {
                    List<Listing> theirListings = new ArrayList<>();
                    List<Post> theirPosts = new ArrayList<>();
                    for (Listing l : listings.getResult()) if (uid.equals(l.getOwnerId())) theirListings.add(l);
                    for (Post p : posts.getResult()) if (uid.equals(p.getAuthorId())) theirPosts.add(p);
                    state.postValue(current().withStatus(AdminEditState.Status.CONTENT,
                            new Data(member.getResult(), theirListings.size(), theirPosts.size(),
                                    uid.equals(repository.getCurrentUserId()), theirListings, theirPosts)));
                })
                .addOnFailureListener(e -> state.postValue(current().withStatus(
                        member.getException() instanceof IllegalStateException
                                ? AdminEditState.Status.NOT_FOUND : AdminEditState.Status.ERROR, null)));
    }

    public void save(String displayName, String location, String bio, String note) {
        state.setValue(current().busy());
        repository.updateMember(uid, displayName, location, bio, note)
                .addOnSuccessListener(ignored -> { state.postValue(current().saved(null)); load(uid); })
                .addOnFailureListener(e -> state.postValue(current().failed()));
    }

    public void setSuspended(boolean suspended, String note) {
        state.setValue(current().busy());
        repository.setSuspended(uid, suspended, note)
                .addOnSuccessListener(ignored -> { state.postValue(current().saved(null)); load(uid); })
                .addOnFailureListener(e -> state.postValue(current().failed()));
    }

    public void setAdmin(boolean grant, String note) {
        state.setValue(current().busy());
        repository.setAdmin(uid, grant, note)
                .addOnSuccessListener(ignored -> { state.postValue(current().saved(null)); load(uid); })
                .addOnFailureListener(e -> { state.postValue(current().failed()); load(uid); });
    }

    private AdminEditState<Data> current() {
        AdminEditState<Data> value = state.getValue();
        return value == null ? AdminEditState.loading() : value;
    }
}
