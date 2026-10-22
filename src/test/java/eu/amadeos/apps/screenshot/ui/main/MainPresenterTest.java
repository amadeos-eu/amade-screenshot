package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.apps.screenshot.QueueExecutor;
import eu.amadeos.apps.screenshot.bootstrap.ScreenshotModule;
import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.ImageFormat;
import eu.amadeos.apps.screenshot.domain.Options;
import eu.amadeos.apps.screenshot.repository.ImageFiles;
import eu.amadeos.apps.screenshot.repository.ScreenshotPreferences;
import eu.amadeos.apps.screenshot.service.CaptureService;
import eu.amadeos.apps.screenshot.service.KeepAlive;
import eu.amadeos.apps.screenshot.service.Screen;
import eu.amadeos.commons.desktop.DesktopLauncher;
import eu.amadeos.commons.i18n.Messages;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainPresenterTest {
    static final Messages MSG = Messages.load(ScreenshotModule.class, ScreenshotModule.MESSAGES, Locale.ENGLISH);
    static final LocalDateTime TIME = LocalDateTime.of(2026, 10, 22, 14, 5, 33);
    static final Rectangle DESKTOP = new Rectangle(0, 0, 320, 200);

    @TempDir
    Path dir;

    private final QueueExecutor executor = new QueueExecutor();
    private final FakeView view = new FakeView();
    private final List<Long> pauses = new ArrayList<>();
    private final List<Path> opened = new ArrayList<>();
    private final KeepAlive keepAlive = new KeepAlive();
    private int quits;
    private int grabs;
    private RuntimeException grabFailure;
    private List<Rectangle> windows = List.of();
    private ScreenshotPreferences preferences;
    private Path pictures;
    private MainPresenter presenter;

    @BeforeEach
    void setUp() {
        preferences = new ScreenshotPreferences(dir.resolve("screenshot.properties"));
        pictures = dir.resolve("Pictures");
        Screen screen = new Screen() {
            @Override
            public Rectangle bounds() {
                return DESKTOP;
            }

            @Override
            public BufferedImage grab(Rectangle area) {
                if (grabFailure != null) throw grabFailure;
                grabs++;
                BufferedImage image = new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB);
                image.setRGB(10, 20, Color.RED.getRGB());
                return image;
            }
        };
        DesktopLauncher launcher = new DesktopLauncher() {
            @Override
            public void open(Path file) {
                opened.add(file);
            }
        };
        presenter = new MainPresenter(view, MSG, null, preferences, new CaptureService(screen, () -> windows),
                new ImageFiles(), launcher, pictures, executor, executor, pauses::add, () -> TIME, keepAlive,
                () -> quits++);
        presenter.start();
    }

    @Test
    void startsWithTheRememberedOptions() {
        assertEquals(Options.DEFAULT, view.options);
        assertEquals(MSG.get("status.ready"), view.lastStatus());
        assertEquals(MainView.Commands.NONE, view.commands);
    }

    @Test
    void optionChangesAreSaved() {
        presenter.setMode(CaptureMode.AREA);
        presenter.setDelay(5);
        presenter.setHideWindow(false);
        assertEquals(new Options(CaptureMode.AREA, 5, false), presenter.options());
        assertEquals(presenter.options(), preferences.options());
        assertEquals(presenter.options(), new ScreenshotPreferences(dir.resolve("screenshot.properties")).options());
    }

    @Test
    void theDesktopIsTakenWithTheWindowOutOfTheWay() {
        presenter.take();
        assertFalse(view.visible);
        assertTrue(view.commands.taking());
        presenter.take();
        executor.runAll();
        assertEquals(1, grabs, "a second press while taking does nothing");
        assertEquals(List.of(MainPresenter.SETTLE_MILLIS), pauses);
        assertEquals(List.of("hide", "show"), view.events);
        assertEquals(320, view.shot.getWidth());
        assertEquals("Screenshot of 320 × 200 pixels.", view.lastStatus());
        assertEquals(new MainView.Commands(true, false, false), view.commands);
    }

    @Test
    void theDelayCountsDown() {
        presenter.setDelay(3);
        presenter.setHideWindow(false);
        presenter.take();
        executor.runAll();
        assertEquals(List.of(1000L, 1000L, 1000L), pauses);
        assertTrue(view.statuses.containsAll(List.of("Taking the screenshot in 3 s...",
                "Taking the screenshot in 2 s...", "Taking the screenshot in 1 s...")));
        assertEquals(List.of("show"), view.events);
    }

    @Test
    void anAreaIsCutFromTheDesktop() {
        presenter.setMode(CaptureMode.AREA);
        view.selection = Optional.of(new Rectangle(10, 20, 30, 40));
        presenter.take();
        executor.runAll();
        assertEquals(CaptureMode.AREA, view.selectedIn);
        assertEquals(30, view.shot.getWidth());
        assertEquals(40, view.shot.getHeight());
        assertEquals(Color.RED.getRGB(), view.shot.getRGB(0, 0));
        assertEquals(List.of("hide", "select", "show"), view.events);
    }

    @Test
    void aWindowIsPickedFromTheList() {
        windows = List.of(new Rectangle(5, 5, 100, 50));
        presenter.setMode(CaptureMode.WINDOW);
        view.selection = Optional.of(windows.getFirst());
        presenter.take();
        executor.runAll();
        assertEquals(CaptureMode.WINDOW, view.selectedIn);
        assertEquals(100, view.shot.getWidth());
    }

    @Test
    void cancellingKeepsTheLastShot() {
        presenter.take();
        executor.runAll();
        BufferedImage first = view.shot;
        presenter.setMode(CaptureMode.AREA);
        presenter.take();
        executor.runAll();
        assertSame(first, view.shot);
        assertSame(first, presenter.shot().orElseThrow());
        assertEquals("Cancelled.", view.lastStatus());
        assertTrue(view.visible);
        assertFalse(view.commands.taking());
    }

    @Test
    void aFailedGrabIsReported() {
        grabFailure = new IllegalStateException("no display");
        presenter.take();
        executor.runAll();
        assertEquals(List.of("Could not take the screenshot: no display"), view.errors);
        assertTrue(view.visible);
        assertFalse(view.commands.taking());
        assertNull(view.shot);
    }

    @Test
    void saveNamesTheFileByDateAndTime() throws IOException {
        presenter.save();
        executor.runAll();
        assertFalse(Files.exists(pictures), "nothing to save yet");
        presenter.take();
        executor.runAll();
        presenter.save();
        presenter.save();
        executor.runAll();
        Path first = pictures.resolve("Screenshot 2026-10-22 14-05-33.png");
        assertEquals(320, ImageIO.read(first.toFile()).getWidth());
        assertTrue(Files.exists(pictures.resolve("Screenshot 2026-10-22 14-05-33 (2).png")));
        assertEquals("Saved as Screenshot 2026-10-22 14-05-33 (2).png in " + pictures + ".", view.lastStatus());
        assertTrue(view.commands.hasSavedFile());
        presenter.openFolder();
        executor.runAll();
        assertEquals(List.of(pictures), opened);
    }

    @Test
    void saveAsAsksForNameAndFormat() throws IOException {
        presenter.take();
        executor.runAll();
        presenter.saveAs();
        assertEquals(pictures, view.chooserFolder);
        assertEquals("Screenshot 2026-10-22 14-05-33.png", view.suggestedName);
        assertTrue(executor.isIdle(), "cancelled: nothing written");
        Path jpg = dir.resolve("elsewhere/shot.jpg");
        view.saveTarget = Optional.of(new MainView.SaveTarget(jpg, ImageFormat.JPEG));
        presenter.saveAs();
        executor.runAll();
        assertEquals(200, ImageIO.read(jpg.toFile()).getHeight());
        presenter.saveAs();
        assertEquals(jpg.getParent(), view.chooserFolder, "the next Save As starts where the last one saved");
    }

    @Test
    void aSaveErrorIsShown() throws IOException {
        Files.writeString(pictures, "a file where the folder should be");
        presenter.take();
        executor.runAll();
        presenter.save();
        executor.runAll();
        assertEquals(1, view.errors.size());
        assertTrue(view.errors.getFirst().startsWith("Could not save " + pictures.resolve("Screenshot")),
                view.errors.getFirst());
        assertFalse(view.commands.hasSavedFile());
    }

    @Test
    void exitWithoutClipboardQuits() {
        presenter.exit();
        assertTrue(view.closed);
        assertEquals(1, quits);
        assertFalse(keepAlive.isHolding());
        presenter.take();
        assertTrue(executor.isIdle(), "nothing happens after closing");
    }

    @Test
    void aCopiedShotOutlivesTheWindow() {
        presenter.take();
        executor.runAll();
        presenter.copy();
        assertSame(view.shot, view.clipboard);
        assertTrue(presenter.ownsClipboard());
        assertTrue(view.lastStatus().startsWith("Copied to the clipboard."));
        presenter.exit();
        assertTrue(view.closed);
        assertEquals(0, quits, "stays for pasting");
        assertTrue(keepAlive.isHolding());
        view.clipboardLost.run();
        executor.runAll();
        assertEquals(1, quits);
        assertFalse(keepAlive.isHolding());
    }

    @Test
    void losingTheClipboardWhileOpenChangesNothingVisible() {
        presenter.take();
        executor.runAll();
        presenter.copy();
        view.clipboardLost.run();
        executor.runAll();
        assertFalse(presenter.ownsClipboard());
        assertEquals(0, quits);
        presenter.exit();
        assertEquals(1, quits);
    }

    @Test
    void about() {
        presenter.about();
        assertEquals(1, view.abouts);
    }
}
