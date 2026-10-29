package com.example.ungdungchiasecaycanh.ui.community;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.ui.inbox.InboxActivity;
import com.example.ungdungchiasecaycanh.ui.profile.FriendsActivity;
import com.example.ungdungchiasecaycanh.ui.profile.MemberProfileActivity;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.snackbar.Snackbar;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.example.ungdungchiasecaycanh.ui.listing.CreateListingActivity;
import com.example.ungdungchiasecaycanh.ui.listing.ListingPreviewActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/**
 * Community (PlantMate Redesign Canvas): brand bar, a greeting, the "green moments" story strip,
 * search, green category tabs with sorting, then post cards.
 */
public class CommunityFragment extends Fragment {
    private CommunityViewModel vm;
    private StoryStripViewModel stories;
    private com.example.ungdungchiasecaycanh.ui.notifications.BellViewModel chatBadge;
    private PostAdapter adapter;
    private int lastLikeError;
    private boolean firstResume = true;
    private boolean postsAnimated;
    /** A listing made from Community is the same shared listing as Home's, then shared as a post. */
    private final ActivityResultLauncher<Intent> createListing = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                String id = r.getResultCode() == Activity.RESULT_OK && r.getData() != null
                        ? r.getData().getStringExtra(ListingPreviewActivity.EXTRA_LISTING_ID) : null;
                if (id != null) this.createPost.launch(new Intent(requireContext(), CreatePostActivity.class)
                        .putExtra(CreatePostActivity.EXTRA_LISTING_ID, id));
            });
    private final ActivityResultLauncher<Intent> createPost = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
        if (r.getResultCode() == Activity.RESULT_OK && vm != null) vm.reload();
    });
    private final ActivityResultLauncher<Intent> createStory = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), r -> {
                if (r.getResultCode() != Activity.RESULT_OK || stories == null) return;
                stories.load();
                if (getView() != null) Snackbar.make(getView(), R.string.story_published, Snackbar.LENGTH_SHORT).show();
            });
    private final ActivityResultLauncher<Intent> postDetail = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
        if (vm != null) vm.reload();
    });

    public CommunityFragment() {
        super(R.layout.fragment_community);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle b) {
        super.onViewCreated(view, b);
        vm = new ViewModelProvider(this).get(CommunityViewModel.class);
        adapter = new PostAdapter(new PostAdapter.Listener() {
            public void onPostClick(String id) { openPost(id); }

            public void onLikeClick(String id) { vm.toggleLike(id); }

            @Override public void onAuthorClick(String authorId) {
                startActivity(MemberProfileActivity.intent(
                        requireContext(), authorId,
                        MemberProfileActivity.CONTEXT_COMMUNITY));
            }
        });
        RecyclerView list = view.findViewById(R.id.communityList);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        stories = new ViewModelProvider(this).get(StoryStripViewModel.class);
        StoryStripAdapter storyAdapter = new StoryStripAdapter(new StoryStripAdapter.Listener() {
            @Override public void onCreateStory() { openCreateStory(); }
            @Override public void onAddFriends() { startActivity(new Intent(requireContext(), FriendsActivity.class)); }
            @Override public void onOpenStory(int index) { openStories(index); }
        });
        ((RecyclerView) view.findViewById(R.id.communityStories)).setAdapter(storyAdapter);
        stories.getStories().observe(getViewLifecycleOwner(), storyAdapter::submit);

        View fab = view.findViewById(R.id.communityFab); // beside the title, not floating (BUG-009)
        View.OnClickListener open = v -> chooseWhatToPost();
        fab.setOnClickListener(open);
        view.findViewById(R.id.communityEmptyAction).setOnClickListener(open);
        view.findViewById(R.id.communityComposer).setOnClickListener(open);
        view.findViewById(R.id.communityRetry).setOnClickListener(v -> vm.reload());
        NestedScrollView scroll = view.findViewById(R.id.communityScroll);
        scroll.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, x, y, oldX, oldY) -> {
                    // Within one screen of the end: fetch the next page.
                    View content = scroll.getChildAt(0);
                    if (y > oldY && content != null && content.getBottom() - (y + scroll.getHeight()) < scroll.getHeight())
                        vm.loadMore();
                });

        EditText search = view.findViewById(R.id.communitySearch);
        view.findViewById(R.id.communitySearchButton).setOnClickListener(v -> {
            view.findViewById(R.id.communitySearchLayout).setVisibility(View.VISIBLE);
            ((NestedScrollView) view.findViewById(R.id.communityScroll)).smoothScrollTo(0, 0);
            search.requestFocus();
            InputMethodManager ime = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (ime != null) ime.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
        });
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            public void onTextChanged(CharSequence s, int st, int before, int count) { vm.setSearch(s.toString()); }
            public void afterTextChanged(Editable e) { }
        });
        chatBadge = new androidx.lifecycle.ViewModelProvider(this).get(com.example.ungdungchiasecaycanh.ui.notifications.BellViewModel.class);
        chatBadge.getChats().observe(getViewLifecycleOwner(), n ->
                com.example.ungdungchiasecaycanh.ui.notifications.BellViewModel.bindBadge(
                        view.findViewById(R.id.communityInboxBadge), view.findViewById(R.id.communityInbox), n,
                        R.string.home_inbox_description, R.plurals.inbox_unread_chats));
        view.findViewById(R.id.communityInbox).setOnClickListener(v -> startActivity(
                new Intent(requireContext(), InboxActivity.class)));
        view.findViewById(R.id.communityAvatar).setOnClickListener(v -> {
            BottomNavigationView nav = requireActivity().findViewById(R.id.bottom_navigation);
            if (nav != null) nav.setSelectedItemId(R.id.nav_profile);
        });
        view.findViewById(R.id.communitySort).setOnClickListener(this::showSortMenu);

        TabLayout tabs = view.findViewById(R.id.communityTabs);
        for (int label : TAB_LABELS) tabs.addTab(tabs.newTab().setText(label));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { vm.setCategory(TAB_CATEGORIES[tab.getPosition()]); }
            @Override public void onTabUnselected(TabLayout.Tab tab) { }
            @Override public void onTabReselected(TabLayout.Tab tab) { }
        });

        Motion.fromLeft(view.findViewById(R.id.communityHero), 0);
        Motion.rise(view.findViewById(R.id.communityTitle), 80);
        Motion.rise(view.findViewById(R.id.communityStories), 110);
        Motion.rise(view.findViewById(R.id.communityComposer), 140);
        Motion.fromRight(view.findViewById(R.id.communityTabs), 200);
        Motion.pop(fab, 480, 520);

        bindAvatar(view, null);
        vm.getMe().observe(getViewLifecycleOwner(), me -> bindAvatar(view, me));
        vm.getState().observe(getViewLifecycleOwner(), s -> {
            adapter.submitList(s.posts);
            vm.loadListings(s.posts);
            if (!postsAnimated && !s.posts.isEmpty()) {
                postsAnimated = true;
                Motion.staggerRows(list, 260, 80);
            }
            view.findViewById(R.id.communityLoading).setVisibility(s.loading ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.communityMoreLoading).setVisibility(s.loadingMore ? View.VISIBLE : View.GONE);
            if (s.hasMore && !s.loading && !s.loadingMore && s.error == null) {
                // A short page (filtered by search or blocks) cannot be scrolled: keep filling the screen.
                scroll.post(() -> { if (!scroll.canScrollVertically(1)) vm.loadMore(); });
            }
            view.findViewById(R.id.communityError).setVisibility(s.error != null ? View.VISIBLE : View.GONE);
            list.setVisibility(!s.loading && s.error == null && !s.posts.isEmpty() ? View.VISIBLE : View.GONE);
            View empty = view.findViewById(R.id.communityEmpty);
            empty.setVisibility(!s.loading && s.error == null && s.posts.isEmpty() ? View.VISIBLE : View.GONE);
            view.findViewById(R.id.communityEmptyAction).setVisibility(!s.hasSourcePosts ? View.VISIBLE : View.GONE);
            ((TextView) view.findViewById(R.id.communityEmptyTitle)).setText(s.hasSourcePosts ? R.string.community_no_results : R.string.community_empty_title);
            view.findViewById(R.id.communityEmptySupporting).setVisibility(s.hasSourcePosts ? View.GONE : View.VISIBLE);
            if (s.likeErrorVersion > lastLikeError) {
                lastLikeError = s.likeErrorVersion;
                Snackbar.make(view, R.string.community_like_error, Snackbar.LENGTH_SHORT).show();
            }
        });
        vm.getListings().observe(getViewLifecycleOwner(), adapter::setListings);
        vm.load();
    }

    private void bindAvatar(View view, @Nullable User me) {
        String name = me == null || me.getDisplayName().trim().isEmpty() ? "?" : me.getDisplayName();
        AvatarBinder.bind(view.findViewById(R.id.communityAvatar), name, me == null ? "" : me.getAvatarUrl(),
                R.drawable.bg_avatar_ink);
    }

    private void showSortMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add(0, 1, 0, R.string.community_sort_newest);
        menu.getMenu().add(0, 2, 1, R.string.community_sort_liked);
        menu.setOnMenuItemClickListener(item -> {
            vm.setSort(item.getItemId() == 2 ? CommunityViewModel.Sort.MOST_LIKED : CommunityViewModel.Sort.NEWEST);
            return true;
        });
        menu.show();
    }

    private void openPost(String id) {
        Intent i = new Intent(requireContext(), PostDetailActivity.class);
        i.putExtra(PostDetailActivity.EXTRA_POST_ID, id);
        postDetail.launch(i);
    }

    // No "General" tab: general posts already show under "All", and a second catch-all tab looked like a duplicate.
    private static final int[] TAB_LABELS = {R.string.community_tab_all, R.string.post_category_question,
            R.string.post_category_sharing, R.string.post_category_win, R.string.post_category_tip};
    private static final PostCategory[] TAB_CATEGORIES = {null, PostCategory.QUESTION, PostCategory.SHARING,
            PostCategory.PLANT_WIN, PostCategory.TIP};

    @Override
    public void onResume() {
        super.onResume();
        if (chatBadge != null) chatBadge.refresh();
        if (firstResume) firstResume = false;
        else if (vm != null) vm.reload();
        if (stories != null) stories.load(); // seen rings and new stories after the viewer or a new friend
    }

    private void openCreateStory() {
        createStory.launch(new Intent(requireContext(), CreateStoryActivity.class));
    }

    private void openStories(int index) {
        startActivity(StoryViewerActivity.intent(requireContext(), stories.storyIds(), index));
    }

    /** "What do you want to post?": a discussion post, or a give-away / trade / sale listing. */
    private void chooseWhatToPost() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_care_type, null, false);
        ((TextView) sheet.findViewById(R.id.careTypeTitle)).setText(R.string.community_post_choose_title);
        ((TextView) sheet.findViewById(R.id.careTypeSubtitle)).setText(R.string.community_post_choose_sub);
        LinearLayout rows = sheet.findViewById(R.id.careTypeRows);
        addChoice(rows, R.drawable.ic_chat, R.color.color_tile_lavender, R.color.color_tile_lavender_ink,
                R.string.community_choose_post, R.string.community_choose_post_hint, () -> {
                    dialog.dismiss();
                    createPost.launch(new Intent(requireContext(), CreatePostActivity.class));
                });
        addChoice(rows, R.drawable.ic_leaf, R.color.color_primary_tint, R.color.color_on_primary_tint,
                R.string.community_choose_listing, R.string.community_choose_listing_hint, () -> {
                    dialog.dismiss();
                    createListing.launch(new Intent(requireContext(), CreateListingActivity.class));
                });
        addChoice(rows, R.drawable.ic_camera, R.color.color_chip_surface, R.color.color_text_primary,
                R.string.story_create, R.string.story_visibility_note, () -> {
                    dialog.dismiss();
                    openCreateStory();
                });
        dialog.setContentView(sheet);
        dialog.show();
    }

    private void addChoice(LinearLayout rows, int icon, int tile, int ink, int name, int hint, Runnable action) {
        View row = getLayoutInflater().inflate(R.layout.item_care_type, rows, false);
        row.findViewById(R.id.careTypeTile).setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), tile));
        ImageView iconView = row.findViewById(R.id.careTypeIcon);
        iconView.setImageResource(icon);
        iconView.setImageTintList(ContextCompat.getColorStateList(requireContext(), ink));
        ((TextView) row.findViewById(R.id.careTypeName)).setText(name);
        ((TextView) row.findViewById(R.id.careTypeHint)).setText(hint);
        row.setOnClickListener(v -> action.run());
        rows.addView(row);
    }
}
