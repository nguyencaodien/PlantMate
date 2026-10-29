package com.example.ungdungchiasecaycanh.ui.admin;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import com.example.ungdungchiasecaycanh.model.ListingStatus;

/** One row in an admin list, already resolved to display text by AdminListViewModel. */
public final class AdminRow {
    public final String id;
    public final String parentId;
    public final String title;
    public final String subtitle;
    public final String meta;
    public final long timeMillis;
    public final String photoUrl;
    public final String initials;
    @DrawableRes public final int iconRes;
    public final String pillText;
    @DrawableRes public final int pillBackground;
    @ColorRes public final int pillInk;
    public final ListingStatus listingStatus;
    public final String filterKey;
    public final String searchText;

    AdminRow(String id, String parentId, String title, String subtitle, String meta, long timeMillis,
             String photoUrl, String initials, int iconRes, String pillText, int pillBackground, int pillInk,
             ListingStatus listingStatus, String filterKey) {
        this.id = id;
        this.parentId = parentId == null ? "" : parentId;
        this.title = title == null ? "" : title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.meta = meta == null ? "" : meta;
        this.timeMillis = timeMillis;
        this.photoUrl = photoUrl == null ? "" : photoUrl;
        this.initials = initials == null ? "" : initials;
        this.iconRes = iconRes;
        this.pillText = pillText == null ? "" : pillText;
        this.pillBackground = pillBackground;
        this.pillInk = pillInk;
        this.listingStatus = listingStatus;
        this.filterKey = filterKey == null ? "" : filterKey;
        this.searchText = this.title + " " + this.subtitle + " " + this.meta;
    }
}
