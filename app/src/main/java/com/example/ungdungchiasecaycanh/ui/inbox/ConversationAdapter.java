package com.example.ungdungchiasecaycanh.ui.inbox;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.DiffList;

import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {
    public interface Listener { void onConversationClick(String conversationId); }

    private final DiffList<InboxConversationItem> items = new DiffList<>(this, i -> i.conversationId,
            i -> Arrays.asList(i.peerDisplayName, i.listingNickname, i.lastMessage, i.updatedAt));
    private final Listener listener;

    public ConversationAdapter(Listener listener) { this.listener = listener; }

    private java.util.Map<String, Long> seen = java.util.Collections.emptyMap();
    private long seenSince = Long.MAX_VALUE;

    /** When each chat was last open here; changes rebind every row. */
    public void setSeen(java.util.Map<String, Long> value, long since) {
        seen = value;
        seenSince = since;
        notifyItemRangeChanged(0, items.size(), DiffList.REBIND);
    }

    public void submitList(List<InboxConversationItem> values) {
        items.submit(values);
    }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        InboxConversationItem item = items.get(position);
        String peerName = nonblank(item.peerDisplayName)
                ? item.peerDisplayName : holder.itemView.getContext().getString(R.string.detail_member_fallback);
        holder.peer.setText(peerName);
        holder.avatar.setText(SecondaryChrome.initials(peerName));
        holder.listing.setVisibility(item.direct ? View.GONE : View.VISIBLE);
        holder.listing.setText(nonblank(item.listingNickname)
                ? item.listingNickname : holder.itemView.getContext().getString(R.string.inbox_listing_unavailable));
        Long at = seen.get(item.conversationId);
        boolean unread = ChatUnread.isUnread(item.lastMessage, item.updatedAt, at == null ? seenSince : at);
        holder.unread.setVisibility(unread ? View.VISIBLE : View.GONE);
        holder.preview.setTypeface(null, unread ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        holder.preview.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(),
                unread ? R.color.color_text_primary : R.color.color_text_secondary));
        holder.preview.setText(nonblank(item.lastMessage)
                ? item.lastMessage : holder.itemView.getContext().getString(R.string.inbox_no_messages));
        holder.time.setText(item.updatedAt == null ? "" : DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT).format(item.updatedAt.toDate()));
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onConversationClick(item.conversationId);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    private static boolean nonblank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView peer, listing, preview, time, avatar;
        final View unread;
        ViewHolder(@NonNull View view) {
            super(view);
            peer = view.findViewById(R.id.inbox_item_peer);
            listing = view.findViewById(R.id.inbox_item_listing);
            preview = view.findViewById(R.id.inbox_item_preview);
            time = view.findViewById(R.id.inbox_item_time);
            avatar = view.findViewById(R.id.inbox_item_avatar);
            unread = view.findViewById(R.id.inbox_item_unread);
        }
    }
}
