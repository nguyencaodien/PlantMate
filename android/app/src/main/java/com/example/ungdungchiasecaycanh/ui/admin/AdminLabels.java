package com.example.ungdungchiasecaycanh.ui.admin;

import android.content.Context;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.AdminAction;
import com.example.ungdungchiasecaycanh.model.ListingType;
import com.example.ungdungchiasecaycanh.model.PostCategory;
import com.example.ungdungchiasecaycanh.model.ReportReason;
import com.example.ungdungchiasecaycanh.model.ReportReview;
import com.example.ungdungchiasecaycanh.model.ReportTargetType;

/** Display labels for admin values. */
final class AdminLabels {
    private AdminLabels() {
    }

    static String reason(Context c, ReportReason reason) {
        if (reason == null) return "";
        switch (reason) {
            case HARASSMENT: return c.getString(R.string.admin_reason_harassment);
            case SPAM: return c.getString(R.string.admin_reason_spam);
            case SCAM: return c.getString(R.string.admin_reason_scam);
            case INAPPROPRIATE: return c.getString(R.string.admin_reason_inappropriate);
            default: return c.getString(R.string.admin_reason_other);
        }
    }

    static String target(Context c, String targetType) {
        if ("USER".equals(targetType)) return c.getString(R.string.admin_target_user);
        if ("LISTING".equals(targetType)) return c.getString(R.string.admin_target_listing);
        if ("POST".equals(targetType)) return c.getString(R.string.admin_target_post);
        if ("COMMENT".equals(targetType)) return c.getString(R.string.admin_target_comment);
        if ("REPORT".equals(targetType)) return c.getString(R.string.admin_report_title);
        return targetType == null ? "" : targetType;
    }

    static String target(Context c, ReportTargetType type) {
        return target(c, type == null ? null : type.name());
    }

    static String category(Context c, PostCategory category) {
        if (category == null) return "";
        switch (category) {
            case SHARING: return c.getString(R.string.post_category_sharing);
            case QUESTION: return c.getString(R.string.post_category_question);
            case PLANT_WIN: return c.getString(R.string.post_category_win);
            case TIP: return c.getString(R.string.post_category_tip);
            default: return c.getString(R.string.post_category_general);
        }
    }

    static String type(Context c, ListingType type) {
        if (type == ListingType.TRADE) return c.getString(R.string.listing_type_trade);
        if (type == ListingType.SELL) return c.getString(R.string.listing_type_sell);
        return c.getString(R.string.listing_type_give_away);
    }

    static String action(Context c, AdminAction action) {
        String target = target(c, action.getTargetType());
        switch (action.getAction() == null ? "" : action.getAction()) {
            case AdminAction.EDIT: return c.getString(R.string.admin_log_edit, target);
            case AdminAction.DELETE: return c.getString(R.string.admin_log_delete, target);
            case AdminAction.SUSPEND: return c.getString(R.string.admin_log_suspend);
            case AdminAction.LIFT_SUSPENSION: return c.getString(R.string.admin_log_lift);
            case AdminAction.RESOLVE: return c.getString(R.string.admin_log_resolve);
            case AdminAction.DISMISS: return c.getString(R.string.admin_log_dismiss);
            case AdminAction.GRANT_ADMIN: return c.getString(R.string.admin_log_grant);
            case AdminAction.REVOKE_ADMIN: return c.getString(R.string.admin_log_revoke);
            default: return action.getAction() + " · " + target;
        }
    }

    static String reviewStatus(Context c, ReportReview review) {
        if (review == null) return c.getString(R.string.admin_status_open);
        return ReportReview.DISMISSED.equals(review.getStatus())
                ? c.getString(R.string.admin_filter_dismissed) : c.getString(R.string.admin_filter_resolved);
    }

    /** {background drawable, ink colour resource} for a review status pill. */
    static int[] reviewPill(ReportReview review) {
        if (review == null) return new int[]{R.drawable.bg_status_reserved, R.color.status_reserved_ink};
        if (ReportReview.DISMISSED.equals(review.getStatus())) return new int[]{R.drawable.bg_status_unavailable, R.color.status_unavailable_ink};
        return new int[]{R.drawable.bg_status_active, R.color.status_active_ink};
    }
}
