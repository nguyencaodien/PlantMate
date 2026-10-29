package com.example.ungdungchiasecaycanh.ui.notifications;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.DiffList;

import java.util.Arrays;
import java.util.List;

/** Section headers ("Today", "Earlier") and notification rows. */
public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface Listener {
        void onOpen(NotificationsViewModel.Row row);
        void onAccept(NotificationsViewModel.Row row);
        void onLater(NotificationsViewModel.Row row);
    }

    private static final int TYPE_HEADER = 0, TYPE_ROW = 1;
    private final Listener listener;
    private final DiffList<Object> items = new DiffList<>(this, NotificationAdapter::key, NotificationAdapter::content);

    public NotificationAdapter(Listener listener) {
        this.listener = listener;
    }

    /** Strings are section headers; everything else is a {@link NotificationsViewModel.Row}. */
    public void submit(List<Object> value) {
        items.submit(value);
    }

    private static Object key(Object item) {
        if (item instanceof String) return "h:" + item;
        NotificationsViewModel.Row r = (NotificationsViewModel.Row) item;
        return r.kind + ":" + r.id;
    }

    private static List<Object> content(Object item) {
        if (item instanceof String) return Arrays.asList(item);
        NotificationsViewModel.Row r = (NotificationsViewModel.Row) item;
        return Arrays.asList(r.unread, r.actorName, r.text, r.plantNames, r.at);
    }

    @Override public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ROW;
    }

    @Override public int getItemCount() {
        return items.size();
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        if (type == TYPE_HEADER) {
            TextView header = new TextView(parent.getContext());
            header.setTextAppearance(R.style.TextAppearance_PlantMate_Meta);
            header.setTextSize(13);
            int pad = Math.round(8 * parent.getResources().getDisplayMetrics().density);
            header.setPadding(pad, pad * 2, pad, pad / 2);
            header.setAccessibilityHeading(true);
            return new RecyclerView.ViewHolder(header) { };
        }
        return new RowHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (item instanceof String) {
            ((TextView) holder.itemView).setText((String) item);
            return;
        }
        NotificationsViewModel.Row row = (NotificationsViewModel.Row) item;
        RowHolder h = (RowHolder) holder;
        Context c = h.itemView.getContext();
        h.title.setText(title(c, row));
        h.meta.setText(meta(c, row));
        h.icon.setImageResource(icon(row.kind));
        h.icon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(c,
                row.kind == NotificationsViewModel.Kind.COMMENT || row.kind == NotificationsViewModel.Kind.REPLY
                        ? R.color.color_background : R.color.white)));
        h.icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, tint(row.kind))));
        h.itemView.setBackgroundResource(row.unread ? R.drawable.bg_notification_unread : R.drawable.bg_notification_row);
        h.dot.setVisibility(row.unread ? View.VISIBLE : View.INVISIBLE);
        boolean friend = row.kind == NotificationsViewModel.Kind.FRIEND;
        h.actions.setVisibility(friend ? View.VISIBLE : View.GONE);
        h.accept.setOnClickListener(v -> listener.onAccept(row));
        h.later.setOnClickListener(v -> listener.onLater(row));
        h.itemView.setOnClickListener(v -> listener.onOpen(row));
    }

    static CharSequence title(Context c, NotificationsViewModel.Row r) {
        String name = r.actorName.isEmpty() ? c.getString(R.string.community_member) : r.actorName;
        switch (r.kind) {
            case WATER:
                return bold(r.plantNames.size() == 1
                        ? c.getString(R.string.notifications_water_one, r.plantNames.get(0))
                        : c.getString(R.string.notifications_water_many, c.getResources().getQuantityString(
                        R.plurals.notifications_plants, r.plantNames.size(), r.plantNames.size())),
                        r.plantNames.size() == 1 ? r.plantNames.get(0)
                                : c.getResources().getQuantityString(R.plurals.notifications_plants, r.plantNames.size(), r.plantNames.size()));
            case FRIEND: return bold(c.getString(R.string.notifications_friend, name), name);
            case MENTION: return bold(c.getString(R.string.notifications_mention, name, r.text), name);
            case REPLY: return bold(c.getString(R.string.notifications_reply, name), name);
            case COMMENT: return bold(c.getString(R.string.notifications_comment, name), name);
            default: return bold(c.getString(R.string.notifications_like, name), name);
        }
    }

    private static String meta(Context c, NotificationsViewModel.Row r) {
        String when = r.at == 0 ? "" : DateUtils.getRelativeTimeSpanString(r.at, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS).toString();
        String what;
        switch (r.kind) {
            case WATER:
                what = android.text.TextUtils.join(", ", r.plantNames);
                return what;
            case FRIEND: return c.getString(R.string.notifications_meta_friends);
            case MENTION: what = c.getString(R.string.notifications_meta_comment); break;
            case REPLY: case COMMENT: what = c.getString(R.string.notifications_quote, r.text); break;
            default: what = c.getString(R.string.notifications_meta_post); break;
        }
        return when.isEmpty() ? what : what + " · " + when;
    }

    /** {@code text} with the first occurrence of {@code name} in bold. */
    private static CharSequence bold(String text, String name) {
        SpannableStringBuilder out = new SpannableStringBuilder(text);
        int at = name.isEmpty() ? -1 : text.indexOf(name);
        if (at >= 0) out.setSpan(new StyleSpan(Typeface.BOLD), at, at + name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return out;
    }

    private static int icon(NotificationsViewModel.Kind kind) {
        switch (kind) {
            case WATER: return R.drawable.ic_care_drop;
            case FRIEND: return R.drawable.ic_person_add;
            case MENTION: return R.drawable.ic_at;
            case LIKE: return R.drawable.ic_heart_filled;
            default: return R.drawable.ic_chat;
        }
    }

    private static int tint(NotificationsViewModel.Kind kind) {
        switch (kind) {
            case WATER: return R.color.care_water;
            case FRIEND: return R.color.notif_friend;
            case MENTION: return R.color.color_success;
            case LIKE: return R.color.notif_like;
            default: return R.color.color_text_primary;
        }
    }

    static final class RowHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title, meta;
        final View actions, accept, later, dot;

        RowHolder(View v) {
            super(v);
            icon = v.findViewById(R.id.notification_icon);
            title = v.findViewById(R.id.notification_title);
            meta = v.findViewById(R.id.notification_meta);
            actions = v.findViewById(R.id.notification_actions);
            accept = v.findViewById(R.id.notification_accept);
            later = v.findViewById(R.id.notification_later);
            dot = v.findViewById(R.id.notification_dot);
        }
    }
}
