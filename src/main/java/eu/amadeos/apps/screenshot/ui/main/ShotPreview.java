package eu.amadeos.apps.screenshot.ui.main;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** The last screenshot, scaled down to fit and centred; a placeholder text before the first. */
public final class ShotPreview extends JComponent {
    private BufferedImage shot;
    private String placeholder = "";

    public ShotPreview() {
        setName("preview");
        setPreferredSize(new Dimension(480, 300));
    }

    public void setShot(BufferedImage newShot) {
        shot = newShot;
        repaint();
    }

    public BufferedImage shot() {
        return shot;
    }

    public void setPlaceholder(String text) {
        placeholder = text;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            Insets in = getInsets();
            int w = getWidth() - in.left - in.right;
            int h = getHeight() - in.top - in.bottom;
            g.setColor(getBackground());
            g.fillRect(in.left, in.top, w, h);
            if (shot == null) {
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g.setColor(getForeground());
                FontMetrics fm = g.getFontMetrics();
                g.drawString(placeholder, in.left + (w - fm.stringWidth(placeholder)) / 2,
                        in.top + (h - fm.getHeight()) / 2 + fm.getAscent());
                return;
            }
            double scale = Math.min(1.0, Math.min((w - 16.0) / shot.getWidth(), (h - 16.0) / shot.getHeight()));
            if (scale <= 0) return;
            int sw = Math.max(1, (int) Math.round(shot.getWidth() * scale));
            int sh = Math.max(1, (int) Math.round(shot.getHeight() * scale));
            int x = in.left + (w - sw) / 2;
            int y = in.top + (h - sh) / 2;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(shot, x, y, sw, sh, null);
            g.setColor(getForeground());
            g.drawRect(x - 1, y - 1, sw + 1, sh + 1);
        } finally {
            g.dispose();
        }
    }
}
