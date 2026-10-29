package com.example.ungdungchiasecaycanh.ui.profile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.repository.ListingRepository;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.Review;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.auth.GoogleSignInHelper;
import com.example.ungdungchiasecaycanh.ui.auth.SignInActivity;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;
import com.example.ungdungchiasecaycanh.ui.inbox.InboxActivity;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailActivity;
import com.example.ungdungchiasecaycanh.ui.listing.MyListingsActivity;
import com.example.ungdungchiasecaycanh.ui.listing.MyListingsViewModel;
import com.example.ungdungchiasecaycanh.ui.listing.SavedListingsActivity;
import com.example.ungdungchiasecaycanh.ui.review.ReviewSummary;
import com.example.ungdungchiasecaycanh.ui.review.UserReviewsActivity;
import com.example.ungdungchiasecaycanh.ui.settings.SettingsActivity;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Profile (canvas B "Hồ sơ"): the member, ink "Edit profile" and green "My garden", four numbers, the rank panel,
 * the "still available?" card, Listed / Given / Saved tabs over real listings, the newest review and the account menu.
 */
public class ProfileFragment extends Fragment {
    /** Listings shown under a tab; the rest open in the full list. */
    static final int MAX_CARDS = 4;
    private View loading;
    private View errorContainer;
    private View profileContent;
    private ProfileViewModel viewModel;
    private User user;
    private ProfileViewModel.Lists lists;
    private ProfileViewModel.Stats stats;
    private int shownActionErrors;
    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && viewModel != null) {
                    viewModel.retry();
                }
            });

    public ProfileFragment() {
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loading = view.findViewById(R.id.profile_loading);
        errorContainer = view.findViewById(R.id.profile_error_container);
        profileContent = view.findViewById(R.id.profile_content);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            loading.setVisibility(state.status == ProfileViewModel.Status.LOADING ? View.VISIBLE : View.GONE);
            profileContent.setVisibility(state.status == ProfileViewModel.Status.LOADED ? View.VISIBLE : View.GONE);
            errorContainer.setVisibility(state.status == ProfileViewModel.Status.ERROR ? View.VISIBLE : View.GONE);
            if (state.status == ProfileViewModel.Status.LOADED && state.user != null) {
                user = state.user;
                bindMember(view);
                bindNumbers(view);
            } else if (state.status == ProfileViewModel.Status.SIGNED_OUT) {
                openSignIn();
            }
        });

        ((MaterialButton) view.findViewById(R.id.profile_retry)).setOnClickListener(v -> viewModel.retry());
        bindVerification(view);
        view.findViewById(R.id.profile_share).setOnClickListener(v -> shareProfile());
        bindThemeToggle(view);
        view.findViewById(R.id.profile_row_settings).setOnClickListener(v -> open(SettingsActivity.class));
        view.findViewById(R.id.profile_edit).setOnClickListener(v -> editProfileLauncher.launch(
                new Intent(requireContext(), EditProfileActivity.class)));
        view.findViewById(R.id.profile_garden).setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setSelectedItemId(R.id.nav_garden);
        });
        view.findViewById(R.id.profile_inbox).setOnClickListener(v -> open(InboxActivity.class));
        view.findViewById(R.id.profile_reviews).setOnClickListener(v -> openReviews());
        view.findViewById(R.id.profile_review_card).setOnClickListener(v -> openReviews());
        view.findViewById(R.id.profile_friends).setOnClickListener(v -> open(FriendsActivity.class));
        view.findViewById(R.id.profile_sign_out).setOnClickListener(v -> showSignOutConfirmation());

        TabLayout tabs = view.findViewById(R.id.profile_tabs);
        for (ProfileViewModel.Tab ignored : ProfileViewModel.Tab.values()) tabs.addTab(tabs.newTab(), false);
        TabLayout.Tab current = tabs.getTabAt(viewModel.tab.ordinal());
        if (current != null) current.select();
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                viewModel.tab = ProfileViewModel.Tab.values()[tab.getPosition()];
                bindTabs(view);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        viewModel.getFriendRequests().observe(getViewLifecycleOwner(), count -> {
            TextView badge = view.findViewById(R.id.profile_friends_requests);
            int n = count == null ? 0 : count;
            badge.setVisibility(n > 0 ? View.VISIBLE : View.GONE);
            badge.setText(getResources().getQuantityString(R.plurals.friend_request_count, n, n));
        });
        viewModel.getStats().observe(getViewLifecycleOwner(), value -> {
            stats = value;
            bindNumbers(view);
            bindReview(view, null);
        });
        viewModel.getLatestReviewer().observe(getViewLifecycleOwner(), reviewer -> bindReview(view, reviewer));
        viewModel.getLists().observe(getViewLifecycleOwner(), value -> {
            lists = value;
            bindInbox(view);
            bindCheck(view);
            bindTabs(view);
            bindMember(view);
            bindNumbers(view);
        });
        viewModel.getActionErrors().observe(getViewLifecycleOwner(), count -> {
            if (count != null && count > shownActionErrors) {
                shownActionErrors = count;
                Snackbar.make(view, R.string.profile_action_failed, Snackbar.LENGTH_SHORT).show();
            }
        });
        bindTabs(view);
        Motion.rise(view.findViewById(R.id.profile_hero), 60);
        Motion.rise(view.findViewById(R.id.profile_stats), 140);
        Motion.rise(view.findViewById(R.id.profile_rank), 200);
        viewModel.loadProfile();
    }

    // ---------------- member ----------------

    private void bindMember(View view) {
        if (user == null) return;
        String name = user.getDisplayName();
        ((TextView) view.findViewById(R.id.profile_display_name)).setText(name);
        AvatarBinder.bind(view.findViewById(R.id.profile_avatar), name, user.getAvatarUrl(), R.drawable.bg_avatar_ink);
        String area = user.getLocationText() == null ? "" : user.getLocationText().trim();
        TextView location = view.findViewById(R.id.profile_location);
        location.setText(area);
        location.setVisibility(area.isEmpty() ? View.GONE : View.VISIBLE);
        TextView joined = view.findViewById(R.id.profile_joined);
        joined.setVisibility(user.getCreatedAt() == null ? View.GONE : View.VISIBLE);
        if (user.getCreatedAt() != null) joined.setText(getString(R.string.profile_joined_month,
                new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(user.getCreatedAt().toDate())));
        boolean verified = viewModel.isEmailVerified();
        view.findViewById(R.id.profile_verified).setVisibility(verified ? View.VISIBLE : View.GONE);
        String bio = user.getBio();
        ((TextView) view.findViewById(R.id.profile_bio)).setText(bio == null || bio.trim().isEmpty()
                ? getString(R.string.profile_no_bio) : bio);
        int given = lists == null || lists.given == null ? 0
                : ProfileViewModel.countRehomed(lists.given, ListingType.GIVE_AWAY);
        view.findViewById(R.id.profile_role).setVisibility(given > 0 ? View.VISIBLE : View.GONE);
    }

    /** Listings · given · swapped · rating, then the rank panel built from the same numbers. */
    private void bindNumbers(View view) {
        boolean listsReady = lists != null && lists.mine != null && lists.given != null;
        int given = listsReady ? ProfileViewModel.countRehomed(lists.given, ListingType.GIVE_AWAY) : 0;
        int swapped = listsReady ? ProfileViewModel.countRehomed(lists.given, ListingType.TRADE) : 0;
        text(view, R.id.profile_stat_listings, listsReady ? String.valueOf(lists.all.size()) : "–");
        text(view, R.id.profile_stat_given, listsReady ? String.valueOf(given) : "–");
        text(view, R.id.profile_stat_swapped, listsReady ? String.valueOf(swapped) : "–");
        ReviewSummary reviews = stats == null ? null : stats.reviews;
        text(view, R.id.profile_stat_rating, reviews != null && reviews.hasReviews
                ? String.format(Locale.getDefault(), "%.1f", reviews.averageRating) : "–");

        long days = user == null || user.getCreatedAt() == null ? 0
                : (System.currentTimeMillis() - user.getCreatedAt().toDate().getTime()) / 86_400_000L;
        int rehomed = listsReady ? lists.given.size() : 0;
        int count = reviews == null ? 0 : reviews.reviewCount;
        double average = reviews == null ? 0 : reviews.averageRating;
        boolean email = viewModel.isEmailVerified();
        boolean phone = user != null && user.isPhoneVerified();
        boolean vneid = user != null && user.isVneidVerified();
        boolean photo = Reputation.hasPhotoAndBio(user);
        int verified = Reputation.verifiedPoints(email, phone, vneid, photo);
        Reputation r = Reputation.of(average, count, rehomed, days, verified);
        text(view, R.id.profile_rank_title, getString(Reputation.labelOf(r.level)));
        text(view, R.id.profile_rank_score, String.valueOf(r.score));
        ((CircularProgressIndicator) view.findViewById(R.id.profile_rank_ring)).setProgressCompat(r.score, true);
        view.findViewById(R.id.profile_rank).setContentDescription(
                getString(Reputation.labelOf(r.level)) + ", " + getString(R.string.profile_rank_score, r.score));

        // The hint names the next missing check and the score it would bring; then the history rules.
        int hint = !email ? R.string.trust_hint_email : !phone ? R.string.trust_hint_phone
                : !vneid ? R.string.trust_hint_vneid : !photo ? R.string.trust_hint_photo : 0;
        int next = !email ? Reputation.EMAIL_POINTS : !phone ? Reputation.PHONE_POINTS
                : !vneid ? Reputation.VNEID_POINTS : Reputation.PROFILE_POINTS;
        text(view, R.id.profile_rank_hint, hint != 0
                ? getString(hint, Reputation.of(average, count, rehomed, days, verified + next).score)
                : rankHint(r.level, count, average, rehomed));

        TrustRow.bind(view.findViewById(R.id.profile_trust_email), R.string.trust_email, Reputation.EMAIL_POINTS,
                email, v -> viewModel.resendVerification());
        TrustRow.bind(view.findViewById(R.id.profile_trust_phone), R.string.trust_phone, Reputation.PHONE_POINTS,
                phone, v -> open(PhoneVerifyActivity.class));
        TrustRow.bind(view.findViewById(R.id.profile_trust_vneid), R.string.trust_vneid, Reputation.VNEID_POINTS,
                vneid, v -> open(VneidDemoActivity.class));
        TrustRow.bind(view.findViewById(R.id.profile_trust_photo), R.string.trust_photo, Reputation.PROFILE_POINTS,
                photo, v -> editProfileLauncher.launch(new Intent(requireContext(), EditProfileActivity.class)));

        text(view, R.id.profile_trust_reviews, count == 0 ? getString(R.string.trust_no_reviews)
                : getString(R.string.trust_reviews_value, String.format(Locale.getDefault(), "%.1f", average), count));
        text(view, R.id.profile_trust_rehomed, listsReady
                ? getResources().getQuantityString(R.plurals.trust_rehomed_value, rehomed, rehomed) : "–");
        text(view, R.id.profile_trust_since, user == null || user.getCreatedAt() == null ? "–"
                : new SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(user.getCreatedAt().toDate()));
    }

    /** What it takes to reach the next level, from the same rules as {@link Reputation#of}. */
    private String rankHint(Reputation.Level level, int reviews, double average, int rehomed) {
        if (level == Reputation.Level.NEW) {
            return getString(R.string.profile_rank_to_trusted, Math.max(1, 2 - rehomed),
                    getString(R.string.member_level_trusted));
        }
        if (level == Reputation.Level.TRUSTED) {
            int moreReviews = Math.max(0, 5 - reviews);
            int morePlants = Math.max(0, 3 - rehomed);
            if (moreReviews == 0 && morePlants == 0) {
                return getString(R.string.profile_rank_keep_rating, getString(R.string.member_level_top));
            }
            return getString(R.string.profile_rank_to_top, moreReviews, morePlants, getString(R.string.member_level_top));
        }
        return getString(level == Reputation.Level.TOP ? R.string.member_level_top_desc : R.string.member_level_caution_desc);
    }


    private void shareProfile() {
        String name = user == null ? "" : user.getDisplayName();
        Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.profile_share_text, name));
        startActivity(Intent.createChooser(send, getString(R.string.profile_share)));
    }

    // ---------------- inbox and review ----------------

    private void bindInbox(View view) {
        TextView count = view.findViewById(R.id.profile_inbox_count);
        boolean any = lists != null && lists.chatCount > 0;
        count.setVisibility(any ? View.VISIBLE : View.GONE);
        if (any) count.setText(String.valueOf(lists.chatCount));
    }

    /** The newest review, or a line saying reviews come after a finished exchange. */
    private void bindReview(View view, @Nullable User reviewer) {
        Review latest = stats == null ? null : stats.latest;
        int total = stats == null ? 0 : stats.reviews.reviewCount;
        MaterialButton all = view.findViewById(R.id.profile_reviews);
        all.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        all.setText(getString(R.string.profile_reviews_all, total));
        boolean has = latest != null;
        view.findViewById(R.id.profile_review_avatar).setVisibility(has ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.profile_review_stars).setVisibility(has ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.profile_review_date).setVisibility(has ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.profile_review_name).setVisibility(has ? View.VISIBLE : View.GONE);
        TextView body = view.findViewById(R.id.profile_review_text);
        ((LinearLayout.LayoutParams) body.getLayoutParams()).topMargin = has ? dp(10) : 0;
        if (!has) {
            body.setText(stats == null ? getString(R.string.profile_cards_loading) : getString(R.string.profile_reviews_none));
            body.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary));
            return;
        }
        String name = reviewer == null ? getString(R.string.community_member) : reviewer.getDisplayName();
        AvatarBinder.bind(view.findViewById(R.id.profile_review_avatar), name,
                reviewer == null ? "" : reviewer.getAvatarUrl(), R.drawable.bg_avatar_neutral);
        text(view, R.id.profile_review_name, name);
        text(view, R.id.profile_review_stars, MemberProfileActivity.stars(latest.getRating()));
        text(view, R.id.profile_review_date, latest.getCreatedAt() == null ? ""
                : new SimpleDateFormat("dd/MM", Locale.getDefault()).format(latest.getCreatedAt().toDate()));
        String comment = latest.getComment() == null ? "" : latest.getComment().trim();
        body.setVisibility(comment.isEmpty() ? View.GONE : View.VISIBLE);
        body.setText(comment);
        body.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_primary));
    }

    // ---------------- "still available?" ----------------

    /** "Is Monstera still available?" for the listing that waited longest; hidden when none needs it. */
    private void bindCheck(View view) {
        long now = System.currentTimeMillis();
        Listing listing = ProfileViewModel.listingToCheck(lists == null ? null : lists.mine, now);
        View card = view.findViewById(R.id.profile_check);
        card.setVisibility(listing == null ? View.GONE : View.VISIBLE);
        if (listing == null) return;
        long days = (now - (listing.getUpdatedAt() != null ? listing.getUpdatedAt() : listing.getCreatedAt())
                .toDate().getTime()) / 86_400_000L;
        boolean hidden = ListingRepository.hiddenAsStale(listing, now);
        text(view, R.id.profile_check_title, getString(R.string.listing_check_title, listing.getNickname()));
        text(view, R.id.profile_check_body, hidden ? getString(R.string.listing_check_hidden)
                : getResources().getQuantityString(R.plurals.listing_check_days, (int) days, (int) days));
        RemoteImageLoader.load(view.findViewById(R.id.profile_check_photo), listing.getCoverImageUrl(), dp(52));
        view.findViewById(R.id.profile_check_yes).setOnClickListener(v -> viewModel.confirmStillAvailable(listing));
        view.findViewById(R.id.profile_check_given).setOnClickListener(
                v -> viewModel.updateStatus(listing, ListingStatus.REHOMED));
        card.setOnClickListener(v -> openListing(listing));
    }

    // ---------------- tabs ----------------

    private void bindTabs(View view) {
        TabLayout tabs = view.findViewById(R.id.profile_tabs);
        int[] labels = {R.string.profile_tab_mine, R.string.profile_tab_given, R.string.profile_tab_saved};
        List<List<Listing>> sources = new ArrayList<>();
        sources.add(lists == null ? null : lists.mine);
        sources.add(lists == null ? null : lists.given);
        sources.add(lists == null ? null : lists.saved);
        for (int i = 0; i < labels.length; i++) {
            TabLayout.Tab tab = tabs.getTabAt(i);
            if (tab == null) continue;
            List<Listing> items = sources.get(i);
            tab.setText(items == null ? getString(labels[i])
                    : getString(R.string.profile_tab_count, getString(labels[i]), items.size()));
        }

        ProfileViewModel.Tab tab = viewModel.tab;
        List<Listing> items = sources.get(tab.ordinal());
        LinearLayout cards = view.findViewById(R.id.profile_cards);
        cards.removeAllViews();
        TextView empty = view.findViewById(R.id.profile_cards_empty);
        MaterialButton all = view.findViewById(R.id.profile_cards_all);
        List<Listing> shown = items == null ? Collections.emptyList() : items;
        boolean failed = lists != null && items == null;
        empty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(lists == null ? getString(R.string.profile_cards_loading)
                : failed ? getString(R.string.profile_cards_error)
                : getString(tab == ProfileViewModel.Tab.MINE ? R.string.profile_cards_empty_mine
                : tab == ProfileViewModel.Tab.GIVEN ? R.string.profile_cards_empty_given : R.string.profile_cards_empty_saved));
        empty.setTextColor(ContextCompat.getColor(requireContext(), failed ? R.color.color_primary : R.color.color_text_secondary));
        empty.setClickable(failed);
        empty.setOnClickListener(failed ? v -> viewModel.loadStats() : null);
        all.setVisibility(shown.size() > MAX_CARDS ? View.VISIBLE : View.GONE);
        all.setText(getString(R.string.profile_cards_all, shown.size()));
        all.setOnClickListener(v -> {
            if (tab == ProfileViewModel.Tab.SAVED) { open(SavedListingsActivity.class); return; }
            startActivity(new Intent(requireContext(), MyListingsActivity.class).putExtra(MyListingsActivity.EXTRA_FILTER,
                    (tab == ProfileViewModel.Tab.MINE ? MyListingsViewModel.Filter.ACTIVE : MyListingsViewModel.Filter.REHOMED).name()));
        });

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int count = Math.min(MAX_CARDS, shown.size());
        if (tab != ProfileViewModel.Tab.MINE) {
            for (int i = 0; i < count; i++) {
                View row = row(inflater, cards, shown.get(i), tab);
                if (i > 0) row.setBackgroundResource(R.drawable.bg_top_hairline_transparent);
                cards.addView(row);
            }
            return;
        }
        LinearLayout line = null;
        for (int i = 0; i < count; i++) {
            if (i % 2 == 0) {
                line = new LinearLayout(requireContext());
                line.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                if (i > 0) lp.topMargin = dp(16);
                cards.addView(line, lp);
            }
            View card = card(inflater, line, shown.get(i));
            if (i % 2 == 1) ((LinearLayout.LayoutParams) card.getLayoutParams()).setMarginStart(dp(12));
            line.addView(card);
        }
        if (line != null && line.getChildCount() == 1) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 0, 1f);
            lp.setMarginStart(dp(12));
            line.addView(new View(requireContext()), lp);
        }
    }

    /** A "Listed" card: type tag while open, the status otherwise; long-press manages the status. */
    private View card(LayoutInflater inflater, ViewGroup parent, Listing listing) {
        View card = inflater.inflate(R.layout.item_profile_listing, parent, false);
        RemoteImageLoader.load(card.findViewById(R.id.pl_photo), listing.getCoverImageUrl(), dp(180));
        ((TextView) card.findViewById(R.id.pl_name)).setText(listing.getNickname());
        ((TextView) card.findViewById(R.id.pl_meta)).setText(offer(listing));
        TextView status = card.findViewById(R.id.pl_status);
        if (listing.getStatus() == ListingStatus.ACTIVE) ListingAdapter.bindCardTag(status, listing.getType());
        else bindStatusTag(status, listing.getStatus());
        card.setOnClickListener(v -> openListing(listing));
        card.setOnLongClickListener(v -> { showStatusChoices(listing); return true; });
        return card;
    }

    /** A "Given" or "Saved" row: photo, name, and when it was given or what is offered. */
    private View row(LayoutInflater inflater, ViewGroup parent, Listing listing, ProfileViewModel.Tab tab) {
        View row = inflater.inflate(R.layout.item_profile_row, parent, false);
        RemoteImageLoader.load(row.findViewById(R.id.pr_photo), listing.getCoverImageUrl(), dp(52));
        ((TextView) row.findViewById(R.id.pr_name)).setText(listing.getNickname());
        TextView meta = row.findViewById(R.id.pr_meta);
        if (tab == ProfileViewModel.Tab.GIVEN) {
            Date when = listing.getUpdatedAt() != null ? listing.getUpdatedAt().toDate()
                    : listing.getCreatedAt() != null ? listing.getCreatedAt().toDate() : null;
            meta.setText(when == null ? offer(listing) : getString(R.string.profile_given_on,
                    new SimpleDateFormat("dd/MM", Locale.getDefault()).format(when)));
        } else {
            ListingAdapter.bindOfferLine(meta, listing);
        }
        row.setOnClickListener(v -> openListing(listing));
        return row;
    }

    /** Reserved = white with a clock, hidden / rehomed = grey. */
    private void bindStatusTag(TextView tag, ListingStatus status) {
        Context c = requireContext();
        int icon = 0, label;
        if (status == ListingStatus.RESERVED) {
            tag.setBackgroundResource(R.drawable.bg_tag_white);
            label = R.string.profile_status_reserved;
            icon = R.drawable.ic_clock_15;
        } else {
            tag.setBackgroundResource(R.drawable.bg_tag_grey);
            label = status == ListingStatus.REHOMED ? R.string.profile_status_given : R.string.profile_status_hidden;
        }
        int color = ContextCompat.getColor(c, status == ListingStatus.RESERVED ? R.color.ink_on_photo : R.color.color_text_primary);
        tag.setText(label);
        tag.setTextColor(color);
        tag.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(tag, ColorStateList.valueOf(color));
    }

    private String offer(Listing l) {
        if (l.getType() == ListingType.TRADE) {
            return l.getLookingFor().trim().isEmpty() ? getString(R.string.listing_type_trade)
                    : getString(R.string.home_offer_trade, l.getLookingFor().trim());
        }
        if (l.getType() == ListingType.SELL) {
            return getString(R.string.listing_type_sell_detail, ListingAdapter.formatVnd(l.getPrice()));
        }
        return getString(R.string.listing_offer_free);
    }

    /** Every status the listing may move to; "given away" asks once more. */
    private void showStatusChoices(Listing listing) {
        List<ListingStatus> legal = new ArrayList<>();
        for (ListingStatus target : ListingStatus.values()) {
            if (MyListingsViewModel.canTransition(listing.getStatus(), target)) legal.add(target);
        }
        if (legal.isEmpty()) return;
        String[] labels = new String[legal.size()];
        for (int i = 0; i < legal.size(); i++) labels[i] = getString(statusLabel(legal.get(i)));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.my_listing_manage_status)
                .setItems(labels, (dialog, which) -> {
                    ListingStatus target = legal.get(which);
                    if (target == ListingStatus.REHOMED) confirmRehomed(listing);
                    else viewModel.updateStatus(listing, target);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void confirmRehomed(Listing listing) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.my_listing_rehomed_title)
                .setMessage(R.string.my_listing_rehomed_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.my_listing_confirm_rehomed,
                        (dialog, which) -> viewModel.updateStatus(listing, ListingStatus.REHOMED))
                .show();
    }

    private static int statusLabel(ListingStatus status) {
        switch (status) {
            case RESERVED: return R.string.listing_status_reserved;
            case UNAVAILABLE: return R.string.listing_status_unavailable;
            case REHOMED: return R.string.listing_status_rehomed;
            default: return R.string.listing_status_active;
        }
    }

    // ---------------- account ----------------

    private void openReviews() {
        String uid = viewModel.getCurrentUserId();
        if (uid == null) { openSignIn(); return; }
        startActivity(new Intent(requireContext(), UserReviewsActivity.class)
                .putExtra(UserReviewsActivity.EXTRA_USER_ID, uid));
    }

    private void showSignOutConfirmation() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_sign_out_title)
                .setMessage(R.string.profile_sign_out_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_sign_out, (dialog, which) -> {
                    viewModel.signOut();
                    GoogleSignInHelper.clearState(requireContext());
                })
                .show();
    }

    private void openSignIn() {
        Intent intent = new Intent(requireContext(), SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void openListing(Listing listing) {
        startActivity(new Intent(requireContext(), ListingDetailActivity.class)
                .putExtra(ListingDetailActivity.EXTRA_LISTING_ID, listing.getListingId()));
    }

    private void open(Class<?> activity) {
        startActivity(new Intent(requireContext(), activity));
    }

    private static void text(View root, int id, CharSequence value) {
        ((TextView) root.findViewById(id)).setText(value);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /** Reminder for email/password members who have not confirmed their address yet. */
    private void bindVerification(View view) {
        View card = view.findViewById(R.id.profile_verify_card);
        TextView body = view.findViewById(R.id.profile_verify_body);
        MaterialButton resend = view.findViewById(R.id.profile_verify_resend);
        resend.setOnClickListener(v -> viewModel.resendVerification());
        view.findViewById(R.id.profile_verify_done).setOnClickListener(v -> viewModel.confirmVerified());
        viewModel.getVerification().observe(getViewLifecycleOwner(), status -> {
            card.setVisibility(status == ProfileViewModel.Verification.HIDDEN ? View.GONE : View.VISIBLE);
            bindMember(view);
            resend.setEnabled(status != ProfileViewModel.Verification.SENDING);
            String email = viewModel.getCurrentUserEmail();
            switch (status) {
                case SENDING: body.setText(R.string.verify_sending); break;
                case SENT: body.setText(getString(R.string.verify_sent, email)); break;
                case FAILED: body.setText(R.string.verify_failed); break;
                case STILL_UNVERIFIED: body.setText(R.string.verify_still_unverified); break;
                default: body.setText(getString(R.string.verify_body, email));
            }
        });
    }

    @Override public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.checkVerification();
            viewModel.refreshProfile();
            viewModel.loadStats();
            viewModel.loadFriendRequests();
        }
    }

    /** Sun/moon pill: shows the theme in use and switches it at once, saved for this account. */
    private void bindThemeToggle(View view) {
        View light = view.findViewById(R.id.profile_theme_light);
        View dark = view.findViewById(R.id.profile_theme_dark);
        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        light.setSelected(!night);
        dark.setSelected(night);
        light.setOnClickListener(v -> setNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO));
        dark.setOnClickListener(v -> setNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES));
    }

    private void setNightMode(int mode) {
        new SettingsRepository(requireContext()).setNightMode(mode);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
    }
}
