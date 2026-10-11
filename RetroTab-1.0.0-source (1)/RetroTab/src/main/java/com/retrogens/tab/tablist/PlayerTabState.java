package com.retrogens.tab.tablist;

import com.retrogens.tab.scoreboard.SidebarView;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;

/**
 * Per-player state: the last rendered tablist and sidebar output (for change detection), the
 * player's private scoreboard view, the applied sort key, join order and the on/off toggle.
 */
public final class PlayerTabState {

    private final UUID uuid;
    private final long joinOrder;
    private volatile boolean enabled = true;
    private volatile boolean vanilla = true;
    private volatile Component lastHeader;
    private volatile Component lastFooter;
    private volatile Component lastListName;
    private volatile Component lastSidebarTitle;
    private volatile List<Component> lastSidebarLines;
    private volatile String lastSortKey;
    private volatile SidebarView view;

    /** Creates state for a player with the given join counter value. */
    public PlayerTabState(UUID uuid, long joinOrder) {
        this.uuid = uuid;
        this.joinOrder = joinOrder;
    }

    /** Returns the player's UUID. */
    public UUID getUuid() { return uuid; }

    /** Returns the join counter value used by JOIN_ORDER sorting. */
    public long getJoinOrder() { return joinOrder; }

    /** Returns whether the custom tablist and sidebar are enabled for this player. */
    public boolean isEnabled() { return enabled; }

    /** Sets whether the custom tablist and sidebar are enabled for this player. */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** Returns true if the player currently sees vanilla visuals (nothing applied). */
    public boolean isVanilla() { return vanilla; }

    /** Marks whether the player currently sees vanilla visuals. */
    public void setVanilla(boolean vanilla) { this.vanilla = vanilla; }

    /** Returns the last tablist header sent to the player. */
    public Component getLastHeader() { return lastHeader; }

    /** Returns the last tablist footer sent to the player. */
    public Component getLastFooter() { return lastFooter; }

    /** Records the header and footer that were just sent. */
    public void setLastHeaderFooter(Component header, Component footer) {
        this.lastHeader = header;
        this.lastFooter = footer;
    }

    /** Returns the last player-list name applied. */
    public Component getLastListName() { return lastListName; }

    /** Records the player-list name that was just applied. */
    public void setLastListName(Component name) { this.lastListName = name; }

    /** Returns the last sidebar title applied. */
    public Component getLastSidebarTitle() { return lastSidebarTitle; }

    /** Returns the last sidebar lines applied. */
    public List<Component> getLastSidebarLines() { return lastSidebarLines; }

    /** Records the sidebar title and lines that were just applied. */
    public void setLastSidebar(Component title, List<Component> lines) {
        this.lastSidebarTitle = title;
        this.lastSidebarLines = lines;
    }

    /** Returns the sort team key this player currently has on every viewer's board, or null. */
    public String getLastSortKey() { return lastSortKey; }

    /** Records the sort team key this player currently has. */
    public void setLastSortKey(String key) { this.lastSortKey = key; }

    /** Returns the player's private scoreboard view, or null if not created. */
    public SidebarView getView() { return view; }

    /** Sets (or clears) the player's private scoreboard view. */
    public void setView(SidebarView view) { this.view = view; }

    /** Forgets cached output so the next refresh re-sends everything (sort key is kept). */
    public void resetCache() {
        this.lastHeader = null;
        this.lastFooter = null;
        this.lastListName = null;
        this.lastSidebarTitle = null;
        this.lastSidebarLines = null;
    }
}
