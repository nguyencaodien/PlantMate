package com.example.ungdungchiasecaycanh.ui.community;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailActivity;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.DiffList;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.Holder> {
    public interface Listener {
        void onPostClick(String postId);

        void onLikeClick(String postId);

        /** Author name or avatar tapped: open their public profile. */
        default void onAuthorClick(String authorId) { }
    }

    private final Listener listener;
    /** Shared listings attached to posts, by listing ID; filled in as they load. */
    private Map<String, Listing> listings = Collections.emptyMap();
    private final DiffList<CommunityPostItem> items = new DiffList<>(this, i -> i.post.getPostId(), PostAdapter::shown);

    public PostAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<CommunityPostItem> value) {
        items.submit(value);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setListings(Map<String, Listing> value) {
        listings = value == null ? Collections.emptyMap() : value;
        notifyDataSetChanged();
    }

    /** What a post card displays, including its relative time, so a like only rebinds that card. */
    private static Object shown(CommunityPostItem i) {
        return Arrays.asList(i.post.getText(), i.post.getCategory(), i.post.getImageUrl(), i.post.getListingId(),
                i.post.getAuthorId(), i.authorName, i.authorAvatar, i.authorArea, i.liked, i.likeCount,
                i.likeInProgress, i.commentCount, relativeTime(i));
    }

    private static CharSequence relativeTime(CommunityPostItem item) {
        return item.post.getCreatedAt() == null ? "" : DateUtils.getRelativeTimeSpanString(
                item.post.getCreatedAt().toDate().getTime(), System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS).toString();
    }

    @NonNull
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false));
    }

    public void onBindViewHolder(@NonNull Holder h, int position) {
        CommunityPostItem item = items.get(position);
        String author = item.authorName.isEmpty()
                ? h.itemView.getContext().getString(R.string.community_member) : item.authorName;
        h.author.setText(author);
        AvatarBinder.bind(h.avatar, author, item.authorAvatar, R.drawable.bg_avatar_neutral);
        View.OnClickListener openAuthor = v -> listener.onAuthorClick(item.post.getAuthorId());
        View.OnClickListener share = v -> {
            Intent send = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, author + ": " + item.post.getText());
            v.getContext().startActivity(Intent.createChooser(send,
                    v.getContext().getString(R.string.community_share_post)));
        };
        h.avatar.setOnClickListener(openAuthor);
        h.author.setOnClickListener(openAuthor);
        h.itemView.findViewById(R.id.postShare).setOnClickListener(share);
        h.category.setText(label(item.post.getCategory()));
        bindHashtags(h.tags, h.category, hashtags(item.post.getText()));
        h.text.setText(item.post.getText());
        RemoteImageLoader.load(h.photo, item.post.getImageUrl(),
                h.itemView.getResources().getDisplayMetrics().widthPixels);
        CharSequence when = relativeTime(item);
        String area = item.authorArea.trim();
        h.time.setText(area.isEmpty() ? when : when.length() == 0 ? area : area + " · " + when);
        h.comment.setText(String.valueOf(item.commentCount));
        h.comment.setContentDescription(h.itemView.getContext().getString(R.string.community_comments_action)
                + ", " + item.commentCount);
        bindListing(h, item.post.getListingId());
        h.like.setText(String.valueOf(item.likeCount));
        h.like.setIconResource(item.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        int likeColor = ContextCompat.getColor(h.itemView.getContext(),
                item.liked ? R.color.color_like : R.color.color_text_primary);
        h.like.setIconTint(ColorStateList.valueOf(likeColor));
        h.like.setTextColor(likeColor);
        h.like.setEnabled(!item.likeInProgress);
        h.like.setContentDescription(h.itemView.getResources().getQuantityString(
                item.liked ? R.plurals.community_unlike_desc : R.plurals.community_like_desc, item.likeCount, item.likeCount));
        h.itemView.setOnClickListener(v -> listener.onPostClick(item.post.getPostId()));
        h.like.setOnClickListener(v -> {
            Motion.beat(h.like);
            listener.onLikeClick(item.post.getPostId());
        });
        h.comment.setOnClickListener(v -> listener.onPostClick(item.post.getPostId()));
    }

    /** The attached listing as a panel (photo, type, name, offer); a plain "shared listing" row until it loads. */
    private void bindListing(Holder h, String listingId) {
        boolean has = listingId != null && !listingId.trim().isEmpty();
        h.listingContext.setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) return;
        Listing listing = listings.get(listingId);
        h.listingType.setVisibility(listing == null ? View.GONE : View.VISIBLE);
        h.listingMeta.setVisibility(listing == null ? View.GONE : View.VISIBLE);
        if (listing == null) {
            h.listingName.setText(R.string.community_shared_listing_context);
            h.listingPhoto.setImageDrawable(null);
        } else {
            ListingAdapter.bindCardTag(h.listingType, listing.getType());
            h.listingName.setText(listing.getNickname());
            ListingAdapter.bindOfferLine(h.listingMeta, listing);
            RemoteImageLoader.load(h.listingPhoto, listing.getCoverImageUrl(),
                    h.itemView.getResources().getDimensionPixelSize(R.dimen.post_listing_thumb));
        }
        h.listingContext.setOnClickListener(v -> v.getContext().startActivity(
                new Intent(v.getContext(), ListingDetailActivity.class)
                        .putExtra(ListingDetailActivity.EXTRA_LISTING_ID, listingId)));
    }

    public int getItemCount() {
        return items.size();
    }

    static final int MAX_HASHTAGS = 3;

    /** Up to {@link #MAX_HASHTAGS} distinct #tags written in a post ("#Monstera"), in order. */
    static List<String> hashtags(String text) {
        List<String> tags = new ArrayList<>();
        if (text == null) return tags;
        Matcher m = HASHTAG.matcher(text);
        while (m.find() && tags.size() < MAX_HASHTAGS) {
            String tag = m.group();
            boolean seen = false;
            for (String t : tags) seen |= t.equalsIgnoreCase(tag);
            if (!seen) tags.add(tag);
        }
        return tags;
    }

    private static final Pattern HASHTAG = Pattern.compile("#[\\p{L}\\p{N}_]+");

    /** Grey hashtag chips before the green category chip; rebuilt on every bind. */
    private static void bindHashtags(ViewGroup row, View category, List<String> tags) {
        for (int i = row.getChildCount() - 1; i >= 0; i--) {
            if (row.getChildAt(i) != category) row.removeViewAt(i);
        }
        row.setVisibility(tags.isEmpty() ? View.GONE : View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(row.getContext());
        for (int i = 0; i < tags.size(); i++) {
            TextView chip = (TextView) inflater.inflate(R.layout.item_hashtag_chip, row, false);
            chip.setText(tags.get(i));
            row.addView(chip, i);
        }
    }

    private int label(PostCategory c) {
        switch (c) {
            case SHARING:
                return R.string.post_category_sharing;
            case QUESTION:
                return R.string.post_category_question;
            case PLANT_WIN:
                return R.string.post_category_win;
            case TIP:
                return R.string.post_category_tip;
            default:
                return R.string.post_category_general;
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView avatar, author, category, text, time, listingType, listingName, listingMeta;
        final ViewGroup tags;
        final View listingContext;
        final ImageView listingPhoto;
        final MaterialButton like, comment;
        final ImageView photo;

        Holder(View v) {
            super(v);
            avatar = v.findViewById(R.id.postAvatar);
            author = v.findViewById(R.id.postAuthor);
            category = v.findViewById(R.id.postCategory);
            text = v.findViewById(R.id.postText);
            time = v.findViewById(R.id.postTime);
            listingType = v.findViewById(R.id.postListingType);
            listingName = v.findViewById(R.id.postListingName);
            listingMeta = v.findViewById(R.id.postListingMeta);
            listingPhoto = v.findViewById(R.id.postListingPhoto);
            tags = v.findViewById(R.id.postTags);
            listingContext = v.findViewById(R.id.postListingContext);
            like = v.findViewById(R.id.postLike);
            comment = v.findViewById(R.id.postComment);
            photo = v.findViewById(R.id.postPhoto);
        }
    }
}
