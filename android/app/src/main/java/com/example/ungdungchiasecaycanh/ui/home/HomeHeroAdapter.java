package com.example.ungdungchiasecaycanh.ui.home;

import android.content.Context;
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
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.User;
import com.example.ungdungchiasecaycanh.util.AvatarBinder;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Home "Featured near you" strip (mockup C1): photo cards with the type tag, the plant's name and the sharer. */
public class HomeHeroAdapter extends RecyclerView.Adapter<HomeHeroAdapter.ViewHolder> {

    public interface OnOpen {
        void open(String listingId);
    }

    /** Cards repeat once (two rounds) when there are enough of them, so the strip can scroll round and round. */
    static final int LOOP_MIN = 3;
    private List<Listing> items = Collections.emptyList();
    private final OnOpen onOpen;
    private Map<String, User> owners = Collections.emptyMap();

    public HomeHeroAdapter(OnOpen onOpen) {
        this.onOpen = onOpen;
    }

    public void submitList(List<Listing> list) {
        List<Listing> next = list == null ? Collections.emptyList() : new java.util.ArrayList<>(list);
        if (next.equals(items)) return;
        items = next;
        notifyDataSetChanged();
    }

    public void setOwners(Map<String, User> owners) {
        this.owners = owners == null ? Collections.emptyMap() : owners;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_hero, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Listing l = items.get(position % items.size());
        Context c = h.itemView.getContext();
        RemoteImageLoader.load(h.photo, l.getCoverImageUrl(), (int) (160 * c.getResources().getDisplayMetrics().density));
        h.name.setText(l.getNickname());
        bindTag(h.tag, l);
        // The sharer's avatar and name; just the area until their profile is known.
        User owner = owners.get(l.getOwnerId());
        String who = owner == null ? "" : owner.getDisplayName();
        String area = l.getLocationText() == null ? "" : l.getLocationText().trim();
        h.avatar.setVisibility(who.isEmpty() ? View.GONE : View.VISIBLE);
        if (!who.isEmpty()) AvatarBinder.bind(h.avatar, who, owner.getAvatarUrl(), R.drawable.bg_avatar_white);
        h.meta.setText(who.isEmpty() ? area : who);
        h.itemView.setOnClickListener(v -> onOpen.open(l.getListingId()));
        h.itemView.setContentDescription(c.getString(R.string.home_hero_open, l.getNickname()));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(h, position);
    }

    /** Green "Give away", white "Swap" or white "Sell · price" on the photo. */
    static void bindTag(TextView tag, Listing l) {
        Context c = tag.getContext();
        boolean give = l.getType() == ListingType.GIVE_AWAY;
        tag.setBackgroundResource(give ? R.drawable.bg_tag_green : R.drawable.bg_tag_white);
        tag.setTextColor(ContextCompat.getColor(c, give ? R.color.color_on_primary : R.color.ink_on_photo));
        if (l.getType() == ListingType.SELL) {
            tag.setText(c.getString(R.string.home_hero_sell_tag,
                    c.getString(R.string.listing_type_sell_detail, ListingAdapter.formatVnd(l.getPrice()))));
        } else {
            tag.setText(give ? R.string.home_filter_give_away : R.string.home_filter_trade);
        }
    }

    @Override
    public int getItemCount() {
        return loops() ? items.size() * 2 : items.size();
    }

    /** Distinct cards (one round). */
    int cardCount() {
        return items.size();
    }

    boolean loops() {
        return items.size() >= LOOP_MIN;
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView photo;
        final TextView tag, name, meta, avatar;
        final View text;

        ViewHolder(@NonNull View v) {
            super(v);
            photo = v.findViewById(R.id.hero_photo);
            tag = v.findViewById(R.id.hero_tag);
            name = v.findViewById(R.id.hero_name);
            meta = v.findViewById(R.id.hero_meta);
            avatar = v.findViewById(R.id.hero_avatar);
            text = v.findViewById(R.id.hero_text);
        }
    }
}
