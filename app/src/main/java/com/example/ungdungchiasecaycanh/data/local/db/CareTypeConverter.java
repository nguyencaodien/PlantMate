package com.example.ungdungchiasecaycanh.data.local.db;

import androidx.room.TypeConverter;

import com.example.ungdungchiasecaycanh.data.local.entity.CareType;

public final class CareTypeConverter {
    @TypeConverter
    public static String fromCareType(CareType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static CareType toCareType(String value) {
        return value == null ? null : CareType.valueOf(value);
    }
}
