package com.example.ungdungchiasecaycanh.ui.admin;

import static org.junit.Assert.assertEquals;

import com.example.ungdungchiasecaycanh.model.Report;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

public class AdminHomeExcerptTest {
    private static Report report(ReportTargetType type, String target, String details) {
        return new Report("r", "u", type, target, "", ReportReason.SPAM, details, null);
    }

    @Test public void excerptShowsWhatWasReported() {
        Map<String, String> posts = Collections.singletonMap("p1", "Mua bán  cây\ngiá rẻ");
        Map<String, String> listings = Collections.singletonMap("l1", "Sen đá");
        Map<String, String> names = Collections.singletonMap("m1", "Lê Thảo Vy");
        assertEquals("Mua bán cây giá rẻ", AdminHomeViewModel.excerpt(report(ReportTargetType.POST, "p1", ""), posts, listings, names));
        assertEquals("Sen đá", AdminHomeViewModel.excerpt(report(ReportTargetType.LISTING, "l1", ""), posts, listings, names));
        assertEquals("Lê Thảo Vy", AdminHomeViewModel.excerpt(report(ReportTargetType.USER, "m1", ""), posts, listings, names));
        // Comments are not loaded on the dashboard: the reporter's note stands in.
        assertEquals("quảng cáo", AdminHomeViewModel.excerpt(report(ReportTargetType.COMMENT, "c1", "quảng cáo"), posts, listings, names));
        assertEquals("", AdminHomeViewModel.excerpt(report(ReportTargetType.POST, "gone", null), posts, listings, names));
    }
}
