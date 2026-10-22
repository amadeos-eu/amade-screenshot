package eu.amadeos.apps.screenshot.ui.main;

import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.FileNames;
import eu.amadeos.apps.screenshot.domain.ImageFormat;
import eu.amadeos.apps.screenshot.domain.Options;
import eu.amadeos.apps.screenshot.repository.ImageFiles;
import eu.amadeos.apps.screenshot.repository.ScreenshotPreferences;
import eu.amadeos.apps.screenshot.service.Capture;
import eu.amadeos.apps.screenshot.service.CaptureService;
import eu.amadeos.apps.screenshot.service.KeepAlive;
import eu.amadeos.commons.desktop.DesktopLauncher;
import eu.amadeos.commons.i18n.Language;
import eu.amadeos.commons.i18n.LocaleSettings;
import eu.amadeos.commons.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Takes, shows, saves and copies screenshots. The waiting and the grab run on the background executor,
 * everything else on the event dispatch thread ({@code ui}).
 */
public class MainPresenter {
    private static final Logger LOG = LoggerFactory.getLogger(MainPresenter.class);
    /** After hiding its window, the desktop needs a moment to draw what was below it. */
    static final long SETTLE_MILLIS = 400;

    /** Waits; a test passes one that returns at once. */
    @FunctionalInterface
    public interface Pause {
        Pause SLEEP = Thread::sleep;

        void sleep(long millis) throws InterruptedException;
    }

    private final MainView view;
    private final Messages msg;
    private final LocaleSettings locales;
    private final ScreenshotPreferences preferences;
    private final CaptureService captures;
    private final ImageFiles files;
    private final DesktopLauncher launcher;
    private final Path picturesFolder;
    private final Executor background;
    private final Executor ui;
    private final Pause pause;
    private final Supplier<LocalDateTime> clock;
    private final KeepAlive keepAlive;
    private final Runnable quit;

    private Options options;
    private BufferedImage shot;
    private Path lastSaved;
    private boolean taking;
    private boolean ownsClipboard;
    private boolean closed;
    /** One save at a time, so that the free name found is still free when written. */
    private final Object saving = new Object();

    public MainPresenter(MainView view, Messages msg, LocaleSettings locales, ScreenshotPreferences preferences,
                         CaptureService captures, ImageFiles files, DesktopLauncher launcher, Path picturesFolder,
                         Executor background, Executor ui, Pause pause, Supplier<LocalDateTime> clock,
                         KeepAlive keepAlive, Runnable quit) {
        this.view = view;
        this.msg = msg;
        this.locales = locales;
        this.preferences = preferences;
        this.captures = captures;
        this.files = files;
        this.launcher = launcher;
        this.picturesFolder = picturesFolder;
        this.background = background;
        this.ui = ui;
        this.pause = pause;
        this.clock = clock;
        this.keepAlive = keepAlive;
        this.quit = quit;
        this.options = preferences.options();
    }

    public void start() {
        view.showOptions(options);
        view.showStatus(msg.get("status.ready"));
        updateCommands();
    }

    public Options options() {
        return options;
    }

    public Optional<BufferedImage> shot() {
        return Optional.ofNullable(shot);
    }

    // ---------------------------------------------------------------- options

    public void setMode(CaptureMode mode) {
        changeOptions(options.withMode(mode));
    }

    public void setDelay(int seconds) {
        changeOptions(options.withDelay(seconds));
    }

    public void setHideWindow(boolean hide) {
        changeOptions(options.withHideWindow(hide));
    }

    private void changeOptions(Options changed) {
        if (changed.equals(options)) return;
        options = changed;
        preferences.setOptions(changed);
    }

    // ---------------------------------------------------------------- taking

    // --8<-- [start:take]
    public void take() {
        if (taking || closed) return;
        taking = true;
        updateCommands();
        Options o = options;
        if (o.hideWindow()) view.hideForCapture();
        background.execute(() -> {
            try {
                for (int left = o.delay(); left > 0; left--) {
                    int seconds = left;
                    ui.execute(() -> view.showStatus(msg.format("status.countdown", seconds)));
                    pause.sleep(1000);
                }
                if (o.hideWindow()) pause.sleep(SETTLE_MILLIS);
                Capture capture = captures.capture(o.mode().needsSelection());
                ui.execute(() -> captured(o.mode(), capture));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ui.execute(this::cancelled);
            } catch (RuntimeException e) {
                LOG.warn("Screenshot failed", e);
                ui.execute(() -> failed(e));
            }
        });
    }
    // --8<-- [end:take]

    // --8<-- [start:captured]
    private void captured(CaptureMode mode, Capture capture) {
        if (!mode.needsSelection()) {
            finished(capture.image());
            return;
        }
        view.select(mode, capture, chosen -> {
            Optional<BufferedImage> part = chosen.flatMap(capture::crop);
            if (part.isPresent()) finished(part.get());
            else cancelled();
        });
    }
    // --8<-- [end:captured]

    private void finished(BufferedImage image) {
        taking = false;
        shot = image;
        view.showAfterCapture();
        view.showShot(image);
        view.showStatus(msg.format("status.taken", image.getWidth(), image.getHeight()));
        updateCommands();
    }

    private void cancelled() {
        taking = false;
        view.showAfterCapture();
        view.showStatus(msg.get("status.cancelled"));
        updateCommands();
    }

    private void failed(RuntimeException e) {
        taking = false;
        view.showAfterCapture();
        view.showStatus(" ");
        view.showError(msg.format("error.capture", message(e)));
        updateCommands();
    }

    // ---------------------------------------------------------------- saving and copying

    // --8<-- [start:save]
    /** Saves in the pictures folder under the date and time, without asking. */
    public void save() {
        if (shot == null) return;
        // The free name is looked for just before writing, so that two quick saves get two names.
        write(() -> FileNames.unused(picturesFolder, clock.get(), ImageFormat.PNG), ImageFormat.PNG);
    }
    // --8<-- [end:save]

    public void saveAs() {
        if (shot == null) return;
        Path folder = lastSaved != null ? lastSaved.getParent() : picturesFolder;
        view.chooseSavePath(folder, FileNames.name(clock.get(), ImageFormat.PNG))
                .ifPresent(target -> write(target::path, target.format()));
    }

    // --8<-- [start:write]
    private void write(Supplier<Path> target, ImageFormat format) {
        BufferedImage picture = shot;
        background.execute(() -> {
            Path path;
            synchronized (saving) {
                path = target.get();
                try {
                    files.write(picture, path, format);
                } catch (IOException | RuntimeException e) {
                    LOG.warn("Could not save {}", path, e);
                    ui.execute(() -> view.showError(msg.format("error.save", path, message(e))));
                    return;
                }
            }
            ui.execute(() -> {
                lastSaved = path;
                view.showStatus(msg.format("status.saved", path.getFileName(), path.getParent()));
                updateCommands();
            });
        });
    }
    // --8<-- [end:write]

    // --8<-- [start:copy]
    public void copy() {
        if (shot == null) return;
        view.copyToClipboard(shot, () -> ui.execute(this::clipboardLost));
        ownsClipboard = true;
        view.showStatus(msg.get("status.copied"));
    }
    // --8<-- [end:copy]

    // --8<-- [start:clipboard-lost]
    private void clipboardLost() {
        ownsClipboard = false;
        if (closed) {
            keepAlive.release();
            quit.run();
        }
    }
    // --8<-- [end:clipboard-lost]

    public boolean ownsClipboard() {
        return ownsClipboard;
    }

    public void openFolder() {
        if (lastSaved == null) return;
        Path folder = lastSaved.getParent();
        background.execute(() -> {
            try {
                launcher.open(folder);
            } catch (IOException | RuntimeException e) {
                LOG.warn("Could not open {}", folder, e);
                ui.execute(() -> view.showError(msg.format("error.open-folder", folder, message(e))));
            }
        });
    }

    // ---------------------------------------------------------------- the rest

    public void setLanguage(Optional<Language> language) {
        try {
            locales.setApplicationLanguage(language);
        } catch (IOException e) {
            LOG.warn("Could not save the language", e);
            view.showError(msg.format("error.save-language", e.getMessage()));
            return;
        }
        if (locales.restartRequired()) {
            view.showInfo(Messages.commons(msg.locale()).format("language.restart", "amade-screenshot"));
        }
    }

    public void about() {
        view.showAbout();
    }

    // --8<-- [start:exit]
    /**
     * Closes the window. While a copied screenshot is on the clipboard the program stays in the background,
     * so that it can still be pasted, and ends when another program takes the clipboard over.
     */
    public void exit() {
        if (closed) return;
        closed = true;
        view.close();
        if (ownsClipboard) {
            keepAlive.hold();
        } else {
            quit.run();
        }
    }
    // --8<-- [end:exit]

    public boolean isClosed() {
        return closed;
    }

    private void updateCommands() {
        view.updateCommands(new MainView.Commands(shot != null, taking, lastSaved != null));
    }

    private static String message(Exception e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
