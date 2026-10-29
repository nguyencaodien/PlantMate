package com.example.ungdungchiasecaycanh.ui.community;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.StoryRepository;
import com.example.ungdungchiasecaycanh.ui.profile.MemberProfileActivity;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import com.google.android.material.button.MaterialButton;

/** Full-screen stories: each shows for {@link #STORY_MS}, then the next; tap left / right to move. */
public class StoryViewerActivity extends AppCompatActivity {
    public static final String EXTRA_STORY_IDS = "story_ids";
    public static final String EXTRA_START = "story_start";
    private static final long STORY_MS = 6_000;

    private StoryViewerViewModel vm;
    private ValueAnimator timer;
    private String shownStoryId;

    public static Intent intent(Context context, ArrayList<String> storyIds, int start) {
        return new Intent(context, StoryViewerActivity.class)
                .putStringArrayListExtra(EXTRA_STORY_IDS, storyIds).putExtra(EXTRA_START, start);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);
        List<String> ids = getIntent().getStringArrayListExtra(EXTRA_STORY_IDS);
        if (ids == null || ids.isEmpty()) { finish(); return; }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
        View top = findViewById(R.id.storyViewerTop);
        View bottom = findViewById(R.id.storyViewerBottom);
        int topPad = top.getPaddingTop(), bottomPad = bottom.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.storyViewerRoot), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            top.setPadding(top.getPaddingLeft(), topPad + bars.top, top.getPaddingRight(), top.getPaddingBottom());
            bottom.setPadding(bottom.getPaddingLeft(), bottom.getPaddingTop(), bottom.getPaddingRight(), bottomPad + bars.bottom);
            return insets;
        });

        vm = new ViewModelProvider(this).get(StoryViewerViewModel.class);
        findViewById(R.id.storyViewerClose).setOnClickListener(v -> finish());
        findViewById(R.id.storyViewerEmptyClose).setOnClickListener(v -> finish());
        findViewById(R.id.storyViewerNext).setOnClickListener(v -> { if (!vm.next()) finish(); });
        findViewById(R.id.storyViewerPrev).setOnClickListener(v -> vm.previous());
        findViewById(R.id.storyViewerDelete).setOnClickListener(v -> confirmDelete());
        vm.getState().observe(this, this::render);
        MaterialButton likeButton = findViewById(R.id.storyViewerLike);
        likeButton.setOnClickListener(v -> vm.toggleLike());
        vm.getLike().observe(this, this::bindLike);
        vm.getDeleteFailed().observe(this, failed -> {
            if (!Boolean.TRUE.equals(failed)) return;
            vm.deleteErrorShown();
            Snackbar.make(findViewById(R.id.storyViewerRoot), R.string.story_delete_error, Snackbar.LENGTH_SHORT).show();
        });
        vm.load(ids, getIntent().getIntExtra(EXTRA_START, 0));
    }

    private void render(StoryViewerViewModel.State s) {
        findViewById(R.id.storyViewerLoading).setVisibility(s.loading ? View.VISIBLE : View.GONE);
        StoryRepository.Item item = s.current();
        boolean has = !s.loading && item != null;
        findViewById(R.id.storyViewerEmpty).setVisibility(!s.loading && item == null ? View.VISIBLE : View.GONE);
        findViewById(R.id.storyViewerTop).setVisibility(has ? View.VISIBLE : View.GONE);
        findViewById(R.id.storyViewerBottom).setVisibility(has ? View.VISIBLE : View.GONE);
        ImageView image = findViewById(R.id.storyViewerImage);
        if (!has) { stopTimer(); image.setImageDrawable(null); return; }

        bindSegments(s.stories.size(), s.index);
        String name = item.author == null || item.author.getDisplayName() == null
                ? getString(R.string.detail_member_fallback) : item.author.getDisplayName();
        ((TextView) findViewById(R.id.storyViewerName)).setText(name);
        AvatarBinder.bind(findViewById(R.id.storyViewerAvatar), name,
                item.author == null ? "" : item.author.getAvatarUrl(), R.drawable.bg_avatar_neutral);
        long age = System.currentTimeMillis() - item.story.createdMillis();
        ((TextView) findViewById(R.id.storyViewerTime)).setText(item.story.getCreatedAt() == null || age < 60_000
                ? getString(R.string.story_just_now)
                : DateUtils.getRelativeTimeSpanString(item.story.createdMillis(), System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS));
        findViewById(R.id.storyViewerAuthor).setOnClickListener(v -> startActivity(MemberProfileActivity.intent(
                this, item.story.getAuthorId(), MemberProfileActivity.CONTEXT_COMMUNITY)));
        findViewById(R.id.storyViewerDelete).setVisibility(item.mine ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.storyViewerTitle)).setText(item.story.getTitle());
        TextView caption = findViewById(R.id.storyViewerCaption);
        caption.setText(item.story.getCaption());
        caption.setVisibility(item.story.getCaption().isEmpty() ? View.GONE : View.VISIBLE);
        image.setContentDescription(getString(R.string.story_photo_desc, item.story.getTitle()));

        if (!item.story.getStoryId().equals(shownStoryId)) {
            shownStoryId = item.story.getStoryId();
            image.setImageDrawable(null);
            RemoteImageLoader.load(image, item.story.getImageUrl(), getResources().getDisplayMetrics().widthPixels);
            startTimer(s.index);
        }
    }

    /** Friends see a heart to tap; the author sees how many friends hearted the story. */
    private void bindLike(StoryViewerViewModel.Like like) {
        StoryViewerViewModel.State s = vm.getState().getValue();
        StoryRepository.Item item = s == null ? null : s.current();
        MaterialButton button = findViewById(R.id.storyViewerLike);
        if (like == null || item == null) { button.setVisibility(View.GONE); return; }
        button.setVisibility(item.mine && like.count < 0 ? View.INVISIBLE : View.VISIBLE);
        if (item.mine) {
            button.setClickable(false);
            button.setIconResource(R.drawable.ic_heart_filled);
            button.setText(getResources().getQuantityString(R.plurals.story_like_count, Math.max(0, like.count),
                    Math.max(0, like.count)));
            button.setContentDescription(button.getText());
        } else {
            button.setClickable(true);
            button.setIconResource(like.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            button.setText(like.liked ? R.string.story_liked : R.string.story_like);
            button.setContentDescription(getString(like.liked ? R.string.story_unlike_desc : R.string.story_like_desc));
        }
    }

    private void bindSegments(int count, int current) {
        LinearLayout row = findViewById(R.id.storyViewerSegments);
        if (row.getChildCount() != count) {
            row.removeAllViews();
            int gap = Math.round(4 * getResources().getDisplayMetrics().density);
            for (int i = 0; i < count; i++) {
                LinearProgressIndicator bar = new LinearProgressIndicator(this);
                bar.setMax(1000);
                bar.setTrackThickness(Math.round(3 * getResources().getDisplayMetrics().density));
                bar.setTrackCornerRadius(bar.getTrackThickness());
                bar.setIndicatorColor(0xFFFFFFFF);
                bar.setTrackColor(0x59FFFFFF);
                bar.setIndicatorTrackGapSize(0); // one continuous bar per story, no M3 gap or stop dot
                bar.setTrackStopIndicatorSize(0);
                bar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
                if (i > 0) lp.setMarginStart(gap);
                row.addView(bar, lp);
            }
        }
        for (int i = 0; i < count; i++) {
            ((LinearProgressIndicator) row.getChildAt(i)).setProgress(i < current ? 1000 : 0);
        }
    }

    private void startTimer(int index) {
        stopTimer();
        LinearLayout row = findViewById(R.id.storyViewerSegments);
        if (index >= row.getChildCount()) return;
        LinearProgressIndicator bar = (LinearProgressIndicator) row.getChildAt(index);
        timer = ValueAnimator.ofInt(0, 1000);
        timer.setDuration(STORY_MS);
        timer.setInterpolator(new LinearInterpolator());
        timer.addUpdateListener(a -> bar.setProgress((int) a.getAnimatedValue()));
        timer.addListener(new android.animation.AnimatorListenerAdapter() {
            private boolean cancelled;
            @Override public void onAnimationCancel(android.animation.Animator animation) { cancelled = true; }
            @Override public void onAnimationEnd(android.animation.Animator animation) {
                if (!cancelled && !isFinishing() && !vm.next()) finish();
            }
        });
        timer.start();
    }

    private void stopTimer() {
        if (timer != null) timer.cancel();
        timer = null;
    }

    private void confirmDelete() {
        if (timer != null) timer.pause();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.story_delete_confirm_title)
                .setMessage(R.string.story_delete_confirm_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.story_delete, (d, w) -> { shownStoryId = null; vm.deleteCurrent(); })
                .setOnDismissListener(d -> { if (timer != null) timer.resume(); })
                .show();
    }

    @Override protected void onPause() {
        super.onPause();
        if (timer != null) timer.pause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (timer != null) timer.resume();
    }

    @Override protected void onDestroy() {
        stopTimer();
        super.onDestroy();
    }
}
