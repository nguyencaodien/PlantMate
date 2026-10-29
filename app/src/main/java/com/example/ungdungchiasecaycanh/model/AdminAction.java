package com.example.ungdungchiasecaycanh.model;

import com.google.firebase.Timestamp;

/** One entry in the append-only admin audit log (adminActions/{actionId}). */
public final class AdminAction {
    public static final String EDIT = "EDIT";
    public static final String DELETE = "DELETE";
    public static final String SUSPEND = "SUSPEND";
    public static final String LIFT_SUSPENSION = "LIFT_SUSPENSION";
    public static final String RESOLVE = "RESOLVE";
    public static final String DISMISS = "DISMISS";
    public static final String GRANT_ADMIN = "GRANT_ADMIN";
    public static final String REVOKE_ADMIN = "REVOKE_ADMIN";

    private final String actionId;
    private final String adminId;
    private final String action;
    private final String targetType;
    private final String targetId;
    private final String note;
    private final Timestamp createdAt;

    public AdminAction(String actionId, String adminId, String action, String targetType,
                       String targetId, String note, Timestamp createdAt) {
        this.actionId = actionId;
        this.adminId = adminId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.note = note == null ? "" : note;
        this.createdAt = createdAt;
    }

    public String getActionId() { return actionId; }
    public String getAdminId() { return adminId; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public String getTargetId() { return targetId; }
    public String getNote() { return note; }
    public Timestamp getCreatedAt() { return createdAt; }
}
