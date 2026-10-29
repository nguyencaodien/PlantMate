package com.example.ungdungchiasecaycanh.ui.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.AdminRepository;
import com.example.ungdungchiasecaycanh.model.Comment;
import com.example.ungdungchiasecaycanh.model.Member;
import com.example.ungdungchiasecaycanh.model.Post;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Admin editor for one post and its comments, reloaded by postId. */
public class AdminPostEditViewModel extends ViewModel {

    public static final class Data {
        public final Post post;
        public final String authorName;
        public final List<Comment> comments;
        public final Map<String, String> names;

        Data(Post post, String authorName, List<Comment> comments, Map<String, String> names) {
            this.post = post;
            this.authorName = authorName;
            this.comments = comments;
            this.names = names;
        }
    }

    private final AdminRepository repository = new AdminRepository();
    private final MutableLiveData<AdminEditState<Data>> state = new MutableLiveData<>(AdminEditState.loading());
    private String postId;

    public LiveData<AdminEditState<Data>> getState() { return state; }

    public void load(String postId) {
        this.postId = postId;
        if (current().data == null) state.setValue(AdminEditState.loading());
        Task<Post> post = repository.getPost(postId);
        Task<List<Comment>> comments = repository.getComments(postId);
        Task<List<Member>> members = repository.getMembers();
        Tasks.whenAllSuccess(post, comments, members)
                .addOnSuccessListener(ignored -> {
                    Map<String, String> names = new HashMap<>();
                    for (Member m : members.getResult()) names.put(m.getUid(), m.getDisplayName());
                    List<Comment> sorted = new ArrayList<>(comments.getResult());
                    Collections.sort(sorted, (a, b) -> {
                        if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                        return a.getCreatedAt().compareTo(b.getCreatedAt());
                    });
                    String author = names.get(post.getResult().getAuthorId());
                    state.postValue(current().withStatus(AdminEditState.Status.CONTENT,
                            new Data(post.getResult(), author == null ? "" : author, sorted, names)));
                })
                .addOnFailureListener(e -> state.postValue(current().withStatus(
                        post.getException() instanceof IllegalStateException
                                ? AdminEditState.Status.NOT_FOUND : AdminEditState.Status.ERROR, null)));
    }

    public void save(String text, PostCategory category, String imageUrl, String note) {
        state.setValue(current().busy());
        repository.updatePost(postId, text, category, imageUrl, note)
                .addOnSuccessListener(ignored -> { state.postValue(current().saved(null)); load(postId); })
                .addOnFailureListener(e -> state.postValue(current().failed()));
    }

    public void deletePost(String note) {
        state.setValue(current().busy());
        repository.deletePost(postId, note)
                .addOnSuccessListener(ignored -> state.postValue(current().deleted()))
                .addOnFailureListener(e -> state.postValue(current().failed()));
    }

    public void deleteComment(String commentId, String note) {
        state.setValue(current().busy());
        repository.deleteComment(postId, commentId, note)
                .addOnSuccessListener(ignored -> { state.postValue(current().saved(null)); load(postId); })
                .addOnFailureListener(e -> state.postValue(current().failed()));
    }

    private AdminEditState<Data> current() {
        AdminEditState<Data> value = state.getValue();
        return value == null ? AdminEditState.loading() : value;
    }
}
