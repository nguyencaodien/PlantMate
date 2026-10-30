package com.example.ungdungchiasecaycanh.ui.inbox;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.ungdungchiasecaycanh.R;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

/** Inbox rows fall back to readable text, hide the plant of a direct chat and open by stable ID. */
@RunWith(AndroidJUnit4.class)
public class ConversationAdapterTest {
    private final Context context = new ContextThemeWrapper(
            ApplicationProvider.getApplicationContext(), R.style.Theme_UngDungChiaSeCayCanh);
    private final AtomicReference<String> opened = new AtomicReference<>();

    private ConversationAdapter.ViewHolder bind(InboxConversationItem item) {
        AtomicReference<ConversationAdapter.ViewHolder> holder = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            ConversationAdapter adapter = new ConversationAdapter(opened::set);
            adapter.submitList(Collections.singletonList(item));
            holder.set(adapter.onCreateViewHolder(new FrameLayout(context), 0));
            adapter.onBindViewHolder(holder.get(), 0);
        });
        return holder.get();
    }

    @Test
    public void blankNamesAndMessagesUseTheFallbackText() {
        ConversationAdapter.ViewHolder row = bind(new InboxConversationItem("c1", " ", "", "", null));
        assertEquals(context.getString(R.string.detail_member_fallback), row.peer.getText().toString());
        assertEquals(context.getString(R.string.inbox_listing_unavailable), row.listing.getText().toString());
        assertEquals(context.getString(R.string.inbox_no_messages), row.preview.getText().toString());
    }

    @Test
    public void aListingChatShowsItsPlant() {
        ConversationAdapter.ViewHolder row = bind(new InboxConversationItem("c1", "Vy", "Pothos", "Hi", null));
        assertEquals(View.VISIBLE, row.listing.getVisibility());
        assertEquals("Pothos", row.listing.getText().toString());
    }

    @Test
    public void aDirectChatHasNoPlantLine() {
        ConversationAdapter.ViewHolder row = bind(new InboxConversationItem("c1", "Vy", "", "Hi", null, true));
        assertEquals(View.GONE, row.listing.getVisibility());
    }

    @Test
    public void tappingARowOpensItsConversationId() {
        ConversationAdapter.ViewHolder row = bind(new InboxConversationItem("c7", "Vy", "Pothos", "Hi", null));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(row.itemView::performClick);
        assertEquals("c7", opened.get());
    }
}
