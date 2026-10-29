package com.example.ungdungchiasecaycanh.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.StoryRepository;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;

import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

/**
 * The Community story strip as narrow story cards (canvas option C, no autoplay): "New story", the stories, and
 * "Add friends" while no friend has one.
 */
public class StoryStripAdapter extends RecyclerView.Adapter<StoryStripAdapter.Holder> {
    public interface Listener {
        void onCreateStory();
        void onAddFriends();
        void onOpenStory(int storyIndex);
    }

    private static final int CREATE = 0, STORY = 1, ADD_FRIENDS = 2;

    private final Listener listener;
    /** One card per author: their first story in the (grouped) strip, where it starts, and whether all are seen. */
    static final class Group {
        final StoryRepository.Item lead;
        final int start;
        final boolean allSeen;
        Group(StoryRepository.Item lead, int start, boolean allSeen) { this.lead = lead; this.start = start; this.allSeen = allSeen; }
        String author() { return lead.story.getAuthorId() == null ? "" : lead.story.getAuthorId(); }
    }

    private final DiffList<Group> stories = new DiffList<>(this, 1, Group::author,
            g -> Arrays.asList(g.allSeen, g.start, g.lead.story.getStoryId(), g.lead.story.getImageUrl(),
                    g.lead.author == null ? null : g.lead.author.getDisplayName()));
    private boolean showAddFriends = true;

    public StoryStripAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<StoryRepository.Item> items) {
        boolean addFriends = true;
        if (items != null) for (StoryRepository.Item item : items) if (!item.mine) { addFriends = false; break; }
        // The trailing tile goes before the stories change and comes back after, so positions stay right.
        if (showAddFriends && !addFriends) {
            showAddFriends = false;
            notifyItemRemoved(1 + stories.size());
        }
        stories.submit(groups(items));
        if (!showAddFriends && addFriends) {
            showAddFriends = true;
            notifyItemInserted(1 + stories.size());
        }
    }

    /** {@code items} are already grouped by author (StoryStripViewModel.groupedByAuthor). */
    static List<Group> groups(List<StoryRepository.Item> items) {
        List<Group> out = new ArrayList<>();
        if (items == null) return out;
        for (int i = 0; i < items.size(); ) {
            String author = items.get(i).story.getAuthorId();
            int j = i;
            boolean allSeen = true;
            while (j < items.size() && java.util.Objects.equals(author, items.get(j).story.getAuthorId())) {
                allSeen &= items.get(j).seen;
                j++;
            }
            out.add(new Group(items.get(i), i, allSeen));
            i = j;
        }
        return out;
    }

    @Override public int getItemCount() { return 1 + stories.size() + (showAddFriends ? 1 : 0); }

    @Override public int getItemViewType(int position) {
        if (position == 0) return CREATE;
        return position <= stories.size() ? STORY : ADD_FRIENDS;
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_story_card, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        int type = getItemViewType(position);
        android.content.Context c = h.itemView.getContext();
        boolean story = type == STORY;
        h.action.setVisibility(story ? View.GONE : View.VISIBLE);
        h.avatar.setVisibility(story ? View.VISIBLE : View.GONE);
        h.label.setVisibility(story ? View.VISIBLE : View.GONE);
        h.fade.setVisibility(story ? View.VISIBLE : View.GONE);
        if (!story) {
            h.image.setVisibility(View.GONE);
            h.image.setTag(null);
            h.ring.setVisibility(View.GONE);
            boolean create = type == CREATE;
            h.add.setImageResource(create ? R.drawable.ic_add : R.drawable.ic_users);
            h.actionLabel.setText(create ? R.string.story_create : R.string.story_add_friends);
            h.itemView.setContentDescription(c.getString(create ? R.string.story_create : R.string.story_add_friends));
            h.itemView.setOnClickListener(v -> { if (create) listener.onCreateStory(); else listener.onAddFriends(); });
            return;
        }
        Group group = stories.get(position - 1);
        StoryRepository.Item item = group.lead;
        int index = group.start;
        h.ring.setVisibility(group.allSeen ? View.GONE : View.VISIBLE);
        String author = item.author == null || item.author.getDisplayName() == null ? "" : item.author.getDisplayName().trim();
        String name = author.isEmpty() ? c.getString(R.string.community_member) : author;
        h.label.setText(name);
        AvatarBinder.bind(h.avatar, name, item.author == null ? null : item.author.getAvatarUrl(),
                R.drawable.bg_avatar_white_ring, androidx.core.content.ContextCompat.getColor(c, R.color.color_primary));
        h.itemView.setContentDescription(c.getString(R.string.story_by, author, item.story.getTitle()));
        RemoteImageLoader.load(h.image, item.story.getImageUrl(), (int) (110 * c.getResources().getDisplayMetrics().density));
        h.itemView.setOnClickListener(v -> listener.onOpenStory(index));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final View ring, fade, action;
        final ImageView image, add;
        final TextView label, avatar, actionLabel;
        Holder(View v) {
            super(v);
            ring = v.findViewById(R.id.storyRing);
            fade = v.findViewById(R.id.storyFade);
            action = v.findViewById(R.id.storyAction);
            image = v.findViewById(R.id.storyImage);
            add = v.findViewById(R.id.storyAddIcon);
            label = v.findViewById(R.id.storyLabel);
            avatar = v.findViewById(R.id.storyAvatar);
            actionLabel = v.findViewById(R.id.storyActionLabel);
        }
    }
}
