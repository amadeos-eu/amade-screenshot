package eu.amadeos.apps.screenshot.service;

import java.awt.Rectangle;
import java.util.List;

/** The windows on the desktop, for picking one. */
public interface WindowList {
    /** No windows: picking a window then picks nothing. */
    WindowList NONE = List::of;

    /** The bounds of the visible top-level windows with their frames, the top of the stack first. */
    List<Rectangle> topFirst();
}
