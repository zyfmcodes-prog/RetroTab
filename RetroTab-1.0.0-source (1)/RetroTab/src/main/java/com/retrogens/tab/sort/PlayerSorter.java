package com.retrogens.tab.sort;

import com.retrogens.tab.tablist.PlayerTabState;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Strategy that produces a scoreboard-team name for a player. Teams sort alphabetically in the
 * client's player list, so the returned key (at most 16 characters) decides list order.
 */
public interface PlayerSorter {

    /** Returns the sort key (team name, max 16 chars) for the player. */
    String sortKey(Player player, PlayerTabState state);

    /** Builds a key where higher weights sort first, followed by the lowercase player name. */
    static String weightKey(int weight, String name) {
        int inverted = 999 - Math.max(0, Math.min(999, weight));
        String n = name.toLowerCase(Locale.ROOT);
        if (n.length() > 11) {
            n = n.substring(0, 11);
        }
        return "rt" + String.format("%03d", inverted) + n;
    }
}
