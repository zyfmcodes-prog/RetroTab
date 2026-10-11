package com.retrogens.tab.listener;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.api.RetroTabAPI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Registers players with the update loop on join and cleans up on quit.
 */
public final class PlayerListener implements Listener {

    private final RetroTab plugin;

    /** Creates the listener. */
    public PlayerListener(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** Adds the joining player to the update loop. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getManager().register(event.getPlayer());
    }

    /** Removes the leaving player from the update loop and drops their API values. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getManager().unregister(event.getPlayer());
        RetroTabAPI.clearPlayer(event.getPlayer().getUniqueId());
    }
}
