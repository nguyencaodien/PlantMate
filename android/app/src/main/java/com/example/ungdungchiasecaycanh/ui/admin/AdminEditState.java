package com.example.ungdungchiasecaycanh.ui.admin;

/**
 * Shared state for admin editor screens: load status, whether a write is in flight, and
 * one-shot event counters (compare with the last value the screen handled).
 */
public final class AdminEditState<T> {
    public enum Status { LOADING, CONTENT, NOT_FOUND, ERROR }

    public final Status status;
    public final T data;
    public final boolean busy;
    public final int savedVersion;
    public final int deletedVersion;
    public final int errorVersion;

    AdminEditState(Status status, T data, boolean busy, int savedVersion, int deletedVersion, int errorVersion) {
        this.status = status;
        this.data = data;
        this.busy = busy;
        this.savedVersion = savedVersion;
        this.deletedVersion = deletedVersion;
        this.errorVersion = errorVersion;
    }

    static <T> AdminEditState<T> loading() {
        return new AdminEditState<>(Status.LOADING, null, false, 0, 0, 0);
    }

    AdminEditState<T> withStatus(Status next, T value) {
        return new AdminEditState<>(next, value, false, savedVersion, deletedVersion, errorVersion);
    }

    AdminEditState<T> busy() {
        return new AdminEditState<>(status, data, true, savedVersion, deletedVersion, errorVersion);
    }

    AdminEditState<T> saved(T value) {
        return new AdminEditState<>(Status.CONTENT, value == null ? data : value, false, savedVersion + 1, deletedVersion, errorVersion);
    }

    AdminEditState<T> deleted() {
        return new AdminEditState<>(status, data, false, savedVersion, deletedVersion + 1, errorVersion);
    }

    AdminEditState<T> failed() {
        return new AdminEditState<>(status, data, false, savedVersion, deletedVersion, errorVersion + 1);
    }
}
