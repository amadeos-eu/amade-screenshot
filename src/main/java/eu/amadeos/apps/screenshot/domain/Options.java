package eu.amadeos.apps.screenshot.domain;

import java.util.List;
import java.util.Objects;

/**
 * How the next screenshot is taken.
 *
 * @param mode       desktop, window or area
 * @param delay      seconds to wait before the shot, e.g. to open a menu first
 * @param hideWindow whether the program's own window gets out of the way
 */
public record Options(CaptureMode mode, int delay, boolean hideWindow) {
    /** The delays offered in the window; the command line takes any number up to {@link #MAX_DELAY}. */
    public static final List<Integer> DELAYS = List.of(0, 3, 5, 10);
    public static final int MAX_DELAY = 60;
    public static final Options DEFAULT = new Options(CaptureMode.DESKTOP, 0, true);

    public Options {
        Objects.requireNonNull(mode, "mode");
        if (delay < 0 || delay > MAX_DELAY) throw new IllegalArgumentException("delay " + delay);
    }

    public Options withMode(CaptureMode newMode) {
        return new Options(newMode, delay, hideWindow);
    }

    public Options withDelay(int newDelay) {
        return new Options(mode, newDelay, hideWindow);
    }

    public Options withHideWindow(boolean hide) {
        return new Options(mode, delay, hide);
    }
}
