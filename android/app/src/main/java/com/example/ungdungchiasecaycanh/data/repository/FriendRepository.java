package com.example.ungdungchiasecaycanh.data.repository;

import com.example.ungdungchiasecaycanh.data.remote.FirebaseAuthDataSource;
import com.example.ungdungchiasecaycanh.data.remote.FirebaseFriendDataSource;
import com.example.ungdungchiasecaycanh.model.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Friend requests and friendships of the signed-in member. */
public class FriendRepository {
    public enum Relation { SELF, NONE, OUTGOING, INCOMING, FRIENDS }

    /** A member in a friends list: UID and public profile. */
    public static final class Person {
        public final String uid;
        public final User user;
        public Person(String uid, User user) { this.uid = uid; this.user = user; }
        public String name() {
            return user == null || user.getDisplayName() == null ? "" : user.getDisplayName().trim();
        }
    }

    private final FirebaseAuthDataSource auth = new FirebaseAuthDataSource();
    private final FirebaseFriendDataSource data = new FirebaseFriendDataSource();
    private final UserRepository users = new UserRepository();

    public String currentUid() {
        String uid = auth.getCurrentUserId();
        return uid == null || uid.trim().isEmpty() ? null : uid;
    }

    /** Friends beat a pending request; a request they sent beats one we sent. */
    public static Relation resolve(String me, String other, boolean friends, boolean outgoing, boolean incoming) {
        if (me != null && me.equals(other)) return Relation.SELF;
        if (friends) return Relation.FRIENDS;
        if (incoming) return Relation.INCOMING;
        if (outgoing) return Relation.OUTGOING;
        return Relation.NONE;
    }

    public Task<Relation> getRelation(String otherUid) {
        String me = currentUid();
        if (me == null) return signedOut();
        if (!validId(otherUid)) return Tasks.forException(new IllegalArgumentException("Invalid ID"));
        if (me.equals(otherUid)) return Tasks.forResult(Relation.SELF);
        return data.relation(me, otherUid).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            boolean[] r = task.getResult();
            return resolve(me, otherUid, r[0], r[1], r[2]);
        });
    }

    /** "Add friend": sends a request, or accepts theirs when they already asked. */
    public Task<Relation> addFriend(String otherUid) {
        String me = currentUid();
        if (me == null) return signedOut();
        return getRelation(otherUid).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            switch (task.getResult()) {
                case NONE: return data.sendRequest(me, otherUid).continueWith(done -> {
                    if (!done.isSuccessful()) throw done.getException();
                    return Relation.OUTGOING;
                });
                case INCOMING: return accept(otherUid);
                default: return Tasks.forResult(task.getResult());
            }
        });
    }

    public Task<Relation> accept(String requesterUid) {
        String me = currentUid();
        if (me == null) return signedOut();
        return data.accept(me, requesterUid).continueWith(done -> {
            if (!done.isSuccessful()) throw done.getException();
            return Relation.FRIENDS;
        });
    }

    public Task<Relation> cancelRequest(String toUid) {
        String me = currentUid();
        if (me == null) return signedOut();
        return data.deleteRequest(me, toUid).continueWith(done -> {
            if (!done.isSuccessful()) throw done.getException();
            return Relation.NONE;
        });
    }

    public Task<Relation> decline(String fromUid) {
        String me = currentUid();
        if (me == null) return signedOut();
        return data.deleteRequest(fromUid, me).continueWith(done -> {
            if (!done.isSuccessful()) throw done.getException();
            return Relation.NONE;
        });
    }

    public Task<Relation> unfriend(String otherUid) {
        String me = currentUid();
        if (me == null) return signedOut();
        return data.unfriend(me, otherUid).continueWith(done -> {
            if (!done.isSuccessful()) throw done.getException();
            return Relation.NONE;
        });
    }

    /** After a block: no friendship and no pending request either way (best effort). */
    public Task<Void> removeAll(String otherUid) {
        String me = currentUid();
        if (me == null || !validId(otherUid)) return Tasks.forResult(null);
        return Tasks.whenAllComplete(data.unfriend(me, otherUid), data.deleteRequest(me, otherUid),
                data.deleteRequest(otherUid, me)).continueWith(task -> null);
    }

    public Task<List<String>> getFriendIds() {
        String me = currentUid();
        return me == null ? signedOut() : data.getFriendIds(me);
    }

    public Task<List<Person>> getFriends() { return people(getFriendIds()); }

    public Task<List<Person>> getIncoming() {
        String me = currentUid();
        return me == null ? signedOut() : people(data.getIncomingIds(me));
    }

    public Task<List<Person>> getOutgoing() {
        String me = currentUid();
        return me == null ? signedOut() : people(data.getOutgoingIds(me));
    }

    public Task<Integer> getIncomingCount() {
        String me = currentUid();
        if (me == null) return signedOut();
        return data.getIncomingIds(me).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            return task.getResult().size();
        });
    }

    /** Public profiles for these members, sorted by name. */
    public Task<List<Person>> getPeople(List<String> uids) {
        return people(Tasks.forResult(uids));
    }

    /** Profiles for the UIDs, sorted by name; members whose profile is gone are left out. */
    private Task<List<Person>> people(Task<List<String>> ids) {
        return ids.continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            List<String> uids = task.getResult();
            List<Task<User>> profiles = new ArrayList<>();
            for (String uid : uids) profiles.add(users.getProfile(uid));
            return Tasks.whenAllComplete(profiles).continueWith(done -> {
                List<Person> result = new ArrayList<>();
                for (int i = 0; i < uids.size(); i++) {
                    Task<User> p = profiles.get(i);
                    if (p.isSuccessful() && p.getResult() != null) result.add(new Person(uids.get(i), p.getResult()));
                }
                sortByName(result);
                return result;
            });
        });
    }

    public static void sortByName(List<Person> people) {
        Collator collator = Collator.getInstance(new Locale("vi"));
        Collections.sort(people, (a, b) -> collator.compare(a.name(), b.name()));
    }

    private static boolean validId(String id) {
        return id != null && !id.trim().isEmpty() && !id.contains("/");
    }

    private static <T> Task<T> signedOut() {
        return Tasks.forException(new IllegalStateException("Authentication required"));
    }
}
