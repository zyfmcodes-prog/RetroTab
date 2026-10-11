package com.retrogens.tab.sort;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.hook.LuckPermsHook;
import com.retrogens.tab.tablist.PlayerTabState;
import org.bukkit.entity.Player;

/**
 * Sorts by the weight configured in sort.rank-weights for the player's LuckPerms primary group,
 * ignoring LuckPerms' own group weights.
 */
public final class CustomSorter implements PlayerSorter {

    private final RetroTab plugin;

    /** Creates the custom sorter. */
    public CustomSorter(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** Returns a key ordered by the configured weight for the primary group, then name. */
    @Override
    public String sortKey(Player player, PlayerTabState state) {
        LuckPermsHook lp = plugin.getLuckPermsHook();
        String group = lp == null ? "default" : lp.getPrimaryGroup(player);
        return PlayerSorter.weightKey(plugin.getTabConfig().weightFor(group), player.getName());
    }
}
