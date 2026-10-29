package com.example.ungdungchiasecaycanh.ui.ai;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Holds the Ask Mầm conversation for this screen session (not saved anywhere). */
public class MamChatViewModel extends ViewModel {
    public static final class Entry {
        public final boolean fromUser;
        public final String text;
        public final MamAdvisor.Guidance guidance;

        Entry(boolean fromUser, String text, MamAdvisor.Guidance guidance) {
            this.fromUser = fromUser;
            this.text = text;
            this.guidance = guidance;
        }
    }

    public static final class State {
        public final List<Entry> entries;
        public final boolean answering;

        State(List<Entry> entries, boolean answering) {
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
            this.answering = answering;
        }
    }

    /** Short pause so the answer reads as a reply; the guidance itself is instant and local. */
    private static final long REPLY_DELAY_MS = 600;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Entry> entries = new ArrayList<>();
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(entries, false));
    private boolean answering;

    public LiveData<State> getState() {
        return state;
    }

    public void ask(String question) {
        String q = question == null ? "" : question.trim();
        if (q.isEmpty() || answering) return;
        entries.add(new Entry(true, q, null));
        answering = true;
        state.setValue(new State(entries, true));
        handler.postDelayed(() -> {
            entries.add(new Entry(false, null, MamAdvisor.reply(q)));
            answering = false;
            state.setValue(new State(entries, false));
        }, REPLY_DELAY_MS);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
    }
}
