package com.retrogens.tab.scoreboard;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One viewer's private scoreboard. It shows the sidebar (up to 15 lines, each held in a team
 * prefix so lines can be long and styled) and also carries the sort teams that order the
 * player list as this viewer sees it. Must only be used on the main thread.
 */
public final class SidebarView {

    private static final int MAX_LINES = 15;
    private static final String LINE_TEAM_PREFIX = "sb_line_";
    private static final String[] ENTRIES = new String[MAX_LINES];

    static {
        for (int i = 0; i < MAX_LINES; i++) {
            // Invisible, unique scoreboard entries (internal identifiers, never displayed).
            ENTRIES[i] = "\u00a7" + Integer.toHexString(i);
        }
    }

    private final Scoreboard board;
    private final Objective objective;
    private final Map<UUID, String> sortTeams = new HashMap<>();
    private int shown;

    /** Creates a fresh scoreboard; the sidebar objective is only created when showSidebar is true. */
    public SidebarView(boolean showSidebar, boolean hideNumbers) {
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        if (showSidebar) {
            this.objective = board.registerNewObjective("retrotab", Criteria.DUMMY, Component.empty());
            this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            if (hideNumbers) {
                tryHideNumbers(objective);
            }
        } else {
            this.objective = null;
        }
    }

    /** Returns the underlying scoreboard to assign to the player. */
    public Scoreboard getBoard() {
        return board;
    }

    /** Updates the sidebar title and lines (top line first); no-op if the sidebar is disabled. */
    public void update(Component title, List<Component> lines) {
        if (objective == null) {
            return;
        }
        objective.displayName(title);
        int n = Math.min(lines.size(), MAX_LINES);
        for (int i = 0; i < n; i++) {
            String entry = ENTRIES[i];
            Team team = board.getTeam(LINE_TEAM_PREFIX + i);
            if (team == null) {
                team = board.registerNewTeam(LINE_TEAM_PREFIX + i);
            }
            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }
            team.prefix(lines.get(i));
            objective.getScore(entry).setScore(n - i);
        }
        for (int i = n; i < shown; i++) {
            board.resetScores(ENTRIES[i]);
            Team team = board.getTeam(LINE_TEAM_PREFIX + i);
            if (team != null) {
                team.unregister();
            }
        }
        shown = n;
    }

    /** Places a target player into the named sort team on this viewer's board. */
    public void moveSortEntry(UUID target, String targetName, String teamName) {
        if (teamName.equals(sortTeams.get(target))) {
            return;
        }
        removeSortEntry(target, targetName);
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.addEntry(targetName);
        sortTeams.put(target, teamName);
    }

    /** Removes a target player from whichever sort team they are in on this board. */
    public void removeSortEntry(UUID target, String targetName) {
        String old = sortTeams.remove(target);
        if (old == null) {
            return;
        }
        Team team = board.getTeam(old);
        if (team != null) {
            team.removeEntry(targetName);
            if (team.getEntries().isEmpty()) {
                team.unregister();
            }
        }
    }

    /** Gives the player this scoreboard. */
    public void show(Player player) {
        player.setScoreboard(board);
    }

    /** Hides the red score numbers via Paper's NumberFormat API when present (reflection, optional). */
    private static void tryHideNumbers(Objective objective) {
        try {
            Class<?> numberFormat = Class.forName("io.papermc.paper.scoreboard.numbers.NumberFormat");
            Object blank = numberFormat.getMethod("blank").invoke(null);
            Method method = Objective.class.getMethod("numberFormat", numberFormat);
            method.invoke(objective, blank);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Older Paper builds without number formats: numbers stay visible.
        }
    }
}
