package com.example.ungdungchiasecaycanh.ui.review;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.ReviewRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.Review;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewViewModel extends ViewModel {
    public enum Status { LOADING, CONTENT, EMPTY, ERROR }

    public static final class UiState {
        public final Status status;
        public final User targetUser;
        public final List<ReviewListItem> items;
        public final ReviewSummary summary;

        private UiState(Status status, User targetUser, List<ReviewListItem> items,
                        ReviewSummary summary) {
            this.status = status;
            this.targetUser = targetUser;
            this.items = new ArrayList<>(items);
            this.summary = summary;
        }
    }

    private final ReviewRepository reviewRepository = new ReviewRepository();
    private final UserRepository userRepository = new UserRepository();
    private final MutableLiveData<UiState> state = new MutableLiveData<>();
    private String targetUserId;
    private int generation;

    public LiveData<UiState> getState() { return state; }

    public void load(String userId) {
        targetUserId = userId;
        int request = ++generation;
        if (userId == null || userId.trim().isEmpty()) {
            publish(Status.ERROR, null, Collections.emptyList(), ReviewSummary.from(null));
            return;
        }
        state.setValue(new UiState(Status.LOADING, null, Collections.emptyList(),
                ReviewSummary.from(null)));
        Task<User> userTask = userRepository.getProfile(userId.trim());
        Task<List<Review>> reviewsTask = reviewRepository.getReviewsForUser(userId.trim());
        Tasks.whenAllComplete(userTask, reviewsTask).addOnCompleteListener(done -> {
            if (request != generation) return;
            if (!reviewsTask.isSuccessful()) {
                publish(Status.ERROR, userTask.isSuccessful() ? userTask.getResult() : null,
                        Collections.emptyList(), ReviewSummary.from(null));
                return;
            }
            User target = userTask.isSuccessful() ? userTask.getResult() : null;
            enrich(request, target, reviewsTask.getResult());
        });
    }

    public void retry() { load(targetUserId); }

    private void enrich(int request, User target, List<Review> reviews) {
        if (reviews.isEmpty()) {
            publish(Status.EMPTY, target, Collections.emptyList(), ReviewSummary.from(reviews));
            return;
        }
        List<Task<User>> profiles = new ArrayList<>();
        List<Task<?>> tasks = new ArrayList<>();
        for (Review review : reviews) {
            Task<User> task = userRepository.getProfile(review.getReviewerId());
            profiles.add(task);
            tasks.add(task);
        }
        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            if (request != generation) return;
            List<ReviewListItem> items = new ArrayList<>();
            for (int i = 0; i < reviews.size(); i++) {
                User reviewer = profiles.get(i).isSuccessful() ? profiles.get(i).getResult() : null;
                String name = reviewer == null ? null : reviewer.getDisplayName();
                items.add(new ReviewListItem(reviews.get(i), name));
            }
            publish(Status.CONTENT, target, items, ReviewSummary.from(reviews));
        });
    }

    private void publish(Status status, User target, List<ReviewListItem> items,
                         ReviewSummary summary) {
        state.setValue(new UiState(status, target, items, summary));
    }
}
