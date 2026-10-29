package com.example.ungdungchiasecaycanh.ui.inbox;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.content.Intent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.recyclerview.widget.ConcatAdapter;
import com.example.ungdungchiasecaycanh.ui.home.HomeHeaderAdapter;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailActivity;
import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.data.repository.ReviewRepository;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;
import com.example.ungdungchiasecaycanh.ui.safety.ReportBottomSheet;
import com.example.ungdungchiasecaycanh.ui.safety.SafetyActionViewModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.ungdungchiasecaycanh.ui.review.RateUserBottomSheet;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;

public class ConversationActivity extends AppCompatActivity implements RateUserBottomSheet.Listener {
    public static final String EXTRA_CONVERSATION_ID = "conversationId";
    /** Optional: the listing the member is asking about; its plant card goes before their message. */
    public static final String EXTRA_ASK_LISTING_ID = "askListingId";

    private ConversationViewModel viewModel;
    private MessageAdapter adapter;
    private RecyclerView recycler;
    private boolean messagesAnimated;
    private EditText input;
    private MaterialButton send;
    private MaterialButton rate;
    private View loading, content, error, notFound, noMessages;
    private int shownSent, shownSendError, shownSummaryWarning, shownRefreshError;
    private boolean firstResume = true;
    private boolean sending;
    private boolean ready;
    private int shownReviewSuccess, shownReviewError;
    private SafetyActionViewModel safetyViewModel;
    private int shownSafetyChange,shownSafetyError;
    private String safetyPeerId;
    private boolean ownBlocked, ownBlockKnown;
    private boolean blockChanging;
    private MaterialButton attach, mic;
    private View inputBar, recordingBar, uploadProgress;
    private TextView recordTime;
    private VoiceRecorder recorder;
    private VoicePlayer player;
    private int shownMediaError;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final Runnable recordTick = new Runnable() {
        @Override public void run() {
            if (recorder == null || !recorder.isRecording()) return;
            long elapsed = recorder.elapsedMs();
            if (elapsed >= VoiceRecorder.MAX_MS) { finishRecording(true); return; }
            recordTime.setText(getString(R.string.chat_recording_time, VoiceRecorder.format(elapsed),
                    VoiceRecorder.format(VoiceRecorder.MAX_MS)));
            timer.postDelayed(this, 250);
        }
    };
    private final ActivityResultLauncher<PickVisualMediaRequest> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) viewModel.sendImage(this, uri, getString(R.string.chat_summary_photo));
            });
    private final ActivityResultLauncher<String> requestMic =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) startRecording();
                else showMessage(R.string.chat_mic_permission_denied);
            });

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_conversation);
        // Volume keys adjust voice-note playback while chatting.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        View root = findViewById(R.id.conversation_root);
        int baseStart = root.getPaddingStart();
        int baseTop = root.getPaddingTop();
        int baseEnd = root.getPaddingEnd();
        int baseBottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPaddingRelative(baseStart, baseTop + bars.top, baseEnd,
                    baseBottom + Math.max(bars.bottom, ime.bottom));
            return windowInsets;
        });
        loading = findViewById(R.id.conversation_loading);
        content = findViewById(R.id.conversation_content);
        error = findViewById(R.id.conversation_error);
        notFound = findViewById(R.id.conversation_not_found);
        noMessages = findViewById(R.id.conversation_no_messages);
        recycler = findViewById(R.id.conversation_messages);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MessageAdapter();
        // The safety reminder follows the last message, like the design.
        View safetyNote = getLayoutInflater().inflate(R.layout.item_chat_safety_note, recycler, false);
        recycler.setAdapter(new ConcatAdapter(adapter, new HomeHeaderAdapter(safetyNote)));
        recorder = new VoiceRecorder(this);
        player = new VoicePlayer(this, new VoicePlayer.Listener() {
            @Override public void onPlayback(String ref, float progress, boolean loadingAudio) {
                adapter.setPlayback(ref, progress, loadingAudio);
            }
            @Override public void onError() { showMessage(R.string.chat_voice_error); }
        });
        adapter.setListener(new MessageAdapter.Listener() {
            @Override public void onPhotoClick(String mediaRef) { showPhoto(mediaRef); }
            @Override public void onVoiceClick(String mediaRef) { player.toggle(mediaRef); }
            @Override public void onListingClick(String listingId) {
                startActivity(new Intent(ConversationActivity.this, ListingDetailActivity.class)
                        .putExtra(ListingDetailActivity.EXTRA_LISTING_ID, listingId));
            }
        });
        attach = findViewById(R.id.conversation_attach);
        mic = findViewById(R.id.conversation_mic);
        inputBar = findViewById(R.id.conversation_input_bar);
        recordingBar = findViewById(R.id.conversation_recording_bar);
        recordTime = findViewById(R.id.conversation_record_time);
        uploadProgress = findViewById(R.id.conversation_upload_progress);
        attach.setOnClickListener(v -> pickPhoto.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build()));
        mic.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) startRecording();
            else requestMic.launch(Manifest.permission.RECORD_AUDIO);
        });
        findViewById(R.id.conversation_record_cancel).setOnClickListener(v -> finishRecording(false));
        findViewById(R.id.conversation_record_send).setOnClickListener(v -> finishRecording(true));
        input = findViewById(R.id.conversation_input);
        send = findViewById(R.id.conversation_send);
        ChipGroup quickReplies = findViewById(R.id.conversation_quick_replies);
        for (int reply : new int[]{R.string.conversation_quick_available, R.string.conversation_quick_thanks,
                R.string.conversation_quick_on_way}) {
            Chip chip = (Chip)
                    getLayoutInflater().inflate(R.layout.item_suggestion_chip, quickReplies, false);
            chip.setText(reply);
            // A quick reply fills the message box; the member still decides to send it.
            chip.setOnClickListener(v -> {
                input.setText(reply);
                input.setSelection(input.getText() == null ? 0 : input.getText().length());
            });
            quickReplies.addView(chip);
        }
        Motion.rise(findViewById(R.id.conversation_listing_card), 80);
        Motion.rise(findViewById(R.id.conversation_status_card), 140);
        Motion.sheet(findViewById(R.id.conversation_bottom), 560, 120);
        rate = findViewById(R.id.conversation_rate);
        viewModel = new ViewModelProvider(this).get(ConversationViewModel.class);
        safetyViewModel = new ViewModelProvider(this).get(SafetyActionViewModel.class);
        viewModel.getState().observe(this, this::render);
        safetyViewModel.getState().observe(this,state->{
            blockChanging=state.busy;
            updateSendEnabled();
            if(state.changedVersion>shownSafetyChange){shownSafetyChange=state.changedVersion;
                viewModel.refreshBlockState();}
            if(state.errorVersion>shownSafetyError){shownSafetyError=state.errorVersion;
                showMessage(R.string.safety_action_failed);}
        });
        findViewById(R.id.conversation_back).setOnClickListener(view -> finish());
        findViewById(R.id.conversation_retry).setOnClickListener(view -> viewModel.retry());
        send.setOnClickListener(view -> viewModel.sendTextMessage(
                input.getText() == null ? "" : input.getText().toString()));
        rate.setOnClickListener(view -> showRateSheet());
        findViewById(R.id.conversation_safety).setOnClickListener(this::showSafetyMenu);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendEnabled();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        viewModel.loadConversation(getIntent().getStringExtra(EXTRA_CONVERSATION_ID),
                getIntent().getStringExtra(EXTRA_ASK_LISTING_ID));
    }

    @Override protected void onStart() {
        super.onStart();
        viewModel.setLive(true);
    }

    @Override protected void onStop() {
        viewModel.setLive(false);
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        new com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository(this)
                .setChatSeen(getIntent().getStringExtra(EXTRA_CONVERSATION_ID));
        // Messages arrive live; the block state is re-checked when returning to the chat.
        if (firstResume) firstResume = false;
        else viewModel.refreshBlockState();
    }

    @Override protected void onPause() {
        super.onPause();
        // Messages sent or read while here are seen; the time is taken on the way out.
        new com.example.ungdungchiasecaycanh.data.preferences.SettingsRepository(this)
                .setChatSeen(getIntent().getStringExtra(EXTRA_CONVERSATION_ID));
        // Leaving the screen discards an unfinished note and stops playback.
        if (recorder.isRecording()) finishRecording(false);
        player.stop();
    }

    @Override protected void onDestroy() {
        timer.removeCallbacksAndMessages(null);
        recorder.stopQuietly();
        player.stop();
        super.onDestroy();
    }

    private void startRecording() {
        if (!canCompose() || recorder.isRecording()) return;
        player.stop();
        try {
            recorder.start(() -> finishRecording(true));
        } catch (Exception failure) {
            recorder.stopQuietly();
            showMessage(R.string.chat_record_error);
            return;
        }
        showRecording(true);
        timer.post(recordTick);
    }

    private void finishRecording(boolean sendIt) {
        if (!recorder.isRecording()) return;
        timer.removeCallbacks(recordTick);
        showRecording(false);
        if (!sendIt) { recorder.stopQuietly(); return; }
        VoiceRecorder.Result result = recorder.stop();
        if (result == null) { showMessage(R.string.chat_record_too_short); return; }
        viewModel.sendVoice(result.file, result.durationMs, getString(R.string.chat_summary_voice));
    }

    private void showRecording(boolean recording) {
        inputBar.setVisibility(recording ? View.GONE : View.VISIBLE);
        recordingBar.setVisibility(recording ? View.VISIBLE : View.GONE);
        if (recording) recordTime.setText(getString(R.string.chat_recording_time,
                VoiceRecorder.format(0), VoiceRecorder.format(VoiceRecorder.MAX_MS)));
    }

    private void showPhoto(String mediaRef) {
        ImageView image = new ImageView(this);
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(getString(R.string.chat_photo));
        image.setMinimumHeight((int) (240 * getResources().getDisplayMetrics().density));
        RemoteImageLoader.load(image, mediaRef, getResources().getDisplayMetrics().widthPixels);
        new MaterialAlertDialogBuilder(this).setView(image)
                .setPositiveButton(R.string.chat_photo_close, null).show();
    }

    private void render(ConversationViewModel.UiState state) {
        loading.setVisibility(state.status == ConversationViewModel.Status.LOADING
                ? View.VISIBLE : View.GONE);
        content.setVisibility(state.status == ConversationViewModel.Status.CONTENT
                ? View.VISIBLE : View.GONE);
        error.setVisibility(state.status == ConversationViewModel.Status.ERROR
                ? View.VISIBLE : View.GONE);
        notFound.setVisibility(state.status == ConversationViewModel.Status.NOT_FOUND
                ? View.VISIBLE : View.GONE);
        ready = state.status == ConversationViewModel.Status.CONTENT;
        if (!ready) {
            send.setEnabled(false);
            attach.setEnabled(false);
            mic.setEnabled(false);
        }
        if (state.status != ConversationViewModel.Status.CONTENT) return;
        ownBlocked=state.ownBlocked;ownBlockKnown=state.ownBlockKnown;
        findViewById(R.id.conversation_safety).setEnabled(state.peerUid!=null&&state.ownBlockKnown);
        findViewById(R.id.conversation_block_notice).setVisibility(state.ownBlocked?View.VISIBLE:View.GONE);
        if(state.peerUid!=null&&!state.peerUid.equals(safetyPeerId)){
            safetyPeerId=state.peerUid;safetyViewModel.load(safetyPeerId);
        }

        String peerName = state.peer == null
                || state.peer.getDisplayName() == null || state.peer.getDisplayName().trim().isEmpty()
                ? getString(R.string.detail_member_fallback) : state.peer.getDisplayName();
        setText(R.id.conversation_peer_name, peerName);
        input.setHint(getString(R.string.chat_input_hint_to, peerName));
        setText(R.id.conversation_peer_avatar, SecondaryChrome.initials(peerName));
        findViewById(R.id.conversation_peer_verified).setVisibility(
                state.peer != null && state.peer.isEmailVerified() ? View.VISIBLE : View.GONE);
        setText(R.id.conversation_peer_sub, peerLine(state));
        boolean direct = state.conversation != null && state.conversation.getListingId().isEmpty();
        findViewById(R.id.conversation_listing_card).setVisibility(direct ? View.GONE : View.VISIBLE);
        if (direct) findViewById(R.id.conversation_status_card).setVisibility(View.GONE);
        else bindListing(state.listing, !state.messages.isEmpty());
        noMessages.setVisibility(state.messages.isEmpty() ? View.VISIBLE : View.GONE);
        int previousCount = adapter.getItemCount();
        adapter.submitList(state.messages, state.currentUid);
        adapter.setListings(state.cardListings);
        if (!messagesAnimated && !state.messages.isEmpty()) {
            messagesAnimated = true;
            Motion.staggerRows(recycler, 200, 120);
        }
        if (state.messages.size() > previousCount && !state.messages.isEmpty()) {
            recycler.scrollToPosition(state.messages.size() - 1);
        }
        sending = state.sending;
        uploadProgress.setVisibility(sending ? View.VISIBLE : View.GONE);
        updateSendEnabled();
        if (state.mediaErrorVersion > shownMediaError) {
            shownMediaError = state.mediaErrorVersion;
            showMessage(R.string.chat_media_send_error);
        }
        bindReviewState(state);
        if (state.sentVersion > shownSent) {
            shownSent = state.sentVersion;
            input.setText("");
        }
        if (state.sendErrorVersion > shownSendError) {
            shownSendError = state.sendErrorVersion;
            showMessage(R.string.safety_messaging_unavailable);
        }
        if (state.summaryWarningVersion > shownSummaryWarning) {
            shownSummaryWarning = state.summaryWarningVersion;
            showMessage(R.string.conversation_summary_warning);
        }
        if (state.refreshErrorVersion > shownRefreshError) {
            shownRefreshError = state.refreshErrorVersion;
            showMessage(R.string.conversation_refresh_error);
        }
    }

    private void bindReviewState(ConversationViewModel.UiState state) {
        boolean eligible = state.reviewEligibility == ReviewRepository.Eligibility.ELIGIBLE;
        boolean reviewed = state.reviewEligibility == ReviewRepository.Eligibility.ALREADY_REVIEWED;
        rate.setVisibility(eligible || reviewed ? View.VISIBLE : View.GONE);
        rate.setText(reviewed ? R.string.review_reviewed : R.string.review_rate_member);
        rate.setEnabled(eligible && !state.reviewSubmitting);
        RateUserBottomSheet sheet = (RateUserBottomSheet) getSupportFragmentManager()
                .findFragmentByTag("rate-user");
        if (sheet != null) sheet.setSubmitting(state.reviewSubmitting);
        if (state.reviewSuccessVersion > shownReviewSuccess) {
            shownReviewSuccess = state.reviewSuccessVersion;
            if (sheet != null) sheet.dismiss();
            showMessage(R.string.review_submit_success);
        }
        if (state.reviewErrorVersion > shownReviewError) {
            shownReviewError = state.reviewErrorVersion;
            showMessage(R.string.review_submit_error);
        }
    }

    private void showRateSheet() {
        ConversationViewModel.UiState state = viewModel.getState().getValue();
        if (state == null || state.reviewEligibility != ReviewRepository.Eligibility.ELIGIBLE) return;
        String name = state.peer == null ? getString(R.string.detail_member_fallback)
                : state.peer.getDisplayName();
        RateUserBottomSheet.newInstance(name).show(getSupportFragmentManager(), "rate-user");
    }

    @Override public void onSubmitReview(int rating, String comment) {
        viewModel.submitReview(rating, comment);
    }

    /** "Giver · Quận 3": the other member's role in this hand-over and their area. */
    private String peerLine(ConversationViewModel.UiState state) {
        List<String> parts = new ArrayList<>();
        if (state.listing != null && state.peerUid != null) {
            boolean owner = state.peerUid.equals(state.listing.getOwnerId());
            parts.add(getString(!owner ? R.string.chat_role_asker
                    : state.listing.getType() == ListingType.TRADE ? R.string.chat_role_trader
                    : state.listing.getType() == ListingType.SELL ? R.string.chat_role_seller : R.string.chat_role_giver));
        }
        String area = state.peer == null || state.peer.getLocationText() == null ? "" : state.peer.getLocationText().trim();
        if (!area.isEmpty()) parts.add(area);
        return String.join(" · ", parts);
    }

    private void bindListing(Listing listing, boolean asked) {
        View typeTag = findViewById(R.id.conversation_listing_type);
        if (listing == null) {
            setText(R.id.conversation_listing_name, getString(R.string.inbox_listing_unavailable));
            setText(R.id.conversation_listing_species, "");
            setText(R.id.conversation_listing_type, "");
            setText(R.id.conversation_listing_status, "");
            typeTag.setVisibility(View.GONE);
            findViewById(R.id.conversation_status_card).setVisibility(View.GONE);
            ((ImageView) findViewById(R.id.conversation_listing_photo)).setImageDrawable(null);
            findViewById(R.id.conversation_listing_card).setOnClickListener(null);
            return;
        }
        typeTag.setVisibility(View.VISIBLE);
        findViewById(R.id.conversation_listing_card).setOnClickListener(v -> startActivity(
                new Intent(this, ListingDetailActivity.class)
                        .putExtra(ListingDetailActivity.EXTRA_LISTING_ID, listing.getListingId())));
        RemoteImageLoader.load(findViewById(R.id.conversation_listing_photo), listing.getCoverImageUrl(),
                (int) (56 * getResources().getDisplayMetrics().density));
        bindTracker(listing.getStatus(), asked);
        setText(R.id.conversation_listing_name, listing.getNickname());
        setText(R.id.conversation_listing_species, listing.getSpecies());
        ListingType type = listing.getType();
        setText(R.id.conversation_listing_type, getString(type == ListingType.TRADE
                ? R.string.listing_type_trade : type == ListingType.SELL
                ? R.string.listing_type_sell : R.string.listing_type_give_away) + " · ");
        ListingStatus status = listing.getStatus();
        setText(R.id.conversation_listing_status, getString(status == ListingStatus.RESERVED
                ? R.string.listing_status_reserved : status == ListingStatus.REHOMED
                ? R.string.listing_status_rehomed : status == ListingStatus.UNAVAILABLE
                ? R.string.listing_status_unavailable : R.string.listing_status_active));
    }

    /**
     * Asked → Arranged → Picked up: a first message means the member asked; Reserved means a hand-over
     * is arranged; Rehomed means the plant was picked up (the listing's real status).
     */
    private void bindTracker(ListingStatus status, boolean asked) {
        View steps = findViewById(R.id.conversation_status_card);
        if (status == ListingStatus.UNAVAILABLE) {
            steps.setVisibility(View.GONE);
            return;
        }
        steps.setVisibility(View.VISIBLE);
        int done = !asked ? 0 : status == ListingStatus.REHOMED ? 3 : status == ListingStatus.RESERVED ? 2 : 1;
        int[] ids = {R.id.conversation_step_active, R.id.conversation_step_reserved, R.id.conversation_step_rehomed};
        int[] labels = {R.string.chat_step_asked, R.string.chat_step_arranged, R.string.chat_step_received};
        for (int i = 0; i < ids.length; i++) {
            View step = findViewById(ids[i]);
            boolean isDone = i < done;
            ImageView icon = step.findViewById(R.id.step_icon);
            TextView label = step.findViewById(R.id.step_label);
            label.setText(labels[i]);
            label.setTextColor(ContextCompat.getColor(this, isDone ? R.color.color_primary : R.color.color_text_secondary));
            icon.setVisibility(isDone ? View.VISIBLE : View.GONE);
            icon.setImageTintList(ContextCompat.getColorStateList(this, R.color.color_primary));
            // The line to the next step turns green once that step is done too.
            View line = step.findViewById(R.id.step_line);
            line.setVisibility(i < ids.length - 1 ? View.VISIBLE : View.GONE);
            line.setBackgroundColor(ContextCompat.getColor(this, i + 1 < done ? R.color.color_primary : R.color.color_panel_line));
            step.setContentDescription(getString(labels[i]));
        }
    }

    private boolean canCompose() {
        return ready && ownBlockKnown && !ownBlocked && !blockChanging && !sending;
    }

    /** Send works once there is text; the mic stays beside it, as in the design's composer. */
    private void updateSendEnabled() {
        String text = input.getText() == null ? "" : input.getText().toString();
        boolean hasText = !text.trim().isEmpty();
        send.setEnabled(canCompose() && hasText && text.trim().length() <= 1000);
        mic.setEnabled(canCompose());
        attach.setEnabled(canCompose());
        if (!canCompose() && recorder.isRecording()) finishRecording(false);
    }

    private void showSafetyMenu(View anchor){
        ConversationViewModel.UiState state=viewModel.getState().getValue();
        if(state==null||state.peerUid==null||!state.ownBlockKnown)return;
        PopupMenu menu=new PopupMenu(this,anchor);
        menu.getMenu().add(0,1,0,R.string.safety_report_member);
        menu.getMenu().add(0,2,1,state.ownBlocked?R.string.safety_unblock:R.string.safety_block);
        menu.setOnMenuItemClickListener(item->{
            if(item.getItemId()==1){ReportBottomSheet.newInstance(ReportTargetType.USER,state.peerUid,"")
                    .show(getSupportFragmentManager(),"report-member");return true;}
            if(state.ownBlocked){safetyViewModel.unblock(state.peerUid);return true;}
            new MaterialAlertDialogBuilder(this).setTitle(R.string.safety_block_title)
                    .setMessage(R.string.safety_block_message)
                    .setNegativeButton(R.string.report_cancel,null)
                    .setPositiveButton(R.string.safety_block,(dialog,which)->safetyViewModel.block(state.peerUid)).show();
            return true;
        });menu.show();
    }

    private void setText(int id, String value) {
        ((TextView) findViewById(id)).setText(value == null ? "" : value);
    }

    private void showMessage(int stringId) {
        Snackbar.make(send, stringId, Snackbar.LENGTH_SHORT).show();
    }
}
