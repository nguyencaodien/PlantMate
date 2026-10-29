package com.example.ungdungchiasecaycanh.ui.review;

import android.os.Bundle;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;

import java.util.Locale;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;

public class UserReviewsActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "userId";
    private ReviewAdapter adapter;
    private View loading, content, empty, error;

    private boolean rowsAnimated;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_reviews);
        SecondaryChrome.apply(this, findViewById(R.id.reviews_root), findViewById(R.id.reviews_hero));
        Motion.rise(findViewById(R.id.reviews_summary_card), 180);
        Motion.start(findViewById(R.id.reviews_mam));
        loading = findViewById(R.id.reviews_loading);
        content = findViewById(R.id.reviews_list);
        empty = findViewById(R.id.reviews_empty);
        error = findViewById(R.id.reviews_error);
        RecyclerView list = (RecyclerView) content;
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter();
        list.setAdapter(adapter);
        ReviewViewModel viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);
        viewModel.getState().observe(this, this::render);
        findViewById(R.id.reviews_back).setOnClickListener(view -> finish());
        findViewById(R.id.reviews_retry).setOnClickListener(view -> viewModel.retry());
        viewModel.load(getIntent().getStringExtra(EXTRA_USER_ID));
    }

    private void render(ReviewViewModel.UiState state) {
        loading.setVisibility(state.status == ReviewViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
        content.setVisibility(state.status == ReviewViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
        empty.setVisibility(state.status == ReviewViewModel.Status.EMPTY ? View.VISIBLE : View.GONE);
        error.setVisibility(state.status == ReviewViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
        String name = state.targetUser == null || state.targetUser.getDisplayName() == null
                ? getString(R.string.detail_member_fallback) : state.targetUser.getDisplayName();
        ((TextView) findViewById(R.id.reviews_member_name)).setText(name);
        TextView summary = findViewById(R.id.reviews_summary);
        summary.setText(state.summary.hasReviews
                ? getString(R.string.reviews_summary_format,
                    String.format(Locale.getDefault(), "%.1f", state.summary.averageRating),
                    state.summary.reviewCount)
                : getString(R.string.reviews_no_reviews));
        RatingBar stars = findViewById(R.id.reviews_summary_stars);
        stars.setVisibility(state.summary.hasReviews ? View.VISIBLE : View.GONE);
        stars.setRating((float) state.summary.averageRating);
        adapter.submitList(state.items);
        if (!rowsAnimated && !state.items.isEmpty()) {
            rowsAnimated = true;
            Motion.staggerRows(findViewById(R.id.reviews_list), 240, 60);
        }
    }
}
