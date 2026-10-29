package com.example.ungdungchiasecaycanh.ui.listing;

import android.content.Intent;

import com.example.ungdungchiasecaycanh.model.LightLevel;
import com.example.ungdungchiasecaycanh.model.ListingDetails;

import java.util.ArrayList;

/** Carries {@link ListingDetails} from the form to the preview as Intent extras (enum names). */
final class ListingDetailsExtras {
    private static final String CONDITION = "create_listing.condition";
    private static final String HEIGHT = "create_listing.height_cm";
    private static final String LIGHT = "create_listing.light";
    private static final String PICKUP = "create_listing.pickup_times";
    private static final String CATEGORY = "create_listing.category";
    private static final String POT = "create_listing.pot_cm";
    private static final String AGE = "create_listing.age";
    private static final String NEGOTIABLE = "create_listing.negotiable";

    private ListingDetailsExtras() { }

    static Intent put(Intent intent, ListingDetails details) {
        return intent.putStringArrayListExtra(CONDITION, new ArrayList<>(ListingDetails.names(details.getCondition())))
                .putExtra(HEIGHT, details.getHeightCm())
                .putExtra(LIGHT, details.getLight() == null ? "" : details.getLight().name())
                .putStringArrayListExtra(PICKUP, new ArrayList<>(ListingDetails.names(details.getPickupTimes())))
                .putExtra(CATEGORY, details.getCategory() == null ? "" : details.getCategory().name())
                .putExtra(POT, details.getPotCm())
                .putExtra(AGE, details.getAge() == null ? "" : details.getAge().name())
                .putExtra(NEGOTIABLE, details.isNegotiable());
    }

    static ListingDetails get(Intent intent) {
        return new ListingDetails(
                ListingDetails.parse(ListingDetails.Condition.class, intent.getStringArrayListExtra(CONDITION)),
                intent.getIntExtra(HEIGHT, 0),
                LightLevel.fromName(intent.getStringExtra(LIGHT)),
                ListingDetails.parse(ListingDetails.PickupTime.class, intent.getStringArrayListExtra(PICKUP)),
                ListingDetails.parseOne(ListingDetails.Category.class, intent.getStringExtra(CATEGORY)),
                intent.getIntExtra(POT, 0),
                ListingDetails.parseOne(ListingDetails.Age.class, intent.getStringExtra(AGE)),
                intent.getBooleanExtra(NEGOTIABLE, false));
    }
}
