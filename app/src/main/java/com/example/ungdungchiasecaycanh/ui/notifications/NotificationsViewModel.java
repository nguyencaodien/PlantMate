package com.example.ungdungchiasecaycanh.ui.notifications;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;
import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.example.ungdungchiasecaycanh.data.repository.NotificationRepository;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.AppNotification;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.garden.WateringStatus;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Notifications screen: today's watering (from Room), friend requests waiting, and community
 * notifications (Firestore), newest first, filtered by All / Care / Community.
 */
public class NotificationsViewModel extends AndroidViewModel {
    public enum Status { LOADING, CONTENT, EMPTY, ERROR }
    public enum Filter { ALL, CARE, COMMUNITY }
    public enum Kind { WATER, FRIEND, COMMENT, REPLY, MENTION, LIKE }

    public static final class Row {
        public final Kind kind;
        /** Firestore id (community rows) or the requester's uid (friend rows); "" for watering. */
        public final String id;
        public final String actorId, actorName, postId, text;
        /** Watering: the plants due today; else empty. */
        public final List<String> plantNames;
        public final long plantId;
        public final long at;
        public final boolean unread;

        Row(Kind kind, String id, String actorId, String actorName, String postId, String text,
            List<String> plantNames, long plantId, long at, boolean unread) {
            this.kind = kind; this.id = id; this.actorId = actorId; this.actorName = actorName;
            this.postId = postId; this.text = text; this.plantNames = plantNames; this.plantId = plantId;
            this.at = at; this.unread = unread;
        }

        Row read() {
            return new Row(kind, id, actorId, actorName, postId, text, plantNames, plantId, at, false);
        }

        boolean isCare() { return kind == Kind.WATER; }
    }

    public static final class State {
        public final Status status;
        public final Filter filter;
        public final List<Row> rows;
        public final boolean anyUnread;

        State(Status status, Filter filter, List<Row> rows, boolean anyUnread) {
            this.status = status; this.filter = filter; this.rows = rows; this.anyUnread = anyUnread;
        }
    }

    private final NotificationRepository notifications = new NotificationRepository();
    private final FriendRepository friends = new FriendRepository();
    private final SafetyRepository safety = new SafetyRepository();
    private final UserRepository users = new UserRepository();
    private final SettingsRepository settings;
    private final MediatorLiveData<State> state = new MediatorLiveData<>();

    private List<PlantEntity> plants;
    private List<Row> remote;
    private boolean remoteFailed;
    private final Set<String> hiddenFriends = new HashSet<>();
    private boolean waterRead;
    private Filter filter = Filter.ALL;

    public NotificationsViewModel(@NonNull Application application) {
        super(application);
        settings = new SettingsRepository(application);
        long today = startOfDay(System.currentTimeMillis());
        waterRead = settings.getNotificationsSeenDay() >= today; // unread until the first visit of the day
        settings.setNotificationsSeenDay(today);
        state.setValue(new State(Status.LOADING, filter, Collections.emptyList(), false));
        LiveData<List<PlantEntity>> source = new GardenRepository(application).observeCurrentUserPlants();
        state.addSource(source, values -> {
            plants = values == null ? Collections.emptyList() : values;
            publish();
        });
        load();
    }

    public LiveData<State> getState() { return state; }

    public void retry() {
        remoteFailed = false;
        remote = null;
        publish();
        load();
    }

    public void setFilter(Filter value) {
        filter = value;
        publish();
    }

    private void load() {
        Task<List<AppNotification>> recent = notifications.recent();
        Task<List<FriendRepository.Person>> incoming = friends.getIncoming();
        Task<Set<String>> blocked = safety.getBlockedUids();
        Tasks.whenAllComplete(recent, incoming, blocked).addOnCompleteListener(done -> {
            // Community notifications failing (offline, rules) still leaves watering and friend requests.
            remoteFailed = !recent.isSuccessful();
            Set<String> block = blocked.isSuccessful() && blocked.getResult() != null ? blocked.getResult() : Collections.emptySet();
            List<AppNotification> list = new ArrayList<>();
            Set<String> actors = new LinkedHashSet<>();
            for (AppNotification n : recent.isSuccessful() ? recent.getResult() : Collections.<AppNotification>emptyList()) {
                if (n.actorId == null || block.contains(n.actorId)) continue;
                list.add(n);
                actors.add(n.actorId);
            }
            Map<String, Task<User>> profiles = new HashMap<>();
            for (String uid : actors) profiles.put(uid, users.getProfile(uid));
            Tasks.whenAllComplete(new ArrayList<>(profiles.values())).addOnCompleteListener(names -> {
                List<Row> rows = new ArrayList<>();
                if (incoming.isSuccessful() && incoming.getResult() != null) {
                    for (FriendRepository.Person p : incoming.getResult()) {
                        if (block.contains(p.uid)) continue;
                        rows.add(new Row(Kind.FRIEND, p.uid, p.uid, p.name(), "", "", Collections.emptyList(), 0, 0, true));
                    }
                }
                for (AppNotification n : list) {
                    Task<User> profile = profiles.get(n.actorId);
                    String name = profile != null && profile.isSuccessful() && profile.getResult() != null
                            ? profile.getResult().getDisplayName() : "";
                    rows.add(new Row(Kind.valueOf(n.type.name()), n.id, n.actorId, name, n.postId, n.text,
                            Collections.emptyList(), 0, n.createdAt == null ? 0 : n.createdAt.toDate().getTime(), !n.read));
                }
                remote = rows;
                publish();
            });
        });
    }

    /** Friend request accepted or put off: it leaves the list. */
    public void acceptFriend(String uid) {
        hiddenFriends.add(uid);
        publish();
        friends.accept(uid);
    }

    public void later(String uid) {
        hiddenFriends.add(uid);
        publish();
    }

    public void markRead(Row row) {
        if (row.kind == Kind.WATER) waterRead = true;
        else if (row.kind != Kind.FRIEND && row.unread) {
            notifications.markRead(Collections.singletonList(row.id));
            replaceRemote(row.id);
        }
        publish();
    }

    public void markAllRead() {
        waterRead = true;
        if (remote != null) {
            List<String> ids = new ArrayList<>();
            List<Row> out = new ArrayList<>();
            for (Row r : remote) {
                if (r.unread && r.kind != Kind.FRIEND) {
                    ids.add(r.id);
                    out.add(r.read());
                } else out.add(r);
            }
            remote = out;
            notifications.markRead(ids);
        }
        publish();
    }

    private void replaceRemote(String id) {
        if (remote == null) return;
        List<Row> out = new ArrayList<>();
        for (Row r : remote) out.add(r.id.equals(id) && r.kind != Kind.FRIEND ? r.read() : r);
        remote = out;
    }

    private void publish() {
        if (remote == null || plants == null) {
            state.setValue(new State(Status.LOADING, filter, Collections.emptyList(), false));
            return;
        }
        List<Row> all = new ArrayList<>();
        Row water = wateringRow(plants, System.currentTimeMillis(), !waterRead);
        if (water != null) all.add(water);
        for (Row r : remote) if (!(r.kind == Kind.FRIEND && hiddenFriends.contains(r.id))) all.add(r);
        List<Row> shown = filter(all, filter);
        boolean unread = false;
        for (Row r : all) unread |= r.unread && r.kind != Kind.FRIEND;
        Status status = !shown.isEmpty() ? Status.CONTENT : remoteFailed ? Status.ERROR : Status.EMPTY;
        state.setValue(new State(status, filter, shown, unread));
    }

    static List<Row> filter(List<Row> rows, Filter filter) {
        if (filter == Filter.ALL) return rows;
        List<Row> out = new ArrayList<>();
        for (Row r : rows) if (r.isCare() == (filter == Filter.CARE)) out.add(r);
        return out;
    }

    /** One row for every plant due (or due later) today, or null when none is. */
    static Row wateringRow(List<PlantEntity> plants, long now, boolean unread) {
        long tomorrow = startOfDay(now) + 86_400_000L;
        List<String> names = new ArrayList<>();
        long firstId = 0;
        for (PlantEntity p : plants) {
            WateringStatus s = WateringStatus.of(p, now);
            boolean today = s.kind == WateringStatus.Kind.DUE
                    || (s.kind == WateringStatus.Kind.UPCOMING && s.nextAt < tomorrow);
            if (!today) continue;
            if (names.isEmpty()) firstId = p.getId();
            names.add(p.getNickname());
        }
        if (names.isEmpty()) return null;
        return new Row(Kind.WATER, "", "", "", "", "", names, names.size() == 1 ? firstId : 0, startOfDay(now), unread);
    }

    static long startOfDay(long at) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(at);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
