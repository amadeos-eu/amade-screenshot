package eu.amadeos.apps.screenshot.service;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The freedesktop.org clipboard manager convention: a program that is about to close asks the owner of
 * {@code CLIPBOARD_MANAGER} to convert it to {@code SAVE_TARGETS}. The manager (amadeOS's
 * {@code amadeos-session clipboard}, or the one of another desktop) then reads the clipboard from this
 * program, takes it over and answers. AWT notices that it lost the clipboard, and the program can end.
 */
public final class X11ClipboardHandoff implements ClipboardHandoff {
    private static final Logger LOG = LoggerFactory.getLogger(X11ClipboardHandoff.class);
    private static final long TIMEOUT_MS = 5000;
    private static final int SELECTION_NOTIFY = 31;

    /** The few libX11 calls needed here. */
    interface X11 extends Library {
        Pointer XOpenDisplay(String name);

        int XCloseDisplay(Pointer display);

        NativeLong XDefaultRootWindow(Pointer display);

        NativeLong XInternAtom(Pointer display, String name, int onlyIfExists);

        NativeLong XGetSelectionOwner(Pointer display, NativeLong selection);

        NativeLong XCreateSimpleWindow(Pointer display, NativeLong parent, int x, int y, int width, int height,
                                       int borderWidth, NativeLong border, NativeLong background);

        int XDestroyWindow(Pointer display, NativeLong window);

        int XConvertSelection(Pointer display, NativeLong selection, NativeLong target, NativeLong property,
                              NativeLong requestor, NativeLong time);

        int XFlush(Pointer display);

        int XPending(Pointer display);

        int XNextEvent(Pointer display, XSelectionEvent event);
    }

    /** XEvent, read as an XSelectionEvent; padded to the size of the union. */
    @Structure.FieldOrder({"type", "serial", "send_event", "display", "requestor", "selection", "target",
            "property", "time", "pad"})
    public static class XSelectionEvent extends Structure {
        public int type;
        public NativeLong serial;
        public int send_event;
        public Pointer display;
        public NativeLong requestor;
        public NativeLong selection;
        public NativeLong target;
        public NativeLong property;
        public NativeLong time;
        public NativeLong[] pad = new NativeLong[16];
    }

    private X11 x11;

    @Override
    public synchronized boolean handOver() {
        try {
            if (x11 == null) x11 = Native.load("X11", X11.class);
        } catch (UnsatisfiedLinkError | RuntimeException e) {
            LOG.warn("libX11 is not available, so the clipboard cannot be handed over", e);
            return false;
        }
        Pointer display = x11.XOpenDisplay(null);
        if (display == null) return false;
        NativeLong window = null;
        try {
            NativeLong manager = x11.XInternAtom(display, "CLIPBOARD_MANAGER", 0);
            NativeLong owner = x11.XGetSelectionOwner(display, manager);
            if (owner == null || owner.longValue() == 0) {
                LOG.debug("No clipboard manager is running");
                return false;
            }
            window = x11.XCreateSimpleWindow(display, x11.XDefaultRootWindow(display), -10, -10, 1, 1, 0,
                    new NativeLong(0), new NativeLong(0));
            NativeLong property = x11.XInternAtom(display, "_AMADE_SCREENSHOT_SAVE", 0);
            x11.XConvertSelection(display, manager, x11.XInternAtom(display, "SAVE_TARGETS", 0), property, window,
                    new NativeLong(0));
            x11.XFlush(display);
            boolean saved = awaitAnswer(display);
            LOG.info(saved ? "The clipboard manager took the screenshot over" : "The clipboard manager did not take the screenshot");
            return saved;
        } finally {
            if (window != null) x11.XDestroyWindow(display, window);
            x11.XCloseDisplay(display);
        }
    }

    private boolean awaitAnswer(Pointer display) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        XSelectionEvent event = new XSelectionEvent();
        while (System.currentTimeMillis() < deadline) {
            if (x11.XPending(display) > 0) {
                x11.XNextEvent(display, event);
                if (event.type == SELECTION_NOTIFY) return event.property != null && event.property.longValue() != 0;
                continue;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
