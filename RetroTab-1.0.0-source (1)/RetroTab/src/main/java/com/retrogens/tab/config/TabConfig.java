package com.retrogens.tab.config;

import com.retrogens.tab.RetroTab;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Loads and validates config.yml. Problems (unknown bar colors, missing keys, malformed
 * MiniMessage) are logged as warnings and replaced with defaults; they never crash the plugin.
 */
public final class TabConfig {

    /** Available player-list sort modes. */
    public enum SortMode { RANK, NAME, JOIN_ORDER, CUSTOM }

    /** A rank's tablist badge and name color, both MiniMessage strings. */
    public record Rank(String badge, String nameColor) { }

    private static final Rank EMPTY_RANK = new Rank("", "<white>");
    private static final Pattern LEFTOVER_TAG = Pattern.compile("<[a-zA-Z/#!][^<>]*>");

    private final RetroTab plugin;

    private String season;
    private int refreshTicks;
    private boolean asyncUpdates;
    private String fallbackValue;
    private SortMode sortMode;
    private List<String> header;
    private List<String> footer;
    private String barCharacter;
    private List<Section> sections;
    private Map<String, String> barColors;
    private String playerListFormat;
    private Map<String, Integer> rankWeights;
    private Map<String, Rank> ranks;
    private Map<String, String> placeholderDefaults;
    private boolean sidebarEnabled;
    private String sidebarTitle;
    private List<String> sidebarFooter;
    private boolean hideNumbers;
    private boolean eventsEnabled;
    private String eventFallbackName;
    private String eventFallbackRemaining;
    private boolean debug;

    /** Creates an unloaded config wrapper; call {@link #load()} next. */
    public TabConfig(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** (Re)loads every value from the plugin's current FileConfiguration. */
    public void load() {
        FileConfiguration cfg = plugin.getConfig();
        Messages msg = plugin.getMessages();

        for (String key : new String[]{"general", "header", "footer", "bar-character", "sections",
                "bar-colors", "player-list.format", "sort.rank-weights", "placeholders", "sidebar", "ranks"}) {
            if (!cfg.contains(key)) {
                msg.warn("warnings.missing-key", "key", key);
            }
        }

        season = cfg.getString("general.season", "Season 1");
        refreshTicks = cfg.getInt("general.refresh-ticks", 10);
        if (refreshTicks < 1) {
            msg.warn("warnings.bad-refresh");
            refreshTicks = 10;
        }
        asyncUpdates = cfg.getBoolean("general.async-updates", true);
        fallbackValue = cfg.getString("general.fallback-value", "N/A");

        String modeRaw = cfg.getString("general.sort-mode", "RANK");
        try {
            sortMode = SortMode.valueOf(modeRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            msg.warn("warnings.unknown-sort-mode", "mode", modeRaw);
            sortMode = SortMode.RANK;
        }

        header = cfg.getStringList("header");
        footer = cfg.getStringList("footer");
        for (String line : header) {
            validateMiniMessage(line, "header");
        }
        for (String line : footer) {
            validateMiniMessage(line, "footer");
        }

        barCharacter = cfg.getString("bar-character", "▎");

        barColors = new LinkedHashMap<>();
        ConfigurationSection colorSec = cfg.getConfigurationSection("bar-colors");
        if (colorSec != null) {
            for (String key : colorSec.getKeys(false)) {
                barColors.put(key.toLowerCase(Locale.ROOT), colorSec.getString(key, "<white>"));
            }
        }
        if (barColors.isEmpty()) {
            barColors.put("red", "<red>");
            barColors.put("yellow", "<yellow>");
            barColors.put("green", "<green>");
            barColors.put("cyan", "<aqua>");
            barColors.put("magenta", "<light_purple>");
            barColors.put("white", "<white>");
        }

        playerListFormat = cfg.getString("player-list.format", "%rank_badge% %rank_name_color%%player_name%");
        validateMiniMessage(playerListFormat, "player-list.format");

        rankWeights = new LinkedHashMap<>();
        ConfigurationSection weightSec = cfg.getConfigurationSection("sort.rank-weights");
        if (weightSec != null) {
            for (String key : weightSec.getKeys(false)) {
                rankWeights.put(key.toLowerCase(Locale.ROOT), weightSec.getInt(key, 0));
            }
        }

        ranks = new LinkedHashMap<>();
        ConfigurationSection rankSec = cfg.getConfigurationSection("ranks");
        if (rankSec != null) {
            for (String key : rankSec.getKeys(false)) {
                ConfigurationSection r = rankSec.getConfigurationSection(key);
                if (r == null) {
                    continue;
                }
                Rank rank = new Rank(r.getString("badge", ""), r.getString("name-color", "<white>"));
                validateMiniMessage(rank.badge(), "ranks." + key + ".badge");
                ranks.put(key.toLowerCase(Locale.ROOT), rank);
            }
        }

        placeholderDefaults = new LinkedHashMap<>();
        ConfigurationSection phSec = cfg.getConfigurationSection("placeholders");
        if (phSec != null) {
            for (String key : phSec.getKeys(false)) {
                placeholderDefaults.put(key.toLowerCase(Locale.ROOT), phSec.getString(key, ""));
            }
        }

        sidebarEnabled = cfg.getBoolean("sidebar.enabled", true);
        sidebarTitle = cfg.getString("sidebar.title",
                "<gradient:#FF5500:#FFAA00><bold>RETROGENS</bold></gradient> <dark_gray>| <white>%season%");
        sidebarFooter = cfg.getStringList("sidebar.footer");
        hideNumbers = cfg.getBoolean("sidebar.hide-numbers", true);
        validateMiniMessage(sidebarTitle, "sidebar.title");
        for (String line : sidebarFooter) {
            validateMiniMessage(line, "sidebar.footer");
        }

        eventsEnabled = cfg.getBoolean("events.enabled", true);
        eventFallbackName = cfg.getString("events.fallback-name", "");
        eventFallbackRemaining = cfg.getString("events.fallback-remaining", "0s");
        debug = cfg.getBoolean("debug.enabled", false);

        loadSections(cfg, msg);
    }

    private void loadSections(FileConfiguration cfg, Messages msg) {
        List<Section> result = new ArrayList<>();
        ConfigurationSection root = cfg.getConfigurationSection("sections");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(id);
                if (sec == null) {
                    msg.warn("warnings.section-no-lines", "section", id);
                    continue;
                }
                List<TabLine> lines = parseLines(sec, "lines", id, msg);
                if (lines.isEmpty()) {
                    msg.warn("warnings.section-no-lines", "section", id);
                    continue;
                }
                List<TabLine> emptyLines = parseLines(sec, "empty-lines", id, msg);
                result.add(new Section(id, sec.getBoolean("enabled", true),
                        sec.getBoolean("hide-when-empty", false), lines, emptyLines));
            }
        }
        sections = Collections.unmodifiableList(result);
    }

    private List<TabLine> parseLines(ConfigurationSection sec, String key, String id, Messages msg) {
        List<TabLine> lines = new ArrayList<>();
        int index = 0;
        for (Map<?, ?> raw : sec.getMapList(key)) {
            index++;
            Object text = raw.get("text");
            if (text == null) {
                msg.warn("warnings.line-invalid", "section", id, "line", String.valueOf(index));
                continue;
            }
            Object colorObj = raw.get("bar-color");
            String color = colorObj == null ? "white" : colorObj.toString().toLowerCase(Locale.ROOT);
            if (!barColors.containsKey(color)) {
                msg.warn("warnings.unknown-bar-color", "color", color, "section", id,
                        "line", String.valueOf(index));
            }
            validateMiniMessage(text.toString(), "sections." + id + "." + key + " line " + index);
            lines.add(new TabLine(color, text.toString()));
        }
        return lines;
    }

    /** Warns if a MiniMessage string leaves unparsed tags behind (unknown or malformed tags). */
    private void validateMiniMessage(String text, String where) {
        try {
            String plain = PlainTextComponentSerializer.plainText().serialize(MiniMessage.miniMessage().deserialize(text));
            if (LEFTOVER_TAG.matcher(plain).find()) {
                plugin.getMessages().warn("warnings.bad-minimessage", "where", where, "text", text);
            }
        } catch (Exception e) {
            plugin.getMessages().warn("warnings.bad-minimessage", "where", where, "text", text);
        }
    }

    /** Returns the season label. */
    public String getSeason() { return season; }

    /** Returns the refresh interval in ticks. */
    public int getRefreshTicks() { return refreshTicks; }

    /** Returns whether updates run on an async timer. */
    public boolean isAsyncUpdates() { return asyncUpdates; }

    /** Returns the value shown when a placeholder has nothing to render. */
    public String getFallbackValue() { return fallbackValue; }

    /** Returns the configured sort mode. */
    public SortMode getSortMode() { return sortMode; }

    /** Returns the tablist header MiniMessage lines. */
    public List<String> getHeader() { return header; }

    /** Returns the tablist footer MiniMessage lines. */
    public List<String> getFooter() { return footer; }

    /** Returns the bar character placed before each sidebar section line. */
    public String getBarCharacter() { return barCharacter; }

    /** Returns the ordered sidebar sections. */
    public List<Section> getSections() { return sections; }

    /** Returns the named bar color to MiniMessage tag map. */
    public Map<String, String> getBarColors() { return barColors; }

    /** Returns the player-list entry format. */
    public String getPlayerListFormat() { return playerListFormat; }

    /** Returns the sort weight for a group name, or the "default" weight if unknown. */
    public int weightFor(String group) {
        String key = group == null ? "default" : group.toLowerCase(Locale.ROOT);
        return rankWeights.getOrDefault(key, rankWeights.getOrDefault("default", 0));
    }

    /** Returns the badge/name-color for a group, falling back to the "default" rank, then to empty. */
    public Rank getRank(String group) {
        String key = group == null ? "default" : group.toLowerCase(Locale.ROOT);
        Rank rank = ranks.get(key);
        if (rank == null) {
            rank = ranks.getOrDefault("default", EMPTY_RANK);
        }
        return rank;
    }

    /** Returns a configured placeholder default (for example "balance-default"), or the given fallback. */
    public String getDefault(String key, String fallback) {
        return placeholderDefaults.getOrDefault(key, fallback);
    }

    /** Returns whether the sidebar scoreboard is enabled. */
    public boolean isSidebarEnabled() { return sidebarEnabled; }

    /** Returns the sidebar title MiniMessage string. */
    public String getSidebarTitle() { return sidebarTitle; }

    /** Returns the lines placed at the bottom of the sidebar. */
    public List<String> getSidebarFooter() { return sidebarFooter; }

    /** Returns whether the red score numbers are hidden on the sidebar (needs Paper support). */
    public boolean isHideNumbers() { return hideNumbers; }

    /** Returns whether the event feature is enabled. */
    public boolean isEventsEnabled() { return eventsEnabled; }

    /** Returns the fallback event name (empty means none). */
    public String getEventFallbackName() { return eventFallbackName; }

    /** Returns the fallback event remaining-time text. */
    public String getEventFallbackRemaining() { return eventFallbackRemaining; }

    /** Returns whether /retrotab debug is allowed. */
    public boolean isDebug() { return debug; }
}
