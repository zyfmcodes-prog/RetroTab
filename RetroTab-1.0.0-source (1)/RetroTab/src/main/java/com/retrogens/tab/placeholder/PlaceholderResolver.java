package com.retrogens.tab.placeholder;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.api.RetroTabAPI;
import com.retrogens.tab.config.TabConfig;
import com.retrogens.tab.hook.LuckPermsHook;
import com.retrogens.tab.hook.PapiHook;
import com.retrogens.tab.hook.VaultHook;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unifies built-in placeholders, API-pushed values, LuckPerms prefix/suffix and PlaceholderAPI
 * passthrough. Resolution order: built-ins, retrotab values, luckperms_prefix/suffix, PAPI.
 * Unknown placeholders are left visible so operators can debug them.
 */
public final class PlaceholderResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("%([A-Za-z0-9_]+)%");

    private final RetroTab plugin;
    private final BuiltInPlaceholders builtIns;

    /** Creates the resolver. */
    public PlaceholderResolver(RetroTab plugin) {
        this.plugin = plugin;
        this.builtIns = new BuiltInPlaceholders(plugin);
    }

    /** Replaces every %placeholder% in the text for the given player. */
    public String resolve(Player player, String text) {
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String value = resolveKey(player, m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(sb);
        String out = sb.toString();

        PapiHook papi = plugin.getPapiHook();
        if (papi != null && out.indexOf('%') >= 0) {
            out = papi.apply(player, out);
        }
        return out;
    }

    /** Resolves a single placeholder key (without percent signs), or null if unknown here. */
    public String resolveKey(Player player, String key) {
        String k = key.toLowerCase(Locale.ROOT);

        String builtIn = builtIns.resolve(player, k);
        if (builtIn != null) {
            return builtIn;
        }
        if (k.equals("luckperms_prefix")) {
            LuckPermsHook lp = plugin.getLuckPermsHook();
            return lp == null ? "" : lp.getPrefix(player);
        }
        if (k.equals("luckperms_suffix")) {
            LuckPermsHook lp = plugin.getLuckPermsHook();
            return lp == null ? "" : lp.getSuffix(player);
        }
        if (k.equals("rank_group") || k.equals("rank_badge") || k.equals("rank_name_color")) {
            LuckPermsHook lp = plugin.getLuckPermsHook();
            String group = lp == null ? "default" : lp.getPrimaryGroup(player);
            if (k.equals("rank_group")) {
                return group;
            }
            TabConfig.Rank rank = plugin.getTabConfig().getRank(group);
            return k.equals("rank_badge") ? rank.badge() : rank.nameColor();
        }
        String stripped = k.startsWith("retrotab_") ? k.substring("retrotab_".length()) : k;
        return resolveRetrotab(player, stripped);
    }

    /** Resolves a retrotab value (balance, money_per_hour, gens_current, ...) or null if unknown. */
    public String resolveRetrotab(OfflinePlayer player, String key) {
        TabConfig cfg = plugin.getTabConfig();
        UUID id = player.getUniqueId();
        String value;
        switch (key) {
            case "balance": {
                Double v = RetroTabAPI.getBalance(id);
                if (v == null) {
                    VaultHook vault = plugin.getVaultHook();
                    v = vault == null ? null : vault.getBalance(player);
                }
                value = v == null ? cfg.getDefault("balance-default", "0")
                        : BuiltInPlaceholders.formatCompact(v);
                break;
            }
            case "money_per_hour": {
                Double v = RetroTabAPI.getMoneyPerHour(id);
                value = v == null ? cfg.getDefault("money-per-hour-default", "0")
                        : BuiltInPlaceholders.formatCompact(v);
                break;
            }
            case "gens_current": {
                Integer v = RetroTabAPI.getGensCurrent(id);
                value = v == null ? cfg.getDefault("gens-current-default", "0") : String.valueOf(v);
                break;
            }
            case "gens_max": {
                Integer v = RetroTabAPI.getGensMax(id);
                value = v == null ? cfg.getDefault("gens-max-default", "25") : String.valueOf(v);
                break;
            }
            case "level": {
                Integer v = RetroTabAPI.getLevel(id);
                value = v == null ? cfg.getDefault("level-default", "1") : String.valueOf(v);
                break;
            }
            case "event_name":
                value = eventName();
                break;
            case "event_remaining":
                value = eventRemaining();
                break;
            case "season":
                value = cfg.getSeason();
                break;
            default:
                return null;
        }
        return value == null || value.isBlank() ? cfg.getFallbackValue() : value;
    }

    /** Returns true if an event is active (API-pushed or config fallback) and events are enabled. */
    public boolean isEventActive() {
        TabConfig cfg = plugin.getTabConfig();
        if (!cfg.isEventsEnabled()) {
            return false;
        }
        if (RetroTabAPI.getEventName() != null) {
            return true;
        }
        return !cfg.getEventFallbackName().isBlank();
    }

    private String eventName() {
        TabConfig cfg = plugin.getTabConfig();
        if (cfg.isEventsEnabled()) {
            String api = RetroTabAPI.getEventName();
            if (api != null) {
                return api;
            }
            if (!cfg.getEventFallbackName().isBlank()) {
                return cfg.getEventFallbackName();
            }
        }
        return cfg.getDefault("event-name-default", "None");
    }

    private String eventRemaining() {
        TabConfig cfg = plugin.getTabConfig();
        if (cfg.isEventsEnabled()) {
            if (RetroTabAPI.getEventName() != null) {
                return BuiltInPlaceholders.formatDuration(RetroTabAPI.getEventRemainingMillis());
            }
            if (!cfg.getEventFallbackName().isBlank()) {
                return cfg.getEventFallbackRemaining();
            }
        }
        return cfg.getDefault("event-remaining-default", "0s");
    }
}
