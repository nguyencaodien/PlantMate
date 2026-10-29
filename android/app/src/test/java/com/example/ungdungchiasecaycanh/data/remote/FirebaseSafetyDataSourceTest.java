package com.example.ungdungchiasecaycanh.data.remote;

import static org.junit.Assert.*;
import com.example.ungdungchiasecaycanh.data.repository.SafetyRepository;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

public class FirebaseSafetyDataSourceTest {
    @Test public void reportMapHasExactlySevenCanonicalFields(){
        Map<String,Object> fields=FirebaseSafetyDataSource.reportFields(
                SafetyRepository.normalizeReport("reporter",ReportTargetType.COMMENT,"comment","post",
                        ReportReason.HARASSMENT,"  details  "));
        assertEquals(new HashSet<>(Arrays.asList("reporterId","targetType","targetId","parentId",
                "reason","details","createdAt")),fields.keySet());
        assertEquals("details",fields.get("details"));
        assertEquals("COMMENT",fields.get("targetType"));
        assertFalse(fields.containsKey("reportId"));assertFalse(fields.containsKey("status"));
        assertFalse(fields.containsKey("targetName"));
    }
    @Test public void blockMapHasCreatedAtOnly(){
        Map<String,Object> fields=FirebaseSafetyDataSource.blockFields();
        assertEquals(1,fields.size());assertTrue(fields.containsKey("createdAt"));
        assertFalse(fields.containsKey("blockedUid"));assertFalse(fields.containsKey("blockerUid"));
        assertFalse(fields.containsKey("displayName"));
    }
}
