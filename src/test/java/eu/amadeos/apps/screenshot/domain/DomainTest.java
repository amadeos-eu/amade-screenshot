package eu.amadeos.apps.screenshot.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 10, 22, 14, 5, 33);

    @TempDir
    Path dir;

    @Test
    void namesSortByDateAndHaveNoColons() {
        assertEquals("Screenshot 2026-10-22 14-05-33.png", FileNames.name(TIME, ImageFormat.PNG));
        assertEquals("Screenshot 2026-10-22 14-05-33.jpg", FileNames.name(TIME, ImageFormat.JPEG));
    }

    @Test
    void aSecondShotInTheSameSecondGetsANumber() throws IOException {
        Path first = FileNames.unused(dir, TIME, ImageFormat.PNG);
        assertEquals(dir.resolve("Screenshot 2026-10-22 14-05-33.png"), first);
        Files.createFile(first);
        Path second = FileNames.unused(dir, TIME, ImageFormat.PNG);
        assertEquals(dir.resolve("Screenshot 2026-10-22 14-05-33 (2).png"), second);
        Files.createFile(second);
        assertEquals(dir.resolve("Screenshot 2026-10-22 14-05-33 (3).png"), FileNames.unused(dir, TIME, ImageFormat.PNG));
    }

    @Test
    void formatsFollowTheExtension() {
        assertEquals(Optional.of(ImageFormat.JPEG), ImageFormat.forPath(Path.of("a.JPEG")));
        assertEquals(Optional.of(ImageFormat.PNG), ImageFormat.forPath(Path.of("a.png")));
        assertEquals(Optional.empty(), ImageFormat.forPath(Path.of("a.gif")));
        assertEquals(Optional.empty(), ImageFormat.forPath(Path.of("shot")));
        assertEquals(Path.of("/x/a.jpg"), ImageFormat.JPEG.withExtension(Path.of("/x/a.png")));
        assertEquals(Path.of("/x/a.b.png"), ImageFormat.PNG.withExtension(Path.of("/x/a.b")));
    }

    @Test
    void modes() {
        assertEquals(Optional.of(CaptureMode.AREA), CaptureMode.forKey("area"));
        assertEquals(Optional.empty(), CaptureMode.forKey("AREA"));
        assertFalse(CaptureMode.DESKTOP.needsSelection());
        assertTrue(CaptureMode.WINDOW.needsSelection());
    }

    @Test
    void optionsCheckTheDelay() {
        Options options = Options.DEFAULT.withMode(CaptureMode.AREA).withDelay(5).withHideWindow(false);
        assertEquals(new Options(CaptureMode.AREA, 5, false), options);
        assertThrows(IllegalArgumentException.class, () -> Options.DEFAULT.withDelay(-1));
        assertThrows(IllegalArgumentException.class, () -> Options.DEFAULT.withDelay(Options.MAX_DELAY + 1));
    }

    @Test
    void anAreaCanBeDrawnInAnyDirection() {
        Rectangle expected = new Rectangle(10, 20, 31, 11);
        assertEquals(expected, Selection.between(new Point(10, 20), new Point(40, 30)));
        assertEquals(expected, Selection.between(new Point(40, 30), new Point(10, 20)));
        assertEquals(expected, Selection.between(new Point(10, 30), new Point(40, 20)));
        assertFalse(Selection.isLargeEnough(Selection.between(new Point(5, 5), new Point(5, 9))));
        assertTrue(Selection.isLargeEnough(Selection.between(new Point(5, 5), new Point(6, 6))));
    }

    @Test
    void theTopmostWindowUnderThePointerIsPicked() {
        Rectangle dialog = new Rectangle(100, 100, 50, 50);
        Rectangle editor = new Rectangle(0, 0, 400, 300);
        List<Rectangle> topFirst = List.of(dialog, editor);
        assertEquals(Optional.of(dialog), Selection.windowAt(topFirst, new Point(120, 120)));
        assertEquals(Optional.of(editor), Selection.windowAt(topFirst, new Point(10, 10)));
        assertEquals(Optional.empty(), Selection.windowAt(topFirst, new Point(500, 10)));
    }

    @Test
    void windowsPartlyOffTheScreenAreCut() {
        Rectangle desktop = new Rectangle(0, 0, 1024, 768);
        assertEquals(Optional.of(new Rectangle(0, 700, 100, 68)), Selection.clip(new Rectangle(-20, 700, 120, 100), desktop));
        assertEquals(Optional.empty(), Selection.clip(new Rectangle(2000, 0, 10, 10), desktop));
    }
}
