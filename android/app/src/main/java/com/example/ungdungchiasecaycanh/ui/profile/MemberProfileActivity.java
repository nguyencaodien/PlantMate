package com.example.ungdungchiasecaycanh.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.FriendRepository;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.community.PostAdapter;
import com.example.ungdungchiasecaycanh.ui.community.PostDetailActivity;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailActivity;
import com.example.ungdungchiasecaycanh.ui.review.ReviewAdapter;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.DateFormat;
import java.util.Locale;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;

/**
 * Another member's public profile. Opened from a listing (shows their listings first) or from
 * a community post (shows their posts first); reviews and the trust level are always visible.
 */
public class MemberProfileActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "member_uid";
    public static final String EXTRA_CONTEXT = "member_context";
    public static final String CONTEXT_LISTINGS = "listings";
    public static final String CONTEXT_COMMUNITY = "community";

    private static final int TAB_LISTINGS = 0, TAB_POSTS = 1, TAB_REVIEWS = 2;
    private static final String STATE_TAB = "tab";

    private MemberProfileViewModel viewModel;
    private ListingAdapter listingAdapter;
    private PostAdapter postAdapter;
    private ReviewAdapter reviewAdapter;
    private MemberProfileViewModel.Data data;
    private int tab;
    private boolean animated;
    private int shownFriendErrors;
    private FriendRepository.Relation lastRelation;

    public static Intent intent(Context context, String uid, String from) {
        return new Intent(context, MemberProfileActivity.class)
                .putExtra(EXTRA_USER_ID, uid).putExtra(EXTRA_CONTEXT, from);
    }

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member_profile);
        String uid = getIntent().getStringExtra(EXTRA_USER_ID);
        if (uid == null || uid.trim().isEmpty()) { finish(); return; }
        SecondaryChrome.apply(this, findViewById(R.id.memberRoot), findViewById(R.id.memberHero));
        findViewById(R.id.memberBack).setOnClickListener(v -> finish());
        findViewById(R.id.memberErrorBack).setOnClickListener(v -> finish());

        listingAdapter = new ListingAdapter(id -> startActivity(new Intent(this, ListingDetailActivity.class)
                .putExtra(ListingDetailActivity.EXTRA_LISTING_ID, id)));
        RecyclerView listings = findViewById(R.id.memberListings);
        listings.setLayoutManager(new GridLayoutManager(this, 2));
        listings.setAdapter(listingAdapter);

        postAdapter = new PostAdapter(new PostAdapter.Listener() {
            @Override public void onPostClick(String postId) { openPost(postId); }
            @Override public void onLikeClick(String postId) { openPost(postId); }
        });
        RecyclerView posts = findViewById(R.id.memberPosts);
        posts.setLayoutManager(new LinearLayoutManager(this));
        posts.setAdapter(postAdapter);

        reviewAdapter = new ReviewAdapter();
        RecyclerView reviews = findViewById(R.id.memberReviews);
        reviews.setLayoutManager(new LinearLayoutManager(this));
        reviews.setAdapter(reviewAdapter);

        TabLayout tabs = findViewById(R.id.memberTabs);
        tabs.addTab(tabs.newTab().setText(R.string.member_tab_listings));
        tabs.addTab(tabs.newTab().setText(R.string.member_tab_posts));
        tabs.addTab(tabs.newTab().setText(R.string.member_tab_reviews));
        tab = savedInstanceState != null ? savedInstanceState.getInt(STATE_TAB)
                : CONTEXT_COMMUNITY.equals(getIntent().getStringExtra(EXTRA_CONTEXT)) ? TAB_POSTS : TAB_LISTINGS;
        TabLayout.Tab initial = tabs.getTabAt(tab);
        if (initial != null) initial.select();
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab t) { tab = t.getPosition(); showTab(); }
            @Override public void onTabUnselected(TabLayout.Tab t) { }
            @Override public void onTabReselected(TabLayout.Tab t) { }
        });

        viewModel = new ViewModelProvider(this).get(MemberProfileViewModel.class);
        findViewById(R.id.memberRetry).setOnClickListener(v -> viewModel.retry());
        viewModel.getState().observe(this, this::render);
        MaterialButton friend = findViewById(R.id.memberFriend);
        friend.setOnClickListener(v -> onFriendClick());
        MaterialButton message = findViewById(R.id.memberMessage);
        message.setOnClickListener(v -> viewModel.message());
        viewModel.getChatBusy().observe(this, busy -> message.setEnabled(!Boolean.TRUE.equals(busy)));
        viewModel.getChatToOpen().observe(this, id -> {
            if (id == null) return;
            viewModel.chatOpened();
            startActivity(new Intent(this, com.example.ungdungchiasecaycanh.ui.inbox.ConversationActivity.class)
                    .putExtra(com.example.ungdungchiasecaycanh.ui.inbox.ConversationActivity.EXTRA_CONVERSATION_ID, id));
        });
        final int[] shownChatErrors = {0};
        viewModel.getChatErrors().observe(this, n -> {
            if (n == null || n <= shownChatErrors[0]) return;
            shownChatErrors[0] = n;
            Snackbar.make(findViewById(R.id.memberRoot), R.string.member_message_failed, Snackbar.LENGTH_SHORT).show();
        });
        viewModel.getRelation().observe(this, r -> bindFriend());
        viewModel.getFriendBusy().observe(this, busy -> bindFriend());
        viewModel.getFriendErrors().observe(this, n -> {
            if (n == null || n <= shownFriendErrors) return;
            shownFriendErrors = n;
            Snackbar.make(findViewById(R.id.memberRoot), R.string.friends_action_error, Snackbar.LENGTH_SHORT).show();
        });
        viewModel.load(uid);
    }

    @Override protected void onResume() {
        super.onResume();
        if (viewModel != null) viewModel.loadRelation(); // accepted elsewhere (Friends) while away
    }

    private void bindFriend() {
        MaterialButton button = findViewById(R.id.memberFriend);
        FriendRepository.Relation r = viewModel.getRelation().getValue();
        findViewById(R.id.memberActions).setVisibility(r == null || r == FriendRepository.Relation.SELF ? View.GONE : View.VISIBLE);
        if (r == null || r == FriendRepository.Relation.SELF) return;
        button.setEnabled(!Boolean.TRUE.equals(viewModel.getFriendBusy().getValue()));
        switch (r) {
            case FRIENDS: button.setText(R.string.friend_friends); button.setIconResource(R.drawable.ic_check); break;
            case OUTGOING: button.setText(R.string.friend_requested_short); button.setIconResource(R.drawable.ic_clock_15); break;
            case INCOMING: button.setText(R.string.friend_accept); button.setIconResource(R.drawable.ic_users); break;
            default: button.setText(R.string.friend_add); button.setIconResource(R.drawable.ic_users); break;
        }
        if (r == FriendRepository.Relation.FRIENDS && lastRelation != null && lastRelation != r && data != null) {
            Snackbar.make(findViewById(R.id.memberRoot), getString(R.string.friend_now_friends,
                    ((TextView) findViewById(R.id.memberName)).getText()), Snackbar.LENGTH_SHORT).show();
        }
        lastRelation = r;
    }

    private void onFriendClick() {
        FriendRepository.Relation r = viewModel.getRelation().getValue();
        if (r == FriendRepository.Relation.FRIENDS) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.friend_unfriend_title, ((TextView) findViewById(R.id.memberName)).getText()))
                    .setMessage(R.string.friend_unfriend_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.friend_action_unfriend, (d, w) -> viewModel.friendAction())
                    .show();
        } else if (r == FriendRepository.Relation.OUTGOING) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.friend_cancel_title)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.friend_action_cancel, (d, w) -> viewModel.friendAction())
                    .show();
        } else {
            viewModel.friendAction();
        }
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_TAB, tab);
    }

    private void openPost(String postId) {
        startActivity(new Intent(this, PostDetailActivity.class).putExtra(PostDetailActivity.EXTRA_POST_ID, postId));
    }

    private void render(MemberProfileViewModel.State state) {
        boolean loading = state.status == MemberProfileViewModel.Status.LOADING;
        boolean content = state.status == MemberProfileViewModel.Status.CONTENT;
        findViewById(R.id.memberLoading).setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.memberContent).setVisibility(content ? View.VISIBLE : View.GONE);
        findViewById(R.id.memberError).setVisibility(!loading && !content ? View.VISIBLE : View.GONE);
        if (!loading && !content) {
            ((TextView) findViewById(R.id.memberErrorText)).setText(
                    state.status == MemberProfileViewModel.Status.NOT_FOUND
                            ? R.string.member_not_found : R.string.member_load_error);
            findViewById(R.id.memberRetry).setVisibility(
                    state.status == MemberProfileViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
        }
        if (!content || state.data == null) return;
        data = state.data;
        bindHeader(data);
        listingAdapter.submitList(data.listings);
        postAdapter.submitList(data.posts);
        reviewAdapter.submitList(data.reviews);
        showTab();
        if (!animated) {
            animated = true;
            Motion.rise(findViewById(R.id.memberStats), 60);
            Motion.rise(findViewById(R.id.memberTrustCard), 140);
        }
    }

    private void bindHeader(MemberProfileViewModel.Data d) {
        User user = d.user;
        String name = user.getDisplayName() == null || user.getDisplayName().trim().isEmpty()
                ? getString(R.string.detail_member_fallback) : user.getDisplayName();
        text(R.id.memberName, name);
        AvatarBinder.bind(findViewById(R.id.memberAvatar), name, user.getAvatarUrl(),
                R.drawable.bg_avatar_lime_ring, ContextCompat.getColor(this, R.color.brand_lime));
        badge(R.id.memberBadgeEmail, user.isEmailVerified());
        badge(R.id.memberBadgePhone, user.isPhoneVerified());
        badge(R.id.memberBadgeVneid, user.isVneidVerified());
        // "Quận 7, TP.HCM · từ 09/2026" on one line.
        TextView location = findViewById(R.id.memberLocation);
        String area = user.getLocationText() == null ? "" : user.getLocationText().trim();
        String since = user.getCreatedAt() == null ? ""
                : new java.text.SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(user.getCreatedAt().toDate());
        location.setText(area.isEmpty() ? (since.isEmpty() ? "" : getString(R.string.member_since, since))
                : since.isEmpty() ? area : getString(R.string.member_location_since, area, since));
        location.setVisibility(location.getText().length() == 0 ? View.GONE : View.VISIBLE);
        TextView bio = findViewById(R.id.memberBio);
        String bioText = user.getBio() == null ? "" : user.getBio().trim();
        bio.setText(bioText.isEmpty() ? "" : "“" + bioText + "”");
        bio.setVisibility(bioText.isEmpty() ? View.GONE : View.VISIBLE);

        text(R.id.memberStatListings, String.valueOf(d.listings.size()));
        text(R.id.memberStatRehomed, String.valueOf(d.rehomedCount));
        text(R.id.memberStatRating, d.summary.hasReviews
                ? String.format(Locale.getDefault(), "%.1f ★", d.summary.averageRating) : "–");
        text(R.id.memberStatRatingLabel, getResources().getQuantityString(
                R.plurals.member_review_count, d.summary.reviewCount, d.summary.reviewCount));

        Reputation r = d.reputation;
        text(R.id.memberTrustScore, String.valueOf(r.score));
        ((CircularProgressIndicator) findViewById(R.id.memberTrustProgress)).setProgressCompat(r.score, true);
        findViewById(R.id.memberTrustRing).setContentDescription(getString(Reputation.labelOf(r.level)) + ", "
                + getString(R.string.member_trust_score, r.score));
        text(R.id.memberTrustDesc, getString(R.string.member_trust_tip,
                getString(Reputation.labelOf(r.level)), getString(levelDescription(r.level))));
        // Caution reads as a warning: amber shield instead of the green leaf.
        boolean caution = r.level == Reputation.Level.CAUTION;
        ImageView trustIcon = findViewById(R.id.memberTrustIcon);
        trustIcon.setImageResource(caution ? R.drawable.ic_shield : R.drawable.ic_leaf);
        trustIcon.setImageTintList(ContextCompat.getColorStateList(this,
                caution ? R.color.status_reserved_ink : R.color.color_success));
    }

    /** Lime with a tick when verified, dashed and dimmed when not. */
    private void badge(int id, boolean verified) {
        TextView view = findViewById(id);
        view.setBackgroundResource(verified ? R.drawable.bg_member_badge_on : R.drawable.bg_member_badge_off);
        view.setTextColor(ContextCompat.getColor(this, verified ? R.color.brand_forest : R.color.member_hero_muted));
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(verified ? R.drawable.ic_check_14 : 0, 0, 0, 0);
        androidx.core.widget.TextViewCompat.setCompoundDrawableTintList(view,
                ContextCompat.getColorStateList(this, R.color.brand_forest));
    }

    private void showTab() {
        if (data == null) return;
        View listings = findViewById(R.id.memberListings);
        View posts = findViewById(R.id.memberPosts);
        View reviews = findViewById(R.id.memberReviews);
        TextView empty = findViewById(R.id.memberEmpty);
        boolean isEmpty;
        int emptyText;
        if (tab == TAB_POSTS) { isEmpty = data.posts.isEmpty(); emptyText = R.string.member_empty_posts; }
        else if (tab == TAB_REVIEWS) { isEmpty = data.reviews.isEmpty(); emptyText = R.string.member_empty_reviews; }
        else { isEmpty = data.listings.isEmpty(); emptyText = R.string.member_empty_listings; }
        listings.setVisibility(tab == TAB_LISTINGS && !isEmpty ? View.VISIBLE : View.GONE);
        posts.setVisibility(tab == TAB_POSTS && !isEmpty ? View.VISIBLE : View.GONE);
        reviews.setVisibility(tab == TAB_REVIEWS && !isEmpty ? View.VISIBLE : View.GONE);
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        empty.setText(emptyText);
    }

    /** Five stars, the average rounded to the nearest whole star (★ filled, ☆ empty). */
    static String stars(double average) {
        long filled = Math.round(Math.max(0, Math.min(5, average)));
        StringBuilder out = new StringBuilder();
        for (int i = 1; i <= 5; i++) out.append(i <= filled ? '★' : '☆');
        return out.toString();
    }


    private static int levelDescription(Reputation.Level level) {
        switch (level) {
            case TOP: return R.string.member_level_top_desc;
            case TRUSTED: return R.string.member_level_trusted_desc;
            case CAUTION: return R.string.member_level_caution_desc;
            default: return R.string.member_level_new_desc;
        }
    }

    private void text(int id, CharSequence value) { ((TextView) findViewById(id)).setText(value); }
}
