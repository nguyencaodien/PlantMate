package com.example.ungdungchiasecaycanh.ui.doctor;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.data.local.entity.DoctorHistoryEntity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

public class DoctorHistoryAdapter extends RecyclerView.Adapter<DoctorHistoryAdapter.Holder> {
    public interface Listener { void onHistoryClick(long historyId); }
    private final DiffList<DoctorHistoryEntity> items = new DiffList<>(this, DoctorHistoryEntity::getId,
            h -> Arrays.asList(h.getPlantName(), h.getDiagnosis(), h.getCreatedAt(), h.getConfidence(), h.getImageUri()));
    private final Listener listener;

    public DoctorHistoryAdapter(Listener listener) { this.listener = listener; }
    public void submitList(List<DoctorHistoryEntity> values) {
        items.submit(values);
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_doctor_history, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        DoctorHistoryEntity item = items.get(position);
        holder.diagnosis.setText(item.getPlantName() == null ? item.getDiagnosis()
                : item.getPlantName() + " · " + item.getDiagnosis());
        holder.date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                .format(new Date(item.getCreatedAt())));
        holder.confidence.setVisibility(item.getConfidence() == null ? View.GONE : View.VISIBLE);
        if (item.getConfidence() != null) holder.confidence.setText(String.format(Locale.getDefault(),
                "%d%%", Math.round(item.getConfidence() * 100d)));
        holder.image.setImageResource(R.drawable.ic_plantmate_brand);
        try { holder.image.setImageURI(Uri.parse(item.getImageUri())); }
        catch (Exception ignored) { holder.image.setImageResource(R.drawable.ic_plantmate_brand); }
        holder.itemView.setOnClickListener(v -> listener.onHistoryClick(item.getId()));
    }

    @Override public int getItemCount() { return items.size(); }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView image; final TextView diagnosis, confidence, date;
        Holder(View view) {
            super(view); image = view.findViewById(R.id.doctorHistoryImage);
            diagnosis = view.findViewById(R.id.doctorHistoryDiagnosis);
            confidence = view.findViewById(R.id.doctorHistoryConfidence);
            date = view.findViewById(R.id.doctorHistoryDate);
        }
    }
}
