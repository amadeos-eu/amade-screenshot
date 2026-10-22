package eu.amadeos.apps.screenshot.service;

/**
 * Hands the copied screenshot to the desktop's clipboard manager, so that the program does not have to stay
 * after its window is closed. {@link X11ClipboardHandoff} does it; {@link #NONE} is for desktops without a
 * manager and for the tests.
 */
public interface ClipboardHandoff {

    /** Does nothing: the program keeps the clipboard itself until another program takes it over. */
    ClipboardHandoff NONE = () -> false;

    /**
     * Asks the clipboard manager to take the clipboard over and waits for its answer; true if it did. The
     * manager reads the copy from this program meanwhile, so the event dispatch thread must stay free.
     */
    boolean handOver();
}
