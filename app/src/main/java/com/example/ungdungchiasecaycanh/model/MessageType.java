package com.example.ungdungchiasecaycanh.model;

public enum MessageType {
    TEXT,
    /** A photo (chatMedia kind IMAGE), with an optional caption. */
    IMAGE,
    /** A voice note up to 60 seconds (chatMedia kind VOICE). */
    VOICE,
    /** A plant card: the listing being asked about, shown above the question. */
    LISTING
}
