package com.retrogens.tab.command;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.api.RetroTabAPI;
import com.retrogens.tab.config.Messages;
import com.retrogens.tab.placeholder.BuiltInPlaceholders;
import com.retrogens.tab.scoreboard.SidebarRenderer;
import com.retrogens.tab.tablist.TablistRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Handles /retrotab: reload, toggle (tablist and sidebar), event, event clear and debug. All require retrotab.admin.
 */
public final class RetroTabCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("reload", "toggle", "event", "debug");

    private final RetroTab plugin;

    /** Creates the command handler. */
    public RetroTabCommand(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** Executes a /retrotab subcommand. */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages m = plugin.getMessages();
        if (!sender.hasPermission("retrotab.admin")) {
            m.send(sender, "command.no-permission");
            return true;
        }
        if (args.length == 0) {
            usage(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                try {
                    plugin.reloadAll();
                    m.send(sender, "command.reload-success");
                } catch (Exception e) {
                    m.send(sender, "command.reload-failed", "error", String.valueOf(e.getMessage()));
                }
                return true;
            case "toggle":
                return toggle(sender, args);
            case "event":
                return event(sender, args);
            case "debug":
                return debug(sender, args);
            default:
                usage(sender);
                return true;
        }
    }

    private void usage(CommandSender sender) {
        Messages m = plugin.getMessages();
        m.send(sender, "command.usage-header");
        m.send(sender, "command.usage-reload");
        m.send(sender, "command.usage-toggle");
        m.send(sender, "command.usage-event");
        m.send(sender, "command.usage-event-clear");
        m.send(sender, "command.usage-debug");
    }

    private boolean toggle(CommandSender sender, String[] args) {
        Messages m = plugin.getMessages();
        if (args.length < 2) {
            m.send(sender, "command.usage-toggle");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            m.send(sender, "command.player-not-found", "player", args[1]);
            return true;
        }
        boolean enabled = plugin.getManager().toggle(target);
        m.send(sender, enabled ? "command.toggle-enabled" : "command.toggle-disabled", "player", target.getName());
        return true;
    }

    private boolean event(CommandSender sender, String[] args) {
        Messages m = plugin.getMessages();
        if (args.length == 2 && args[1].equalsIgnoreCase("clear")) {
            RetroTabAPI.clearEvent();
            m.send(sender, "command.event-cleared");
            return true;
        }
        if (args.length < 3) {
            m.send(sender, "command.usage-event");
            m.send(sender, "command.usage-event-clear");
            return true;
        }
        String rawSeconds = args[args.length - 1];
        long seconds;
        try {
            seconds = Long.parseLong(rawSeconds);
        } catch (NumberFormatException e) {
            seconds = -1;
        }
        if (seconds <= 0) {
            m.send(sender, "command.event-invalid-seconds", "value", rawSeconds);
            return true;
        }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
        RetroTabAPI.setEvent(name, seconds * 1000L);
        m.send(sender, "command.event-started", "name", name,
                "time", BuiltInPlaceholders.formatDuration(seconds * 1000L));
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        Messages m = plugin.getMessages();
        if (!plugin.getTabConfig().isDebug()) {
            m.send(sender, "command.debug-disabled");
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                m.send(sender, "command.player-not-found", "player", args[1]);
                return true;
            }
        } else if (sender instanceof Player self) {
            target = self;
        } else {
            m.send(sender, "command.debug-console-needs-player");
            return true;
        }

        TablistRenderer renderer = plugin.getRenderer();
        TablistRenderer.RenderedTab tab = renderer.render(target);
        m.log("debug.header", "player", target.getName());
        for (String line : renderer.toPlainLines(tab)) {
            plugin.getLogger().info(line);
        }
        if (plugin.getTabConfig().isSidebarEnabled()) {
            m.log("debug.sidebar-divider");
            SidebarRenderer.RenderedSidebar sidebar = plugin.getSidebarRenderer().render(target);
            PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
            plugin.getLogger().info(plain.serialize(sidebar.title()));
            for (Component line : sidebar.lines()) {
                plugin.getLogger().info(plain.serialize(line));
            }
        }
        m.log("debug.end");
        m.send(sender, "command.debug-sent", "player", target.getName());
        return true;
    }

    /** Provides tab completion for subcommands and player names. */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("retrotab.admin")) {
            return out;
        }
        if (args.length == 1) {
            for (String s : SUBCOMMANDS) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("toggle") || sub.equals("debug")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                        out.add(p.getName());
                    }
                }
            } else if (sub.equals("event") && "clear".startsWith(args[1].toLowerCase(Locale.ROOT))) {
                out.add("clear");
            }
        }
        return out;
    }
}
