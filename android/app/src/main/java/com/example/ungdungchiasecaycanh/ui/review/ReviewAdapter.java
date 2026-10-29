package com.example.ungdungchiasecaycanh.ui.review;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
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

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private final DiffList<ReviewListItem> items = new DiffList<>(this, i -> i.review.getReviewId(),
            i -> Arrays.asList(i.reviewerName, i.review.getRating(), i.review.getComment(), i.review.getCreatedAt()));

    public void submitList(List<ReviewListItem> values) {
        items.submit(values);
    }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReviewListItem item = items.get(position);
        String reviewer = item.reviewerName == null || item.reviewerName.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.detail_member_fallback)
                : item.reviewerName;
        holder.name.setText(reviewer);
        holder.avatar.setText(SecondaryChrome.initials(reviewer));
        holder.rating.setRating(item.review.getRating());
        String comment = item.review.getComment();
        holder.comment.setVisibility(comment == null || comment.isEmpty() ? View.GONE : View.VISIBLE);
        holder.comment.setText(comment == null ? "" : comment);
        holder.date.setText(item.review.getCreatedAt() == null ? "" : DateFormat.getDateInstance()
                .format(item.review.getCreatedAt().toDate()));
    }

    @Override public int getItemCount() { return items.size(); }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name, comment, date, avatar;
        final RatingBar rating;
        ViewHolder(@NonNull View view) {
            super(view);
            name = view.findViewById(R.id.review_reviewer_name);
            comment = view.findViewById(R.id.review_comment);
            date = view.findViewById(R.id.review_date);
            rating = view.findViewById(R.id.review_rating);
            avatar = view.findViewById(R.id.review_avatar);
        }
    }
}
