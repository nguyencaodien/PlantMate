package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

/** Admin review state for a report (reportReviews/{reportId}); a report without one is open. */
public final class ReportReview {
    public static final String RESOLVED = "RESOLVED";
    public static final String DISMISSED = "DISMISSED";

    private final String status;
    private final String note;
    private final String adminId;
    private final Timestamp reviewedAt;

    public ReportReview(String status, String note, String adminId, Timestamp reviewedAt) {
        this.status = status;
        this.note = note == null ? "" : note;
        this.adminId = adminId;
        this.reviewedAt = reviewedAt;
    }

    public String getStatus() { return status; }
    public String getNote() { return note; }
    public String getAdminId() { return adminId; }
    public Timestamp getReviewedAt() { return reviewedAt; }
}
