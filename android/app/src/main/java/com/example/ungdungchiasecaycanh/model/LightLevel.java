package com.example.ungdungchiasecaycanh.model;

/** How much light a plant is used to: direct sun, bright indirect, or shade. Shared by listings and Garden plants. */
public enum LightLevel {
    SUN, BRIGHT_INDIRECT, SHADE;

    /** Null for a missing or unknown stored value. */
    public static LightLevel fromName(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
