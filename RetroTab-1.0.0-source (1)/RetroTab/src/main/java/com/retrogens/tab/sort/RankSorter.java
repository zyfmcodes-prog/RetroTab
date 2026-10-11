package com.retrogens.tab.sort;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.hook.LuckPermsHook;
import com.retrogens.tab.tablist.PlayerTabState;
import org.bukkit.entity.Player;

import java.util.OptionalInt;

/**
 * Sorts by LuckPerms primary-group weight (highest first), then by name. Falls back to
 * config rank-weights by group name when the group has no weight, or when LuckPerms is absent.
 */
public final class RankSorter implements PlayerSorter {

    private final RetroTab plugin;

    /** Creates the rank sorter. */
    public RankSorter(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** Returns a key ordered by group weight then name. */
    @Override
    public String sortKey(Player player, PlayerTabState state) {
        LuckPermsHook lp = plugin.getLuckPermsHook();
        int weight;
        if (lp == null) {
            weight = plugin.getTabConfig().weightFor("default");
        } else {
            OptionalInt w = lp.getPrimaryGroupWeight(player);
            weight = w.isPresent() ? w.getAsInt() : plugin.getTabConfig().weightFor(lp.getPrimaryGroup(player));
        }
        return PlayerSorter.weightKey(weight, player.getName());
    }
}
