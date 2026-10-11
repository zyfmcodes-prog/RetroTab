package com.retrogens.tab.tablist;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.config.TabConfig;
import com.retrogens.tab.scoreboard.SidebarRenderer;
import com.retrogens.tab.scoreboard.SidebarView;
import com.retrogens.tab.sort.CustomSorter;
import com.retrogens.tab.sort.JoinOrderSorter;
import com.retrogens.tab.sort.NameSorter;
import com.retrogens.tab.sort.PlayerSorter;
import com.retrogens.tab.sort.RankSorter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the repeating update loop for both the tablist and the sidebar scoreboard. Rendering runs
 * on an async timer (configurable); only the final sends, list names, scoreboard updates and sort
 * team changes run on the main thread, and only when the rendered output actually changed.
 * Each player gets a private scoreboard; sort teams are mirrored onto every viewer's board so
 * the player list stays ordered.
 */
public final class TablistManager {

    private static final String SEE_PERMISSION = "retrotab.see";
    private static final long ERROR_THROTTLE_MS = 30_000L;

    private final RetroTab plugin;
    private final Map<UUID, PlayerTabState> states = new ConcurrentHashMap<>();
    private final Map<TabConfig.SortMode, PlayerSorter> sorters = new EnumMap<>(TabConfig.SortMode.class);
    private final AtomicLong joinCounter = new AtomicLong();
    private volatile long lastErrorLog;
    private BukkitTask task;

    /** Creates the manager and its sorters. */
    public TablistManager(RetroTab plugin) {
        this.plugin = plugin;
        sorters.put(TabConfig.SortMode.RANK, new RankSorter(plugin));
        sorters.put(TabConfig.SortMode.NAME, new NameSorter());
        sorters.put(TabConfig.SortMode.JOIN_ORDER, new JoinOrderSorter());
        sorters.put(TabConfig.SortMode.CUSTOM, new CustomSorter(plugin));
    }

    /** Starts the repeating refresh task using the current config (first run after one second). */
    public void start() {
        start(20L);
    }

    private void start(long initialDelay) {
        cancelTask();
        TabConfig cfg = plugin.getTabConfig();
        long interval = cfg.getRefreshTicks();
        if (cfg.isAsyncUpdates()) {
            task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tick, initialDelay, interval);
        } else {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, initialDelay, interval);
        }
    }

    /** Restarts with the new config: rebuilds every player's board and re-sends everything. */
    public void restart() {
        cancelTask();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerTabState state = states.get(player.getUniqueId());
            if (state != null) {
                restoreVanilla(player, state);
            }
        }
        start(1L);
    }

    /** Stops the task and restores vanilla tablists and scoreboards for every tracked player. */
    public void stop() {
        cancelTask();
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerTabState state = states.get(player.getUniqueId());
            if (state != null) {
                restoreVanilla(player, state);
            }
        }
        states.clear();
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    /** Adds a player to the update loop. */
    public void register(Player player) {
        states.computeIfAbsent(player.getUniqueId(),
                id -> new PlayerTabState(id, joinCounter.incrementAndGet()));
    }

    /** Removes a player from the update loop and from every viewer's sort teams (main thread). */
    public void unregister(Player player) {
        PlayerTabState state = states.remove(player.getUniqueId());
        if (state != null) {
            removeSortEverywhere(player.getUniqueId(), player.getName());
        }
    }

    /** Toggles the custom tablist and sidebar for a player (main thread); returns the new state. */
    public boolean toggle(Player player) {
        PlayerTabState state = states.computeIfAbsent(player.getUniqueId(),
                id -> new PlayerTabState(id, joinCounter.incrementAndGet()));
        boolean enabled = !state.isEnabled();
        state.setEnabled(enabled);
        if (enabled) {
            state.resetCache();
        } else {
            restoreVanilla(player, state);
        }
        return enabled;
    }

    /** Returns true if the player is tracked by the update loop. */
    public boolean isRegistered(UUID uuid) {
        return states.containsKey(uuid);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerTabState state = states.get(player.getUniqueId());
            if (state == null) {
                continue;
            }
            try {
                process(player, state);
            } catch (Throwable t) {
                long now = System.currentTimeMillis();
                if (now - lastErrorLog > ERROR_THROTTLE_MS) {
                    lastErrorLog = now;
                    plugin.getMessages().warn("log.refresh-error", "player", player.getName(),
                            "error", String.valueOf(t));
                }
            }
        }
    }

    private void process(Player player, PlayerTabState state) {
        if (!state.isEnabled() || !player.hasPermission(SEE_PERMISSION)) {
            if (!state.isVanilla()) {
                runSync(() -> restoreVanilla(player, state));
            }
            return;
        }

        TabConfig cfg = plugin.getTabConfig();
        TablistRenderer.RenderedTab tab = plugin.getRenderer().render(player);
        SidebarRenderer.RenderedSidebar sidebar = cfg.isSidebarEnabled()
                ? plugin.getSidebarRenderer().render(player) : null;
        String key = sorters.get(cfg.getSortMode()).sortKey(player, state);

        boolean changed = state.getView() == null
                || !tab.header().equals(state.getLastHeader())
                || !tab.footer().equals(state.getLastFooter())
                || !tab.listName().equals(state.getLastListName())
                || !key.equals(state.getLastSortKey())
                || (sidebar != null && (!sidebar.title().equals(state.getLastSidebarTitle())
                || !sidebar.lines().equals(state.getLastSidebarLines())));
        if (!changed) {
            return;
        }
        runSync(() -> apply(player, state, tab, sidebar, key));
    }

    /** Applies rendered output on the main thread, sending only what changed. */
    private void apply(Player player, PlayerTabState state, TablistRenderer.RenderedTab tab,
                       SidebarRenderer.RenderedSidebar sidebar, String key) {
        if (!player.isOnline() || !state.isEnabled() || !player.hasPermission(SEE_PERMISSION)) {
            return;
        }
        SidebarView view = ensureView(player, state);

        if (!tab.header().equals(state.getLastHeader()) || !tab.footer().equals(state.getLastFooter())) {
            player.sendPlayerListHeaderAndFooter(tab.header(), tab.footer());
            state.setLastHeaderFooter(tab.header(), tab.footer());
        }
        if (!tab.listName().equals(state.getLastListName())) {
            player.playerListName(tab.listName());
            state.setLastListName(tab.listName());
        }
        if (sidebar != null && (!sidebar.title().equals(state.getLastSidebarTitle())
                || !sidebar.lines().equals(state.getLastSidebarLines()))) {
            view.update(sidebar.title(), sidebar.lines());
            state.setLastSidebar(sidebar.title(), sidebar.lines());
        }
        if (!key.equals(state.getLastSortKey())) {
            state.setLastSortKey(key);
            for (PlayerTabState viewer : states.values()) {
                SidebarView v = viewer.getView();
                if (v != null) {
                    v.moveSortEntry(player.getUniqueId(), player.getName(), key);
                }
            }
        }
        state.setVanilla(false);
    }

    /** Creates the player's private scoreboard if needed and mirrors all known sort teams onto it. */
    private SidebarView ensureView(Player player, PlayerTabState state) {
        SidebarView existing = state.getView();
        if (existing != null) {
            return existing;
        }
        TabConfig cfg = plugin.getTabConfig();
        SidebarView view = new SidebarView(cfg.isSidebarEnabled(), cfg.isHideNumbers());
        state.setView(view);
        view.show(player);
        for (Map.Entry<UUID, PlayerTabState> entry : states.entrySet()) {
            String targetKey = entry.getValue().getLastSortKey();
            if (targetKey == null) {
                continue;
            }
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target != null) {
                view.moveSortEntry(target.getUniqueId(), target.getName(), targetKey);
            }
        }
        return view;
    }

    private void restoreVanilla(Player player, PlayerTabState state) {
        if (player.isOnline()) {
            player.sendPlayerListHeaderAndFooter(Component.empty(), Component.empty());
            player.playerListName(null);
            if (state.getView() != null) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        removeSortEverywhere(player.getUniqueId(), player.getName());
        state.setView(null);
        state.setLastSortKey(null);
        state.resetCache();
        state.setVanilla(true);
    }

    private void removeSortEverywhere(UUID uuid, String name) {
        for (PlayerTabState viewer : states.values()) {
            SidebarView v = viewer.getView();
            if (v != null) {
                v.removeSortEntry(uuid, name);
            }
        }
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }
}
