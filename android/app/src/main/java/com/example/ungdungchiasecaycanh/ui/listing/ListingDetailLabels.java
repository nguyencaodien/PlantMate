package com.example.ungdungchiasecaycanh.ui.listing;

import android.content.Context;

import androidx.annotation.StringRes;

import com.example.ungdungchiasecaycanh.R;
import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.model.ListingDetails;

import java.util.ArrayList;
import java.util.List;

/** Words for listing and plant details (condition, light, pick-up times), shared by every screen. */
public final class ListingDetailLabels {
    private ListingDetailLabels() { }

    @StringRes public static int of(ListingDetails.Condition c) {
        switch (c) {
            case WITH_POT: return R.string.listing_condition_with_pot;
            case NEW_LEAVES: return R.string.listing_condition_new_leaves;
            case ROOTED: return R.string.listing_condition_rooted;
            case NEEDS_CARE: return R.string.listing_condition_needs_care;
            default: return R.string.listing_condition_healthy;
        }
    }

    @StringRes public static int of(LightLevel light) {
        switch (light) {
            case SUN: return R.string.light_sun;
            case SHADE: return R.string.light_shade;
            default: return R.string.light_bright_indirect;
        }
    }

    @StringRes public static int of(ListingDetails.PickupTime time) {
        switch (time) {
            case SUN_AFTERNOON: return R.string.pickup_sun_afternoon;
            case WEEKDAY_EVENING: return R.string.pickup_weekday_evening;
            case WEEKDAY_DAYTIME: return R.string.pickup_weekday_daytime;
            default: return R.string.pickup_sat_morning;
        }
    }

    @StringRes public static int of(ListingDetails.Category c) {
        switch (c) {
            case SUCCULENT: return R.string.category_succulent;
            case HANGING: return R.string.category_hanging;
            case FLOWERING: return R.string.category_flowering;
            case HERB: return R.string.category_herb;
            case BONSAI: return R.string.category_bonsai;
            case OTHER: return R.string.category_other;
            default: return R.string.category_desk_leafy;
        }
    }

    @StringRes public static int of(ListingDetails.Age a) {
        switch (a) {
            case UNDER_3_MONTHS: return R.string.age_under_3_months;
            case OVER_A_YEAR: return R.string.age_over_a_year;
            default: return R.string.age_3_to_12_months;
        }
    }

    /** "Healthy, New leaves" style list; empty when none. */
    public static String conditions(Context context, List<ListingDetails.Condition> values) {
        List<String> words = new ArrayList<>();
        for (ListingDetails.Condition c : values) words.add(context.getString(of(c)));
        return String.join(", ", words);
    }

    /** Short card line like "Quận 3 · 70 cm · Tán xạ" from whatever the listing has. */
    public static String cardMeta(Context context, String location, ListingDetails details) {
        List<String> parts = new ArrayList<>();
        if (location != null && !location.trim().isEmpty()) parts.add(location.trim());
        if (details.getHeightCm() > 0) parts.add(context.getString(R.string.listing_height_value, details.getHeightCm()));
        if (details.getLight() != null) parts.add(context.getString(of(details.getLight())));
        return String.join(" · ", parts);
    }
}
