package com.example.ungdungchiasecaycanh.data.remote;

/** A Plant Doctor failure the UI can explain: each kind maps to its own message. */
public class DoctorAiException extends Exception {
    public enum Kind {
        /** The photo does not show a plant. */
        NOT_A_PLANT,
        /** The photo could not be read from the device. */
        IMAGE_UNREADABLE,
        /** Too many requests for now (free-tier quota). */
        QUOTA,
        /** Gemini is overloaded ("high demand", 503); it usually clears within minutes. */
        BUSY,
        /** The request or answer was blocked by safety filters. */
        BLOCKED,
        /** Firebase App Check rejected this copy of the app (not registered or token not allowed). */
        APP_CHECK,
        /** Firebase AI Logic is not enabled for this Firebase project. */
        NOT_CONFIGURED,
        /** Network, server or unreadable answer. */
        FAILED
    }

    private final Kind kind;

    public DoctorAiException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    public DoctorAiException(Kind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    public Kind getKind() { return kind; }
}
