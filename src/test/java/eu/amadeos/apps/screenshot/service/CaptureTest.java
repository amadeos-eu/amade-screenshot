package eu.amadeos.apps.screenshot.service;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptureTest {
    /** A desktop that starts at (-100, 0), as with a second screen to the left. */
    static final Rectangle DESKTOP = new Rectangle(-100, 0, 300, 200);

    static BufferedImage desktopImage() {
        BufferedImage image = new BufferedImage(DESKTOP.width, DESKTOP.height, BufferedImage.TYPE_INT_RGB);
        // Desktop point (0, 0) is image point (100, 0).
        image.setRGB(100, 0, Color.RED.getRGB());
        image.setRGB(299, 199, Color.BLUE.getRGB());
        return image;
    }

    @Test
    void cropsInDesktopCoordinates() {
        Capture capture = new Capture(desktopImage(), DESKTOP, List.of());
        BufferedImage part = capture.crop(new Rectangle(0, 0, 10, 10)).orElseThrow();
        assertEquals(10, part.getWidth());
        assertEquals(Color.RED.getRGB(), part.getRGB(0, 0));
        BufferedImage corner = capture.crop(new Rectangle(190, 190, 50, 50)).orElseThrow();
        assertEquals(new Rectangle(0, 0, 10, 10), new Rectangle(corner.getWidth(), corner.getHeight()));
        assertEquals(Color.BLUE.getRGB(), corner.getRGB(9, 9));
        assertEquals(Optional.empty(), capture.crop(new Rectangle(500, 500, 5, 5)));
    }

    @Test
    void theCropIsACopy() {
        BufferedImage image = desktopImage();
        BufferedImage part = new Capture(image, DESKTOP, List.of()).crop(new Rectangle(0, 0, 5, 5)).orElseThrow();
        image.setRGB(100, 0, Color.GREEN.getRGB());
        assertEquals(Color.RED.getRGB(), part.getRGB(0, 0));
    }

    @Test
    void windowsAreOnlyListedWhenOneIsToBePicked() {
        List<Rectangle> grabbed = new ArrayList<>();
        Screen screen = new Screen() {
            @Override
            public Rectangle bounds() {
                return DESKTOP;
            }

            @Override
            public BufferedImage grab(Rectangle area) {
                grabbed.add(area);
                return desktopImage();
            }
        };
        int[] asked = {0};
        WindowList windows = () -> {
            asked[0]++;
            return List.of(new Rectangle(0, 0, 50, 50));
        };
        CaptureService service = new CaptureService(screen, windows);
        Capture desktop = service.capture(false);
        assertTrue(desktop.windows().isEmpty());
        assertEquals(0, asked[0]);
        Capture withWindows = service.capture(true);
        assertEquals(List.of(new Rectangle(0, 0, 50, 50)), withWindows.windows());
        assertEquals(List.of(DESKTOP, DESKTOP), grabbed);
        assertEquals(DESKTOP, withWindows.bounds());
        assertFalse(WindowList.NONE.topFirst().iterator().hasNext());
    }

    @Test
    void keepAliveHoldsUntilReleased() throws InterruptedException {
        KeepAlive keepAlive = new KeepAlive();
        assertFalse(keepAlive.isHolding());
        keepAlive.hold();
        assertTrue(keepAlive.isHolding());
        keepAlive.release();
        assertFalse(keepAlive.isHolding());
    }
}
