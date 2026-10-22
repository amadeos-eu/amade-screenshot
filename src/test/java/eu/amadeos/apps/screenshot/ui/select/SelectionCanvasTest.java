package eu.amadeos.apps.screenshot.ui.select;

import eu.amadeos.apps.screenshot.OnEdt;
import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.service.Capture;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The gestures on the frozen desktop; the canvas works without a display. */
class SelectionCanvasTest {
    /** A desktop whose left screen starts at x = -100. */
    private static final Rectangle DESKTOP = new Rectangle(-100, 0, 400, 300);
    private static final Rectangle TOP = new Rectangle(50, 50, 40, 40);
    private static final Rectangle BOTTOM = new Rectangle(-150, 0, 250, 200);

    private final List<Optional<Rectangle>> results = new ArrayList<>();

    private SelectionCanvas canvas(CaptureMode mode) {
        Capture capture = new Capture(new BufferedImage(DESKTOP.width, DESKTOP.height, BufferedImage.TYPE_INT_RGB),
                DESKTOP, List.of(TOP, BOTTOM));
        return new SelectionCanvas(capture, mode, "hint", results::add);
    }

    @Test
    void anAreaIsDrawn() throws Exception {
        OnEdt.run(() -> {
            SelectionCanvas canvas = canvas(CaptureMode.AREA);
            canvas.pressed(new Point(60, 40));
            canvas.dragged(new Point(20, 10));
            assertEquals(Optional.of(new Rectangle(20, 10, 41, 31)), canvas.marked());
            canvas.released(new Point(10, 5));
            assertEquals(List.of(Optional.of(new Rectangle(10, 5, 51, 36))), results);
            // Only the first choice counts.
            canvas.cancel();
            assertEquals(1, results.size());
        });
    }

    @Test
    void aClickIsNotAnArea() throws Exception {
        OnEdt.run(() -> {
            SelectionCanvas canvas = canvas(CaptureMode.AREA);
            canvas.pressed(new Point(60, 40));
            canvas.released(new Point(60, 41));
            assertTrue(results.isEmpty());
            assertEquals(Optional.empty(), canvas.marked());
            canvas.released(new Point(80, 80));
            assertTrue(results.isEmpty(), "a release without a press does nothing");
        });
    }

    // --8<-- [start:window]
    @Test
    void theWindowUnderThePointerIsLitAndPicked() throws Exception {
        OnEdt.run(() -> {
            SelectionCanvas canvas = canvas(CaptureMode.WINDOW);
            canvas.moved(new Point(60, 60));
            assertEquals(Optional.of(TOP), canvas.marked());
            canvas.moved(new Point(0, 10));
            assertEquals(Optional.of(BOTTOM), canvas.marked());
            canvas.moved(new Point(250, 250));
            assertEquals(Optional.empty(), canvas.marked());
            canvas.released(new Point(250, 250));
            assertTrue(results.isEmpty(), "a click beside every window picks nothing");
            canvas.released(new Point(0, 10));
            // The part off the desktop is cut away.
            assertEquals(List.of(Optional.of(new Rectangle(-100, 0, 200, 200))), results);
        });
    }
    // --8<-- [end:window]

    @Test
    void cancelling() throws Exception {
        OnEdt.run(() -> {
            canvas(CaptureMode.AREA).cancel();
            assertEquals(List.of(Optional.empty()), results);
        });
    }

    @Test
    void theDesktopNeedsNoSelection() {
        assertThrows(IllegalArgumentException.class, () -> OnEdt.run(() -> canvas(CaptureMode.DESKTOP)));
    }

    @Test
    void paintsWithAndWithoutAMark() throws Exception {
        OnEdt.run(() -> {
            SelectionCanvas canvas = canvas(CaptureMode.AREA);
            canvas.setSize(canvas.getPreferredSize());
            BufferedImage image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
            canvas.paint(image.createGraphics());
            int dimmed = image.getRGB(200, 200);
            canvas.pressed(new Point(100, 100));
            canvas.dragged(new Point(200, 200));
            image = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
            canvas.paint(image.createGraphics());
            // Inside the mark the frozen desktop (black here) shows undimmed; both are black, so just check
            // that painting worked and the frame is drawn.
            assertEquals(dimmed, image.getRGB(399, 299));
            assertTrue(image.getRGB(200, 150) != image.getRGB(250, 150), "the frame is drawn at the mark's edge");
        });
    }
}
