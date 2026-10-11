package com.retrogens.tab.placeholder;

import com.retrogens.tab.RetroTab;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * PlaceholderAPI expansion with identifier "retrotab" so other plugins can read
 * %retrotab_balance%, %retrotab_level%, %retrotab_event_name% and friends.
 */
public final class RetrotabExpansion extends PlaceholderExpansion {

    private final RetroTab plugin;

    /** Creates the expansion. */
    public RetrotabExpansion(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** Returns the expansion identifier, "retrotab". */
    @Override
    public @NotNull String getIdentifier() {
        return "retrotab";
    }

    /** Returns the expansion author. */
    @Override
    public @NotNull String getAuthor() {
        return "RetroGens";
    }

    /** Returns the expansion version (the plugin version). */
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    /** Keeps the expansion registered across PAPI reloads. */
    @Override
    public boolean persist() {
        return true;
    }

    /** Resolves %retrotab_params% for a player. */
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return null;
        }
        return plugin.getResolver().resolveRetrotab(player, params.toLowerCase(Locale.ROOT));
    }
}
