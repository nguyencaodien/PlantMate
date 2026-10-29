package com.example.ungdungchiasecaycanh.ui.home;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.ui.listing.ListingDetailLabels;
import com.example.ungdungchiasecaycanh.util.DiffList;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ListingViewHolder> {
    public interface OnListingClickListener {
        void onListingClick(String listingId);
    }

    private Set<String> savedIds = Collections.emptySet();
    private String uid;
    /** Owner profiles by UID; a card shows the owner's name once their profile is known. */
    private Map<String, User> owners = Collections.emptyMap();
    private final DiffList<Listing> listings = new DiffList<>(this, Listing::getListingId,
            l -> Arrays.asList(shown(l), savedIds.contains(l.getListingId())));
    private final OnListingClickListener listener;
    private final OnListingClickListener onSave;

    public ListingAdapter(OnListingClickListener listener) {
        this(listener, null);
    }

    /** {@code onSave} non-null shows a save heart on other members' cards. */
    public ListingAdapter(OnListingClickListener listener, OnListingClickListener onSave) {
        this.listener = listener;
        this.onSave = onSave;
    }

    public void submitList(List<Listing> values) {
        listings.submit(values);
    }

    /** Listings plus which of them the member saved (and who the member is, to skip their own). */
    public void submitList(List<Listing> values, Set<String> saved, String currentUid) {
        savedIds = saved == null ? Collections.emptySet() : saved;
        uid = currentUid;
        listings.submit(values);
    }

    public void setOwners(Map<String, User> values) {
        owners = values == null ? Collections.emptyMap() : values;
        notifyItemRangeChanged(0, listings.size(), DiffList.REBIND);
    }

    /** What a card displays; cards whose values did not change are not bound again. */
    static Object shown(Listing l) {
        return Arrays.asList(l.getCoverImageUrl(), l.getNickname(), l.getLocationText(),
                l.getType(), l.getStatus(), l.getLookingFor(), l.getPrice(), l.getDetails().getHeightCm(), l.getDetails().getLight());
    }

    @NonNull
    @Override
    public ListingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ListingViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listing, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ListingViewHolder holder, int position) {
        Listing listing = listings.get(position);
        Context context = holder.itemView.getContext();
        RemoteImageLoader.load(holder.photo, listing.getCoverImageUrl(),
                context.getResources().getDisplayMetrics().widthPixels / 2);
        holder.nickname.setText(listing.getNickname());
        bindCardTag(holder.offer, listing.getType());
        bindOfferLine(holder.price, listing);
        bindSaleStatus(holder.status, holder.photo, listing);
        bindOwner(holder, listing);
        bindSave(holder.save, listing);
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onListingClick(listing.getListingId());
        });
    }

    /** "Owner · area" with the owner's avatar; just the area until the profile is known. */
    private void bindOwner(ListingViewHolder holder, Listing listing) {
        Context context = holder.itemView.getContext();
        User owner = owners.get(listing.getOwnerId());
        String area = ListingDetailLabels.cardMeta(context, listing.getLocationText(), listing.getDetails()).toString();
        String name = owner == null ? "" : owner.getDisplayName();
        holder.ownerAvatar.setVisibility(name.isEmpty() ? View.GONE : View.VISIBLE);
        if (name.isEmpty()) {
            holder.location.setText(area);
            return;
        }
        AvatarBinder.bind(holder.ownerAvatar, name, owner.getAvatarUrl(), R.drawable.bg_avatar_neutral);
        holder.location.setText(area.isEmpty() ? name : context.getString(R.string.home_owner_meta, name, area));
    }

    /** Type tag on the card photo (canvas B): green "Give away", white "Swap", ink "Sell". */
    public static void bindCardTag(TextView tag, ListingType type) {
        Context c = tag.getContext();
        if (type == ListingType.SELL) {
            tag.setBackgroundResource(R.drawable.bg_tag_ink);
            tag.setTextColor(ContextCompat.getColor(c, R.color.color_background));
            tag.setText(R.string.home_filter_sell);
        } else if (type == ListingType.TRADE) {
            tag.setBackgroundResource(R.drawable.bg_tag_white);
            tag.setTextColor(ContextCompat.getColor(c, R.color.ink_on_photo));
            tag.setText(R.string.home_filter_trade);
        } else {
            tag.setBackgroundResource(R.drawable.bg_tag_green);
            tag.setTextColor(ContextCompat.getColor(c, R.color.color_on_primary));
            tag.setText(R.string.home_filter_give_away);
        }
    }

    /** Grey photo for a sold plant (the price stays as it was); built on first use, on the main thread. */
    private static ColorMatrixColorFilter soldPhoto;

    private static ColorMatrixColorFilter soldPhoto() {
        if (soldPhoto == null) {
            ColorMatrix grey = new ColorMatrix();
            grey.setSaturation(0f);
            soldPhoto = new ColorMatrixColorFilter(grey);
        }
        return soldPhoto;
    }

    /**
     * Sale status pill (mockup B1): "Available", "Reserved" or "Sold" with its icon, on sale listings only;
     * a sold plant's photo turns grey. Other types hide the pill and keep a colour photo.
     */
    public static void bindSaleStatus(TextView pill, ImageView photo, Listing listing) {
        boolean sale = listing.getType() == ListingType.SELL;
        ListingStatus status = listing.getStatus();
        photo.setColorFilter(sale && status == ListingStatus.REHOMED ? soldPhoto() : null);
        int text, icon, bg, fg;
        if (sale && status == ListingStatus.ACTIVE) {
            text = R.string.sale_status_available; icon = R.drawable.ic_sale_available;
            bg = R.color.sale_available_bg; fg = R.color.sale_available_fg;
        } else if (sale && status == ListingStatus.RESERVED) {
            text = R.string.sale_status_reserved; icon = R.drawable.ic_sale_reserved;
            bg = R.color.sale_reserved_bg; fg = R.color.sale_reserved_fg;
        } else if (sale && status == ListingStatus.REHOMED) {
            text = R.string.sale_status_sold; icon = R.drawable.ic_sale_sold;
            bg = R.color.sale_sold_bg; fg = R.color.sale_sold_fg;
        } else {
            pill.setVisibility(View.GONE);
            return;
        }
        Context c = pill.getContext();
        ColorStateList ink = ColorStateList.valueOf(ContextCompat.getColor(c, fg));
        pill.setText(text);
        pill.setTextColor(ink);
        pill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(c, bg)));
        pill.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(pill, ink);
        pill.setVisibility(View.VISIBLE);
    }

    /** Offer line under the name (canvas B): green "Free", "For …" for swaps, the price in bold for sales. */
    public static void bindOfferLine(TextView line, Listing listing) {
        Context c = line.getContext();
        if (listing.getType() == ListingType.SELL) {
            line.setText(c.getString(R.string.listing_type_sell_detail, formatVnd(listing.getPrice())));
            line.setTextColor(ContextCompat.getColor(c, R.color.color_text_primary));
            line.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (listing.getType() == ListingType.TRADE) {
            String wanted = listing.getLookingFor().trim();
            line.setText(wanted.isEmpty() ? c.getString(R.string.listing_type_trade) : c.getString(R.string.home_offer_trade, wanted));
            line.setTextColor(ContextCompat.getColor(c, R.color.color_text_primary));
            line.setTypeface(null, android.graphics.Typeface.NORMAL);
        } else {
            line.setText(R.string.listing_offer_free);
            line.setTextColor(ContextCompat.getColor(c, R.color.color_primary));
            line.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void bindSave(ImageButton save, Listing listing) {
        boolean own = uid != null && uid.equals(listing.getOwnerId());
        save.setVisibility(onSave == null || own ? View.GONE : View.VISIBLE);
        if (save.getVisibility() != View.VISIBLE) return;
        boolean saved = savedIds.contains(listing.getListingId());
        Context context = save.getContext();
        save.setImageResource(saved ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
        save.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context,
                saved ? R.color.color_primary : R.color.ink_on_photo)));
        save.setContentDescription(context.getString(saved ? R.string.home_unsave_desc : R.string.home_save_desc,
                listing.getNickname()));
        save.setOnClickListener(v -> onSave.onListingClick(listing.getListingId()));
    }

    /** Canvas offer chip on the photo: green "Free", white "For ..." with a swap mark, white price. */
    public static void bindOfferChip(TextView chip, Listing listing) {
        Context context = chip.getContext();
        boolean give = listing.getType() == ListingType.GIVE_AWAY;
        chip.setBackgroundResource(give ? R.drawable.bg_badge_give_away : R.drawable.bg_pill_white);
        int fg = ContextCompat.getColor(context, give ? R.color.badge_give_text : R.color.ink_on_photo);
        chip.setTextColor(fg);
        int icon = 0;
        if (listing.getType() == ListingType.TRADE) {
            String wanted = listing.getLookingFor().trim();
            chip.setText(wanted.isEmpty() ? context.getString(R.string.listing_type_trade)
                    : context.getString(R.string.home_offer_trade, wanted));
            icon = R.drawable.ic_swap_14;
        } else if (listing.getType() == ListingType.SELL) {
            chip.setText(context.getString(R.string.listing_type_sell_detail, formatVnd(listing.getPrice())));
        } else {
            chip.setText(R.string.listing_offer_free);
        }
        chip.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        TextViewCompat.setCompoundDrawableTintList(chip, ColorStateList.valueOf(fg));
    }

    private static void bindFrame(View frame, ListingType type) {
        frame.setBackgroundResource(type == ListingType.TRADE ? R.drawable.bg_frame_trade
                : type == ListingType.SELL ? R.drawable.bg_frame_sell : R.drawable.bg_frame_give_away);
    }

    /** Applies the type tag (pastel per type) and the matching image ground. */
    public static void bindTypeTag(TextView tag, View frame, ListingType type) {
        if (type == ListingType.TRADE) {
            tag.setText(R.string.listing_type_trade);
            tag.setBackgroundResource(R.drawable.bg_badge_trade);
            tag.setTextColor(ContextCompat.getColor(tag.getContext(), R.color.badge_trade_text));
        } else if (type == ListingType.SELL) {
            tag.setText(R.string.listing_type_sell);
            tag.setBackgroundResource(R.drawable.bg_badge_sell);
            tag.setTextColor(ContextCompat.getColor(tag.getContext(), R.color.badge_sell_text));
        } else {
            tag.setText(R.string.listing_type_give_away_detail);
            tag.setBackgroundResource(R.drawable.bg_badge_give_away);
            tag.setTextColor(ContextCompat.getColor(tag.getContext(), R.color.badge_give_text));
        }
        if (frame != null) bindFrame(frame, type);
    }

    public static String formatVnd(long value) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(value);
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static class ListingViewHolder extends RecyclerView.ViewHolder {
        final ImageView photo;
        final TextView nickname;
        final TextView location;
        final TextView offer;
        final TextView price;
        final TextView status;
        final TextView ownerAvatar;
        final ImageButton save;
        final View frame;

        ListingViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.listing_card_photo);
            nickname = itemView.findViewById(R.id.listing_card_nickname);
            location = itemView.findViewById(R.id.listing_card_location);
            offer = itemView.findViewById(R.id.listing_card_offer);
            price = itemView.findViewById(R.id.listing_card_price);
            status = itemView.findViewById(R.id.listing_card_status);
            ownerAvatar = itemView.findViewById(R.id.listing_card_owner_avatar);
            save = itemView.findViewById(R.id.listing_card_save);
            frame = itemView.findViewById(R.id.listing_card_frame);
        }
    }
}
