package com.example.ungdungchiasecaycanh.ui.community;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.Holder> {
    public interface Listener {
        /** {@code threadId} is the top-level comment; {@code toReply} when answering a reply (mention it). */
        void onReply(String threadId, String author, boolean toReply);

        void onExpand(String threadId);

        void onReport(String commentId);

        void onBlock(String authorId);

        /** Name, avatar or "@Name" tapped: open that member's profile. */
        void onMember(String uid);
    }

    private final Listener listener;
    private static final int TYPE_COMMENT = 0, TYPE_MORE = 1;
    private final DiffList<CommunityCommentItem> items = new DiffList<>(this, CommunityCommentItem::key,
            i -> i.comment == null ? Arrays.<Object>asList(i.moreCount)
                    : Arrays.<Object>asList(i.authorName, i.comment.getText(), i.comment.getCreatedAt(), i.reply,
                    i.comment.getAuthorId(), i.mentionLength));
    private String currentUid;

    public CommentAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<CommunityCommentItem> value) {
        items.submit(value);
    }

    public void setCurrentUid(String uid) {
        if (java.util.Objects.equals(currentUid, uid)) return;
        currentUid = uid;
        // Report and block show on other members' comments: every row may change.
        notifyItemRangeChanged(0, items.size(), DiffList.REBIND);
    }

    @Override public int getItemViewType(int position) {
        return items.get(position).comment == null ? TYPE_MORE : TYPE_COMMENT;
    }

    @NonNull
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(
                type == TYPE_MORE ? R.layout.item_comment_more : R.layout.item_comment, parent, false));
    }

    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CommunityCommentItem item = items.get(position);
        if (item.comment == null) {
            holder.more.setText(holder.itemView.getResources().getQuantityString(
                    R.plurals.post_detail_view_replies, item.moreCount, item.moreCount));
            holder.more.setOnClickListener(v -> listener.onExpand(item.threadId));
            return;
        }
        int indent = item.reply ? (int) (24 * holder.itemView.getResources().getDisplayMetrics().density) : 0;
        holder.itemView.setPadding(indent, 0, 0, 0);
        String name = item.authorName.isEmpty() ? holder.itemView.getContext().getString(R.string.community_member) : item.authorName;
        holder.author.setText(name);
        holder.avatar.setText(SecondaryChrome.initials(name));
        CharSequence body = item.comment.getText();
        if (item.mentionLength > 0) {
            android.text.SpannableString span = new android.text.SpannableString(body);
            span.setSpan(new android.text.style.ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(
                    holder.itemView.getContext(), R.color.color_success)), 0, item.mentionLength, 0);
            span.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, item.mentionLength, 0);
            if (!item.mentionUid.isEmpty()) {
                span.setSpan(new android.text.style.ClickableSpan() {
                    @Override public void onClick(@NonNull View widget) { listener.onMember(item.mentionUid); }
                    @Override public void updateDrawState(@NonNull android.text.TextPaint paint) { } // keep the green, no underline
                }, 0, item.mentionLength, 0);
            }
            body = span;
        }
        holder.text.setText(body);
        holder.text.setMovementMethod(item.mentionLength > 0 && !item.mentionUid.isEmpty()
                ? android.text.method.LinkMovementMethod.getInstance() : null);
        View.OnClickListener openAuthor = v -> listener.onMember(item.comment.getAuthorId());
        holder.avatar.setOnClickListener(openAuthor);
        holder.author.setOnClickListener(openAuthor);
        holder.avatar.setContentDescription(holder.itemView.getContext().getString(R.string.member_open_profile, name));
        holder.time.setText(item.comment.getCreatedAt() == null ? "" : DateFormat.getDateInstance(DateFormat.MEDIUM).format(item.comment.getCreatedAt().toDate()));
        // Option A: every comment can be answered; a reply to a reply stays in the same thread.
        holder.reply.setOnClickListener(v -> listener.onReply(item.threadId, name, item.reply));
        boolean other = currentUid != null && !currentUid.equals(item.comment.getAuthorId());
        holder.report.setVisibility(other ? View.VISIBLE : View.GONE);
        holder.block.setVisibility(other ? View.VISIBLE : View.GONE);
        holder.report.setOnClickListener(v -> listener.onReport(item.comment.getCommentId()));
        holder.block.setOnClickListener(v -> listener.onBlock(item.comment.getAuthorId()));
    }

    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView author, text, time, reply, report, block, avatar, more;

        Holder(View view) {
            super(view);
            author = view.findViewById(R.id.commentAuthor);
            avatar = view.findViewById(R.id.commentAvatar);
            text = view.findViewById(R.id.commentText);
            time = view.findViewById(R.id.commentTime);
            reply = view.findViewById(R.id.commentReply);
            report = view.findViewById(R.id.commentReport);
            block = view.findViewById(R.id.commentBlock);
            more = view.findViewById(R.id.commentMore);
        }
    }
}
