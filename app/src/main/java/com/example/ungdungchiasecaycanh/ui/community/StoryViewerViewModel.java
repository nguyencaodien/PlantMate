package com.example.ungdungchiasecaycanh.ui.community;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.repository.StoryRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Loads the stories by ID (skipping ones that are gone) and keeps the current position. */
public class StoryViewerViewModel extends AndroidViewModel {
    public static final class State {
        public final boolean loading;
        public final List<StoryRepository.Item> stories;
        public final int index;
        State(boolean loading, List<StoryRepository.Item> stories, int index) {
            this.loading = loading; this.stories = stories; this.index = index;
        }
        public StoryRepository.Item current() {
            return index >= 0 && index < stories.size() ? stories.get(index) : null;
        }
    }

    private final StoryRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(true, Collections.emptyList(), 0));
    private final MutableLiveData<Boolean> deleteFailed = new MutableLiveData<>(false);

    /** The heart on the current story: a friend's own heart, or the author's count (-1 while unknown). */
    public static final class Like {
        public final String storyId;
        public final boolean liked;
        public final int count;
        Like(String storyId, boolean liked, int count) { this.storyId = storyId; this.liked = liked; this.count = count; }
    }

    private final MutableLiveData<Like> like = new MutableLiveData<>();
    public LiveData<Like> getLike() { return like; }

    private void loadLike(StoryRepository.Item item) {
        String id = item.story.getStoryId();
        like.setValue(new Like(id, false, -1));
        if (item.mine) {
            repository.getLikeCount(id).addOnSuccessListener(n -> {
                if (isCurrent(id)) like.setValue(new Like(id, false, n));
            });
        } else {
            repository.isLiked(id).addOnSuccessListener(liked -> {
                if (isCurrent(id)) like.setValue(new Like(id, Boolean.TRUE.equals(liked), -1));
            });
        }
    }

    /** Hearts or un-hearts the current friend's story at once; reverts if the write fails. */
    public void toggleLike() {
        Like now = like.getValue();
        State s = state.getValue();
        StoryRepository.Item item = s == null ? null : s.current();
        if (now == null || item == null || item.mine || !now.storyId.equals(item.story.getStoryId())) return;
        boolean next = !now.liked;
        like.setValue(new Like(now.storyId, next, -1));
        repository.setLiked(now.storyId, next).addOnFailureListener(e -> {
            if (isCurrent(now.storyId)) like.setValue(new Like(now.storyId, !next, -1));
        });
    }

    private boolean isCurrent(String storyId) {
        State s = state.getValue();
        StoryRepository.Item item = s == null ? null : s.current();
        return item != null && item.story.getStoryId().equals(storyId);
    }
    private boolean started;

    public StoryViewerViewModel(@NonNull Application application) {
        super(application);
        repository = new StoryRepository(application);
    }

    public LiveData<State> getState() { return state; }
    public LiveData<Boolean> getDeleteFailed() { return deleteFailed; }

    public void load(List<String> ids, int start) {
        if (started) return;
        started = true;
        List<Task<StoryRepository.Item>> tasks = new ArrayList<>();
        for (String id : ids) tasks.add(repository.get(id));
        Tasks.whenAllComplete(tasks).addOnCompleteListener(done -> {
            List<StoryRepository.Item> items = new ArrayList<>();
            int index = 0;
            for (int i = 0; i < tasks.size(); i++) {
                Task<StoryRepository.Item> t = tasks.get(i);
                if (!t.isSuccessful() || t.getResult() == null) continue;
                if (i <= start) index = items.size();
                items.add(t.getResult());
            }
            show(items, index);
        });
    }

    /** Next story; returns false at the end (the viewer closes). */
    public boolean next() {
        State s = state.getValue();
        if (s == null || s.index + 1 >= s.stories.size()) return false;
        show(s.stories, s.index + 1);
        return true;
    }

    public void previous() {
        State s = state.getValue();
        if (s != null && s.index > 0) show(s.stories, s.index - 1);
    }

    /** Deletes the current story (the author's own) and moves on. */
    public void deleteCurrent() {
        State s = state.getValue();
        StoryRepository.Item item = s == null ? null : s.current();
        if (item == null || !item.mine) return;
        repository.delete(item.story.getStoryId()).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) { deleteFailed.setValue(true); return; }
            State now = state.getValue();
            if (now == null) return;
            List<StoryRepository.Item> rest = new ArrayList<>(now.stories);
            rest.remove(item);
            show(rest, Math.min(now.index, Math.max(0, rest.size() - 1)));
        });
    }

    public void deleteErrorShown() { deleteFailed.setValue(false); }

    private void show(List<StoryRepository.Item> items, int index) {
        State s = new State(false, items, index);
        StoryRepository.Item current = s.current();
        if (current != null) repository.markSeen(current.story.getStoryId());
        state.setValue(s);
        if (current != null && (like.getValue() == null || !like.getValue().storyId.equals(current.story.getStoryId())))
            loadLike(current);
    }
}
