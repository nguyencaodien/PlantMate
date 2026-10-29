package com.example.ungdungchiasecaycanh.data.local.db;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.data.local.entity.CareType;

import org.junit.Test;

public class CareTypeConverterTest {
    @Test public void allCareTypesRoundTripAsCanonicalStrings() {
        for (CareType type : CareType.values()) {
            assertEquals(type.name(), CareTypeConverter.fromCareType(type));
            assertEquals(type, CareTypeConverter.toCareType(type.name()));
        }
    }
}
