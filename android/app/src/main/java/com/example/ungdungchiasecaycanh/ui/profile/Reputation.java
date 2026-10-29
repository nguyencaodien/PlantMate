package com.example.ungdungchiasecaycanh.ui.profile;

/**
 * Member trust level shown on profiles and the listing owner card. It comes from facts other members can
 * check: received reviews, completed exchanges and how long they have been a member (75 points), plus
 * verification (25 points: email 8, phone 7, VNeID 7, photo and bio 3).
 */
public final class Reputation {
    public enum Level { NEW, TRUSTED, TOP, CAUTION }

    public final Level level;
    /** 0–100, for the progress bar next to the level. */
    public final int score;

    private Reputation(Level level, int score) {
        this.level = level;
        this.score = score;
    }

    /** Points for each verification (they add up to {@link #VERIFIED_MAX}). */
    public static final int EMAIL_POINTS = 8, PHONE_POINTS = 7, VNEID_POINTS = 7, PROFILE_POINTS = 3;
    public static final int VERIFIED_MAX = EMAIL_POINTS + PHONE_POINTS + VNEID_POINTS + PROFILE_POINTS;

    /** History only (no verification counted). */
    public static Reputation of(double averageRating, int reviewCount, int rehomedCount, long memberDays) {
        return of(averageRating, reviewCount, rehomedCount, memberDays, 0);
    }

    /** {@code verifiedPoints} from {@link #verifiedPoints}. */
    public static Reputation of(double averageRating, int reviewCount, int rehomedCount, long memberDays,
                                int verifiedPoints) {
        int reviews = Math.max(0, reviewCount);
        int rehomed = Math.max(0, rehomedCount);
        double average = reviews == 0 ? 0 : Math.max(1, Math.min(5, averageRating));
        // Ratings weigh most once there are enough of them; activity, tenure and verification add confidence.
        double ratingPart = reviews == 0 ? 0 : (average - 1) / 4.0 * Math.min(1.0, reviews / 5.0) * 45;
        double activityPart = Math.min(1.0, rehomed / 5.0) * 20;
        double tenurePart = Math.min(1.0, Math.max(0, memberDays) / 180.0) * 10;
        int verified = Math.max(0, Math.min(VERIFIED_MAX, verifiedPoints));
        int score = (int) Math.round(ratingPart + activityPart + tenurePart + verified);

        Level level;
        if (reviews >= 3 && average < 3.0) level = Level.CAUTION;
        else if (reviews >= 5 && average >= 4.5 && rehomed >= 3) level = Level.TOP;
        else if ((reviews >= 1 && average >= 4.0) || rehomed >= 2) level = Level.TRUSTED;
        else level = Level.NEW;
        return new Reputation(level, Math.max(0, Math.min(100, score)));
    }

    public static int verifiedPoints(boolean email, boolean phone, boolean vneid, boolean photoAndBio) {
        return (email ? EMAIL_POINTS : 0) + (phone ? PHONE_POINTS : 0) + (vneid ? VNEID_POINTS : 0)
                + (photoAndBio ? PROFILE_POINTS : 0);
    }

    /** Verification points of a profile; a photo and a bio count together. */
    public static int verifiedPoints(com.example.ungdungchiasecaycanh.model.User user) {
        if (user == null) return 0;
        return verifiedPoints(user.isEmailVerified(), user.isPhoneVerified(), user.isVneidVerified(), hasPhotoAndBio(user));
    }

    /** The level's display name. */
    public static int labelOf(Level level) {
        switch (level) {
            case TOP: return com.example.ungdungchiasecaycanh.R.string.member_level_top;
            case TRUSTED: return com.example.ungdungchiasecaycanh.R.string.member_level_trusted;
            case CAUTION: return com.example.ungdungchiasecaycanh.R.string.member_level_caution;
            default: return com.example.ungdungchiasecaycanh.R.string.member_level_new;
        }
    }

    public static boolean hasPhotoAndBio(com.example.ungdungchiasecaycanh.model.User user) {
        return user != null && user.getAvatarUrl() != null && !user.getAvatarUrl().trim().isEmpty()
                && user.getBio() != null && !user.getBio().trim().isEmpty();
    }
}
