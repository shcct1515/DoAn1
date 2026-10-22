package com.calculator.ui;

import com.calculator.settings.Theme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Dòng kết quả của máy tính: hiện số bình thường hoặc phân số/hỗn số xếp chồng (tử trên mẫu). */
public class ResultView extends JComponent {
    private Font big = new Font(Font.MONOSPACED, Font.BOLD, 32);
    private Font medium = new Font(Font.MONOSPACED, Font.BOLD, 22);
    private Color fg = new Color(0x1d2318);

    private String text = "0";
    private boolean fraction = false;
    private boolean negative;
    private long whole, numerator, denominator;

    public ResultView() {
        setOpaque(false);
        applyTheme(Theme.classic(), 22);
    }

    /** Đổi màu và cỡ chữ (size = cỡ chữ của dòng nhập). */
    public void applyTheme(Theme t, int size) {
        fg = t.get("lcdText");
        big = new Font(Font.MONOSPACED, Font.BOLD, size + 10);
        medium = new Font(Font.MONOSPACED, Font.BOLD, size);
        int h = size * 3;
        setPreferredSize(new Dimension(100, h));
        setMinimumSize(new Dimension(100, h));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        revalidate();
        repaint();
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
        g2.setColor(fg);

        int w = getWidth(), h = getHeight(), pad = 4;
        if (!fraction) {
            Font f = big;
            if (g2.getFontMetrics(f).stringWidth(text) > w - 2 * pad) f = medium;
            g2.setFont(f);
            FontMetrics fm = g2.getFontMetrics();
            int baseline = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, w - pad - fm.stringWidth(text), baseline);
        } else {
            FontMetrics bigM = g2.getFontMetrics(big);
            FontMetrics medM = g2.getFontMetrics(medium);
            String lead = (negative ? "-" : "") + (whole > 0 ? Long.toString(whole) : "");
            String num = Long.toString(numerator);
            String den = Long.toString(denominator);

            int fracW = Math.max(medM.stringWidth(num), medM.stringWidth(den)) + 12;
            int leadW = bigM.stringWidth(lead);
            int gap = lead.isEmpty() ? 0 : 6;
            int x = w - pad - (leadW + gap + fracW);

            int barY = h / 2;
            int digitH = (int) (medium.getSize() * 0.72);
            if (!lead.isEmpty()) {
                g2.setFont(big);
                g2.drawString(lead, x, barY + big.getSize() * 3 / 8);
            }
            int fx = x + leadW + gap;
            g2.setFont(medium);
            g2.drawString(num, fx + (fracW - medM.stringWidth(num)) / 2, barY - 5);
            g2.drawString(den, fx + (fracW - medM.stringWidth(den)) / 2, barY + 5 + digitH);
            g2.fillRect(fx, barY - 1, fracW, 3);
        }
        g2.dispose();
    }
}
