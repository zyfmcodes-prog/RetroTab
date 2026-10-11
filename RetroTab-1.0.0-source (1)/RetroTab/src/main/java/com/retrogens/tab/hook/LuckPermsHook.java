package com.retrogens.tab.hook;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

import java.util.OptionalInt;

/**
 * Soft hook for LuckPerms: prefix, suffix, primary group and group weight.
 * Only instantiate when LuckPerms is installed; all lookups use LuckPerms' cached data
 * and are safe to call off the main thread.
 */
public final class LuckPermsHook {

    private final LuckPerms api;

    /** Connects to the LuckPerms API; throws if LuckPerms is not loaded. */
    public LuckPermsHook() {
        this.api = LuckPermsProvider.get();
    }

    private User user(Player player) {
        return api.getPlayerAdapter(Player.class).getUser(player);
    }

    /** Returns the player's prefix as a MiniMessage string ("" if none). */
    public String getPrefix(Player player) {
        String prefix = user(player).getCachedData().getMetaData().getPrefix();
        return toMiniMessage(prefix);
    }

    /** Returns the player's suffix as a MiniMessage string ("" if none). */
    public String getSuffix(Player player) {
        String suffix = user(player).getCachedData().getMetaData().getSuffix();
        return toMiniMessage(suffix);
    }

    /** Returns the player's primary group name. */
    public String getPrimaryGroup(Player player) {
        String group = user(player).getPrimaryGroup();
        return group == null ? "default" : group;
    }

    /** Returns the weight of the player's primary group, if the group defines one. */
    public OptionalInt getPrimaryGroupWeight(Player player) {
        Group group = api.getGroupManager().getGroup(getPrimaryGroup(player));
        return group == null ? OptionalInt.empty() : group.getWeight();
    }

    /** Converts legacy ampersand/section codes to MiniMessage; plain MiniMessage passes through. */
    private static String toMiniMessage(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.indexOf('&') >= 0 || value.indexOf('§') >= 0) {
            String normalized = value.replace('§', '&');
            return MiniMessage.miniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(normalized));
        }
        return value;
    }
}
