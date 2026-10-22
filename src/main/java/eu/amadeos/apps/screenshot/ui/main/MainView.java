package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.ImageFormat;
import eu.amadeos.apps.screenshot.domain.Options;
import eu.amadeos.apps.screenshot.service.Capture;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

/** What the presenter needs from the window. All calls on the event dispatch thread. */
public interface MainView {
    /** Which commands are possible now. */
    record Commands(boolean hasShot, boolean taking, boolean hasSavedFile) {
        public static final Commands NONE = new Commands(false, false, false);
    }

    record SaveTarget(Path path, ImageFormat format) {
    }

    void showOptions(Options options);

    void showShot(BufferedImage shot);

    void showStatus(String text);

    void updateCommands(Commands commands);

    /** Takes the window off the screen for the shot. */
    void hideForCapture();

    /** Brings the window back after the shot. */
    void showAfterCapture();

    /** Lets the user pick a window or draw an area on the frozen desktop. */
    void select(CaptureMode mode, Capture capture, Consumer<Optional<Rectangle>> done);

    Optional<SaveTarget> chooseSavePath(Path folder, String suggestedName);

    /** Puts the picture on the clipboard; {@code lost} runs when another program takes the clipboard over. */
    void copyToClipboard(BufferedImage picture, Runnable lost);

    void showError(String message);

    void showInfo(String message);

    void showAbout();

    /** Closes the window for good. */
    void close();
}
