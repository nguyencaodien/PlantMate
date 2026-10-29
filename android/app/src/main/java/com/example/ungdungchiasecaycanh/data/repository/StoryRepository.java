package com.example.ungdungchiasecaycanh.data.repository;

import android.content.Context;

import com.example.ungdungchiasecaycanh.data.local.dao.StoryViewDao;
import com.example.ungdungchiasecaycanh.data.local.db.PlantMateDatabase;
import com.example.ungdungchiasecaycanh.data.local.entity.StoryViewEntity;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseStoryDataSource;
import com.example.ungdungchiasecaycanh.model.Story;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Community "green moments": the member's and their friends' stories of the last 24 hours. The
 * stories live in Firestore; which ones this account has opened is kept in Room (story_views).
 */
public class StoryRepository {
    private static final Executor IO = Executors.newSingleThreadExecutor();

    /** One story with its author, as the strip and the viewer show it. */
    public static final class Item {
        public final Story story;
        public final User author;
        public final boolean mine;
        public final boolean seen;
        public Item(Story story, User author, boolean mine, boolean seen) {
            this.story = story; this.author = author; this.mine = mine; this.seen = seen;
        }
    }

    private final FirebaseAuthDataSource auth = new FirebaseAuthDataSource();
    private final FirebaseStoryDataSource data = new FirebaseStoryDataSource();
    private final FriendRepository friends = new FriendRepository();
    private final UserRepository users = new UserRepository();
    private final ImageRepository images;
    private final StoryViewDao views;

    public StoryRepository(Context context) {
        images = new ImageRepository(context);
        views = PlantMateDatabase.getInstance(context).storyViewDao();
    }

    /** A friend hearts (or un-hearts) a story; the author cannot heart their own. */
    public Task<Void> setLiked(String storyId, boolean liked) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        return data.setLiked(storyId, uid, liked);
    }

    public Task<Boolean> isLiked(String storyId) {
        String uid = currentUid();
        if (uid == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        return data.isLiked(storyId, uid);
    }

    public Task<Integer> getLikeCount(String storyId) {
        return data.getLikeCount(storyId);
    }

    public String currentUid() {
        String uid = auth.getCurrentUserId();
        return uid == null || uid.trim().isEmpty() ? null : uid;
    }

    /**
     * The strip: the member's own stories, then friends' (unseen first), newest first in each group.
     * ponytail: one query per friend; batch with an `in` query if friend lists grow past a few dozen.
     */
    public Task<List<Item>> loadStrip() {
        String me = currentUid();
        if (me == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        long now = System.currentTimeMillis();
        Task<Set<String>> seenTask = Tasks.call(IO, () -> {
            views.deleteOlderThan(me, now - 2 * Story.LIFETIME_MS);
            return new HashSet<>(views.getViewedIds(me));
        });
        return friends.getFriendIds().continueWithTask(ids -> {
            List<String> authors = new ArrayList<>();
            authors.add(me);
            if (ids.isSuccessful() && ids.getResult() != null) authors.addAll(ids.getResult());
            List<Task<List<Story>>> perAuthor = new ArrayList<>();
            Map<String, Task<User>> profiles = new HashMap<>();
            for (String uid : authors) {
                perAuthor.add(data.getForAuthor(uid));
                profiles.put(uid, users.getProfile(uid));
            }
            List<Task<?>> all = new ArrayList<>(perAuthor);
            all.addAll(profiles.values());
            all.add(seenTask);
            return Tasks.whenAllComplete(all).continueWith(done -> {
                if (!perAuthor.get(0).isSuccessful()) throw perAuthor.get(0).getException();
                Set<String> seen = seenTask.isSuccessful() ? seenTask.getResult() : Collections.emptySet();
                List<Item> items = new ArrayList<>();
                for (int i = 0; i < authors.size(); i++) {
                    Task<List<Story>> stories = perAuthor.get(i);
                    if (!stories.isSuccessful() || stories.getResult() == null) continue;
                    Task<User> author = profiles.get(authors.get(i));
                    User user = author != null && author.isSuccessful() ? author.getResult() : null;
                    for (Story s : stories.getResult()) {
                        boolean mine = me.equals(s.getAuthorId());
                        if (s.isActive(now)) items.add(new Item(s, user, mine, seen.contains(s.getStoryId())));
                        else if (mine) data.delete(s.getStoryId()); // tidy up our own expired stories
                    }
                }
                return order(items);
            });
        });
    }

    /** Own stories first, then unseen before seen; newest first within each group. */
    public static List<Item> order(List<Item> items) {
        List<Item> sorted = new ArrayList<>(items);
        Collections.sort(sorted, (a, b) -> {
            if (a.mine != b.mine) return a.mine ? -1 : 1;
            if (a.seen != b.seen) return a.seen ? 1 : -1;
            return Long.compare(b.story.createdMillis(), a.story.createdMillis());
        });
        return sorted;
    }

    /** One story with its author; null when it is gone, expired, or not visible to this member. */
    public Task<Item> get(String storyId) {
        String me = currentUid();
        if (me == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        return data.get(storyId).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            Story s = task.getResult();
            if (s == null || !s.isActive(System.currentTimeMillis())) return Tasks.forResult(null);
            return users.getProfile(s.getAuthorId()).continueWith(author -> new Item(s,
                    author.isSuccessful() ? author.getResult() : null, me.equals(s.getAuthorId()), true));
        });
    }

    public void markSeen(String storyId) {
        String me = currentUid();
        if (me == null || storyId == null) return;
        long now = System.currentTimeMillis();
        IO.execute(() -> views.insert(new StoryViewEntity(me, storyId, now)));
    }

    /** Uploads the photo and posts the story; resolves to its ID. */
    public Task<String> create(String title, String caption, String localPhotoUri) {
        String me = currentUid();
        if (me == null) return Tasks.forException(new IllegalStateException("Authentication required"));
        final String[] text;
        try { text = normalize(title, caption, localPhotoUri); }
        catch (IllegalArgumentException error) { return Tasks.forException(error); }
        return images.uploadForFriends(localPhotoUri).continueWithTask(upload -> {
            if (!upload.isSuccessful()) throw upload.getException();
            if (!me.equals(currentUid())) throw new IllegalStateException("Authentication changed");
            return data.create(me, text[0], text[1], upload.getResult());
        });
    }

    public Task<Void> delete(String storyId) {
        return data.delete(storyId);
    }

    /** [title, caption], trimmed; a title (1–24 characters) and a photo are required. */
    public static String[] normalize(String title, String caption, String photo) {
        String t = title == null ? "" : title.trim();
        String c = caption == null ? "" : caption.trim();
        if (photo == null || photo.trim().isEmpty()) throw new IllegalArgumentException("Photo required");
        if (t.isEmpty() || t.length() > Story.TITLE_MAX) throw new IllegalArgumentException("Invalid title");
        if (c.length() > Story.CAPTION_MAX) throw new IllegalArgumentException("Caption too long");
        return new String[]{t, c};
    }
}
