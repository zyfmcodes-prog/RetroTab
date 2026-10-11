package com.retrogens.tab.scoreboard;

import com.retrogens.tab.RetroTab;
import com.retrogens.tab.config.Section;
import com.retrogens.tab.config.TabConfig;
import com.retrogens.tab.config.TabLine;
import com.retrogens.tab.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the sidebar title and lines for a player. Layout: a blank line before each visible
 * section, then a blank line and the sidebar footer. Each section line is "[colored bar] [text]".
 * Safe to call off the main thread.
 */
public final class SidebarRenderer {

    /** The rendered sidebar for one player (title plus lines, top line first). */
    public record RenderedSidebar(Component title, List<Component> lines) { }

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final RetroTab plugin;

    /** Creates the sidebar renderer. */
    public SidebarRenderer(RetroTab plugin) {
        this.plugin = plugin;
    }

    /** Renders the complete sidebar for a player. */
    public RenderedSidebar render(Player player) {
        TabConfig cfg = plugin.getTabConfig();
        PlaceholderResolver resolver = plugin.getResolver();

        Component title = MM.deserialize(resolver.resolve(player, cfg.getSidebarTitle()));
        List<Component> lines = new ArrayList<>();
        boolean eventActive = resolver.isEventActive();

        for (Section section : cfg.getSections()) {
            if (!section.isEnabled()) {
                continue;
            }
            List<TabLine> use = section.getLines();
            if (section.dependsOnEvent() && !eventActive) {
                if (!section.getEmptyLines().isEmpty()) {
                    use = section.getEmptyLines();
                } else if (section.isHideWhenEmpty()) {
                    continue;
                }
            }
            lines.add(Component.empty());
            for (TabLine line : use) {
                lines.add(renderLine(player, line));
            }
        }

        if (!cfg.getSidebarFooter().isEmpty()) {
            lines.add(Component.empty());
            for (String footer : cfg.getSidebarFooter()) {
                lines.add(MM.deserialize(resolver.resolve(player, footer)));
            }
        }
        return new RenderedSidebar(title, lines);
    }

    private Component renderLine(Player player, TabLine line) {
        TabConfig cfg = plugin.getTabConfig();
        String barTag = cfg.getBarColors().getOrDefault(line.getBarColor(), "<white>");
        TabLine resolved = line.withResolved(plugin.getResolver().resolve(player, line.getText()));
        return MM.deserialize(barTag + cfg.getBarCharacter() + "<reset> " + resolved.getResolved());
    }
}
