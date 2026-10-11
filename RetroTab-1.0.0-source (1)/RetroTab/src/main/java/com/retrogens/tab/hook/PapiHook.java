package com.retrogens.tab.hook;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.placeholder.RetrotabExpansion;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

/**
 * Soft hook for PlaceholderAPI: registers the "retrotab" expansion and passes unresolved
 * placeholders through to PAPI. Only instantiate when PlaceholderAPI is installed.
 */
public final class PapiHook {

    private final RetrotabExpansion expansion;

    /** Creates and registers the retrotab expansion. */
    public PapiHook(RetroTab plugin) {
        this.expansion = new RetrotabExpansion(plugin);
        this.expansion.register();
    }

    /** Resolves any %placeholders% in the text for the given player through PAPI. */
    public String apply(Player player, String text) {
        return PlaceholderAPI.setPlaceholders(player, text);
    }

    /** Unregisters the retrotab expansion. */
    public void unregister() {
        expansion.unregister();
    }
}
