package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

public final class Report {
    private final String reportId;
    private final String reporterId;
    private final ReportTargetType targetType;
    private final String targetId;
    private final String parentId;
    private final ReportReason reason;
    private final String details;
    private final Timestamp createdAt;

    public Report(String reportId, String reporterId, ReportTargetType targetType,
                  String targetId, String parentId, ReportReason reason, String details,
                  Timestamp createdAt) {
        this.reportId = reportId;
        this.reporterId = reporterId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.parentId = parentId;
        this.reason = reason;
        this.details = details;
        this.createdAt = createdAt;
    }
    public String getReportId() { return reportId; }
    public String getReporterId() { return reporterId; }
    public ReportTargetType getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getParentId() { return parentId; }
    public ReportReason getReason() { return reason; }
    public String getDetails() { return details; }
    public Timestamp getCreatedAt() { return createdAt; }
}
