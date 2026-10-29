package com.example.ungdungchiasecaycanh.util;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** DiffList sends the adapter only the rows that changed. */
@RunWith(AndroidJUnit4.class)
public class DiffListTest {

    static final class Row {
        final String id;
        String text;

        Row(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    static final class TestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final DiffList<Row> rows = new DiffList<>(this, row -> row.id, row -> row.text);

        @NonNull @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            throw new UnsupportedOperationException();
        }

        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        }

        @Override public int getItemCount() {
            return rows.size();
        }
    }

    private final List<String> events = new ArrayList<>();
    private final List<Object> payloads = new ArrayList<>();
    private TestAdapter adapter;

    @Before
    public void setUp() {
        adapter = new TestAdapter();
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override public void onChanged() {
                events.add("reset");
            }

            @Override public void onItemRangeInserted(int start, int count) {
                events.add("insert " + start + " " + count);
            }

            @Override public void onItemRangeRemoved(int start, int count) {
                events.add("remove " + start + " " + count);
            }

            @Override public void onItemRangeMoved(int from, int to, int count) {
                events.add("move " + from + " " + to);
            }

            @Override public void onItemRangeChanged(int start, int count, @Nullable Object payload) {
                events.add("change " + start + " " + count);
                payloads.add(payload);
            }
        });
    }

    @Test
    public void firstListIsInserted() {
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "1")));
        assertEquals(Collections.singletonList("insert 0 2"), events);
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void newMessageAtTheEndIsTheOnlyInsert() {
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "1")));
        events.clear();
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "1"), new Row("c", "1")));
        assertEquals(Collections.singletonList("insert 2 1"), events);
    }

    @Test
    public void sameContentDispatchesNothing() {
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "1")));
        events.clear();
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "1")));
        assertEquals(Collections.emptyList(), events);
    }

    @Test
    public void changedRowIsReboundInPlace() {
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "1")));
        events.clear();
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "2")));
        assertEquals(Collections.singletonList("change 1 1"), events);
        assertSame(DiffList.REBIND, payloads.get(0));
        assertEquals("2", adapter.rows.get(1).text);
    }

    @Test
    public void removedRowIsRemoved() {
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "1"), new Row("c", "1")));
        events.clear();
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("c", "1")));
        assertEquals(Collections.singletonList("remove 1 1"), events);
    }

    @Test
    public void mutatedRowIsComparedWithWhatWasShown() {
        Row row = new Row("a", "1");
        adapter.rows.submit(Collections.singletonList(row));
        events.clear();
        row.text = "2"; // e.g. a Room entity changed in place before the next list arrives
        adapter.rows.submit(Collections.singletonList(row));
        assertEquals(Collections.singletonList("change 0 1"), events);
    }

    @Test
    public void nullListClearsTheRows() {
        adapter.rows.submit(Arrays.asList(new Row("a", "1"), new Row("b", "1")));
        events.clear();
        adapter.rows.submit(null);
        assertEquals(Collections.singletonList("remove 0 2"), events);
        assertEquals(0, adapter.getItemCount());
    }
}
