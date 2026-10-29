package com.example.ungdungchiasecaycanh.ui.listing;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ungdungchiasecaycanh.MainActivity;
import com.example.ungdungchiasecaycanh.model.ListingDetails;
import com.example.ungdungchiasecaycanh.ui.ai.MamChatActivity;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import androidx.lifecycle.ViewModelProvider;

import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import androidx.core.content.ContextCompat;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;
import com.example.ungdungchiasecaycanh.ui.ai.MamAdvisor;
import com.example.ungdungchiasecaycanh.ui.safety.ReportBottomSheet;
import com.example.ungdungchiasecaycanh.ui.safety.SafetyActionViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.ungdungchiasecaycanh.ui.community.CreatePostActivity;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;
import com.example.ungdungchiasecaycanh.ui.inbox.ConversationActivity;
import com.example.ungdungchiasecaycanh.ui.profile.MemberProfileActivity;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.example.ungdungchiasecaycanh.ui.profile.VneidDemoActivity;
import com.example.ungdungchiasecaycanh.ui.profile.Reputation;
import com.example.ungdungchiasecaycanh.util.DiffList;


public class ListingDetailActivity extends AppCompatActivity {
    public static final String EXTRA_LISTING_ID = "listingId";
    private View loadingView;
    private View contentView;
    private View notFoundView;
    private View errorView;
    private ListingDetailViewModel viewModel;
    private ImageButton saveButton;
    private MaterialButton contactButton;
    /** A sale: messaging the seller (buying) needs the VNeID step first. */
    private boolean selling;
    private ImageButton shareButton;
    private final PhotoPager photos = new PhotoPager();
    private int shownSaveErrorVersion;
    private int shownContactErrorVersion;
    private SafetyActionViewModel safetyViewModel;
    private String safetyOwnerId;
    private int shownSafetyChange, shownSafetyError;
    private boolean safetyBusy;
    private boolean firstResume=true;

    private boolean entranceShown;
    private Boolean statusIconsDark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_detail);

        loadingView = findViewById(R.id.detail_loading);
        contentView = findViewById(R.id.detail_content);
        notFoundView = findViewById(R.id.detail_not_found);
        errorView = findViewById(R.id.detail_error);
        saveButton = findViewById(R.id.detail_action_save);
        contactButton = findViewById(R.id.detail_action_contact);
        shareButton = findViewById(R.id.detail_action_share);
        ViewPager2 pager = findViewById(R.id.detail_photos);
        pager.setAdapter(photos);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { bindDots(position); }
        });
        findViewById(R.id.detail_action_ask).setOnClickListener(v ->
                startActivity(new Intent(this, MamChatActivity.class)));
        viewModel = new ViewModelProvider(this).get(ListingDetailViewModel.class);
        safetyViewModel = new ViewModelProvider(this).get(SafetyActionViewModel.class);
        viewModel.getState().observe(this, this::render);
        viewModel.getSimilar().observe(this, items -> bindOthers());
        viewModel.getOwnerStats().observe(this, stats -> bindOthers());
        // Once the photo scrolls away, the round buttons get a white bar so they never sit on text.
        View topBar = (View) findViewById(R.id.detail_top_bar).getParent();
        int surface = ContextCompat.getColor(this, R.color.color_surface);
        ((NestedScrollView) contentView).setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener) (v, x, y, ox, oy) -> {
                    int fadeStart = findViewById(R.id.detail_cover_frame).getHeight() - topBar.getHeight() * 2;
                    float t = Math.max(0f, Math.min(1f, (y - fadeStart) / (float) Math.max(1, topBar.getHeight())));
                    topBar.setBackgroundColor(ColorUtils.setAlphaComponent(surface, Math.round(255 * t)));
                    setStatusIconsDark(t > 0.5f);
                });
        setStatusIconsDark(false);
        safetyViewModel.getState().observe(this, safety -> {
            safetyBusy=safety.busy;
            MaterialButton block=findViewById(R.id.detail_block);
            block.setEnabled(safety.known&&!safety.busy);
            block.setText(safety.blocked?R.string.safety_unblock:R.string.safety_block);
            ListingDetailViewModel.UiState listingState=viewModel.getState().getValue();
            if(listingState!=null)contactButton.setEnabled(listingState.contactEligible
                    &&!listingState.contactInProgress&&!safety.busy);
            if(safety.changedVersion>shownSafetyChange){
                shownSafetyChange=safety.changedVersion;
                if(safety.blocked)viewModel.hideBlockedListing();
                else viewModel.retry();
            }
            if(safety.errorVersion>shownSafetyError){shownSafetyError=safety.errorVersion;
                Snackbar.make(block,R.string.safety_action_failed,Snackbar.LENGTH_SHORT).show();}
        });

        findViewById(R.id.detail_back).setOnClickListener(view -> finish());
        findViewById(R.id.detail_more).setOnClickListener(this::showSafetyMenu);
        findViewById(R.id.detail_retry).setOnClickListener(view -> viewModel.retry());
        findViewById(R.id.detail_error_retry).setOnClickListener(view -> viewModel.retry());
        saveButton.setOnClickListener(view -> {
            Motion.beat(saveButton);
            viewModel.toggleSaved();
        });
        contactButton.setOnClickListener(view -> {
            if (!selling || VneidDemoActivity.passGate(this)) viewModel.openConversation();
        });
        shareButton.setOnClickListener(view -> {
            ListingDetailViewModel.UiState current=viewModel.getState().getValue();
            if(current==null||current.listing==null||current.listing.getStatus()!=ListingStatus.ACTIVE)return;
            Intent intent=new Intent(this,CreatePostActivity.class);
            intent.putExtra(CreatePostActivity.EXTRA_LISTING_ID,current.listing.getListingId());
            startActivity(intent);
        });
        findViewById(R.id.detail_report).setOnClickListener(v->{
            ListingDetailViewModel.UiState current=viewModel.getState().getValue();
            if(current!=null&&current.listing!=null)
                ReportBottomSheet.newInstance(ReportTargetType.LISTING,current.listing.getListingId(),"")
                        .show(getSupportFragmentManager(),"report-listing");
        });
        findViewById(R.id.detail_block).setOnClickListener(v->{
            SafetyActionViewModel.State current=safetyViewModel.getState().getValue();
            if(current==null||!current.known||current.busy)return;
            if(current.blocked)safetyViewModel.toggle();
            else new MaterialAlertDialogBuilder(this).setTitle(R.string.safety_block_title)
                    .setMessage(R.string.safety_block_message)
                    .setNegativeButton(R.string.report_cancel,null)
                    .setPositiveButton(R.string.safety_block,(dialog,which)->safetyViewModel.toggle()).show();
        });
        findViewById(R.id.detail_hidden_unblock).setOnClickListener(v->{
            String uid=viewModel.getHiddenOwnerUid();if(uid!=null)safetyViewModel.unblock(uid);
        });
        findViewById(R.id.detail_hidden_back).setOnClickListener(v->finish());

        viewModel.loadListing(getIntent().getStringExtra(EXTRA_LISTING_ID));
    }

    @Override protected void onResume() {
        super.onResume();
        if(firstResume)firstResume=false;
        else if(viewModel!=null){safetyOwnerId=null;viewModel.retry();}
    }

    private void render(ListingDetailViewModel.UiState state) {
        loadingView.setVisibility(state.status == ListingDetailViewModel.Status.LOADING
                ? View.VISIBLE : View.GONE);
        contentView.setVisibility(state.status == ListingDetailViewModel.Status.CONTENT
                ? View.VISIBLE : View.GONE);
        findViewById(R.id.detail_action_bar).setVisibility(
                state.status == ListingDetailViewModel.Status.CONTENT ? View.VISIBLE : View.GONE);
        notFoundView.setVisibility(state.status == ListingDetailViewModel.Status.NOT_FOUND
                ? View.VISIBLE : View.GONE);
        errorView.setVisibility(state.status == ListingDetailViewModel.Status.ERROR
                ? View.VISIBLE : View.GONE);
        findViewById(R.id.detail_hidden).setVisibility(state.status == ListingDetailViewModel.Status.HIDDEN
                ? View.VISIBLE : View.GONE);
        if (state.status == ListingDetailViewModel.Status.HIDDEN) {
            String ownerUid=viewModel.getHiddenOwnerUid();
            if(ownerUid!=null&&!ownerUid.equals(safetyOwnerId)){
                safetyOwnerId=ownerUid;safetyViewModel.load(ownerUid);
            }
        }
        if (state.status == ListingDetailViewModel.Status.CONTENT) {
            boolean other=!viewModel.isOwnedByCurrentUser(state.listing);
            findViewById(R.id.detail_report).setVisibility(other?View.VISIBLE:View.GONE);
            findViewById(R.id.detail_block).setVisibility(other?View.VISIBLE:View.GONE);
            findViewById(R.id.detail_more).setVisibility(other?View.VISIBLE:View.GONE);
            if(other&&!state.listing.getOwnerId().equals(safetyOwnerId)){
                safetyOwnerId=state.listing.getOwnerId();safetyViewModel.load(safetyOwnerId);
            }
            bindListing(state.listing, state.owner);
            saveButton.setImageResource(state.saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
            saveButton.setImageTintList(ContextCompat.getColorStateList(this, state.saved ? R.color.saved_heart : R.color.color_text_primary)); // action bar follows the theme
            saveButton.setContentDescription(getString(state.saved ? R.string.detail_saved : R.string.detail_save));
            saveButton.setEnabled(state.savedStateKnown && !state.saveInProgress);
            contactButton.setVisibility(state.contactEligible ? View.VISIBLE : View.GONE);
            contactButton.setEnabled(state.contactEligible && !state.contactInProgress && !safetyBusy);
            shareButton.setVisibility(state.listing.getStatus()==ListingStatus.ACTIVE?View.VISIBLE:View.GONE);
            contactButton.setVisibility(state.contactEligible ? View.VISIBLE : View.GONE);
            shareButton.setEnabled(state.listing.getStatus()==ListingStatus.ACTIVE);
            if (state.contactErrorVersion > shownContactErrorVersion) {
                shownContactErrorVersion = state.contactErrorVersion;
                Snackbar.make(contactButton, R.string.safety_messaging_unavailable,
                        Snackbar.LENGTH_SHORT).show();
            }
            String openedId = viewModel.consumeOpenedConversationId();
            if (openedId != null) {
                Intent intent = new Intent(this, ConversationActivity.class);
                intent.putExtra(ConversationActivity.EXTRA_CONVERSATION_ID, openedId);
                intent.putExtra(ConversationActivity.EXTRA_ASK_LISTING_ID, state.listing.getListingId());
                startActivity(intent);
            }
            if (state.saveErrorVersion > shownSaveErrorVersion) {
                shownSaveErrorVersion = state.saveErrorVersion;
                Snackbar.make(saveButton, R.string.detail_save_error,
                        Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void bindListing(Listing listing, User owner) {
        selling = listing.getType() == ListingType.SELL;
        ImageView cover = findViewById(R.id.detail_cover_image);
        if (!entranceShown) {
            entranceShown = true;
            Motion.zoom(findViewById(R.id.detail_cover_frame));
            Motion.sheet(findViewById(R.id.detail_sheet), 640, 200);
            ViewGroup sheet = findViewById(R.id.detail_sheet);
            for (int i = 0; i < sheet.getChildCount() && i < 6; i++) {
                Motion.rise(sheet.getChildAt(i), 120 + (i == 0 ? 0 : 140 + i * 60L));
            }
            Motion.sheet(findViewById(R.id.detail_action_bar), 600, 360);
        }
        photos.submit(listing.getImageUrls());
        bindDots(((ViewPager2) findViewById(R.id.detail_photos)).getCurrentItem());
        cover.setImageResource(R.drawable.ic_botanical_placeholder);
        TextView typeBadge = findViewById(R.id.detail_type);
        bindTypeChip(typeBadge, listing);
        TextView statusTag = findViewById(R.id.detail_status);
        statusTag.setVisibility(listing.getStatus() == ListingStatus.ACTIVE ? View.GONE : View.VISIBLE);
        statusTag.setText(ListingDetailViewModel.statusLabel(this, listing.getStatus()));
        setText(R.id.detail_posted, postedText(listing));
        findViewById(R.id.detail_posted).setVisibility(postedText(listing).isEmpty() ? View.GONE : View.VISIBLE);
        setText(R.id.detail_note, getString(listing.getType() == ListingType.TRADE ? R.string.detail_note_trade
                : listing.getType() == ListingType.SELL ? R.string.detail_note_sell : R.string.detail_note_give));
        setText(R.id.detail_owner_title, getString(listing.getType() == ListingType.TRADE ? R.string.detail_owner_title_trade
                : listing.getType() == ListingType.SELL ? R.string.detail_owner_title_sell : R.string.detail_owner_title_give));
        setText(R.id.detail_nickname, listing.getNickname());
        TextView species = findViewById(R.id.detail_species);
        String speciesText = listing.getSpecies() == null ? "" : listing.getSpecies().trim();
        species.setText(speciesText);
        String area = listing.getLocationText() == null ? "" : listing.getLocationText().trim();
        TextView location = findViewById(R.id.detail_location);
        location.setText(area);
        location.setVisibility(area.isEmpty() ? View.GONE : View.VISIBLE);

        bindFacts(listing);
        bindPickup(listing, area);
        TextView note = findViewById(R.id.detail_description);
        String description = listing.getDescription() == null ? "" : listing.getDescription().trim();
        note.setVisibility(description.isEmpty() ? View.GONE : View.VISIBLE);
        findViewById(R.id.detail_description_title).setVisibility(description.isEmpty() ? View.GONE : View.VISIBLE);
        note.setText(description);
        contactButton.setText(listing.getType() == ListingType.TRADE ? R.string.detail_message_trader
                : listing.getType() == ListingType.SELL ? R.string.detail_message_seller : R.string.detail_message_giver);

        String name = owner == null ? getString(R.string.detail_member_fallback) : owner.getDisplayName();
        setText(R.id.detail_owner_name, name);
        findViewById(R.id.detail_owner_verified).setVisibility(
                owner != null && owner.isEmailVerified() ? View.VISIBLE : View.GONE);
        AvatarBinder.bind(findViewById(R.id.detail_owner_avatar), name,
                owner == null ? "" : owner.getAvatarUrl(), R.drawable.bg_avatar_white);
        View.OnClickListener openOwner = v -> startActivity(MemberProfileActivity.intent(this, listing.getOwnerId(),
                MemberProfileActivity.CONTEXT_LISTINGS));
        View ownerRow = findViewById(R.id.detail_owner);
        ownerRow.setOnClickListener(openOwner);
        findViewById(R.id.detail_owner_garden).setOnClickListener(openOwner);
        ownerRow.setContentDescription(getString(R.string.member_open_profile, name));
        bindOwnerStats();
        TextView bio = findViewById(R.id.detail_owner_bio);
        String bioText = owner == null || owner.getBio() == null ? "" : owner.getBio().trim();
        bio.setVisibility(bioText.isEmpty() ? View.GONE : View.VISIBLE);
        bio.setText(bioText);
    }

    /** Green "Free" for give away, ink "Swap · for …" for trade, ink price for sale (canvas B). */
    private void bindTypeChip(TextView chip, Listing listing) {
        boolean give = listing.getType() == ListingType.GIVE_AWAY;
        chip.setBackgroundResource(give ? R.drawable.bg_tag_green : R.drawable.bg_tag_ink);
        chip.setTextColor(ContextCompat.getColor(this, give ? R.color.color_on_primary : R.color.color_background));
        String wanted = listing.getLookingFor() == null ? "" : listing.getLookingFor().trim();
        chip.setText(give ? getString(R.string.detail_give_free)
                : listing.getType() == ListingType.TRADE
                        ? (wanted.isEmpty() ? getString(R.string.listing_type_trade) : getString(R.string.home_offer_trade, wanted))
                : getString(R.string.listing_type_sell_detail, ListingAdapter.formatVnd(listing.getPrice())));
    }

    /** Facts grid (canvas B): rows of three cells with an icon, a bold value and a label; only known facts. */
    private void bindFacts(Listing listing) {
        ViewGroup facts = findViewById(R.id.detail_facts);
        facts.removeAllViews();
        ListingDetails d = listing.getDetails();
        List<int[]> icons = new ArrayList<>();
        List<String[]> cells = new ArrayList<>();
        List<ListingDetails.Condition> health = new ArrayList<>(d.getCondition());
        boolean pot = health.remove(ListingDetails.Condition.WITH_POT);
        if (d.getHeightCm() > 0) addCell(icons, cells, R.drawable.ic_ruler,
                getString(R.string.detail_height_value, d.getHeightCm()), getString(R.string.detail_fact_height));
        if (d.getPotCm() > 0 || pot) addCell(icons, cells, R.drawable.ic_pot,
                d.getPotCm() > 0 ? getString(R.string.detail_pot_cm, d.getPotCm()) : getString(R.string.detail_pot_with),
                d.getPotCm() > 0 && pot ? getString(R.string.detail_pot_with_cm) : getString(R.string.detail_fact_pot_label));
        if (!health.isEmpty()) addCell(icons, cells, R.drawable.ic_shield,
                getString(ListingDetailLabels.of(health.get(0))), getString(R.string.detail_fact_health));
        if (d.getAge() != null) addCell(icons, cells, R.drawable.ic_calendar,
                getString(ListingDetailLabels.of(d.getAge())), getString(R.string.detail_fact_age));
        if (d.getLight() != null) addCell(icons, cells, R.drawable.ic_sun,
                getString(ListingDetailLabels.of(d.getLight())), getString(R.string.detail_fact_light));
        if (d.getCategory() != null) addCell(icons, cells, R.drawable.ic_leaf,
                getString(ListingDetailLabels.of(d.getCategory())), getString(R.string.detail_fact_category));
        if (cells.isEmpty()) {
            // The "Type" cell names the type (Give away / Swap / Sell), never the price (BUG-020).
            addCell(icons, cells, R.drawable.ic_leaf, getString(listing.getType() == ListingType.SELL ? R.string.home_filter_sell
                    : listing.getType() == ListingType.TRADE ? R.string.home_filter_trade : R.string.home_filter_give_away),
                    getString(R.string.detail_fact_type));
            addCell(icons, cells, R.drawable.ic_shield, ListingDetailViewModel.statusLabel(this, listing.getStatus()),
                    getString(R.string.detail_fact_status));
        }
        int perRow = cells.size() == 4 ? 2 : 3;
        float dp = getResources().getDisplayMetrics().density;
        int line = ContextCompat.getColor(this, R.color.color_panel_line);
        LinearLayout row = null;
        for (int i = 0; i < cells.size(); i++) {
            if (i % perRow == 0) {
                if (row != null) {
                    View divider = new View(this);
                    divider.setBackgroundColor(line);
                    facts.addView(divider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, (int) dp)));
                }
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                facts.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            } else {
                View divider = new View(this);
                divider.setBackgroundColor(line);
                row.addView(divider, new LinearLayout.LayoutParams(Math.max(1, (int) dp), ViewGroup.LayoutParams.MATCH_PARENT));
            }
            View cell = getLayoutInflater().inflate(R.layout.item_detail_fact_cell, row, false);
            ((ImageView) cell.findViewById(R.id.factIcon)).setImageResource(icons.get(i)[0]);
            ((TextView) cell.findViewById(R.id.factValue)).setText(cells.get(i)[0]);
            ((TextView) cell.findViewById(R.id.factLabel)).setText(cells.get(i)[1]);
            cell.setContentDescription(cells.get(i)[1] + ", " + cells.get(i)[0]);
            row.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        // An unfinished last row keeps equal widths.
        int missing = row == null ? 0 : (perRow - cells.size() % perRow) % perRow;
        for (int i = 0; i < missing; i++) row.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1f));
    }

    private static void addCell(List<int[]> icons, List<String[]> cells, int icon, String value, String label) {
        icons.add(new int[]{icon});
        cells.add(new String[]{value, label});
    }

    /** Stars and counts from the owner's listings and reviews; blanks while they load. */
    private void bindOwnerStats() {
        ListingDetailViewModel.OwnerStats s = viewModel.getOwnerStats().getValue();
        TextView rating = findViewById(R.id.detail_owner_location);
        if (s == null) {
            rating.setText("");
            findViewById(R.id.detail_owner_stats).setVisibility(View.INVISIBLE);
            return;
        }
        rating.setText(s.ratingCount == 0 ? getString(R.string.detail_no_rating)
                : getString(R.string.detail_rating, String.format(java.util.Locale.getDefault(), "%.1f", s.ratingAverage), s.ratingCount));
        findViewById(R.id.detail_owner_stats).setVisibility(View.VISIBLE);
        bindStat(R.id.detail_owner_given, s.given, R.string.detail_stat_given);
        bindStat(R.id.detail_owner_swapped, s.swapped, R.string.detail_stat_swapped);
        bindStat(R.id.detail_owner_listing, s.open, R.string.detail_stat_listing);
        bindOwnerBadges(s);
    }

    /** ✓ Email · ✓ Phone · ✓ VNeID and the trust level, from the same rules as the profile. */
    private void bindOwnerBadges(ListingDetailViewModel.OwnerStats s) {
        ListingDetailViewModel.UiState state = viewModel.getState().getValue();
        User owner = state == null ? null : state.owner;
        findViewById(R.id.detail_owner_badge_email).setVisibility(owner != null && owner.isEmailVerified() ? View.VISIBLE : View.GONE);
        findViewById(R.id.detail_owner_badge_phone).setVisibility(owner != null && owner.isPhoneVerified() ? View.VISIBLE : View.GONE);
        findViewById(R.id.detail_owner_badge_vneid).setVisibility(owner != null && owner.isVneidVerified() ? View.VISIBLE : View.GONE);
        TextView level = findViewById(R.id.detail_owner_level);
        level.setVisibility(owner == null ? View.GONE : View.VISIBLE);
        if (owner == null) return;
        long days = owner.getCreatedAt() == null ? 0
                : (System.currentTimeMillis() - owner.getCreatedAt().toDate().getTime()) / 86_400_000L;
        Reputation r = Reputation.of(s.ratingAverage, s.ratingCount, s.given + s.swapped, days,
                Reputation.verifiedPoints(owner));
        level.setText(getString(R.string.owner_badge_level, getString(Reputation.labelOf(r.level)), r.score));
    }

    /** "12" in bold over "Given". */
    private void bindStat(int id, int count, int label) {
        String number = String.valueOf(count);
        SpannableStringBuilder text = new SpannableStringBuilder(number).append('\n').append(getString(label));
        text.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new android.text.style.AbsoluteSizeSpan(17, true), 0, number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new android.text.style.ForegroundColorSpan(ContextCompat.getColor(this, R.color.owner_card_text)),
                0, number.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ((TextView) findViewById(id)).setText(text);
    }

    /** Black pick-up card: where, and the owner's free times when given. */
    private void bindPickup(Listing listing, String area) {
        setText(R.id.detail_pickup_area, area.isEmpty() ? getString(R.string.detail_pickup_anywhere)
                : getString(R.string.detail_pickup_at, area));
        List<ListingDetails.PickupTime> times = listing.getDetails().getPickupTimes();
        findViewById(R.id.detail_pickup_times_label).setVisibility(times.isEmpty() ? View.GONE : View.VISIBLE);
        ChipGroup group = findViewById(R.id.detail_pickup_times);
        group.removeAllViews();
        group.setVisibility(times.isEmpty() ? View.GONE : View.VISIBLE);
        for (ListingDetails.PickupTime time : times) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_pickup_chip, group, false);
            chip.setText(ListingDetailLabels.of(time));
            group.addView(chip);
        }
    }

    /** Thumbnail strip over the photo's lower edge: tapping one shows that photo; the current one has an ink ring. */
    private void bindDots(int selected) {
        LinearLayout strip = findViewById(R.id.detail_dots);
        strip.removeAllViews();
        List<String> urls = photos.urls();
        findViewById(R.id.detail_thumbs_scroll).setVisibility(urls.size() < 2 ? View.GONE : View.VISIBLE);
        if (urls.size() < 2) return;
        float dp = getResources().getDisplayMetrics().density;
        int size = (int) (66 * dp);
        for (int i = 0; i < urls.size(); i++) {
            android.widget.FrameLayout frame = new android.widget.FrameLayout(this);
            frame.setBackgroundResource(i == selected ? R.drawable.bg_thumb_selected : R.drawable.bg_thumb);
            int pad = (int) ((i == selected ? 5 : 3) * dp);
            frame.setPadding(pad, pad, pad, pad);
            com.google.android.material.imageview.ShapeableImageView image = new com.google.android.material.imageview.ShapeableImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setShapeAppearanceModel(image.getShapeAppearanceModel().toBuilder().setAllCornerSizes(12 * dp).build());
            frame.addView(image, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            RemoteImageLoader.load(image, urls.get(i), size);
            final int index = i;
            frame.setOnClickListener(v -> ((ViewPager2) findViewById(R.id.detail_photos)).setCurrentItem(index, true));
            frame.setContentDescription(getString(R.string.detail_photo_n, i + 1, urls.size()));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd((int) (6 * dp));
            strip.addView(frame, lp);
        }
    }

    /** The owner's other open listings ("More from …"), else similar plants nearby. */
    private void bindOthers() {
        ListingDetailViewModel.OwnerStats stats = viewModel.getOwnerStats().getValue();
        ListingDetailViewModel.UiState state = viewModel.getState().getValue();
        bindOwnerStats();
        boolean mine = stats != null && !stats.others.isEmpty();
        List<Listing> items = mine ? stats.others : viewModel.getSimilar().getValue();
        String ownerName = state == null || state.owner == null ? "" : state.owner.getDisplayName();
        setText(R.id.detail_similar_title, mine && !ownerName.isEmpty()
                ? getString(R.string.detail_others_title, ownerName) : getString(R.string.detail_similar_title));
        findViewById(R.id.detail_similar_section).setVisibility(items == null || items.isEmpty() ? View.GONE : View.VISIBLE);
        ViewGroup row = findViewById(R.id.detail_similar);
        row.removeAllViews();
        if (items == null) return;
        int px = (int) (140 * getResources().getDisplayMetrics().density);
        for (Listing l : items) {
            View card = getLayoutInflater().inflate(R.layout.item_similar_listing, row, false);
            RemoteImageLoader.load(card.findViewById(R.id.similarPhoto), l.getCoverImageUrl(), px);
            ListingAdapter.bindCardTag(card.findViewById(R.id.similarOffer), l.getType());
            ((TextView) card.findViewById(R.id.similarName)).setText(l.getNickname());
            ListingAdapter.bindOfferLine(card.findViewById(R.id.similarMeta), l);
            card.setOnClickListener(v -> startActivity(new Intent(this, ListingDetailActivity.class)
                    .putExtra(EXTRA_LISTING_ID, l.getListingId())));
            row.addView(card);
        }
        findViewById(R.id.detail_similar_all).setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(MainActivity.EXTRA_SELECT_TAB, MainActivity.TAB_HOME)));
    }

    /** Full-width photos, swiped; the placeholder shows through when there are none. */
    private static final class PhotoPager extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final DiffList<String> urls = new DiffList<>(this, u -> u, u -> u);

        List<String> urls() {
            List<String> out = new ArrayList<>();
            for (int i = 0; i < urls.size(); i++) out.add(urls.get(i));
            return out;
        }

        void submit(List<String> values) {
            List<String> usable = new ArrayList<>();
            if (values != null) for (String u : values) if (u != null && !u.trim().isEmpty()) usable.add(u);
            urls.submit(usable);
        }

        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView image = new ImageView(parent.getContext());
            image.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setContentDescription(parent.getContext().getString(R.string.listing_cover_photo));
            return new RecyclerView.ViewHolder(image) { };
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ImageView image = (ImageView) holder.itemView;
            image.setVisibility(View.VISIBLE);
            RemoteImageLoader.load(image, urls.get(position), image.getResources().getDisplayMetrics().widthPixels);
        }

        @Override public int getItemCount() { return urls.size(); }
    }

    private String postedText(Listing listing) {
        if (listing.getCreatedAt() == null) return "";
        long days = (System.currentTimeMillis() - listing.getCreatedAt().toDate().getTime()) / 86_400_000L;
        if (days < 1) return getString(R.string.detail_posted_today);
        return getResources().getQuantityString(R.plurals.detail_posted_days, (int) days, (int) days);
    }

    /** Report and Block stay available from the overflow menu and reuse the existing handlers. */
    private void showSafetyMenu(View anchor) {
        MaterialButton report = findViewById(R.id.detail_report);
        MaterialButton block = findViewById(R.id.detail_block);
        PopupMenu menu = new PopupMenu(this, anchor);
        menu.getMenu().add(0, 1, 0, report.getText());
        menu.getMenu().add(0, 2, 1, block.getText()).setEnabled(block.isEnabled());
        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) report.performClick();
            else block.performClick();
            return true;
        });
        menu.show();
    }

    /** Light status icons over the photo, dark once the top bar turns white (night mode keeps light). */
    private void setStatusIconsDark(boolean dark) {
        boolean value = dark && SecondaryChrome.useDarkStatusIcons(this);
        if (statusIconsDark != null && statusIconsDark == value) return;
        statusIconsDark = value;
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(value);
    }

    private void setText(int id, String value) {
        ((TextView) findViewById(id)).setText(value == null ? "" : value);
    }
}
