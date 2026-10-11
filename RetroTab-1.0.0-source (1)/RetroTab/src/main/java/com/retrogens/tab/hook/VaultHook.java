package com.retrogens.tab.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Soft hook for Vault: provides a balance fallback when no plugin pushed one via the API.
 * Only instantiate when Vault is installed.
 */
public final class VaultHook {

    private final Economy economy;

    /** Looks up the registered Vault economy provider (may be absent). */
    public VaultHook() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        this.economy = rsp == null ? null : rsp.getProvider();
    }

    /** Returns true if an economy provider is registered. */
    public boolean isAvailable() {
        return economy != null;
    }

    /** Returns the player's Vault balance, or null if no economy is available. */
    public Double getBalance(OfflinePlayer player) {
        return economy == null ? null : economy.getBalance(player);
    }
}
