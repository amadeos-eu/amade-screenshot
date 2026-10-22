package eu.amadeos.apps.screenshot.service;

import com.google.inject.Inject;

import java.awt.Rectangle;
import java.util.List;

/** Takes the whole desktop, and the window list when a window is to be picked. */
public class CaptureService {
    private final Screen screen;
    private final WindowList windows;

    @Inject
    public CaptureService(Screen screen, WindowList windows) {
        this.screen = screen;
        this.windows = windows;
    }

    // --8<-- [start:capture]
    /** Call off the event dispatch thread: the X server is asked synchronously. */
    public Capture capture(boolean withWindows) {
        Rectangle bounds = screen.bounds();
        List<Rectangle> list = withWindows ? windows.topFirst() : List.of();
        return new Capture(screen.grab(bounds), bounds, list);
    }
    // --8<-- [end:capture]
}
