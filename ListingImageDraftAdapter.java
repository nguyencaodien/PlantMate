package com.example.ungdungchiasecaycanh.ui.listing;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;

import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

/**
 * Listing photos in a row. On the form the first photo is the large cover and the row ends with
 * "take a photo" and "choose from library" tiles while there is room; the preview shows photos only.
 */
public class ListingImageDraftAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private static final int PHOTO = 0, CAMERA = 1, LIBRARY = 2;
    private String coverUri;
    /** The first photo is the cover (bigger, with a badge), so moving into first place rebinds it. */
    private final DiffList<String> imageUris = new DiffList<>(this, u -> u, u -> Arrays.asList(u, u.equals(coverUri)));
    private final OnRemoveListener onRemoveListener;
    @Nullable private final Runnable onCamera, onLibrary;
    private boolean canAdd = true;

    /** Preview gallery: photos only, no remove buttons when {@code onRemoveListener} is null. */
    public ListingImageDraftAdapter(OnRemoveListener onRemoveListener) {
        this(onRemoveListener, null, null);
    }

    public ListingImageDraftAdapter(OnRemoveListener onRemoveListener, @Nullable Runnable onCamera,
                                    @Nullable Runnable onLibrary) {
        this.onRemoveListener = onRemoveListener;
        this.onCamera = onCamera;
        this.onLibrary = onLibrary;
    }

    public void submitList(List<String> uris) {
        List<String> next = uris == null ? new ArrayList<>() : uris;
        boolean add = next.size() < CreateListingDraft.MAX_IMAGES;
        // The camera and library tiles follow the photos: remove them first, add them after.
        if (form() && canAdd && !add) {
            canAdd = false;
            notifyItemRangeRemoved(imageUris.size(), 2);
        }
        coverUri = next.isEmpty() ? null : next.get(0);
        imageUris.submit(next);
        if (form() && !canAdd && add) {
            canAdd = true;
            notifyItemRangeInserted(imageUris.size(), 2);
        }
        canAdd = add;
    }

    private boolean form() {
        return onCamera != null && onLibrary != null;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < imageUris.size()) return PHOTO;
        return position == imageUris.size() ? CAMERA : LIBRARY;
    }

    @Override
    public int getItemCount() {
        return imageUris.size() + (form() && canAdd ? 2 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == PHOTO) {
            return new ImageViewHolder(inflater.inflate(R.layout.item_listing_image_draft, parent, false));
        }
        return new RecyclerView.ViewHolder(inflater.inflate(viewType == CAMERA
                ? R.layout.item_listing_image_camera : R.layout.item_listing_image_library, parent, false)) { };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        if (type == CAMERA) {
            holder.itemView.setOnClickListener(v -> { if (onCamera != null) onCamera.run(); });
            return;
        }
        if (type == LIBRARY) {
            holder.itemView.setOnClickListener(v -> { if (onLibrary != null) onLibrary.run(); });
            return;
        }
        ImageViewHolder h = (ImageViewHolder) holder;
        boolean cover = position == 0;
        int size = (int) ((form() && cover ? 132 : form() ? 74 : 112) * h.itemView.getResources().getDisplayMetrics().density);
        ViewGroup.LayoutParams lp = h.itemView.getLayoutParams();
        lp.width = size;
        lp.height = size;
        h.itemView.setLayoutParams(lp);
        h.image.setImageURI(Uri.parse(imageUris.get(position)));
        h.image.setContentDescription(h.itemView.getContext().getString(
                cover ? R.string.listing_cover_photo : R.string.listing_photo));
        h.coverBadge.setVisibility(form() && cover ? View.VISIBLE : View.GONE);
        if (onRemoveListener == null) {
            h.remove.setVisibility(View.GONE);
        } else {
            h.remove.setVisibility(View.VISIBLE);
            h.remove.setOnClickListener(view -> onRemoveListener.onRemove(h.getBindingAdapterPosition()));
        }
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final View remove, coverBadge;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.listing_draft_image);
            remove = itemView.findViewById(R.id.listing_draft_remove);
            coverBadge = itemView.findViewById(R.id.listing_draft_cover);
        }
    }
}
