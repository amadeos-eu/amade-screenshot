package eu.amadeos.apps.screenshot.service;

import java.util.concurrent.CountDownLatch;

// --8<-- [start:keep-alive]
/**
 * Keeps the program running without a window. On X11 the clipboard holds no data: it only names the
 * program that has it, and a program that pastes asks that one. So after Copy the program has to stay until
 * another program takes the clipboard over, the way {@code xclip} does.
 */
public final class KeepAlive {
    private final CountDownLatch released = new CountDownLatch(1);
    private Thread thread;

    /** Starts a non-daemon thread that waits until {@link #release()}. */
    public synchronized void hold() {
        if (thread != null) return;
        thread = new Thread(() -> {
            try {
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
