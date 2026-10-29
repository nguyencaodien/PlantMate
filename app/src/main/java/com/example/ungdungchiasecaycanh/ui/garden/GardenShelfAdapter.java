package com.example.ungdungchiasecaycanh.ui.garden;

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
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.util.DiffList;
import com.example.ungdungchiasecaycanh.util.LocalImageLoader;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.Arrays;
import java.util.List;

/**
 * "My plants" grid (canvas B): a card per plant with its photo, a "Water today" tag when due, the area, the next
 * watering and a bar of the time left until it is due (from {@link WateringStatus}).
 */
public class GardenShelfAdapter extends RecyclerView.Adapter<GardenShelfAdapter.Holder> {
    public interface OnPlantClick { void onPlant(long id); }

    private long now = System.currentTimeMillis();
    private final DiffList<PlantEntity> plants = new DiffList<>(this, PlantEntity::getId, p -> {
        WateringStatus s = WateringStatus.of(p, now);
        return Arrays.asList(p.getNickname(), p.getImageUri(), p.getRoom(), s.kind, s.progressPercent, s.daysUntil, s.daily, s.nextAt);
    });
    private final OnPlantClick onPlant;

    public GardenShelfAdapter(OnPlantClick onPlant) {
        this.onPlant = onPlant;
    }

    public void submit(List<PlantEntity> values, long now) {
        this.now = now;
        plants.submit(values);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_garden_plant_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Context c = h.itemView.getContext();
        PlantEntity plant = plants.get(position);
        WateringStatus status = WateringStatus.of(plant, now);
        boolean due = status.kind == WateringStatus.Kind.DUE;
        boolean scheduled = due || status.kind == WateringStatus.Kind.UPCOMING;
        h.due.setVisibility(due ? View.VISIBLE : View.GONE);
        h.name.setText(plant.getNickname());
        String room = plant.getRoom().trim();
        h.room.setText(room.isEmpty() ? c.getString(R.string.garden_area_unset) : room);
        h.status.setText(statusText(c, status));
        h.status.setTextColor(ContextCompat.getColor(c, due ? R.color.color_primary : R.color.color_text_primary));
        h.progress.setVisibility(scheduled ? View.VISIBLE : View.INVISIBLE);
        // The bar shows time left until watering: full right after watering, empty when due.
        h.progress.setProgressCompat(scheduled ? 100 - status.progressPercent : 0, false);
        h.photo.setVisibility(View.GONE);
        LocalImageLoader.load(h.photo, plant.getImageUri(), (int) (180 * c.getResources().getDisplayMetrics().density),
                () -> h.photo.setVisibility(View.VISIBLE));
        h.itemView.setContentDescription(plant.getNickname() + ", " + h.room.getText() + ", " + h.status.getText());
        h.itemView.setOnClickListener(v -> onPlant.onPlant(plant.getId()));
    }

    static String statusText(Context c, WateringStatus status) {
        switch (status.kind) {
            case DUE: return c.getString(R.string.garden_ring_due);
            case UPCOMING:
                if (status.daily) return WateringStatus.dailyLabel(c, status.nextAt, System.currentTimeMillis());
                if (status.daysUntil <= 1) return c.getString(R.string.garden_status_tomorrow);
                return c.getResources().getQuantityString(R.plurals.garden_status_days, status.daysUntil, status.daysUntil);
            default: return c.getString(R.string.garden_ring_none);
        }
    }

    @Override
    public int getItemCount() {
        return plants.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final LinearProgressIndicator progress;
        final View due;
        final ImageView photo;
        final TextView name, room, status;

        Holder(@NonNull View v) {
            super(v);
            progress = v.findViewById(R.id.ringProgress);
            due = v.findViewById(R.id.ringDue);
            photo = v.findViewById(R.id.ringPhoto);
            name = v.findViewById(R.id.ringName);
            room = v.findViewById(R.id.ringRoom);
            status = v.findViewById(R.id.ringStatus);
        }
    }
}
