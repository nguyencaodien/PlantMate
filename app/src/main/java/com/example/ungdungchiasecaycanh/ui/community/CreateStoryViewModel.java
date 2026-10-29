package com.example.ungdungchiasecaycanh.ui.community;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.StoryRepository;

/** New story: the chosen photo survives rotation; publishing uploads it and posts the story once. */
public class CreateStoryViewModel extends AndroidViewModel {
    public static final class State {
        public final String photo;
        public final boolean publishing;
        public final boolean published;
        /** String resource of the error to show, or 0. */
        public final int error;
        State(String photo, boolean publishing, boolean published, int error) {
            this.photo = photo; this.publishing = publishing; this.published = published; this.error = error;
        }
    }

    private final StoryRepository repository;
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(null, false, false, 0));

    public CreateStoryViewModel(@NonNull Application application) {
        super(application);
        repository = new StoryRepository(application);
    }

    public LiveData<State> getState() { return state; }

    public void setPhoto(String uri) {
        State s = state.getValue();
        if (s == null || s.publishing) return;
        state.setValue(new State(uri, false, false, 0));
    }

    public void publish(String title, String caption) {
        State s = state.getValue();
        if (s == null || s.publishing || s.published) return;
        if (s.photo == null) { state.setValue(new State(null, false, false, R.string.story_error_photo)); return; }
        try { StoryRepository.normalize(title, caption, s.photo); }
        catch (IllegalArgumentException invalid) {
            state.setValue(new State(s.photo, false, false, R.string.story_error_title));
            return;
        }
        String photo = s.photo;
        state.setValue(new State(photo, true, false, 0));
        repository.create(title, caption, photo).addOnCompleteListener(task -> state.setValue(
                task.isSuccessful() ? new State(photo, false, true, 0)
                        : new State(photo, false, false, R.string.story_publish_error)));
    }
}
