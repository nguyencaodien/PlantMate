package com.example.ungdungchiasecaycanh.ui.inbox;

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
import com.example.ungdungchiasecaycanh.model.Message;
import com.example.ungdungchiasecaycanh.model.MessageType;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;
import com.example.ungdungchiasecaycanh.util.DiffList;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Chat messages: text bubbles, photos and voice notes, sent on the right and received on the left. */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    static final int SENT = 1;
    static final int RECEIVED = 2;

    public interface Listener {
        void onPhotoClick(String mediaRef);
        void onVoiceClick(String mediaRef);
        /** A plant card was tapped: open its listing. */
        default void onListingClick(String listingId) { }
    }

    private final DiffList<Message> messages = new DiffList<>(this, Message::getMessageId, m -> Arrays.asList(
            m.getSenderId(), m.getType(), m.getText(), m.getMediaRef(), m.getDurationMs(), m.getListingId()));
    /** Listings shown by plant cards, by ID; a card shows "listing unavailable" until its listing loads. */
    private Map<String, Listing> listings = Collections.emptyMap();
    private String currentUid;
    private Listener listener;
    private String playingRef;
    private float playingProgress;
    private boolean playingLoading;

    public void setListener(Listener listener) { this.listener = listener; }

    public void submitList(List<Message> values, String uid) {
        boolean sameAccount = Objects.equals(uid, currentUid);
        currentUid = uid;
        messages.submit(values);
        // Sent/received sides depend on the account; a different one redraws every bubble.
        if (!sameAccount) notifyItemRangeChanged(0, messages.size(), DiffList.REBIND);
    }

    /** New listing data for plant cards: rebinds only the cards. */
    public void setListings(Map<String, Listing> value) {
        Map<String, Listing> next = value == null ? Collections.emptyMap() : value;
        if (next.equals(listings)) return;
        listings = next;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getType() == MessageType.LISTING) notifyItemChanged(i, DiffList.REBIND);
        }
    }

    /** Updates the voice bubble that is playing (and resets the one that stopped). */
    public void setPlayback(String ref, float progress, boolean loading) {
        String previous = playingRef;
        playingRef = ref;
        playingProgress = progress;
        playingLoading = loading;
        for (int i = 0; i < messages.size(); i++) {
            String r = messages.get(i).getMediaRef();
            if (r.isEmpty()) continue;
            if (r.equals(ref) || r.equals(previous)) notifyItemChanged(i, "playback");
        }
    }

    @Override public int getItemViewType(int position) {
        return viewTypeFor(messages.get(position), currentUid);
    }

    public static int viewTypeFor(Message message, String uid) {
        return message != null && uid != null && uid.equals(message.getSenderId())
                ? SENT : RECEIVED;
    }

    @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == SENT ? R.layout.item_message_sent : R.layout.item_message_received;
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false), viewType == SENT);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && !payloads.contains(DiffList.REBIND)) {
            bindPlayback(holder, messages.get(position));
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Message m = messages.get(position);
        MessageType type = m.getType();
        boolean hasText = m.getText() != null && !m.getText().trim().isEmpty();
        h.text.setVisibility(type == MessageType.TEXT || (type == MessageType.IMAGE && hasText) ? View.VISIBLE : View.GONE);
        h.text.setText(m.getText());
        h.photoFrame.setVisibility(type == MessageType.IMAGE ? View.VISIBLE : View.GONE);
        h.voice.setVisibility(type == MessageType.VOICE ? View.VISIBLE : View.GONE);
        h.card.setVisibility(type == MessageType.LISTING ? View.VISIBLE : View.GONE);
        if (type == MessageType.LISTING) bindCard(h, m.getListingId());
        if (type == MessageType.IMAGE) {
            RemoteImageLoader.load(h.photo, m.getMediaRef(), (int) (220 * h.itemView.getResources().getDisplayMetrics().density));
            h.photoFrame.setOnClickListener(v -> { if (listener != null) listener.onPhotoClick(m.getMediaRef()); });
        }
        if (type == MessageType.VOICE) {
            h.wave.setSeed(m.getMessageId());
            int played = ContextCompat.getColor(h.itemView.getContext(), h.sent ? R.color.color_on_primary : R.color.color_primary);
            h.wave.setColors(played, (played & 0x00FFFFFF) | 0x66000000);
            h.play.setOnClickListener(v -> { if (listener != null) listener.onVoiceClick(m.getMediaRef()); });
            bindPlayback(h, m);
        }
    }

    /** Photo, type, name and offer of the listing asked about; tapping it opens the listing. */
    private void bindCard(ViewHolder h, String listingId) {
        Listing listing = listings.get(listingId);
        h.cardType.setVisibility(listing == null ? View.GONE : View.VISIBLE);
        h.cardOffer.setVisibility(listing == null ? View.GONE : View.VISIBLE);
        if (listing == null) {
            h.cardName.setText(R.string.inbox_listing_unavailable);
            h.cardPhoto.setImageDrawable(null);
        } else {
            ListingAdapter.bindCardTag(h.cardType, listing.getType());
            h.cardName.setText(listing.getNickname());
            ListingAdapter.bindOfferLine(h.cardOffer, listing);
            RemoteImageLoader.load(h.cardPhoto, listing.getCoverImageUrl(),
                    (int) (240 * h.itemView.getResources().getDisplayMetrics().density));
        }
        h.card.setContentDescription(h.cardName.getText());
        h.card.setOnClickListener(v -> { if (listener != null) listener.onListingClick(listingId); });
    }

    private void bindPlayback(ViewHolder h, Message m) {
        if (m.getType() != MessageType.VOICE) return;
        boolean playing = m.getMediaRef().equals(playingRef);
        h.play.setIconResource(playing && !playingLoading ? R.drawable.ic_pause : R.drawable.ic_play);
        h.play.setContentDescription(h.itemView.getContext().getString(playing ? R.string.chat_voice_stop : R.string.chat_voice_play));
        h.wave.setProgress(playing ? playingProgress : 0f);
        long shown = playing ? (long) (m.getDurationMs() * (1 - playingProgress)) : m.getDurationMs();
        h.duration.setText(VoiceRecorder.format(shown));
        h.voice.setContentDescription(h.itemView.getContext().getString(R.string.chat_voice_desc,
                VoiceRecorder.format(m.getDurationMs())));
    }

    @Override public int getItemCount() { return messages.size(); }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final boolean sent;
        final TextView text, duration, cardType, cardName, cardOffer;
        final View photoFrame, voice, card;
        final ImageView photo, cardPhoto;
        final MaterialButton play;
        final WaveformView wave;

        ViewHolder(@NonNull View view, boolean sent) {
            super(view);
            this.sent = sent;
            text = view.findViewById(R.id.message_text);
            photoFrame = view.findViewById(R.id.message_photo_frame);
            photo = view.findViewById(R.id.message_photo);
            voice = view.findViewById(R.id.message_voice);
            play = view.findViewById(R.id.message_voice_play);
            wave = view.findViewById(R.id.message_voice_wave);
            duration = view.findViewById(R.id.message_voice_duration);
            card = view.findViewById(R.id.message_listing);
            cardPhoto = view.findViewById(R.id.message_listing_photo);
            cardType = view.findViewById(R.id.message_listing_type);
            cardName = view.findViewById(R.id.message_listing_name);
            cardOffer = view.findViewById(R.id.message_listing_offer);
        }
    }
}
