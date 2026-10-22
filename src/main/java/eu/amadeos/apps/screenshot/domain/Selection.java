package eu.amadeos.apps.screenshot.domain;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

/** The geometry of picking an area or a window on the frozen desktop. */
public final class Selection {
    /** An area smaller than this in either direction counts as a click, not a frame. */
    public static final int MIN_SIZE = 2;

    private Selection() {
    }

    /** The rectangle between two corners, whichever way the mouse was dragged; both corners are inside. */
    public static Rectangle between(Point start, Point end) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        return new Rectangle(x, y, Math.abs(end.x - start.x) + 1, Math.abs(end.y - start.y) + 1);
    }

    public static boolean isLargeEnough(Rectangle area) {
        return area.width >= MIN_SIZE && area.height >= MIN_SIZE;
    }

    /**
     * The window under the point.
     *
     * @param topFirst the windows' bounds, the top of the stack first
     */
    public static Optional<Rectangle> windowAt(List<Rectangle> topFirst, Point point) {
        for (Rectangle window : topFirst) {
            if (window.contains(point)) return Optional.of(window);
        }
        return Optional.empty();
    }

    /** The part of the area on the desktop; empty if none. */
    public static Optional<Rectangle> clip(Rectangle area, Rectangle desktop) {
        Rectangle visible = area.intersection(desktop);
        return visible.isEmpty() ? Optional.empty() : Optional.of(visible);
    }
}
