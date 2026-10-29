package com.example.ungdungchiasecaycanh.ui.admin;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.ui.listing.MyListingAdapter;
import com.example.ungdungchiasecaycanh.util.RemoteImageLoader;

import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

/** Shared row adapter for every admin list. */
public class AdminRowAdapter extends RecyclerView.Adapter<AdminRowAdapter.Holder> {

    public interface Listener { void onRowClick(AdminRow row); }

    private final DiffList<AdminRow> rows = new DiffList<>(this, r -> Arrays.asList(r.id, r.parentId),
            r -> Arrays.asList(r.title, r.subtitle, r.meta, r.timeMillis, r.photoUrl, r.initials, r.iconRes,
                    r.pillText, r.pillBackground, r.pillInk, r.listingStatus));
    private final Listener listener;

    public AdminRowAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<AdminRow> values) {
        rows.submit(values);
    }

    @NonNull @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        AdminRow row = rows.get(position);
        h.title.setText(row.title);
        h.subtitle.setText(row.subtitle);
        h.subtitle.setVisibility(row.subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        String when = row.timeMillis <= 0 ? "" : String.valueOf(DateUtils.getRelativeTimeSpanString(
                row.timeMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
        String meta = row.meta.isEmpty() ? when : when.isEmpty() ? row.meta : row.meta + " · " + when;
        h.meta.setText(meta);
        h.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);

        boolean hasPhoto = !row.photoUrl.isEmpty();
        boolean hasIcon = !hasPhoto && row.iconRes != 0;
        h.initials.setText(row.initials);
        h.initials.setVisibility(!hasPhoto && !hasIcon ? View.VISIBLE : View.GONE);
        h.icon.setVisibility(hasIcon ? View.VISIBLE : View.GONE);
        if (hasIcon) h.icon.setImageResource(row.iconRes);
        if (hasPhoto) {
            RemoteImageLoader.load(h.photo, row.photoUrl, (int) (56 * h.itemView.getResources().getDisplayMetrics().density));
        } else {
            h.photo.setVisibility(View.GONE);
        }

        if (row.listingStatus != null) {
            h.pill.setVisibility(View.VISIBLE);
            MyListingAdapter.bindStatusPill(h.pill, row.listingStatus);
            h.pill.setCompoundDrawables(null, null, null, null);
        } else if (!row.pillText.isEmpty()) {
            h.pill.setVisibility(View.VISIBLE);
            h.pill.setText(row.pillText);
            h.pill.setBackgroundResource(row.pillBackground);
            h.pill.setTextColor(ContextCompat.getColor(h.pill.getContext(), row.pillInk));
        } else {
            h.pill.setVisibility(View.GONE);
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onRowClick(row); });
        h.itemView.setClickable(listener != null);
    }

    @Override public int getItemCount() { return rows.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title, subtitle, meta, initials, pill;
        final ImageView icon, photo;

        Holder(@NonNull View view) {
            super(view);
            title = view.findViewById(R.id.adminRowTitle);
            subtitle = view.findViewById(R.id.adminRowSubtitle);
            meta = view.findViewById(R.id.adminRowMeta);
            initials = view.findViewById(R.id.adminRowInitials);
            pill = view.findViewById(R.id.adminRowPill);
            icon = view.findViewById(R.id.adminRowIcon);
            photo = view.findViewById(R.id.adminRowPhoto);
        }
    }
}
