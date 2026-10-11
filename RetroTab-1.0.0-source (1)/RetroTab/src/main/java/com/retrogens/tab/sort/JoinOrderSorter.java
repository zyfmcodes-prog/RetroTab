package com.retrogens.tab.sort;

import com.retrogens.tab.tablist.PlayerTabState;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Sorts players by the order they joined (oldest session first).
 */
public final class JoinOrderSorter implements PlayerSorter {

    /** Returns a key that sorts by join counter, then name. */
    @Override
    public String sortKey(Player player, PlayerTabState state) {
        String n = player.getName().toLowerCase(Locale.ROOT);
        if (n.length() > 8) {
            n = n.substring(0, 8);
        }
        return "rt" + String.format("%06d", state.getJoinOrder() % 1_000_000L) + n;
    }
}
