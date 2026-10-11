package com.retrogens.tab.api;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public static API other plugins call to push data into RetroTab. There is no compile-time
 * dependency requirement beyond having RetroTab on the classpath; values are plain in-memory
 * state read by the tablist on each refresh. All methods are thread-safe.
 * Callers should declare RetroTab as a softdepend and guard calls if it may be absent.
 */
public final class RetroTabAPI {

    private record EventData(String name, long endMillis) { }

    private static final Map<UUID, Double> BALANCE = new ConcurrentHashMap<>();
    private static final Map<UUID, Double> MONEY_PER_HOUR = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LEVEL = new ConcurrentHashMap<>();
    private static final Map<UUID, int[]> GENS = new ConcurrentHashMap<>();
    private static volatile EventData event;

    private RetroTabAPI() { }

    /** Starts (or replaces) the global event; a blank name or non-positive time clears it. */
    public static void setEvent(String name, long remainingMillis) {
        if (name == null || name.isBlank() || remainingMillis <= 0) {
            clearEvent();
            return;
        }
        event = new EventData(name, System.currentTimeMillis() + remainingMillis);
    }

    /** Clears the active global event. */
    public static void clearEvent() {
        event = null;
    }

    /** Sets a player's current and maximum generator count. */
    public static void setGensCount(UUID player, int current, int max) {
        GENS.put(player, new int[]{current, max});
    }

    /** Sets a player's money-per-hour value. */
    public static void setMoneyPerHour(UUID player, double value) {
        MONEY_PER_HOUR.put(player, value);
    }

    /** Sets a player's balance. */
    public static void setBalance(UUID player, double value) {
        BALANCE.put(player, value);
    }

    /** Sets a player's level. */
    public static void setLevel(UUID player, int level) {
        LEVEL.put(player, level);
    }

    /** Returns the pushed balance, or null if none was pushed. */
    public static Double getBalance(UUID player) {
        return BALANCE.get(player);
    }

    /** Returns the pushed money-per-hour, or null if none was pushed. */
    public static Double getMoneyPerHour(UUID player) {
        return MONEY_PER_HOUR.get(player);
    }

    /** Returns the pushed level, or null if none was pushed. */
    public static Integer getLevel(UUID player) {
        return LEVEL.get(player);
    }

    /** Returns the pushed current gens count, or null if none was pushed. */
    public static Integer getGensCurrent(UUID player) {
        int[] g = GENS.get(player);
        return g == null ? null : g[0];
    }

    /** Returns the pushed max gens count, or null if none was pushed. */
    public static Integer getGensMax(UUID player) {
        int[] g = GENS.get(player);
        return g == null ? null : g[1];
    }

    /** Returns the active event name, or null if none is active or it has expired. */
    public static String getEventName() {
        EventData e = event;
        if (e == null) {
            return null;
        }
        if (e.endMillis() <= System.currentTimeMillis()) {
            event = null;
            return null;
        }
        return e.name();
    }

    /** Returns the remaining event time in milliseconds, or 0 if no event is active. */
    public static long getEventRemainingMillis() {
        EventData e = event;
        if (e == null) {
            return 0L;
        }
        return Math.max(0L, e.endMillis() - System.currentTimeMillis());
    }

    /** Drops all per-player values for a player (called when they quit). */
    public static void clearPlayer(UUID player) {
        BALANCE.remove(player);
        MONEY_PER_HOUR.remove(player);
        LEVEL.remove(player);
        GENS.remove(player);
    }
}
