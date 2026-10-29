package com.example.ungdungchiasecaycanh.ui.safety;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ungdungchiasecaycanh.R;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.util.SecondaryChrome;
import com.example.ungdungchiasecaycanh.util.DiffList;
import java.util.Arrays;

public class BlockedUserAdapter extends RecyclerView.Adapter<BlockedUserAdapter.Holder> {
    public interface Listener { void onUnblock(String uid); }
    private final Listener listener;
    private String busyUid;
    private final DiffList<BlockedUsersViewModel.Item> items = new DiffList<>(this, i -> i.uid,
            i -> Arrays.asList(i.displayName, busyUid == null));
    public BlockedUserAdapter(Listener listener){this.listener=listener;}
    public void submit(List<BlockedUsersViewModel.Item> values,String busyUid){
        this.busyUid = busyUid;
        items.submit(values);
    }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blocked_user,parent,false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        BlockedUsersViewModel.Item item=items.get(position);
        holder.name.setText(item.displayName);
        holder.avatar.setText(SecondaryChrome.initials(item.displayName));
        holder.unblock.setEnabled(busyUid==null);
        holder.unblock.setOnClickListener(v->listener.onUnblock(item.uid));
    }
    @Override public int getItemCount(){return items.size();}
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, avatar;final MaterialButton unblock;
        Holder(View view){super(view);name=view.findViewById(R.id.blocked_user_name);avatar=view.findViewById(R.id.blocked_user_avatar);unblock=view.findViewById(R.id.blocked_user_unblock);}
    }
}
