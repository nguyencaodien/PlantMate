package com.example.ungdungchiasecaycanh.ui.listing;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;
import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.Listing;
import com.example.ungdungchiasecaycanh.ui.home.ListingAdapter;

import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

public class SavedListingAdapter extends RecyclerView.Adapter<SavedListingAdapter.ViewHolder> {
    private final DiffList<Listing> listings = new DiffList<>(this, Listing::getListingId, l -> Arrays.asList(
            l.getNickname(), l.getSpecies(), l.getType(), l.getLocationText(), l.getStatus(), l.getCoverImageUrl()));
    private final ListingAdapter.OnListingClickListener listener;

    public SavedListingAdapter(ListingAdapter.OnListingClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Listing> values) {
        listings.submit(values);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_saved_listing, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
        MyListingAdapter.bindStatusPill(holder.status, listing.getStatus());
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) listener.onListingClick(listing.getListingId());
        });
    }

    @Override
    public int getItemCount() {
        return listings.size();
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView cover;
        final ImageView photo;
        final TextView nickname;
        final TextView species;
        final TextView type;
        final TextView location;
        final TextView status;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.saved_listing_cover);
            photo = itemView.findViewById(R.id.saved_listing_photo);
            nickname = itemView.findViewById(R.id.saved_listing_nickname);
            species = itemView.findViewById(R.id.saved_listing_species);
            type = itemView.findViewById(R.id.saved_listing_type);
            location = itemView.findViewById(R.id.saved_listing_location);
            status = itemView.findViewById(R.id.saved_listing_status);
        }
    }
}
