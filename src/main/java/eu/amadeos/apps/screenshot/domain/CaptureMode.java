package eu.amadeos.apps.screenshot.domain;

import java.util.Locale;
import java.util.Optional;

/** What a screenshot shows. */
public enum CaptureMode {
    /** Every screen. */
    DESKTOP,
    /** One window with its frame, picked with a click. */
    WINDOW,
    /** A rectangle drawn with the mouse. */
    AREA;

    /** The lower-case name, as in the settings file and on the command line. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<CaptureMode> forKey(String key) {
        for (CaptureMode mode : values()) {
            if (mode.key().equals(key)) return Optional.of(mode);
        }
        return Optional.empty();
    }

    /** Whether the picture is cut from the desktop after the shot. */
    public boolean needsSelection() {
        return this != DESKTOP;
    }
}
