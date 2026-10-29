package com.example.ungdungchiasecaycanh.ui.profile;

import android.view.View;
import android.widget.TextView;

import com.example.ungdungchiasecaycanh.R;

/** Binds one {@code item_trust_row}: label, optional points, then a tick when done or a "Verify" button. */
public final class TrustRow {
    private TrustRow() { }

    public static void bind(View row, int label, int points, boolean done, View.OnClickListener verify) {
        ((TextView) row.findViewById(R.id.trust_label)).setText(label);
        TextView pointsView = row.findViewById(R.id.trust_points);
        pointsView.setVisibility(points > 0 ? View.VISIBLE : View.GONE);
        if (points > 0) pointsView.setText(row.getContext().getString(R.string.trust_points, points));
        row.findViewById(R.id.trust_done).setVisibility(done ? View.VISIBLE : View.GONE);
        View action = row.findViewById(R.id.trust_action);
        action.setVisibility(done ? View.GONE : View.VISIBLE);
        action.setOnClickListener(verify);
    }
}
