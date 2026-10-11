package com.retrogens.tab.sort;

import com.retrogens.tab.tablist.PlayerTabState;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Sorts players alphabetically by name.
 */
public final class NameSorter implements PlayerSorter {

    /** Returns a key that sorts alphabetically by lowercase name. */
    @Override
    public String sortKey(Player player, PlayerTabState state) {
        String n = player.getName().toLowerCase(Locale.ROOT);
        if (n.length() > 14) {
            n = n.substring(0, 14);
        }
        return "rt" + n;
    }
}
