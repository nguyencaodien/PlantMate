package com.example.ungdungchiasecaycanh.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

/** Friends (open profile, unfriend from the menu) or requests (accept / decline, or cancel one we sent). */
public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.Holder> {
    public enum Kind { FRIEND, INCOMING, OUTGOING, SUGGESTED }

    public interface Listener {
        void onOpen(String uid);
        void onAccept(String uid);
        void onDecline(String uid);
        void onCancel(String uid);
        void onUnfriend(FriendRepository.Person person);
        void onAdd(String uid);
    }

    public static final class Row {
        final FriendRepository.Person person;
        final Kind kind;
        /** Suggestions only: lives in the same area as you. */
        final boolean sameArea;
        public Row(FriendRepository.Person person, Kind kind) { this(person, kind, false); }
        public Row(FriendRepository.Person person, Kind kind, boolean sameArea) {
            this.person = person; this.kind = kind; this.sameArea = sameArea;
        }
    }

    private final Listener listener;
    private Set<String> busy = Collections.emptySet();
    private final DiffList<Row> rows = new DiffList<>(this, r -> r.person.uid, r -> Arrays.asList(r.kind, r.sameArea,
            r.person.name(), r.person.user.getAvatarUrl(), r.person.user.getLocationText(), busy.contains(r.person.uid)));

    public FriendAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<Row> rows, Set<String> busy) {
        this.busy = busy;
        this.rows.submit(rows);
    }

    @Override public int getItemCount() { return rows.size(); }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        Row row = rows.get(position);
        FriendRepository.Person p = row.person;
        String name = p.name().isEmpty() ? h.itemView.getContext().getString(R.string.detail_member_fallback) : p.name();
        h.name.setText(name);
        AvatarBinder.bind(h.avatar, name, p.user.getAvatarUrl(), R.drawable.bg_avatar_neutral);
        String area = p.user.getLocationText() == null ? "" : p.user.getLocationText().trim();
        h.sub.setText(row.kind == Kind.INCOMING ? h.itemView.getContext().getString(R.string.friend_request_incoming_sub)
                : row.kind == Kind.OUTGOING ? h.itemView.getContext().getString(R.string.friend_request_outgoing_sub)
                : row.kind == Kind.SUGGESTED ? h.itemView.getContext().getString(row.sameArea
                        ? R.string.friend_suggest_same_area : R.string.friend_suggest_chatted, area)
                : area);
        h.sub.setVisibility(h.sub.getText().length() == 0 ? View.GONE : View.VISIBLE);
        boolean working = busy.contains(p.uid);
        h.itemView.setOnClickListener(v -> listener.onOpen(p.uid));

        boolean suggested = row.kind == Kind.SUGGESTED;
        h.primary.setVisibility(row.kind == Kind.INCOMING || suggested ? View.VISIBLE : View.GONE);
        h.secondary.setVisibility(row.kind == Kind.FRIEND || suggested ? View.GONE : View.VISIBLE);
        h.more.setVisibility(row.kind == Kind.FRIEND ? View.VISIBLE : View.GONE);
        h.primary.setEnabled(!working);
        h.secondary.setEnabled(!working);
        h.more.setEnabled(!working);
        h.primary.setText(suggested ? R.string.friend_add : R.string.friend_action_accept);
        h.primary.setOnClickListener(v -> {
            if (suggested) listener.onAdd(p.uid); else listener.onAccept(p.uid);
        });
        h.secondary.setText(row.kind == Kind.INCOMING ? R.string.friend_action_decline : R.string.friend_action_cancel);
        h.secondary.setOnClickListener(v -> {
            if (row.kind == Kind.INCOMING) listener.onDecline(p.uid); else listener.onCancel(p.uid);
        });
        h.more.setContentDescription(h.itemView.getContext().getString(R.string.friend_more_desc, name));
        h.more.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            menu.getMenu().add(R.string.friend_action_unfriend);
            menu.setOnMenuItemClickListener(item -> { listener.onUnfriend(p); return true; });
            menu.show();
        });
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView avatar, name, sub;
        final MaterialButton primary, secondary, more;
        Holder(View v) {
            super(v);
            avatar = v.findViewById(R.id.friendAvatar);
            name = v.findViewById(R.id.friendName);
            sub = v.findViewById(R.id.friendSub);
            primary = v.findViewById(R.id.friendPrimary);
            secondary = v.findViewById(R.id.friendSecondary);
            more = v.findViewById(R.id.friendMore);
        }
    }
}
