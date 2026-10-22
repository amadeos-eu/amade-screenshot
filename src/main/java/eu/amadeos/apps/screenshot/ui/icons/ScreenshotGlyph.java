package eu.amadeos.apps.screenshot.ui.icons;

import eu.amadeos.commons.swing.StandardGlyph;
import eu.amadeos.commons.swing.ToolIcon;

import java.awt.BasicStroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

/**
 * The glyphs only Screenshot needs; save and copy come from {@link StandardGlyph}. Highlights (the lens,
 * the picked window, the drawn frame) are in the theme's selection colour.
 */
public enum ScreenshotGlyph implements ToolIcon.Glyph {
    CAMERA, FOLDER, DESKTOP, WINDOW, AREA;

    @Override
    public void paint(ToolIcon.Pen pen) {
        switch (this) {
            case CAMERA -> camera(pen);
            case FOLDER -> folder(pen);
            case DESKTOP -> desktop(pen);
            case WINDOW -> window(pen);
            case AREA -> area(pen);
        }
    }

    /** A camera body with a lens and a flash. */
    private static void camera(ToolIcon.Pen pen) {
        Path2D body = new Path2D.Double();
        body.moveTo(1.5, 4.5);
        body.lineTo(5, 4.5);
        body.lineTo(6.5, 2.5);
        body.lineTo(9.5, 2.5);
        body.lineTo(11, 4.5);
        body.lineTo(14.5, 4.5);
        body.lineTo(14.5, 13.5);
        body.lineTo(1.5, 13.5);
        body.closePath();
        pen.fillAndDraw(body, pen.paper());
        pen.fillAndDraw(new Ellipse2D.Double(4.5, 6, 7, 7), pen.ink());
        pen.fill(new Ellipse2D.Double(6, 7.5, 4, 4), pen.light());
        pen.fill(new Rectangle2D.Double(12, 5.5, 1.5, 1.5), pen.light());
    }

    private static void folder(ToolIcon.Pen pen) {
        Path2D back = new Path2D.Double();
        back.moveTo(1.5, 3.5);
        back.lineTo(6, 3.5);
        back.lineTo(7.5, 5);
        back.lineTo(14.5, 5);
        back.lineTo(14.5, 13.5);
        back.lineTo(1.5, 13.5);
        back.closePath();
        pen.fillAndDraw(back, pen.paper());
        Path2D front = new Path2D.Double();
        front.moveTo(1.5, 13.5);
        front.lineTo(3.5, 7.5);
        front.lineTo(15.5, 7.5);
        front.lineTo(14.5, 13.5);
        front.closePath();
        pen.fillAndDraw(front, pen.light());
    }

    /** A monitor on a stand. */
    private static void desktop(ToolIcon.Pen pen) {
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 1.5, 13, 10), pen.light());
        pen.fillAndDraw(new Rectangle2D.Double(6.5, 11.5, 3, 2), pen.paper());
        pen.line(4, 14.5, 12, 14.5);
    }

    /** A window with its title bar, in the selection colour. */
    private static void window(ToolIcon.Pen pen) {
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 2.5, 13, 11), pen.paper());
        pen.fillAndDraw(new Rectangle2D.Double(1.5, 2.5, 13, 3), pen.light());
    }

    /** A dashed frame with a corner handle. */
    private static void area(ToolIcon.Pen pen) {
        pen.fill(new RoundRectangle2D.Double(3.5, 3.5, 9, 9, 1, 1), pen.light());
        pen.g().setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[]{2f, 1.5f}, 0));
        pen.draw(new Rectangle2D.Double(1.5, 1.5, 13, 13));
        pen.thin();
        pen.fillAndDraw(new Rectangle2D.Double(12.5, 12.5, 3, 3), pen.paper());
    }
}
