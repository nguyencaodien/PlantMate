package com.example.ungdungchiasecaycanh.ui.ai;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ungdungchiasecaycanh.util.Motion;
import com.example.ungdungchiasecaycanh.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import com.example.ungdungchiasecaycanh.util.DiffList;

/** Chat rows: the greeting, user questions, Mầm's guidance cards and a typing row. */
public class MamChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface ActionListener { void onAction(MamAdvisor.Action action); }

    private static final int INTRO = 0, USER = 1, BOT = 2, TYPING = 3;
    private final DiffList<MamChatViewModel.Entry> entries = new DiffList<>(this, 1, e -> e, e -> e);
    private final ActionListener listener;
    private boolean typing;
    private int animatedUpTo;

    public MamChatAdapter(ActionListener listener) {
        this.listener = listener;
    }

    public void submit(MamChatViewModel.State state) {
        // The typing row sits after the entries: remove it first, add it after, so positions stay right.
        if (typing && !state.answering) {
            typing = false;
            notifyItemRemoved(1 + entries.size());
        }
        entries.submit(state.entries);
        if (!typing && state.answering) {
            typing = true;
            notifyItemInserted(1 + entries.size());
        }
    }

    @Override public int getItemCount() {
        return 1 + entries.size() + (typing ? 1 : 0);
    }

    @Override public int getItemViewType(int position) {
        if (position == 0) return INTRO;
        if (position > entries.size()) return TYPING;
        return entries.get(position - 1).fromUser ? USER : BOT;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        int layout = viewType == INTRO ? R.layout.item_mam_intro
                : viewType == USER ? R.layout.item_mam_user
                : viewType == BOT ? R.layout.item_mam_answer : R.layout.item_mam_typing;
        return new RecyclerView.ViewHolder(inflater.inflate(layout, parent, false)) { };
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int type = getItemViewType(position);
        View v = holder.itemView;
        if (position > animatedUpTo) {
            // New rows slide in from their side: questions from the right, Mầm from the left.
            animatedUpTo = holder.getBindingAdapterPosition();
            if (type == USER) Motion.fromRight(v, 0);
            else if (type == BOT) Motion.fromLeft(v, 0);
            else if (type == TYPING) Motion.fromLeft(v, 0);
        }
        if (type == TYPING) {
            ViewGroup dots = (ViewGroup) ((ViewGroup) v).getChildAt(0);
            for (int i = 0; i < dots.getChildCount(); i++) Motion.typingDot(dots.getChildAt(i), i * 150L);
        }
        if (type == USER) {
            ((TextView) v.findViewById(R.id.mam_user_text)).setText(entries.get(position - 1).text);
        } else if (type == BOT) {
            MamAdvisor.Guidance g = entries.get(position - 1).guidance;
            ((TextView) v.findViewById(R.id.mam_answer_title)).setText(g.title);
            LinearLayout steps = v.findViewById(R.id.mam_answer_steps);
            steps.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(v.getContext());
            for (int i = 0; i < g.steps.size(); i++) {
                View row = inflater.inflate(R.layout.item_mam_step, steps, false);
                ((TextView) row.findViewById(R.id.mam_step_number)).setText(String.valueOf(i + 1));
                ((TextView) row.findViewById(R.id.mam_step_text)).setText(g.steps.get(i));
                steps.addView(row);
            }
            TextView watch = v.findViewById(R.id.mam_answer_watch);
            watch.setText(g.watchFor);
            watch.setVisibility(g.watchFor.isEmpty() ? View.GONE : View.VISIBLE);
            MaterialButton action = v.findViewById(R.id.mam_answer_action);
            action.setVisibility(g.action == MamAdvisor.Action.NONE ? View.GONE : View.VISIBLE);
            action.setText(g.action == MamAdvisor.Action.OPEN_DOCTOR
                    ? R.string.mam_action_doctor : R.string.mam_action_garden);
            action.setOnClickListener(ignored -> listener.onAction(g.action));
        }
    }
}
