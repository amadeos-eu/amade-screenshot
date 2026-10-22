package eu.amadeos.apps.screenshot.bootstrap;

import eu.amadeos.apps.screenshot.domain.CommandLine;
import eu.amadeos.apps.screenshot.domain.FileNames;
import eu.amadeos.apps.screenshot.domain.ImageFormat;
import eu.amadeos.apps.screenshot.repository.ImageFiles;
import eu.amadeos.apps.screenshot.service.Capture;
import eu.amadeos.apps.screenshot.service.CaptureService;
import eu.amadeos.apps.screenshot.service.KeepAlive;
import eu.amadeos.apps.screenshot.ui.main.MainPresenter.Pause;
import eu.amadeos.apps.screenshot.ui.select.SelectionOverlay;
import eu.amadeos.commons.desktop.ImageSelection;
import eu.amadeos.commons.i18n.Messages;

import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * One screenshot from the command line, without the window: wait, take, let the user pick a window or an
 * area, then save and/or copy. Exit status 0 on success, 1 if cancelled or failed.
 */
public final class QuickShot {
    private final CaptureService captures;
    private final ImageFiles files;
    private final Messages msg;
    private final Path picturesFolder;
    private final Executor background;
    private final Executor ui;
    private final Pause pause;
    private final Supplier<LocalDateTime> clock;
    private final KeepAlive keepAlive;
    private final PrintStream out;
    private final IntConsumer exit;

    public QuickShot(CaptureService captures, ImageFiles files, Messages msg, Path picturesFolder, Executor background,
                     Executor ui, Pause pause, Supplier<LocalDateTime> clock, KeepAlive keepAlive, PrintStream out,
                     IntConsumer exit) {
        this.captures = captures;
        this.files = files;
        this.msg = msg;
        this.picturesFolder = picturesFolder;
        this.background = background;
        this.ui = ui;
        this.pause = pause;
        this.clock = clock;
        this.keepAlive = keepAlive;
        this.out = out;
        this.exit = exit;
    }

    // --8<-- [start:run]
    public void run(CommandLine.Take take) {
        background.execute(() -> {
            try {
                pause.sleep(take.delay() * 1000L);
                Capture capture = captures.capture(take.mode().needsSelection());
                ui.execute(() -> {
                    if (!take.mode().needsSelection()) {
                        deliver(take, capture.image());
                        return;
                    }
                    SelectionOverlay.show(capture, take.mode(), msg.get("select." + take.mode().key()),
                            chosen -> chosen.flatMap(capture::crop).ifPresentOrElse(
                                    picture -> deliver(take, picture), () -> exit.accept(1)));
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exit.accept(1);
            } catch (RuntimeException e) {
                fail(msg.format("error.capture", e.getMessage()));
            }
        });
    }
    // --8<-- [end:run]

    // --8<-- [start:deliver]
    private void deliver(CommandLine.Take take, BufferedImage picture) {
        background.execute(() -> {
            if (take.save() || take.file().isPresent()) {
                Path path = take.file().map(Path::toAbsolutePath)
                        .orElseGet(() -> FileNames.unused(picturesFolder, clock.get(), ImageFormat.PNG));
                ImageFormat format = ImageFormat.forPath(path).orElse(ImageFormat.PNG);
                try {
                    files.write(picture, path, format);
                    out.println(path);
                } catch (IOException | RuntimeException e) {
                    fail(msg.format("error.save", path, e.getMessage()));
                    return;
                }
            }
            if (!take.copy()) {
                exit.accept(0);
                return;
            }
            ui.execute(() -> {
                ImageSelection.put(Toolkit.getDefaultToolkit().getSystemClipboard(), picture, () -> {
                    keepAlive.release();
                    exit.accept(0);
                });
                keepAlive.hold();
            });
        });
    }
    // --8<-- [end:deliver]

    private void fail(String message) {
        System.err.println("amade-screenshot: " + message);
        exit.accept(1);
    }
}
