package com.retrogens.tab.config;

import com.retrogens.tab.RetroTab;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Loads messages.yml and renders every user-facing string (chat and console) from it.
 * Placeholders use the {key} syntax; values are escaped so they cannot inject MiniMessage tags.
 */
public final class Messages {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final RetroTab plugin;
    private YamlConfiguration yaml;

    /** Creates the manager and loads messages.yml (copying the default if absent). */
    public Messages(RetroTab plugin) {
        this.plugin = plugin;
        reload();
    }

    /** Reloads messages.yml from disk. */
    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(file);
    }

    /** Returns the raw MiniMessage string for a path with {key} pairs substituted. */
    public String raw(String path, String... pairs) {
        String s = yaml.getString(path, "<red>Missing message: " + path);
        s = s.replace("{prefix}", yaml.getString("prefix", ""));
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            s = s.replace("{" + pairs[i] + "}", MM.escapeTags(pairs[i + 1]));
        }
        return s;
    }

    /** Returns the message as an Adventure component. */
    public Component component(String path, String... pairs) {
        return MM.deserialize(raw(path, pairs));
    }

    /** Returns the message as plain text with no formatting. */
    public String plain(String path, String... pairs) {
        return PlainTextComponentSerializer.plainText().serialize(component(path, pairs));
    }

    /** Sends the message to a command sender. */
    public void send(CommandSender sender, String path, String... pairs) {
        sender.sendMessage(component(path, pairs));
    }

    /** Logs the message to console at INFO level. */
    public void log(String path, String... pairs) {
        plugin.getLogger().info(plain(path, pairs));
    }

    /** Logs the message to console at WARNING level. */
    public void warn(String path, String... pairs) {
        plugin.getLogger().warning(plain(path, pairs));
    }
}
