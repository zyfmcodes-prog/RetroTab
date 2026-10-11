package com.retrogens.tab.config;

/**
 * One line of a section: a named bar color, the raw MiniMessage text, and (after rendering)
 * the text with placeholders resolved. Instances are immutable.
 */
public final class TabLine {

    private final String barColor;
    private final String text;
    private final String resolved;

    /** Creates an unresolved line. */
    public TabLine(String barColor, String text) {
        this(barColor, text, null);
    }

    private TabLine(String barColor, String text, String resolved) {
        this.barColor = barColor;
        this.text = text;
        this.resolved = resolved;
    }

    /** Returns the configured bar color name (a key of bar-colors). */
    public String getBarColor() {
        return barColor;
    }

    /** Returns the raw MiniMessage text with unresolved placeholders. */
    public String getText() {
        return text;
    }

    /** Returns the text with placeholders resolved, or null if not yet resolved. */
    public String getResolved() {
        return resolved;
    }

    /** Returns a copy of this line carrying the given resolved output. */
    public TabLine withResolved(String resolved) {
        return new TabLine(barColor, text, resolved);
    }
}
