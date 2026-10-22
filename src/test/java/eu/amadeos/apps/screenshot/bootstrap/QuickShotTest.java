package eu.amadeos.apps.screenshot.bootstrap;

import eu.amadeos.apps.screenshot.QueueExecutor;
import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.CommandLine;
import eu.amadeos.apps.screenshot.repository.ImageFiles;
import eu.amadeos.apps.screenshot.service.CaptureService;
import eu.amadeos.apps.screenshot.service.KeepAlive;
import eu.amadeos.apps.screenshot.service.Screen;
import eu.amadeos.commons.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuickShotTest {
    @TempDir
    Path dir;

    private final QueueExecutor executor = new QueueExecutor();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final List<Integer> exits = new ArrayList<>();
    private final List<Long> pauses = new ArrayList<>();
    private RuntimeException failure;

    private QuickShot quickShot() {
        Screen screen = new Screen() {
            @Override
            public Rectangle bounds() {
                return new Rectangle(0, 0, 200, 100);
            }

            @Override
            public BufferedImage grab(Rectangle area) {
                if (failure != null) throw failure;
                return new BufferedImage(area.width, area.height, BufferedImage.TYPE_INT_RGB);
            }
        };
        return new QuickShot(new CaptureService(screen, List::of), new ImageFiles(),
                Messages.load(ScreenshotModule.class, ScreenshotModule.MESSAGES, Locale.ENGLISH),
                dir.resolve("Pictures"), executor, executor, pauses::add, () -> LocalDateTime.of(2026, 10, 22, 9, 0),
                new KeepAlive(), new PrintStream(out, true, StandardCharsets.UTF_8), exits::add);
    }

    @Test
    void theDesktopIsSavedInThePicturesFolder() throws IOException {
        quickShot().run(new CommandLine.Take(CaptureMode.DESKTOP, 2, false, true, Optional.empty()));
        executor.runAll();
        Path saved = dir.resolve("Pictures/Screenshot 2026-10-22 09-00-00.png");
        assertEquals(saved + System.lineSeparator(), out.toString(StandardCharsets.UTF_8));
        assertEquals(200, ImageIO.read(saved.toFile()).getWidth());
        assertEquals(List.of(2000L), pauses);
        assertEquals(List.of(0), exits);
    }

    @Test
    void aNamedFileGetsItsFormat() throws IOException {
        Path file = dir.resolve("shot.jpg");
        quickShot().run(new CommandLine.Take(CaptureMode.DESKTOP, 0, false, false, Optional.of(file)));
        executor.runAll();
        assertEquals(100, ImageIO.read(file.toFile()).getHeight());
        byte[] head = Files.readAllBytes(file);
        assertEquals((byte) 0xFF, head[0], "a JPEG file");
        assertEquals(List.of(0), exits);
    }

    @Test
    void failuresEndWithStatusOne() {
        failure = new IllegalStateException("no display");
        quickShot().run(new CommandLine.Take(CaptureMode.DESKTOP, 0, false, true, Optional.empty()));
        executor.runAll();
        assertEquals(List.of(1), exits);
        assertTrue(out.toString(StandardCharsets.UTF_8).isEmpty());
    }
}
