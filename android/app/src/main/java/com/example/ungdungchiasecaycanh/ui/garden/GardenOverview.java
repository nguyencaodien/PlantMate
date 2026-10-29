package com.example.ungdungchiasecaycanh.ui.garden;

import com.example.ungdungchiasecaycanh.data.local.entity.CareLogEntity;
import com.example.ungdungchiasecaycanh.data.local.entity.CareType;
import com.example.ungdungchiasecaycanh.data.local.entity.PlantEntity;
import com.example.ungdungchiasecaycanh.data.repository.GardenRepository;
import com.example.ungdungchiasecaycanh.model.LightLevel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

/**
 * Everything the Garden tab shows besides the shelf, worked out from Room only: today's care (due,
 * later today, done), the Monday-to-Sunday week strip and chart, the care streak and the areas.
 * Planned waterings come from each plant's schedule; nothing is invented.
 */
public final class GardenOverview {
    public enum TaskState { DUE, LATER, DONE }

    public static final class Task {
        public final PlantEntity plant;
        public final CareType type;
        public final TaskState state;
        /** Planned time (due / later) or when it was logged (done). */
        public final long at;
        /** Due tasks: whole calendar days late (0 = due today). */
        public final int lateDays;
        /** Daily plans: which watering of the day (1-based) and how many; 0 otherwise. */
        public final int slot, slots;

        public Task(PlantEntity plant, CareType type, TaskState state, long at, int lateDays, int slot, int slots) {
            this.plant = plant; this.type = type; this.state = state; this.at = at;
            this.lateDays = lateDays; this.slot = slot; this.slots = slots;
        }
    }

    public static final class Area {
        /** "" for plants without an area. */
        public final String room;
        public final List<PlantEntity> plants;
        /** Most common light among its plants, or null. */
        public final LightLevel light;
        public final int due;

        public Area(String room, List<PlantEntity> plants, LightLevel light, int due) {
            this.room = room; this.plants = plants; this.light = light; this.due = due;
        }
    }

    public static final int DAYS = 7;

    /** Pending first (due, then later today by time), then done (newest first). */
    public final List<Task> today = new ArrayList<>();
    public int pending, done;
    /** Monday = 0. */
    public int todayIndex;
    public final long[] weekDays = new long[DAYS];
    /** Up to three marks per day: care logged (past, today) or waterings planned (today, future). */
    public final int[] weekDots = new int[DAYS];
    /** Care entries logged each day this week. */
    public final int[] weekCare = new int[DAYS];
    public int weekWatered, weekWaterPlanned;
    /** Days in a row with at least one care entry, ending today (or yesterday). */
    public int streak;
    public final List<Area> areas = new ArrayList<>();
    public int namedAreas;

    private GardenOverview() { }

    public static GardenOverview of(List<PlantEntity> plants, List<CareLogEntity> logs, long now) {
        GardenOverview o = new GardenOverview();
        List<PlantEntity> all = plants == null ? Collections.emptyList() : plants;
        List<CareLogEntity> history = logs == null ? Collections.emptyList() : logs;
        Map<Long, PlantEntity> byId = new HashMap<>();
        for (PlantEntity p : all) byId.put(p.getId(), p);

        long todayStart = startOfDay(now);
        long tomorrow = GardenRepository.addCalendarDays(todayStart, 1);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(todayStart);
        o.todayIndex = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        long weekStart = GardenRepository.addCalendarDays(todayStart, -o.todayIndex);
        for (int i = 0; i < DAYS; i++) o.weekDays[i] = GardenRepository.addCalendarDays(weekStart, i);
        long weekEnd = GardenRepository.addCalendarDays(weekStart, DAYS);

        // Logged care: today's rows, the week chart and the streak.
        Set<Long> careDays = new HashSet<>();
        List<Task> doneToday = new ArrayList<>();
        for (CareLogEntity log : history) {
            PlantEntity plant = byId.get(log.getPlantId());
            if (plant == null || log.getType() == CareType.NOTE) continue; // notes are journal, not care
            long day = startOfDay(log.getCreatedAt());
            careDays.add(day);
            if (log.getCreatedAt() >= weekStart && log.getCreatedAt() < weekEnd) {
                o.weekCare[dayIndex(o.weekDays, log.getCreatedAt())]++;
                if (log.getType() == CareType.WATER) o.weekWatered++;
            }
            if (day == todayStart) doneToday.add(new Task(plant, log.getType(), TaskState.DONE, log.getCreatedAt(), 0, 0, 0));
        }
        doneToday.sort((a, b) -> Long.compare(b.at, a.at));

        // Planned waterings: due now, later today, and the rest of the week.
        List<Task> due = new ArrayList<>();
        List<Task> later = new ArrayList<>();
        int[] planned = new int[DAYS];
        for (PlantEntity p : all) {
            WateringStatus status = WateringStatus.of(p, now);
            if (status.kind != WateringStatus.Kind.DUE && status.kind != WateringStatus.Kind.UPCOMING) continue;
            List<Integer> times = GardenRepository.parseTimes(p.getWateringTimes());
            int slot = times.isEmpty() ? 0 : slotOf(times, status.nextAt);
            if (status.kind == WateringStatus.Kind.DUE) {
                due.add(new Task(p, CareType.WATER, TaskState.DUE, status.nextAt,
                        Math.max(0, CareLogAdapter.calendarDaysBetween(status.nextAt, now)), slot, times.size()));
            } else if (status.nextAt < tomorrow) {
                later.add(new Task(p, CareType.WATER, TaskState.LATER, status.nextAt, 0, slot, times.size()));
            }
            for (int i = o.todayIndex + 1; i < DAYS; i++) {
                planned[i] += plannedOn(p, times, status.nextAt, o.weekDays[i],
                        GardenRepository.addCalendarDays(o.weekDays[i], 1));
            }
        }
        due.sort((a, b) -> Long.compare(a.at, b.at));
        later.sort((a, b) -> Long.compare(a.at, b.at));
        o.today.addAll(due);
        o.today.addAll(later);
        o.today.addAll(doneToday);
        o.pending = due.size() + later.size();
        o.done = doneToday.size();

        int plannedLater = 0;
        for (int i = 0; i < DAYS; i++) {
            int marks;
            if (i < o.todayIndex) marks = o.weekCare[i];
            else if (i == o.todayIndex) marks = o.done + o.pending;
            else { marks = planned[i]; plannedLater += planned[i]; }
            o.weekDots[i] = Math.min(3, marks);
        }
        o.weekWaterPlanned = o.weekWatered + o.pending + plannedLater;

        long day = careDays.contains(todayStart) ? todayStart : GardenRepository.addCalendarDays(todayStart, -1);
        while (careDays.contains(day)) {
            o.streak++;
            day = GardenRepository.addCalendarDays(day, -1);
        }

        o.buildAreas(all, now);
        return o;
    }

    private void buildAreas(List<PlantEntity> plants, long now) {
        Map<String, List<PlantEntity>> groups = new LinkedHashMap<>();
        for (PlantEntity p : plants) {
            String room = p.getRoom().trim();
            String key = room.toLowerCase(Locale.ROOT);
            List<PlantEntity> group = groups.get(key);
            if (group == null) groups.put(key, group = new ArrayList<>());
            group.add(p);
        }
        for (List<PlantEntity> group : groups.values()) {
            int[] lights = new int[LightLevel.values().length];
            int dueCount = 0;
            for (PlantEntity p : group) {
                LightLevel light = LightLevel.fromName(p.getLight());
                if (light != null) lights[light.ordinal()]++;
                if (WateringStatus.of(p, now).kind == WateringStatus.Kind.DUE) dueCount++;
            }
            LightLevel common = null;
            for (LightLevel level : LightLevel.values()) {
                if (lights[level.ordinal()] > 0 && (common == null || lights[level.ordinal()] > lights[common.ordinal()])) common = level;
            }
            String room = group.get(0).getRoom().trim();
            areas.add(new Area(room, group, common, dueCount));
            if (!room.isEmpty()) namedAreas++;
        }
        // Named areas with the most plants first; plants without an area last.
        areas.sort((a, b) -> {
            if (a.room.isEmpty() != b.room.isEmpty()) return a.room.isEmpty() ? 1 : -1;
            if (a.plants.size() != b.plants.size()) return Integer.compare(b.plants.size(), a.plants.size());
            return a.room.compareToIgnoreCase(b.room);
        });
    }

    /**
     * Care for one day of this week (Monday = 0): today's list for today, what was logged on a past day
     * (newest first), and the waterings the schedules put on a later day (by time).
     */
    public List<Task> tasksOn(int day, List<PlantEntity> plants, List<CareLogEntity> logs, long now) {
        return tasksForDay(weekDays[day], plants, logs, now);
    }

    /** Same as {@link #tasksOn} for any day (its local midnight), e.g. in the month calendar. */
    public List<Task> tasksForDay(long start, List<PlantEntity> plants, List<CareLogEntity> logs, long now) {
        long todayStart = weekDays[todayIndex];
        if (start == todayStart) return today;
        List<Task> out = new ArrayList<>();
        long end = GardenRepository.addCalendarDays(start, 1);
        Map<Long, PlantEntity> byId = new HashMap<>();
        for (PlantEntity p : plants) byId.put(p.getId(), p);
        if (start < todayStart) {
            for (CareLogEntity log : logs) {
                PlantEntity plant = byId.get(log.getPlantId());
                if (plant == null || log.getType() == CareType.NOTE) continue;
                if (log.getCreatedAt() >= start && log.getCreatedAt() < end) {
                    out.add(new Task(plant, log.getType(), TaskState.DONE, log.getCreatedAt(), 0, 0, 0));
                }
            }
            out.sort((a, b) -> Long.compare(b.at, a.at));
            return out;
        }
        for (PlantEntity p : plants) {
            WateringStatus status = WateringStatus.of(p, now);
            if (status.kind != WateringStatus.Kind.DUE && status.kind != WateringStatus.Kind.UPCOMING) continue;
            List<Integer> times = GardenRepository.parseTimes(p.getWateringTimes());
            int count = plannedOn(p, times, status.nextAt, start, end);
            if (count == 0) continue;
            long at = start + (times.isEmpty() ? minuteOfDay(status.nextAt) : times.get(0)) * 60_000L;
            out.add(new Task(p, CareType.WATER, TaskState.LATER, at, 0, 0, times.size()));
        }
        out.sort((a, b) -> Long.compare(a.at, b.at));
        return out;
    }

    /** Month calendar dots, same meaning as the week strip. */
    public static final int DAY_NONE = 0, DAY_PLANNED = 1, DAY_DONE = 2;

    /**
     * One state per day of the month holding {@code monthStart} (index 0 = the 1st): past days with care
     * logged are done, today is done once nothing is pending, later days with waterings planned are planned.
     */
    public int[] monthStates(long monthStart, List<PlantEntity> plants, List<CareLogEntity> logs, long now) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(monthStart);
        int days = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        c.set(Calendar.DAY_OF_MONTH, 1);
        long first = startOfDay(c.getTimeInMillis());
        long todayStart = weekDays[todayIndex];
        int[] out = new int[days];
        for (int i = 0; i < days; i++) {
            long day = GardenRepository.addCalendarDays(first, i);
            if (day == todayStart) {
                out[i] = pending > 0 ? DAY_PLANNED : done > 0 ? DAY_DONE : DAY_NONE;
            } else if (!tasksForDay(day, plants, logs, now).isEmpty()) {
                out[i] = day < todayStart ? DAY_DONE : DAY_PLANNED;
            }
        }
        return out;
    }

    /** Waterings a plant's schedule puts in [dayStart, dayEnd) from its next watering on. */
    static int plannedOn(PlantEntity p, List<Integer> times, long next, long dayStart, long dayEnd) {
        if (next >= dayEnd) return 0;
        if (!times.isEmpty()) {
            int count = 0;
            for (int minute : times) {
                long at = dayStart + minute * 60_000L;
                if (at >= next && at < dayEnd) count++;
            }
            return count;
        }
        int interval = Math.max(1, p.getWateringIntervalDays());
        long at = next;
        int guard = 0;
        while (at < dayStart && guard++ < 400) at = GardenRepository.addCalendarDays(at, interval);
        return at >= dayStart && at < dayEnd ? 1 : 0;
    }

    /** 1-based watering of the day that {@code at} falls on, for daily plans. */
    static int slotOf(List<Integer> times, long at) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(at);
        int minute = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
        int index = times.indexOf(minute);
        return index < 0 ? 0 : index + 1;
    }

    private static int dayIndex(long[] days, long at) {
        for (int i = DAYS - 1; i >= 0; i--) if (at >= days[i]) return i;
        return 0;
    }

    private static int minuteOfDay(long at) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(at);
        return c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);
    }

    static long startOfDay(long at) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(at);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
