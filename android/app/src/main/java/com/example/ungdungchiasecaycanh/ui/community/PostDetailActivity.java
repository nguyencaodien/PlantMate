package com.example.ungdungchiasecaycanh.ui.community;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailActivity;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailViewModel;
import com.example.ungdungchiasecaycanh.ui.profile.MemberProfileActivity;
import com.example.ungdungchiasecaycanh.ui.safety.ReportBottomSheet;
import com.example.ungdungchiasecaycanh.ui.safety.SafetyActionViewModel;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;

public class PostDetailActivity extends AppCompatActivity {
    public static final String EXTRA_POST_ID = "postId";
    private PostDetailViewModel vm;
    private SafetyActionViewModel safety;
    private CommentAdapter adapter;
    private TextInputEditText input;
    private MaterialButton reportPost, blockAuthor, deletePost;
    private int shownDeleteFailed;
    private boolean cardAnimated;
    private TextView hidden;
    private MaterialButton hiddenUnblock;
    private String safetyAuthorId;
    private int shownErrors, shownSuccess, shownSafetyChange, shownSafetyError;
    private boolean firstResume = true;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_post_detail);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.postDetailRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(SecondaryChrome.useDarkStatusIcons(this));
        Motion.sheet(findViewById(R.id.postDetailComposer), 560, 160);
        setResult(Activity.RESULT_OK);
        vm = new ViewModelProvider(this).get(PostDetailViewModel.class);
        safety = new ViewModelProvider(this).get(SafetyActionViewModel.class);
        adapter = new CommentAdapter(new CommentAdapter.Listener() {
            @Override
            public void onReply(String threadId, String name, boolean toReply) {
                vm.beginReply(threadId, name);
                // Answering a reply: start with "@Name " so the thread shows who it is for.
                if (toReply) {
                    String mention = "@" + name + " ";
                    input.setText(mention);
                    input.setSelection(mention.length());
                }
                input.requestFocus();
            }

            @Override
            public void onMember(String uid) {
                if (uid != null && !uid.isEmpty()) startActivity(MemberProfileActivity.intent(PostDetailActivity.this, uid,
                        MemberProfileActivity.CONTEXT_COMMUNITY));
            }

            @Override
            public void onExpand(String threadId) {
                vm.expandThread(threadId);
            }

            @Override
            public void onReport(String id) {
                ReportBottomSheet.newInstance(ReportTargetType.COMMENT, id, vm.getState().getValue().post.getPostId())
                        .show(getSupportFragmentManager(), "report-comment");
            }

            @Override
            public void onBlock(String uid) {
                confirmBlock(uid);
            }
        });
        adapter.setCurrentUid(vm.getCurrentUserId());
        RecyclerView list = findViewById(R.id.postDetailComments);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setNestedScrollingEnabled(false);
        list.setAdapter(adapter);
        input = findViewById(R.id.postDetailInput);
        addSafetyControls();
        findViewById(R.id.postDetailBack).setOnClickListener(v -> finish());
        findViewById(R.id.postDetailRetry).setOnClickListener(v -> vm.retry());
        findViewById(R.id.postDetailErrorRetry).setOnClickListener(v -> vm.retry());
        findViewById(R.id.postDetailLike).setOnClickListener(v -> {
            Motion.beat(findViewById(R.id.postDetailLike));
            vm.toggleLike();
        });
        findViewById(R.id.postDetailSend).setOnClickListener(v -> vm.submitComment(String.valueOf(input.getText())));
        findViewById(R.id.postDetailCancelReply).setOnClickListener(v -> {
            String typed = String.valueOf(input.getText());
            String mention = "@" + vm.getState().getValue().replyAuthorName + " ";
            if (typed.equals(mention)) input.setText(""); // only the untouched "@Name " we filled in
            vm.cancelReply();
        });
        reportPost.setOnClickListener(v -> {
            PostDetailViewModel.State state = vm.getState().getValue();
            if (state != null && state.post != null)
                ReportBottomSheet.newInstance(ReportTargetType.POST, state.post.getPostId(), "")
                        .show(getSupportFragmentManager(), "report-post");
        });
        blockAuthor.setOnClickListener(v -> {
            SafetyActionViewModel.State state = safety.getState().getValue();
            if (state == null || !state.known || state.busy) return;
            if (state.blocked) safety.toggle();
            else confirmBlock(state.targetUid);
        });
        deletePost.setOnClickListener(v -> new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(R.string.community_delete_post_title)
                .setMessage(R.string.community_delete_post_body)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.community_delete_post_confirm, (dialog, which) -> {
                    deletePost.setEnabled(false);
                    vm.deletePost();
                })
                .show());
        vm.getDeleted().observe(this, done -> {
            if (!Boolean.TRUE.equals(done)) return;
            android.widget.Toast.makeText(this, R.string.community_post_deleted, android.widget.Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        });
        vm.getDeleteFailed().observe(this, n -> {
            if (n == null || n <= shownDeleteFailed) return;
            shownDeleteFailed = n;
            deletePost.setEnabled(true);
            Snackbar.make(input, R.string.community_delete_post_failed, Snackbar.LENGTH_SHORT).show();
        });
        vm.getState().observe(this, this::render);
        safety.getState().observe(this, this::renderSafety);
        vm.load(getIntent().getStringExtra(EXTRA_POST_ID));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) firstResume = false;
        else {
            safetyAuthorId = null;
            vm.retry();
        }
    }

    private void addSafetyControls() {
        NestedScrollView scroll = findViewById(R.id.postDetailContent);
        LinearLayout body = (LinearLayout) scroll.getChildAt(0);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        reportPost = new MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        reportPost.setText(R.string.safety_report_post);
        blockAuthor = new MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        blockAuthor.setText(R.string.safety_block);
        int quiet = ContextCompat.getColor(this, R.color.color_text_secondary);
        reportPost.setTextColor(quiet);
        blockAuthor.setTextColor(quiet);
        // The author sees "Delete post" in the same place instead.
        deletePost = new MaterialButton(this, null, androidx.appcompat.R.attr.borderlessButtonStyle);
        deletePost.setText(R.string.community_delete_post);
        deletePost.setTextColor(ContextCompat.getColor(this, R.color.color_danger));
        deletePost.setIconResource(R.drawable.ic_delete_outline);
        deletePost.setIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_danger)));
        row.addView(deletePost, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(reportPost, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.addView(blockAuthor, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        // Safety actions sit right under the post card.
        body.addView(row, body.indexOfChild(findViewById(R.id.postDetailCard)) + 1);
        row.setVisibility(View.GONE);
        row.setTag("safetyRow");
        LinearLayout hiddenGroup = new LinearLayout(this);
        hiddenGroup.setOrientation(LinearLayout.VERTICAL);
        hiddenGroup.setGravity(Gravity.CENTER);
        hiddenGroup.setPadding(24, 24, 24, 24);
        hidden = new TextView(this);
        hidden.setText(R.string.safety_hidden_post);
        hidden.setGravity(Gravity.CENTER);
        hiddenGroup.addView(hidden);
        hiddenUnblock = new MaterialButton(this);
        hiddenUnblock.setText(R.string.safety_unblock);
        hiddenUnblock.setOnClickListener(v -> {
            String uid = vm.getHiddenAuthorUid();
            if (uid != null) safety.unblock(uid);
        });
        hiddenGroup.addView(hiddenUnblock);
        MaterialButton hiddenBack = new MaterialButton(this);
        hiddenBack.setText(R.string.safety_back);
        hiddenBack.setOnClickListener(v -> finish());
        hiddenGroup.addView(hiddenBack);
        hiddenGroup.setVisibility(View.GONE);
        hiddenGroup.setTag("hiddenGroup");
        ((FrameLayout) scroll.getParent()).addView(hiddenGroup, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
    }

    private void confirmBlock(String uid) {
        if (uid == null || uid.equals(vm.getCurrentUserId())) return;
        new MaterialAlertDialogBuilder(this).setTitle(R.string.safety_block_title)
                .setMessage(R.string.safety_block_message).setNegativeButton(R.string.report_cancel, null)
                .setPositiveButton(R.string.safety_block, (dialog, which) -> {
                    safety.block(uid);
                }).show();
    }

    private void renderSafety(SafetyActionViewModel.State state) {
        blockAuthor.setEnabled(state.known && !state.busy && safetyAuthorId != null && safetyAuthorId.equals(state.targetUid));
        blockAuthor.setText(state.blocked ? R.string.safety_unblock : R.string.safety_block);
        if (state.changedVersion > shownSafetyChange) {
            shownSafetyChange = state.changedVersion;
            if (state.blocked && state.targetUid != null && state.targetUid.equals(safetyAuthorId))
                vm.hideBlockedPost();
            else if (vm.getHiddenAuthorUid() != null) {
                vm.retry();
            } else {
                vm.refreshComments();
                if (safetyAuthorId != null) safety.load(safetyAuthorId);
            }
        }
        if (state.errorVersion > shownSafetyError) {
            shownSafetyError = state.errorVersion;
            Snackbar.make(input, R.string.safety_action_failed, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void render(PostDetailViewModel.State s) {
        findViewById(R.id.postDetailLoading).setVisibility(s.status == PostDetailViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
        findViewById(R.id.postDetailContent).setVisibility(s.status == PostDetailViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
        findViewById(R.id.postDetailNotFound).setVisibility(s.status == PostDetailViewModel.Status.NOT_FOUND ? View.VISIBLE : View.GONE);
        findViewById(R.id.postDetailError).setVisibility(s.status == PostDetailViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
        ((View) hidden.getParent()).setVisibility(s.status == PostDetailViewModel.Status.HIDDEN ? View.VISIBLE : View.GONE);
        if (s.status == PostDetailViewModel.Status.HIDDEN) {
            String uid = vm.getHiddenAuthorUid();
            if (uid != null && !uid.equals(safetyAuthorId)) {
                safetyAuthorId = uid;
                safety.load(uid);
            }
        }
        if (s.status != PostDetailViewModel.Status.CONTENT) return;
        boolean signedIn = vm.getCurrentUserId() != null;
        boolean other = signedIn && !vm.getCurrentUserId().equals(s.post.getAuthorId());
        ((View) reportPost.getParent()).setVisibility(signedIn ? View.VISIBLE : View.GONE);
        reportPost.setVisibility(other ? View.VISIBLE : View.GONE);
        blockAuthor.setVisibility(other ? View.VISIBLE : View.GONE);
        deletePost.setVisibility(signedIn && !other ? View.VISIBLE : View.GONE);
        if (other && !s.post.getAuthorId().equals(safetyAuthorId)) {
            safetyAuthorId = s.post.getAuthorId();
            safety.load(safetyAuthorId);
        }
        String author = s.authorName.isEmpty() ? getString(R.string.community_member) : s.authorName;
        text(R.id.postDetailAuthor, author);
        AvatarBinder.bind(findViewById(R.id.postDetailAvatar), author, vm.getAuthorAvatar(), R.drawable.bg_lime_circle);
        String authorId = s.post.getAuthorId();
        View.OnClickListener openAuthor = v -> startActivity(
                MemberProfileActivity.intent(this, authorId,
                        MemberProfileActivity.CONTEXT_COMMUNITY));
        findViewById(R.id.postDetailAuthor).setOnClickListener(openAuthor);
        findViewById(R.id.postDetailProfile).setOnClickListener(openAuthor);
        String shareText = author + ": " + s.post.getText();
        findViewById(R.id.postDetailShare).setOnClickListener(v -> startActivity(Intent.createChooser(
                new Intent(Intent.ACTION_SEND).setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, shareText),
                getString(R.string.community_share_post))));
        findViewById(R.id.postDetailAvatar).setOnClickListener(openAuthor);
        text(R.id.postDetailTime, s.post.getCreatedAt() == null ? "" : String.valueOf(
                DateUtils.getRelativeTimeSpanString(s.post.getCreatedAt().toDate().getTime(),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)));
        text(R.id.postDetailCategory, "# " + getString(category(s.post.getCategory())));
        if (!cardAnimated) {
            cardAnimated = true;
            Motion.rise(findViewById(R.id.postDetailCard), 120);
        }
        text(R.id.postDetailText, s.post.getText());
        ImageView photo = findViewById(R.id.postDetailPhoto);
        if (!s.post.getImageUrl().equals(photo.getTag())) {
            RemoteImageLoader.load(photo, s.post.getImageUrl(),
                    getResources().getDisplayMetrics().widthPixels);
        }
        MaterialButton like = findViewById(R.id.postDetailLike);
        like.setText(String.valueOf(s.likeCount));
        like.setIconResource(s.liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        like.setIconTint(ColorStateList.valueOf(ContextCompat.getColor(
                this, s.liked ? R.color.color_like : R.color.color_text_secondary)));
        like.setEnabled(!s.likeBusy);
        like.setContentDescription(getResources().getQuantityString(
                s.liked ? R.plurals.community_unlike_desc : R.plurals.community_like_desc, s.likeCount, s.likeCount));
        adapter.submitList(s.comments);
        text(R.id.postDetailCommentCount, getString(R.string.post_detail_comment_count, s.totalCommentCount));
        text(R.id.postDetailCommentBadge, String.valueOf(s.totalCommentCount));
        View listingCard = findViewById(R.id.postDetailListing);
        listingCard.setVisibility(PostDetailViewModel.hasListingContext(s.post) ? View.VISIBLE : View.GONE);
        if (s.listing != null) {
            bindListing(s.listing);
            listingCard.setEnabled(true);
            listingCard.setOnClickListener(v -> {
                Intent i = new Intent(this, ListingDetailActivity.class);
                i.putExtra(ListingDetailActivity.EXTRA_LISTING_ID, s.listing.getListingId());
                startActivity(i);
            });
        } else if (s.listingUnavailable) {
            text(R.id.postDetailListingName, getString(R.string.inbox_listing_unavailable));
            text(R.id.postDetailListingMeta, "");
            listingCard.setEnabled(false);
        }
        View replyBar = findViewById(R.id.postDetailReplyBar);
        replyBar.setVisibility(s.replyParentId.isEmpty() ? View.GONE : View.VISIBLE);
        text(R.id.postDetailReplying, getString(R.string.post_detail_replying, s.replyAuthorName));
        findViewById(R.id.postDetailSend).setEnabled(!s.submitting);
        if (s.commentSuccessVersion > shownSuccess) {
            shownSuccess = s.commentSuccessVersion;
            input.setText("");
        }
        if (s.actionErrorVersion > shownErrors) {
            shownErrors = s.actionErrorVersion;
            Snackbar.make(input, R.string.post_detail_action_error, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void bindListing(Listing l) {
        TextView name = findViewById(R.id.postDetailListingName);
        name.setText(l.getNickname());
        name.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_plantmate_brand, 0, 0, 0);
        name.setCompoundDrawablePadding((int) (8 * getResources().getDisplayMetrics().density));
        String species = l.getSpecies() == null || l.getSpecies().trim().isEmpty() ? "" : l.getSpecies() + " · ";
        text(R.id.postDetailListingMeta, species + ListingDetailViewModel.typeLabel(this, l) + " · " + ListingDetailViewModel.statusLabel(this, l.getStatus()) + " · " + l.getLocationText());
    }

    private void text(int id, String value) {
        ((TextView) findViewById(id)).setText(value == null ? "" : value);
    }

    private void text(int id, int value) {
        ((TextView) findViewById(id)).setText(value);
    }

    private int category(PostCategory c) {
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
}
