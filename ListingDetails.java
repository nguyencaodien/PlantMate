package com.example.ungdungchiasecaycanh.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Optional facts the owner adds to a listing: condition, height, light, pick-up times, the kind of
 * plant, pot size, the plant's age and (for sales) whether the price is negotiable.
 * Every part may be empty; older listings have none. Values are enum names so Firestore stays
 * language-neutral and the rules can check them.
 */
public final class ListingDetails {
    public enum Condition { HEALTHY, WITH_POT, NEW_LEAVES, ROOTED, NEEDS_CARE }

    public enum PickupTime { SAT_MORNING, SUN_AFTERNOON, WEEKDAY_EVENING, WEEKDAY_DAYTIME }

    /** Kind of plant, for browsing and the facts grid. */
    public enum Category { DESK_LEAFY, SUCCULENT, HANGING, FLOWERING, HERB, BONSAI, OTHER }

    public enum Age { UNDER_3_MONTHS, MONTHS_3_TO_12, OVER_A_YEAR }

    /** Tallest height the form offers; 0 means "not given". */
    public static final int MAX_HEIGHT_CM = 300;
    /** Widest pot the form offers; 0 means "not given". */
    public static final int MAX_POT_CM = 100;
    public static final ListingDetails NONE = new ListingDetails(null, 0, null, null);

    private final List<Condition> condition;
    private final int heightCm;
    private final LightLevel light;
    private final List<PickupTime> pickupTimes;
    private final Category category;
    private final int potCm;
    private final Age age;
    private final boolean negotiable;

    public ListingDetails(List<Condition> condition, int heightCm, LightLevel light, List<PickupTime> pickupTimes) {
        this(condition, heightCm, light, pickupTimes, null, 0, null, false);
    }

    public ListingDetails(List<Condition> condition, int heightCm, LightLevel light, List<PickupTime> pickupTimes,
                          Category category, int potCm, Age age, boolean negotiable) {
        this.condition = unique(condition);
        this.heightCm = heightCm < 0 || heightCm > MAX_HEIGHT_CM ? 0 : heightCm;
        this.light = light;
        this.pickupTimes = unique(pickupTimes);
        this.category = category;
        this.potCm = potCm < 0 || potCm > MAX_POT_CM ? 0 : potCm;
        this.age = age;
        this.negotiable = negotiable;
    }

    public List<Condition> getCondition() { return condition; }
    public int getHeightCm() { return heightCm; }
    /** Null when the owner did not say. */
    public LightLevel getLight() { return light; }
    public List<PickupTime> getPickupTimes() { return pickupTimes; }
    /** Null when the owner did not say. */
    public Category getCategory() { return category; }
    public int getPotCm() { return potCm; }
    /** Null when the owner did not say. */
    public Age getAge() { return age; }
    public boolean isNegotiable() { return negotiable; }

    public boolean isEmpty() {
        return condition.isEmpty() && heightCm == 0 && light == null && pickupTimes.isEmpty()
                && category == null && potCm == 0 && age == null && !negotiable;
    }

    /** Reads one stored enum name; null for a missing or unknown value. */
    public static <E extends Enum<E>> E parseOne(Class<E> type, Object raw) {
        if (!(raw instanceof String)) return null;
        try {
            return Enum.valueOf(type, (String) raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Enum names, in declaration order, for storage. */
    public static <E extends Enum<E>> List<String> names(List<E> values) {
        List<String> out = new ArrayList<>(values.size());
        for (E value : values) out.add(value.name());
        return out;
    }

    /** Reads stored names, skipping unknown values written by a newer app. */
    public static <E extends Enum<E>> List<E> parse(Class<E> type, List<?> raw) {
        List<E> out = new ArrayList<>();
        if (raw == null) return out;
        for (Object item : raw) {
            if (!(item instanceof String)) continue;
            try {
                out.add(Enum.valueOf(type, (String) item));
            } catch (IllegalArgumentException ignored) {
                // a value this version does not know
            }
        }
        return out;
    }

    private static <E extends Enum<E>> List<E> unique(List<E> values) {
        if (values == null || values.isEmpty()) return Collections.emptyList();
        List<E> out = new ArrayList<>();
        for (E value : values) if (value != null && !out.contains(value)) out.add(value);
        Collections.sort(out);
        return Collections.unmodifiableList(out);
    }
}
