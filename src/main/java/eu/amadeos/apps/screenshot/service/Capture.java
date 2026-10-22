package eu.amadeos.apps.screenshot.service;

import eu.amadeos.apps.screenshot.domain.Selection;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Optional;

// --8<-- [start:capture]
/**
 * The whole desktop at one moment, with the windows that were on it. Areas and windows are cut from this
 * frozen picture, so menus that close when the focus moves are still in it.
 *
 * @param image   the pixels of {@code bounds}
 * @param bounds  the desktop, in desktop coordinates
 * @param windows the windows' bounds in desktop coordinates, the top first; empty for a desktop shot
 */
public record Capture(BufferedImage image, Rectangle bounds, List<Rectangle> windows) {
    public Capture {
        windows = List.copyOf(windows);
        bounds = new Rectangle(bounds);
    }

    /** A copy of the part of the picture inside the area (desktop coordinates); empty if outside. */
    public Optional<BufferedImage> crop(Rectangle area) {
        return Selection.clip(area, bounds).map(part -> {
            BufferedImage sub = image.getSubimage(part.x - bounds.x, part.y - bounds.y, part.width, part.height);
            BufferedImage copy = new BufferedImage(part.width, part.height, BufferedImage.TYPE_INT_RGB);
            copy.createGraphics().drawImage(sub, 0, 0, null);
            return copy;
        });
    }
}
// --8<-- [end:capture]
