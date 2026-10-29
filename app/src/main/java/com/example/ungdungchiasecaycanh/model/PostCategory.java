package com.example.ungdungchiasecaycanh.model;

public enum PostCategory {
    GENERAL, SHARING, QUESTION, PLANT_WIN, TIP;

    public static PostCategory fromFirestore(String value) {
        if (value == null) return null;
        try { return valueOf(value); } catch (IllegalArgumentException error) { return null; }
    }
}
