package com.retrogens.tab.placeholder;

import com.retrogens.tab.RetroTab;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Placeholders that are always available without PlaceholderAPI (player_name, tps, ping, ...),
 * plus the shared number and duration formatters.
 */
public final class BuiltInPlaceholders {

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Qa", "Qi"};

    private final RetroTab plugin;

    /** Creates the built-in placeholder provider. */
    public BuiltInPlaceholders(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** Resolves a lowercase built-in key for a player, or returns null if the key is not built in. */
    public String resolve(Player player, String key) {
        switch (key) {
            case "player_name":
                return player.getName();
            case "player_displayname":
                return MiniMessage.miniMessage().serialize(player.displayName());
            case "server_online":
                return String.valueOf(Bukkit.getOnlinePlayers().size());
            case "server_max":
                return String.valueOf(Bukkit.getMaxPlayers());
            case "season":
                return plugin.getTabConfig().getSeason();
            case "tps":
                return String.format(Locale.US, "%.1f", Math.min(20.0, Bukkit.getTPS()[0]));
            case "ping":
                return String.valueOf(player.getPing());
            case "world":
                return player.getWorld().getName();
            default:
                return null;
        }
    }

    /** Formats a number compactly with one decimal and no trailing zeros, e.g. 10700000 gives "10.7M". */
    public static String formatCompact(double value) {
        double v = value;
        int idx = 0;
        while (idx < SUFFIXES.length - 1 && Math.abs(Math.round(v * 10) / 10.0) >= 1000) {
            v /= 1000;
            idx++;
        }
        BigDecimal rounded = BigDecimal.valueOf(Math.round(v * 10) / 10.0).stripTrailingZeros();
        return rounded.toPlainString() + SUFFIXES[idx];
    }

    /** Formats milliseconds as "1h 5m", "2m 14s" or "45s" (rounded up to whole seconds). */
    public static String formatDuration(long millis) {
        if (millis <= 0) {
            return "0s";
        }
        long total = (long) Math.ceil(millis / 1000.0);
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }
}
