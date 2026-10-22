package eu.amadeos.apps.screenshot.service;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** The pixels on the screens. */
public interface Screen {
    /** All screens together, in desktop coordinates. */
    Rectangle bounds();

    /** The pixels of the rectangle, in desktop coordinates. */
    BufferedImage grab(Rectangle area);
}
