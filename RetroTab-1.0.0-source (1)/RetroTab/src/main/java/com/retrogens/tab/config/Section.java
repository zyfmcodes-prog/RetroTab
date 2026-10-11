package com.retrogens.tab.config;

import java.util.Collections;
import java.util.List;

/**
 * A configured sidebar section (for example EVENT or STATISTICS) made of ordered lines.
 * A section may also define "empty lines" shown instead when it follows event state and no
 * event is active (for example the "NO EVENT" display).
 */
public final class Section {

    private final String id;
    private final boolean enabled;
    private final boolean hideWhenEmpty;
    private final List<TabLine> lines;
    private final List<TabLine> emptyLines;
    private final boolean dependsOnEvent;

    /** Creates a section; detects whether any line references event placeholders. */
    public Section(String id, boolean enabled, boolean hideWhenEmpty, List<TabLine> lines, List<TabLine> emptyLines) {
        this.id = id;
        this.enabled = enabled;
        this.hideWhenEmpty = hideWhenEmpty;
        this.lines = Collections.unmodifiableList(lines);
        this.emptyLines = Collections.unmodifiableList(emptyLines);
        boolean event = false;
        for (TabLine line : lines) {
            String t = line.getText().toLowerCase();
            if (t.contains("%event_") || t.contains("%retrotab_event_")) {
                event = true;
                break;
            }
        }
        this.dependsOnEvent = event;
    }

    /** Returns the section key from config.yml. */
    public String getId() {
        return id;
    }

    /** Returns whether the section is enabled. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Returns whether the section hides itself when no event is active and it has no empty-lines. */
    public boolean isHideWhenEmpty() {
        return hideWhenEmpty;
    }

    /** Returns the ordered, unmodifiable list of lines. */
    public List<TabLine> getLines() {
        return lines;
    }

    /** Returns the lines shown when the section depends on an event and none is active. */
    public List<TabLine> getEmptyLines() {
        return emptyLines;
    }

    /** Returns true if any line uses event placeholders, so the section follows event state. */
    public boolean dependsOnEvent() {
        return dependsOnEvent;
    }
}
