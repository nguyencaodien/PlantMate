package com.example.ungdungchiasecaycanh.ui.listing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.model.ListingStatus;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;

import java.util.ArrayList;
import java.util.List;

import com.google.android.material.button.MaterialButton;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

public class MyListingAdapter extends RecyclerView.Adapter<MyListingAdapter.ViewHolder> {
    public interface Listener {
        void onListingClick(String listingId);
        void onManageStatus(Listing listing);
    }

    private String updatingListingId;
    private final DiffList<Listing> listings = new DiffList<>(this, Listing::getListingId, l -> Arrays.asList(
            l.getNickname(), l.getSpecies(), l.getType(), l.getLocationText(), l.getStatus(), l.getCoverImageUrl(),
            l.getListingId().equals(updatingListingId)));
    private final Listener listener;

    public MyListingAdapter(Listener listener) { this.listener = listener; }

    public void submitList(List<Listing> values, String updatingId) {
        updatingListingId = updatingId;
        listings.submit(values);
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_listing, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Listing listing = listings.get(position);
        holder.cover.setImageResource(R.drawable.ic_botanical_placeholder);
        RemoteImageLoader.load(holder.photo, listing.getCoverImageUrl(),
                holder.itemView.getResources().getDisplayMetrics().widthPixels / 3);
        holder.nickname.setText(listing.getNickname());
        String species = listing.getSpecies() == null ? "" : listing.getSpecies().trim();
        holder.species.setText(species);
        holder.species.setVisibility(species.isEmpty() ? View.GONE : View.VISIBLE);
        ListingAdapter.bindTypeTag(
                holder.type, holder.cover, listing.getType());
        holder.location.setText(listing.getLocationText());
        bindStatusPill(holder.status, listing.getStatus());
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onListingClick(listing.getListingId());
        });
        holder.manage.setEnabled(!listing.getListingId().equals(updatingListingId)
                && listing.getStatus() != ListingStatus.REHOMED);
        holder.manage.setText(listing.getStatus()
                == ListingStatus.REHOMED
                ? R.string.my_listing_status_completed : R.string.my_listing_manage_status);
        holder.manage.setOnClickListener(view -> {
            if (listener != null) listener.onManageStatus(listing);
        });
    }

    @Override public int getItemCount() { return listings.size(); }

    /** Status pill colours: Active mint, Reserved amber, Rehomed lime, Unavailable grey. */
    public static void bindStatusPill(TextView pill, ListingStatus status) {
        pill.setText(ListingDetailViewModel.statusLabel(pill.getContext(), status));
        int background;
        int ink;
        int dot;
        if (status == ListingStatus.RESERVED) {
            background = R.drawable.bg_status_reserved; ink = R.color.status_reserved_ink; dot = R.color.status_reserved_dot;
        } else if (status == ListingStatus.REHOMED) {
            background = R.drawable.bg_status_rehomed; ink = R.color.status_rehomed_ink; dot = R.color.status_rehomed_ink;
        } else if (status == ListingStatus.UNAVAILABLE) {
            background = R.drawable.bg_status_unavailable; ink = R.color.status_unavailable_ink; dot = R.color.status_unavailable_dot;
        } else {
            background = R.drawable.bg_status_active; ink = R.color.status_active_ink; dot = R.color.status_active_dot;
        }
        pill.setBackgroundResource(background);
        pill.setTextColor(ContextCompat.getColor(pill.getContext(), ink));
        TextViewCompat.setCompoundDrawableTintList(pill,
                ContextCompat.getColorStateList(pill.getContext(), dot));
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final ImageView photo;
        final TextView nickname;
        final TextView species;
        final TextView type;
        final TextView location;
        final TextView status;
        final MaterialButton manage;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.my_listing_cover);
            photo = itemView.findViewById(R.id.my_listing_photo);
            nickname = itemView.findViewById(R.id.my_listing_nickname);
            species = itemView.findViewById(R.id.my_listing_species);
            type = itemView.findViewById(R.id.my_listing_type);
            location = itemView.findViewById(R.id.my_listing_location);
            status = itemView.findViewById(R.id.my_listing_status);
            manage = itemView.findViewById(R.id.my_listing_manage_status);
        }
    }
}
