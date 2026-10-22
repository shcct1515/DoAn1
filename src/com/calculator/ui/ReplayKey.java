package com.calculator.ui;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * Phím điều hướng hình elip (như phím REPLAY của máy Casio): ◄ ► di chuyển con trỏ,
 * ▲ ▼ duyệt lại các biểu thức đã tính. Giữ chuột để lặp lại.
 */
public class ReplayKey extends JComponent {
    public enum Dir { LEFT, RIGHT, UP, DOWN }

    private final Consumer<Dir> listener;
    private Color fill = new Color(0x6b7380);
    private Color arrow = Color.WHITE;
    private Dir pressed = null;
    private final Timer repeat;

    public ReplayKey(Consumer<Dir> listener) {
        this.listener = listener;
        setPreferredSize(new Dimension(120, 48));
        setFocusable(false);

        repeat = new Timer(90, e -> {
            if (pressed != null) listener.accept(pressed);
        });
        repeat.setInitialDelay(400);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Dir d = hit(e.getX(), e.getY());
                if (d == null) return;
                pressed = d;
                listener.accept(d);
                repeat.restart();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = null;
                repeat.stop();
                repaint();
            }
        });
    }

    public void applyColors(Color fill, Color arrow) {
        this.fill = fill;
        this.arrow = arrow;
        repaint();
    }

    /** Vùng nào của hình elip được bấm (null nếu ngoài elip). */
    private Dir hit(int x, int y) {
        double a = getWidth() / 2.0, b = getHeight() / 2.0;
        double dx = (x - a) / a, dy = (y - b) / b;
        if (dx * dx + dy * dy > 1) return null;
        if (Math.abs(dx) * 0.8 > Math.abs(dy)) return dx < 0 ? Dir.LEFT : Dir.RIGHT;
        return dy < 0 ? Dir.UP : Dir.DOWN;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(fill);
        g2.fillOval(1, 1, w - 3, h - 3);
        g2.setColor(fill.darker().darker());
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(1, 1, w - 3, h - 3);

        int cx = w / 2, cy = h / 2;
        drawArrow(g2, Dir.LEFT, cx - w * 3 / 10, cy);
        drawArrow(g2, Dir.RIGHT, cx + w * 3 / 10, cy);
        drawArrow(g2, Dir.UP, cx, cy - h * 3 / 11);
        drawArrow(g2, Dir.DOWN, cx, cy + h * 3 / 11);
        g2.dispose();
    }

    private void drawArrow(Graphics2D g2, Dir d, int x, int y) {
        int s = 6;
        Polygon p = new Polygon();
        switch (d) {
            case LEFT:  p.addPoint(x - s, y); p.addPoint(x + s, y - s); p.addPoint(x + s, y + s); break;
            case RIGHT: p.addPoint(x + s, y); p.addPoint(x - s, y - s); p.addPoint(x - s, y + s); break;
            case UP:    p.addPoint(x, y - s + 1); p.addPoint(x - s, y + s - 2); p.addPoint(x + s, y + s - 2); break;
            default:    p.addPoint(x, y + s - 1); p.addPoint(x - s, y - s + 2); p.addPoint(x + s, y - s + 2); break;
        }
        g2.setColor(pressed == d ? new Color(0xffd54f) : arrow);
        g2.fillPolygon(p);
    }
}
