package com.example.ungdungchiasecaycanh.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.example.ungdungchiasecaycanh.data.repository.ChatRepository;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.data.repository.UserRepository;
import com.example.ungdungchiasecaycanh.model.Conversation;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.home.HomeViewModel;
import java.util.ArrayList;

/** Friends and friend requests (received first, then sent) with accept / decline / cancel / unfriend. */
public class FriendsViewModel extends ViewModel {
    public enum Tab { FRIENDS, REQUESTS }

    public static final class State {
        public final boolean loading;
        public final boolean error;
        public final List<FriendRepository.Person> friends, incoming, outgoing;
        /** Members you have chatted with who are not friends yet; same area first. */
        public List<Suggestion> suggestions = Collections.emptyList();
        public final Tab tab;
        /** Members whose request or friendship is being changed right now. */
        public final Set<String> busy;
        State(boolean loading, boolean error, List<FriendRepository.Person> friends,
              List<FriendRepository.Person> incoming, List<FriendRepository.Person> outgoing, Tab tab, Set<String> busy) {
            this.loading = loading; this.error = error; this.friends = friends; this.incoming = incoming;
            this.outgoing = outgoing; this.tab = tab; this.busy = busy;
        }
        State with(boolean loading, boolean error, Set<String> busy) {
            State next = new State(loading, error, friends, incoming, outgoing, tab, busy);
            next.suggestions = suggestions;
            return next;
        }
    }

    public static final class Suggestion {
        public final FriendRepository.Person person;
        public final boolean sameArea;
        Suggestion(FriendRepository.Person person, boolean sameArea) { this.person = person; this.sameArea = sameArea; }
    }

    static final int MAX_SUGGESTIONS = 5;

    private final FriendRepository repository = new FriendRepository();
    private final ChatRepository chats = new ChatRepository();
    private final SafetyRepository safety = new SafetyRepository();
    private final UserRepository users = new UserRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(true, false,
            Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Tab.FRIENDS, Collections.emptySet()));
    private final MutableLiveData<Integer> actionErrors = new MutableLiveData<>(0);
    private boolean tabChosen;

    public LiveData<State> getState() { return state; }
    /** Increments on every failed action (shown once as a snackbar). */
    public LiveData<Integer> getActionErrors() { return actionErrors; }

    public void load() {
        State s = state.getValue();
        if (s == null) return;
        if (s.friends.isEmpty() && s.incoming.isEmpty() && s.outgoing.isEmpty()) state.setValue(s.with(true, false, s.busy));
        Task<List<FriendRepository.Person>> friends = repository.getFriends();
        Task<List<FriendRepository.Person>> incoming = repository.getIncoming();
        Task<List<FriendRepository.Person>> outgoing = repository.getOutgoing();
        Tasks.whenAllComplete(friends, incoming, outgoing).addOnCompleteListener(done -> {
            State now = state.getValue();
            if (!friends.isSuccessful()) { state.setValue(now.with(false, true, now.busy)); return; }
            List<FriendRepository.Person> in = incoming.isSuccessful() ? incoming.getResult() : Collections.emptyList();
            List<FriendRepository.Person> out = outgoing.isSuccessful() ? outgoing.getResult() : Collections.emptyList();
            Tab tab = now.tab;
            if (!tabChosen && !in.isEmpty()) tab = Tab.REQUESTS; // open on new requests the first time
            tabChosen = true;
            State next = new State(false, false, friends.getResult(), in, out, tab, now.busy);
            next.suggestions = now.suggestions;
            state.setValue(next);
            loadSuggestions(next);
        });
    }

    /** Best effort: a failure only leaves the suggestions out. */
    private void loadSuggestions(State base) {
        String me = repository.currentUid();
        Task<List<Conversation>> inbox = chats.getConversationsForCurrentUser();
        Task<Set<String>> blocked = safety.getBlockedUids();
        Task<User> profile = users.getCurrentProfile();
        Tasks.whenAllComplete(inbox, blocked, profile).addOnCompleteListener(done -> {
            if (!inbox.isSuccessful() || !blocked.isSuccessful()) return;
            Set<String> exclude = new HashSet<>(blocked.getResult());
            for (FriendRepository.Person p : base.friends) exclude.add(p.uid);
            for (FriendRepository.Person p : base.incoming) exclude.add(p.uid);
            for (FriendRepository.Person p : base.outgoing) exclude.add(p.uid);
            List<String> peers = new ArrayList<>();
            for (Conversation c : inbox.getResult()) {
                String peer = ProfileViewModel.peerOf(c, me);
                if (peer != null && !exclude.contains(peer) && !peers.contains(peer)) peers.add(peer);
            }
            String area = profile.isSuccessful() && profile.getResult() != null ? profile.getResult().getLocationText() : "";
            repository.getPeople(peers).addOnSuccessListener(people -> {
                State now = state.getValue();
                if (now == null) return;
                State next = now.with(now.loading, now.error, now.busy);
                next.suggestions = suggest(people, area, MAX_SUGGESTIONS);
                state.setValue(next);
            });
        });
    }

    /** Same area (accent- and case-insensitive) first, then the others, at most {@code max}. */
    static List<Suggestion> suggest(List<FriendRepository.Person> people, String myArea, int max) {
        String area = HomeViewModel.normalize(myArea);
        List<Suggestion> near = new ArrayList<>(), rest = new ArrayList<>();
        for (FriendRepository.Person p : people) {
            boolean same = !area.isEmpty() && area.equals(HomeViewModel.normalize(p.user.getLocationText()));
            (same ? near : rest).add(new Suggestion(p, same));
        }
        near.addAll(rest);
        return near.size() > max ? new ArrayList<>(near.subList(0, max)) : near;
    }

    public void add(String uid) { run(uid, repository.addFriend(uid)); }

    public void selectTab(Tab tab) {
        State s = state.getValue();
        tabChosen = true;
        if (s != null && s.tab != tab) {
            State next = new State(s.loading, s.error, s.friends, s.incoming, s.outgoing, tab, s.busy);
            next.suggestions = s.suggestions;
            state.setValue(next);
        }
    }

    public void accept(String uid) { run(uid, repository.accept(uid)); }
    public void decline(String uid) { run(uid, repository.decline(uid)); }
    public void cancel(String uid) { run(uid, repository.cancelRequest(uid)); }
    public void unfriend(String uid) { run(uid, repository.unfriend(uid)); }

    private void run(String uid, Task<FriendRepository.Relation> action) {
        setBusy(uid, true);
        action.addOnCompleteListener(task -> {
            setBusy(uid, false);
            if (!task.isSuccessful()) {
                Integer n = actionErrors.getValue();
                actionErrors.setValue(n == null ? 1 : n + 1);
            }
            load();
        });
    }

    private void setBusy(String uid, boolean busy) {
        State s = state.getValue();
        if (s == null) return;
        Set<String> ids = new HashSet<>(s.busy);
        if (busy) ids.add(uid); else ids.remove(uid);
        state.setValue(s.with(s.loading, s.error, ids));
    }
}
