package com.retrogens.tab.tablist;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.config.TabConfig;
import com.retrogens.tab.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds the tablist header, footer and per-player list name (rank badge plus name).
 * The sidebar is rendered separately by the SidebarRenderer. Safe to call off the main thread.
 */
public final class TablistRenderer {

    /** The rendered tablist for one player. */
    public record RenderedTab(Component header, Component footer, Component listName) { }

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final RetroTab plugin;

    /** Creates the renderer. */
    public TablistRenderer(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** Renders the header, footer and list name for a player. */
    public RenderedTab render(Player player) {
        TabConfig cfg = plugin.getTabConfig();
        PlaceholderResolver resolver = plugin.getResolver();

        List<Component> header = new ArrayList<>();
        for (String line : cfg.getHeader()) {
            header.add(MM.deserialize(resolver.resolve(player, line)));
        }
        List<Component> footer = new ArrayList<>();
        for (String line : cfg.getFooter()) {
            footer.add(MM.deserialize(resolver.resolve(player, line)));
        }

        String listRaw = resolver.resolve(player, cfg.getPlayerListFormat()).replaceAll(" {2,}", " ").strip();
        return new RenderedTab(
                Component.join(JoinConfiguration.newlines(), header),
                Component.join(JoinConfiguration.newlines(), footer),
                MM.deserialize(listRaw));
    }

    /** Returns header, list name and footer as plain-text lines (used by /retrotab debug). */
    public List<String> toPlainLines(RenderedTab tab) {
        PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();
        List<String> lines = new ArrayList<>(Arrays.asList(plain.serialize(tab.header()).split("\n", -1)));
        lines.add(plugin.getMessages().plain("debug.list-divider") + " " + plain.serialize(tab.listName()));
        lines.add(plugin.getMessages().plain("debug.footer-divider"));
        lines.addAll(Arrays.asList(plain.serialize(tab.footer()).split("\n", -1)));
        return lines;
    }
}
