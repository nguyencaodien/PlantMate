package com.example.ungdungchiasecaycanh.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import androidx.recyclerview.widget.ListUpdateCallback;

/**
 * An adapter's rows. {@link #submit} works out what changed with DiffUtil, so a new message or
 * listing animates in, the scroll position is kept and unchanged rows are not bound again.
 * <p>
 * What each row showed is recorded when it is submitted, because some rows are mutable (Room
 * entities) and could otherwise already match the new data by the time the next list arrives.
 */
public final class DiffList<T> {
    /** Rows that changed are rebound in place with this payload (no cross-fade). */
    public static final Object REBIND = new Object();

    public interface Key<T> {
        Object of(T item);
    }

    private final RecyclerView.Adapter<?> adapter;
    /** Adapter position of the first row (rows before it are fixed, like an "add" tile). */
    private final int offset;
    private final Key<T> id;
    private final Key<T> contents;
    private final List<T> items = new ArrayList<>();
    private List<Object> shownIds = new ArrayList<>();
    private List<Object> shownContents = new ArrayList<>();

    /**
     * @param id       stable identity of a row (listing ID, message ID, plant ID...)
     * @param contents everything the row displays; equal contents mean no rebind
     */
    public DiffList(@NonNull RecyclerView.Adapter<?> adapter, @NonNull Key<T> id, @NonNull Key<T> contents) {
        this(adapter, 0, id, contents);
    }

    public DiffList(@NonNull RecyclerView.Adapter<?> adapter, int offset, @NonNull Key<T> id,
                    @NonNull Key<T> contents) {
        this.adapter = adapter;
        this.offset = offset;
        this.id = id;
        this.contents = contents;
    }

    public T get(int position) {
        return items.get(position);
    }

    public int size() {
        return items.size();
    }

    public void submit(@Nullable List<T> next) {
        List<T> incoming = next == null ? new ArrayList<>() : new ArrayList<>(next);
        List<Object> newIds = new ArrayList<>(incoming.size());
        List<Object> newContents = new ArrayList<>(incoming.size());
        for (T item : incoming) {
            newIds.add(id.of(item));
            newContents.add(contents.of(item));
        }
        List<Object> oldIds = shownIds;
        List<Object> oldContents = shownContents;
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() {
                return oldIds.size();
            }

            @Override public int getNewListSize() {
                return newIds.size();
            }

            @Override public boolean areItemsTheSame(int oldPosition, int newPosition) {
                return Objects.equals(oldIds.get(oldPosition), newIds.get(newPosition));
            }

            @Override public boolean areContentsTheSame(int oldPosition, int newPosition) {
                return Objects.equals(oldContents.get(oldPosition), newContents.get(newPosition));
            }

            @Override public Object getChangePayload(int oldPosition, int newPosition) {
                return REBIND;
            }
        });
        items.clear();
        items.addAll(incoming);
        shownIds = newIds;
        shownContents = newContents;
        result.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override public void onInserted(int position, int count) {
                adapter.notifyItemRangeInserted(position + offset, count);
            }

            @Override public void onRemoved(int position, int count) {
                adapter.notifyItemRangeRemoved(position + offset, count);
            }

            @Override public void onMoved(int fromPosition, int toPosition) {
                adapter.notifyItemMoved(fromPosition + offset, toPosition + offset);
            }

            @Override public void onChanged(int position, int count, @Nullable Object payload) {
                adapter.notifyItemRangeChanged(position + offset, count, payload);
            }
        });
    }
}
