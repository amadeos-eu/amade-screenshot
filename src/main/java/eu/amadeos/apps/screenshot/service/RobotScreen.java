package eu.amadeos.apps.screenshot.service;

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

/**
 * Reads the screens with {@link Robot}, which on X11 asks the X server for the root window's pixels. No
 * external program is needed. The mouse pointer is not in the picture.
 */
public final class RobotScreen implements Screen {
    private Robot robot;

    @Override
    public Rectangle bounds() {
        Rectangle all = new Rectangle();
        for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
            Rectangle screen = device.getDefaultConfiguration().getBounds();
            all = all.isEmpty() ? screen : all.union(screen);
        }
        return all;
    }

    @Override
    public synchronized BufferedImage grab(Rectangle area) {
        try {
            if (robot == null) robot = new Robot();
        } catch (AWTException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
        return robot.createScreenCapture(area);
    }
}
