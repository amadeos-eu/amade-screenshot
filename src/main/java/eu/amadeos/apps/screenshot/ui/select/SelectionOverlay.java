package eu.amadeos.apps.screenshot.ui.select;

import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.service.Capture;
import eu.amadeos.commons.swing.EdtExecutor;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A borderless window over all screens that shows the {@link SelectionCanvas}. With one screen it is a
 * full-screen window, so that panels stay below it; with several it covers their bounds. It closes itself
 * when the choice is made.
 */
public final class SelectionOverlay extends JFrame {
    private final SelectionCanvas canvas;

    private SelectionOverlay(Capture capture, CaptureMode mode, String hint, Consumer<Optional<Rectangle>> done) {
        super(hint);
        setUndecorated(true);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        canvas = new SelectionCanvas(capture, mode, hint, result -> {
            close();
            done.accept(result);
        });
        setContentPane(canvas);
        canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                "cancel");
        canvas.getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                canvas.cancel();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                canvas.cancel();
            }
        });
        setBounds(capture.bounds());
    }

    // --8<-- [start:show]
    /** Shows the overlay; {@code done} gets the chosen rectangle in desktop coordinates, or nothing. */
    public static SelectionOverlay show(Capture capture, CaptureMode mode, String hint,
                                        Consumer<Optional<Rectangle>> done) {
        EdtExecutor.requireEdt();
        SelectionOverlay overlay = new SelectionOverlay(capture, mode, hint, done);
        GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (screens.length == 1 && screens[0].isFullScreenSupported()) {
            screens[0].setFullScreenWindow(overlay);
        } else {
            overlay.setVisible(true);
        }
        overlay.toFront();
        overlay.canvas.requestFocusInWindow();
        return overlay;
    }
    // --8<-- [end:show]

    SelectionCanvas canvas() {
        return canvas;
    }

    // --8<-- [start:close]
    private void close() {
        GraphicsDevice device = getGraphicsConfiguration().getDevice();
        if (device.getFullScreenWindow() == this) device.setFullScreenWindow(null);
        dispose();
    }
    // --8<-- [end:close]
}
