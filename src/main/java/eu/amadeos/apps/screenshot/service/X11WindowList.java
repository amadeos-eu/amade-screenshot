package eu.amadeos.apps.screenshot.service;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Asks the X server for the children of the root window. Under a reparenting window manager such as the
 * amadeOS one these are the frames, so a window is taken with its title bar and border. Menus and tooltips
 * (override-redirect windows) are windows of their own and can be picked as well.
 */
public final class X11WindowList implements WindowList {
    private static final Logger LOG = LoggerFactory.getLogger(X11WindowList.class);
    private static final int IS_VIEWABLE = 2;

    /** The few libX11 calls needed here. */
    interface X11 extends Library {
        Pointer XOpenDisplay(String name);

        int XCloseDisplay(Pointer display);

        NativeLong XDefaultRootWindow(Pointer display);

        int XQueryTree(Pointer display, NativeLong window, NativeLongByReference root, NativeLongByReference parent,
                       PointerByReference children, IntByReference count);

        int XGetWindowAttributes(Pointer display, NativeLong window, XWindowAttributes attributes);

        int XFree(Pointer data);

        int XSync(Pointer display, int discard);

        ErrorHandler XSetErrorHandler(ErrorHandler handler);
    }

    /**
     * A window can close between listing and asking for its size. The default handler would end the
     * program for that error; this one only ignores it.
     */
    interface ErrorHandler extends Callback {
        int callback(Pointer display, Pointer event);
    }

    @Structure.FieldOrder({"x", "y", "width", "height", "border_width", "depth", "visual", "root", "c_class",
            "bit_gravity", "win_gravity", "backing_store", "backing_planes", "backing_pixel", "save_under",
            "colormap", "map_installed", "map_state", "all_event_masks", "your_event_mask",
            "do_not_propagate_mask", "override_redirect", "screen"})
    public static class XWindowAttributes extends Structure {
        public int x, y, width, height, border_width, depth;
        public Pointer visual;
        public NativeLong root;
        public int c_class, bit_gravity, win_gravity, backing_store;
        public NativeLong backing_planes, backing_pixel;
        public int save_under;
        public NativeLong colormap;
        public int map_installed, map_state;
        public NativeLong all_event_masks, your_event_mask, do_not_propagate_mask;
        public int override_redirect;
        public Pointer screen;
    }

    private static final ErrorHandler IGNORE = (display, event) -> 0;

    private X11 x11;

    @Override
    public synchronized List<Rectangle> topFirst() {
        try {
            if (x11 == null) {
                x11 = Native.load("X11", X11.class);
                x11.XSetErrorHandler(IGNORE);
            }
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            LOG.warn("libX11 is not available, so no window can be picked", e);
            return List.of();
        }
        Pointer display = x11.XOpenDisplay(null);
        if (display == null) {
            LOG.warn("Cannot open the X display, so no window can be picked");
            return List.of();
        }
        try {
            return windows(display);
        } finally {
            x11.XCloseDisplay(display);
        }
    }

    private List<Rectangle> windows(Pointer display) {
        NativeLongByReference root = new NativeLongByReference();
        NativeLongByReference parent = new NativeLongByReference();
        PointerByReference children = new PointerByReference();
        IntByReference count = new IntByReference();
        if (x11.XQueryTree(display, x11.XDefaultRootWindow(display), root, parent, children, count) == 0) {
            return List.of();
        }
        Pointer array = children.getValue();
        if (array == null) return List.of();
        List<Rectangle> bottomFirst = new ArrayList<>();
        try {
            for (int i = 0; i < count.getValue(); i++) {
                NativeLong window = array.getNativeLong((long) i * Native.LONG_SIZE);
                XWindowAttributes a = new XWindowAttributes();
                if (x11.XGetWindowAttributes(display, window, a) == 0) continue;
                if (a.map_state != IS_VIEWABLE || a.width <= 1 || a.height <= 1) continue;
                bottomFirst.add(new Rectangle(a.x, a.y, a.width + 2 * a.border_width, a.height + 2 * a.border_width));
            }
        } finally {
            x11.XFree(array);
        }
        x11.XSync(display, 0);
        Collections.reverse(bottomFirst);
        return bottomFirst;
    }
}
