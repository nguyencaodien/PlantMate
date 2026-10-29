package com.example.ungdungchiasecaycanh.ui.community;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.ungdungchiasecaycanh.data.repository.StoryRepository;

import java.util.ArrayList;
import java.util.List;

/** The Community story strip. On failure it shows no stories: "New story" and "Add friends" still work. */
public class StoryStripViewModel extends AndroidViewModel {
    private final StoryRepository repository;
    private final MutableLiveData<List<StoryRepository.Item>> stories = new MutableLiveData<>(new ArrayList<>());
    private String loadedFor;
    private int generation;

    public StoryStripViewModel(@NonNull Application application) {
        super(application);
        repository = new StoryRepository(application);
    }

    public LiveData<List<StoryRepository.Item>> getStories() { return stories; }

    public void load() {
        String uid = repository.currentUid();
        if (uid == null) { stories.setValue(new ArrayList<>()); return; }
        if (!uid.equals(loadedFor)) stories.setValue(new ArrayList<>()); // never show another account's strip
        loadedFor = uid;
        int request = ++generation;
        repository.loadStrip().addOnCompleteListener(task -> {
            if (request != generation || !uid.equals(repository.currentUid())) return;
            if (task.isSuccessful() && task.getResult() != null) stories.setValue(groupedByAuthor(task.getResult()));
        });
    }

    /**
     * The same stories with each author's together, authors in the order they first appear (so the newest or
     * unseen author stays first). The strip shows one card per author; the viewer plays an author's stories in a row.
     */
    static List<StoryRepository.Item> groupedByAuthor(List<StoryRepository.Item> items) {
        java.util.LinkedHashMap<String, List<StoryRepository.Item>> byAuthor = new java.util.LinkedHashMap<>();
        for (StoryRepository.Item item : items) {
            String author = item.story.getAuthorId() == null ? "" : item.story.getAuthorId();
            List<StoryRepository.Item> list = byAuthor.get(author);
            if (list == null) byAuthor.put(author, list = new ArrayList<>());
            list.add(item);
        }
        List<StoryRepository.Item> out = new ArrayList<>();
        for (List<StoryRepository.Item> list : byAuthor.values()) out.addAll(list);
        return out;
    }

    /** IDs of the strip's stories, in order (grouped by author), for the viewer. */
    public ArrayList<String> storyIds() {
        ArrayList<String> ids = new ArrayList<>();
        List<StoryRepository.Item> items = stories.getValue();
        if (items != null) for (StoryRepository.Item item : items) ids.add(item.story.getStoryId());
        return ids;
    }
}
