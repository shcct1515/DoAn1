package com.calculator.ui;

import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Dòng kết quả của máy tính: hiện số bình thường hoặc phân số/hỗn số xếp chồng (tử trên mẫu). */
public class ResultView extends JComponent {
    private static final int HEIGHT = 66;
    private static final Font BIG = new Font(Font.MONOSPACED, Font.BOLD, 32);
    private static final Font MEDIUM = new Font(Font.MONOSPACED, Font.BOLD, 22);

    private String text = "0";
    private boolean fraction = false;
    private boolean negative;
    private long whole, numerator, denominator;

    public ResultView() {
        setOpaque(false);
        setPreferredSize(new Dimension(100, HEIGHT));
        setMinimumSize(new Dimension(100, HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
    }

    public void showText(String t) {
        text = t;
        fraction = false;
        repaint();
    }

    /** Hiện phân số xếp chồng; whole = 0 thì chỉ hiện phân số, ngược lại hiện hỗn số. */
    public void showFraction(boolean negative, long whole, long numerator, long denominator) {
        this.negative = negative;
        this.whole = whole;
        this.numerator = numerator;
        this.denominator = denominator;
        fraction = true;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ExpressionPane.LCD_FG);

        int w = getWidth(), h = getHeight(), pad = 4;
        if (!fraction) {
            Font f = BIG;
            if (g2.getFontMetrics(f).stringWidth(text) > w - 2 * pad) f = MEDIUM;
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, w - pad - fm.stringWidth(text), baseline);
        } else {
            FontMetrics big = g2.getFontMetrics(BIG);
            FontMetrics med = g2.getFontMetrics(MEDIUM);
            String lead = (negative ? "-" : "") + (whole > 0 ? Long.toString(whole) : "");
            String num = Long.toString(numerator);
            String den = Long.toString(denominator);

            int fracW = Math.max(med.stringWidth(num), med.stringWidth(den)) + 12;
            int leadW = big.stringWidth(lead);
            int gap = lead.isEmpty() ? 0 : 6;
            int x = w - pad - (leadW + gap + fracW);

            int barY = h / 2;
            int digitH = (int) (MEDIUM.getSize() * 0.72);
            if (!lead.isEmpty()) {
                g2.setFont(BIG);
                g2.drawString(lead, x, barY + 12);
            }
            int fx = x + leadW + gap;
            g2.setFont(MEDIUM);
            g2.drawString(num, fx + (fracW - med.stringWidth(num)) / 2, barY - 5);
            g2.drawString(den, fx + (fracW - med.stringWidth(den)) / 2, barY + 5 + digitH);
            g2.fillRect(fx, barY - 1, fracW, 3);
        }
        g2.dispose();
    }
}
