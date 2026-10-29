package com.example.ungdungchiasecaycanh.ui.garden;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mầm's tip on the Garden (canvas B): a plant on an every-N-days plan that was watered well before its
 * interval at least twice in the last 30 days. Read only from the member's own care log.
 */
public final class GardenTips {
    public static final class EarlyWatering {
        public final PlantEntity plant;
        public final int times;

        EarlyWatering(PlantEntity plant, int times) {
            this.plant = plant;
            this.times = times;
        }
    }

    static final long WINDOW_MS = TimeUnit.DAYS.toMillis(30);
    /** A watering counts as early when it comes this many days or more before the interval. */
    static final int EARLY_BY_DAYS = 2;
    static final int MIN_EARLY = 2;

    private GardenTips() { }

    /** The plant watered early most often, or null when none qualifies. */
    public static EarlyWatering earlyWatering(List<PlantEntity> plants, List<CareLogEntity> logs, long now) {
        EarlyWatering best = null;
        for (PlantEntity p : plants) {
            int interval = p.getWateringIntervalDays();
            if (!p.getWateringTimes().isEmpty() || interval <= EARLY_BY_DAYS) continue; // daily plans water often by design
            List<Long> waterings = new ArrayList<>();
            for (CareLogEntity log : logs) {
                if (log.getPlantId() == p.getId() && log.getType() == CareType.WATER
                        && log.getCreatedAt() >= now - WINDOW_MS && log.getCreatedAt() <= now) {
                    waterings.add(log.getCreatedAt());
                }
            }
            waterings.sort(Long::compare);
            long limit = TimeUnit.DAYS.toMillis(interval - EARLY_BY_DAYS);
            int early = 0;
            for (int i = 1; i < waterings.size(); i++) if (waterings.get(i) - waterings.get(i - 1) <= limit) early++;
            if (early >= MIN_EARLY && (best == null || early > best.times)) best = new EarlyWatering(p, early);
        }
        return best;
    }
}
