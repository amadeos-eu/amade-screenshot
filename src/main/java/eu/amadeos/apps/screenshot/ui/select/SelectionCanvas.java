package eu.amadeos.apps.screenshot.ui.select;

import eu.amadeos.apps.screenshot.domain.CaptureMode;
import eu.amadeos.apps.screenshot.domain.Selection;
import eu.amadeos.apps.screenshot.service.Capture;
import eu.amadeos.commons.swing.AmadeUI;
import eu.amadeos.commons.swing.EdtExecutor;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * The frozen desktop, dimmed, with the area being drawn or the window under the pointer lit up. Draw a
 * frame (area) or click (window) to choose; the right mouse button cancels. Component coordinates are
 * desktop coordinates minus the capture's origin.
 */
public final class SelectionCanvas extends JComponent {
    static final Color DIM = new Color(0, 0, 0, 110);

    private final Capture capture;
    private final CaptureMode mode;
    private final String hint;
    private final Consumer<Optional<Rectangle>> done;
    private Point start;
    /** The lit rectangle in desktop coordinates, or null. */
    private Rectangle marked;
    private boolean finished;

    public SelectionCanvas(Capture capture, CaptureMode mode, String hint, Consumer<Optional<Rectangle>> done) {
        EdtExecutor.requireEdt();
        if (!mode.needsSelection()) throw new IllegalArgumentException("nothing to select in " + mode);
        this.capture = capture;
        this.mode = mode;
        this.hint = hint;
        this.done = done;
        setName("selection");
        setOpaque(true);
        setCursor(Cursor.getPredefinedCursor(mode == CaptureMode.AREA ? Cursor.CROSSHAIR_CURSOR : Cursor.HAND_CURSOR));
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) cancel();
                else if (SwingUtilities.isLeftMouseButton(e)) pressed(desktop(e.getPoint()));
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) dragged(desktop(e.getPoint()));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) released(desktop(e.getPoint()));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                moved(desktop(e.getPoint()));
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    private Point desktop(Point component) {
        return new Point(component.x + capture.bounds().x, component.y + capture.bounds().y);
    }

    void pressed(Point at) {
        if (mode == CaptureMode.AREA) {
            start = at;
            mark(Selection.between(at, at));
        } else {
            moved(at);
        }
    }

    void dragged(Point at) {
        if (mode == CaptureMode.AREA && start != null) mark(Selection.between(start, at));
    }

    void released(Point at) {
        if (mode == CaptureMode.AREA) {
            if (start == null) return;
            Rectangle area = Selection.between(start, at);
            start = null;
            if (Selection.isLargeEnough(area)) finish(Selection.clip(area, capture.bounds()));
            else mark(null);
        } else {
            Optional<Rectangle> window = Selection.windowAt(capture.windows(), at);
            if (window.isPresent()) finish(Selection.clip(window.get(), capture.bounds()));
        }
    }

    void moved(Point at) {
        if (mode == CaptureMode.WINDOW) mark(Selection.windowAt(capture.windows(), at).orElse(null));
    }

    /** Escape or the right mouse button. */
    public void cancel() {
        finish(Optional.empty());
    }

    private void finish(Optional<Rectangle> result) {
        if (finished) return;
        finished = true;
        done.accept(result);
    }

    private void mark(Rectangle rectangle) {
        marked = rectangle;
        repaint();
    }

    /** The lit rectangle in desktop coordinates, for tests. */
    Optional<Rectangle> marked() {
        return Optional.ofNullable(marked);
    }

    @Override
    public Dimension getPreferredSize() {
        return capture.bounds().getSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.drawImage(capture.image(), 0, 0, null);
            Rectangle lit = marked == null ? null : new Rectangle(marked.x - capture.bounds().x,
                    marked.y - capture.bounds().y, marked.width, marked.height);
            g.setColor(DIM);
            if (lit == null) {
                g.fillRect(0, 0, getWidth(), getHeight());
            } else {
                // Dim everything around the lit rectangle.
                g.fillRect(0, 0, getWidth(), lit.y);
                g.fillRect(0, lit.y + lit.height, getWidth(), getHeight() - lit.y - lit.height);
                g.fillRect(0, lit.y, lit.x, lit.height);
                g.fillRect(lit.x + lit.width, lit.y, getWidth() - lit.x - lit.width, lit.height);
                Color accent = accent();
                g.setColor(accent);
                g.setStroke(new BasicStroke(2f));
                g.drawRect(lit.x, lit.y, lit.width - 1, lit.height - 1);
                label(g, lit.width + " × " + lit.height, lit, accent);
            }
            hint(g);
        } finally {
            g.dispose();
        }
    }

    /** The theme's title-bar colour, which the desktop uses for "selected". */
    private static Color accent() {
        return AmadeUI.theme() != null ? AmadeUI.theme().activeTitleBg() : new Color(0x4a6fa5);
    }

    private void label(Graphics2D g, String text, Rectangle lit, Color accent) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(text) + 10;
        int h = fm.getHeight() + 4;
        int x = lit.x;
        int y = lit.y - h - 2 >= 0 ? lit.y - h - 2 : lit.y + 2;
        g.setColor(accent);
        g.fillRect(x, y, w, h);
        g.setColor(Color.WHITE);
        g.drawString(text, x + 5, y + 2 + fm.getAscent());
    }

    private void hint(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(hint) + 24;
        int h = fm.getHeight() + 12;
        // On the screen at the desktop's top left, i.e. the first screen.
        int x = Math.max(0, (Math.min(getWidth(), screenWidth()) - w) / 2);
        int y = 24;
        g.setComposite(AlphaComposite.SrcOver.derive(0.85f));
        g.setColor(Color.BLACK);
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(Color.WHITE);
        g.drawString(hint, x + 12, y + 6 + fm.getAscent());
    }

    private int screenWidth() {
        try {
            return getGraphicsConfiguration() != null ? getGraphicsConfiguration().getBounds().width : getWidth();
        } catch (RuntimeException e) {
            return getWidth();
        }
    }
}
