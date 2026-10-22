package eu.amadeos.apps.screenshot.service;

import com.google.inject.Inject;

import java.util.concurrent.CountDownLatch;

// --8<-- [start:keep-alive]
/**
 * Keeps the program running without a window while its screenshot is on the clipboard. On X11 the clipboard
 * holds no data: it only names the program that has it, and a program that pastes asks that one. So after
 * Copy the program first asks the desktop's clipboard manager to take the screenshot over
 * ({@link ClipboardHandoff}); the manager then owns the clipboard and the program ends. Without a manager it
 * stays until another program takes the clipboard over, the way {@code xclip} does.
 */
public final class KeepAlive {
    private final ClipboardHandoff handoff;
    private final CountDownLatch released = new CountDownLatch(1);
    private Thread thread;

    /** Without a clipboard manager, for the tests. */
    public KeepAlive() {
        this(ClipboardHandoff.NONE);
    }

    @Inject
    public KeepAlive(ClipboardHandoff handoff) {
        this.handoff = handoff;
    }

    /**
     * Starts a non-daemon thread that hands the clipboard to the clipboard manager and waits until
     * {@link #release()}, which the loss of the clipboard calls.
     */
    public synchronized void hold() {
        if (thread != null) return;
        thread = new Thread(() -> {
            try {
                handoff.handOver();
                released.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "clipboard-keeper");
        thread.setDaemon(false);
        thread.start();
    }

    public void release() {
        released.countDown();
    }

    public synchronized boolean isHolding() {
        return thread != null && released.getCount() > 0;
    }
}
// --8<-- [end:keep-alive]
