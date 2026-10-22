package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.apps.screenshot.OnEdt;
import eu.amadeos.apps.screenshot.QueueExecutor;
import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.Options;
import eu.amadeos.apps.screenshot.repository.ImageFiles;
import eu.amadeos.apps.screenshot.repository.ScreenshotPreferences;
import eu.amadeos.apps.screenshot.service.CaptureService;
import eu.amadeos.apps.screenshot.service.KeepAlive;
import eu.amadeos.apps.screenshot.service.Screen;
import eu.amadeos.commons.desktop.DesktopLauncher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** The real window with a fake screen: options, actions, preview. Needs a display (Xvfb in CI). */
class MainWindowTest {
    @TempDir
    Path dir;

    @Test
    void takeAndSaveThroughTheWindow() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "needs a display");
        QueueExecutor executor = new QueueExecutor();
        Path pictures = dir.resolve("Pictures");
        Screen screen = new Screen() {
            @Override
            public Rectangle bounds() {
                return new Rectangle(0, 0, 640, 400);
            }

            @Override
            public BufferedImage grab(Rectangle area) {
                return new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB);
            }
        };
        OnEdt.run(() -> {
            ScreenshotPreferences preferences = new ScreenshotPreferences(dir.resolve("screenshot.properties"));
            preferences.setOptions(new Options(CaptureMode.WINDOW, 5, false));
            MainWindow window = new MainWindow(MainPresenterTest.MSG, null);
            MainPresenter presenter = new MainPresenter(window, MainPresenterTest.MSG, null, preferences,
                    new CaptureService(screen, List::of), new ImageFiles(), new DesktopLauncher(), pictures,
                    executor, executor, millis -> { }, () -> MainPresenterTest.TIME, new KeepAlive(), () -> { });
            window.bind(presenter);
            presenter.start();
            try {
                assertTrue(window.modeButton(CaptureMode.WINDOW).isSelected());
                assertEquals(5, window.delayBox().getSelectedItem());
                assertFalse(window.hideBox().isSelected());
                assertEquals("5 s", window.delayText(5));
                assertEquals("None", window.delayText(0));
                ScreenshotActions actions = window.actions();
                assertTrue(actions.take.isEnabled());
                assertFalse(actions.save.isEnabled());
                assertFalse(actions.copy.isEnabled());
                assertFalse(actions.openFolder.isEnabled());

                // The controls change the options.
                window.modeButton(CaptureMode.DESKTOP).doClick();
                window.delayBox().setSelectedItem(0);
                window.hideBox().doClick();
                assertEquals(new Options(CaptureMode.DESKTOP, 0, true), presenter.options());

                // Ctrl+N takes, Ctrl+S saves.
                Action take = (Action) window.getRootPane().getActionMap().get(window.getRootPane()
                        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK)));
                assertNotNull(take);
                take.actionPerformed(null);
                assertFalse(window.isVisible(), "hidden while taking");
                assertFalse(actions.take.isEnabled());
                executor.runAll();
                assertEquals(640, window.preview().shot().getWidth());
                assertEquals("Screenshot of 640 × 400 pixels.", window.statusText());
                assertTrue(actions.save.isEnabled());
                assertTrue(actions.copy.isEnabled());
                actions.save.actionPerformed(null);
                executor.runAll();
                assertTrue(Files.exists(pictures.resolve("Screenshot 2026-10-22 14-05-33.png")));
                assertTrue(actions.openFolder.isEnabled());
                BufferedImage painted = new BufferedImage(window.getWidth(), window.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                window.paint(painted.createGraphics());
            } finally {
                window.dispose();
            }
        });
    }
}
