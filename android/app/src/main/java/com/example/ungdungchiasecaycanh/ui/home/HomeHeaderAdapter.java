package com.example.ungdungchiasecaycanh.ui.home;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Holds Home's single header view as the first item of the listing grid. The fragment inflates
 * and binds the view; the holder is never recycled, so it keeps its state while off screen.
 */
public class HomeHeaderAdapter extends RecyclerView.Adapter<HomeHeaderAdapter.Holder> {
    private final View header;

    public HomeHeaderAdapter(View header) { this.header = header; }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (header.getParent() instanceof ViewGroup) ((ViewGroup) header.getParent()).removeView(header);
        header.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Holder holder = new Holder(header);
        holder.setIsRecyclable(false);
        return holder;
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) { }

    @Override public int getItemCount() { return 1; }

    static final class Holder extends RecyclerView.ViewHolder {
        Holder(@NonNull View view) { super(view); }
    }
}
