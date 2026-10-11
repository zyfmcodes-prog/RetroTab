package com.retrogens.tab;

import com.retrogens.tab.command.RetroTabCommand;
import com.retrogens.tab.config.Messages;
import com.retrogens.tab.config.TabConfig;
import com.retrogens.tab.hook.LuckPermsHook;
import com.retrogens.tab.hook.PapiHook;
import com.retrogens.tab.hook.VaultHook;
import com.retrogens.tab.listener.PlayerListener;
import com.retrogens.tab.placeholder.PlaceholderResolver;
import com.retrogens.tab.scoreboard.SidebarRenderer;
import com.retrogens.tab.tablist.TablistManager;
import com.retrogens.tab.tablist.TablistRenderer;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main class of RetroTab, a standalone, fully configurable tablist and sidebar scoreboard plugin.
 * Wires together config, hooks, placeholder resolution, rendering and the async update loop.
 */
public final class RetroTab extends JavaPlugin {

    private Messages messages;
    private TabConfig tabConfig;
    private PlaceholderResolver resolver;
    private TablistRenderer renderer;
    private SidebarRenderer sidebarRenderer;
    private TablistManager manager;
    private LuckPermsHook luckPermsHook;
    private VaultHook vaultHook;
    private PapiHook papiHook;

    /** Starts the plugin: loads config, hooks soft dependencies and starts the update loop. */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        messages = new Messages(this);
        tabConfig = new TabConfig(this);
        tabConfig.load();

        setupHooks();

        resolver = new PlaceholderResolver(this);
        renderer = new TablistRenderer(this);
        sidebarRenderer = new SidebarRenderer(this);
        manager = new TablistManager(this);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        RetroTabCommand command = new RetroTabCommand(this);
        PluginCommand pluginCommand = getCommand("retrotab");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
        }

        manager.start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            manager.register(player);
        }

        messages.log("log.enabled",
                "version", getDescription().getVersion(),
                "ticks", String.valueOf(tabConfig.getRefreshTicks()),
                "async", String.valueOf(tabConfig.isAsyncUpdates()),
                "sections", String.valueOf(tabConfig.getSections().size()));
    }

    /** Stops the update loop, restores vanilla tablists and unregisters the PAPI expansion. */
    @Override
    public void onDisable() {
        if (manager != null) {
            manager.stop();
        }
        if (papiHook != null) {
            try {
                papiHook.unregister();
            } catch (Throwable ignored) {
                // expansion may already be gone during shutdown
            }
        }
        if (messages != null) {
            messages.log("log.disabled");
        }
    }

    /** Hot-reloads config.yml and messages.yml without kicking players. */
    public void reloadAll() {
        reloadConfig();
        messages.reload();
        tabConfig.load();
        manager.restart();
    }

    private void setupHooks() {
        PluginManager pm = getServer().getPluginManager();

        if (pm.isPluginEnabled("LuckPerms")) {
            try {
                luckPermsHook = new LuckPermsHook();
                messages.log("log.hook-luckperms");
            } catch (Throwable t) {
                messages.warn("log.hook-failed", "hook", "LuckPerms", "error", String.valueOf(t.getMessage()));
            }
        }
        if (pm.isPluginEnabled("Vault")) {
            try {
                VaultHook hook = new VaultHook();
                if (hook.isAvailable()) {
                    vaultHook = hook;
                    messages.log("log.hook-vault");
                }
            } catch (Throwable t) {
                messages.warn("log.hook-failed", "hook", "Vault", "error", String.valueOf(t.getMessage()));
            }
        }
        if (pm.isPluginEnabled("PlaceholderAPI")) {
            try {
                papiHook = new PapiHook(this);
                messages.log("log.hook-papi");
            } catch (Throwable t) {
                messages.warn("log.hook-failed", "hook", "PlaceholderAPI", "error", String.valueOf(t.getMessage()));
            }
        }
    }

    /** Returns the message manager. */
    public Messages getMessages() {
        return messages;
    }

    /** Returns the parsed configuration. */
    public TabConfig getTabConfig() {
        return tabConfig;
    }

    /** Returns the placeholder resolver. */
    public PlaceholderResolver getResolver() {
        return resolver;
    }

    /** Returns the tablist renderer. */
    public TablistRenderer getRenderer() {
        return renderer;
    }

    /** Returns the sidebar scoreboard renderer. */
    public SidebarRenderer getSidebarRenderer() {
        return sidebarRenderer;
    }

    /** Returns the tablist and sidebar manager. */
    public TablistManager getManager() {
        return manager;
    }

    /** Returns the LuckPerms hook, or null if LuckPerms is not installed. */
    public LuckPermsHook getLuckPermsHook() {
        return luckPermsHook;
    }

    /** Returns the Vault hook, or null if Vault or an economy is missing. */
    public VaultHook getVaultHook() {
        return vaultHook;
    }

    /** Returns the PlaceholderAPI hook, or null if PlaceholderAPI is not installed. */
    public PapiHook getPapiHook() {
        return papiHook;
    }
}
