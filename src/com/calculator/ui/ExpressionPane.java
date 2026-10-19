package com.calculator.ui;

import com.calculator.model.Symbols;

import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ô nhập biểu thức trên màn hình LCD.
 * - Số mũ (sau dấu ^) hiện nhỏ và nâng lên trên; dấu ^ được ẩn.
 * - Phân số a⁄b hiện tử nâng lên, mẫu hạ xuống.
 * - Con trỏ thu nhỏ và nâng lên khi đang sửa số mũ.
 * - Phím ► ở cuối số mũ sẽ thoát khỏi số mũ.
 */
public class ExpressionPane extends JTextPane {
    static final Color LCD = new Color(0xc5d0b5);
    static final Color LCD_FG = new Color(0x1d2318);

    private static final char CARET = '^';
    private static final char FRAC = Symbols.FRACTION_SLASH;
    private static final char END = Symbols.EXPONENT_END;
    private static final Pattern FRACTION = Pattern.compile("(\\d+)" + FRAC + "(\\d+)(?:" + FRAC + "(\\d+))?");

    /** Các chuỗi cần xoá nguyên khối khi bấm DEL (dài trước, ngắn sau). */
    private static final String[] WHOLE_TOKENS = {
            "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "sqrt(", "cbrt(",
            "sin(", "cos(", "tan(", "log(", "exp(", "abs(", "ln(", "√(",
            "Ans", "root", "nPr", "nCr"};

    // ---- kiểu chữ
    private static final SimpleAttributeSet NORMAL = style(22, false, false, LCD_FG);
    private static final SimpleAttributeSet SUPER = style(18, true, false, LCD_FG);
    private static final SimpleAttributeSet SUB = style(18, false, true, LCD_FG);
    private static final SimpleAttributeSet SLASH = style(18, false, false, LCD_FG);
    private static final SimpleAttributeSet GAP = style(10, false, false, LCD);      // khoảng trống giữa số nguyên và phân số
    private static final SimpleAttributeSet HIDDEN = style(2, false, false, LCD);    // ký tự ẩn (^ và dấu kết thúc số mũ)

    private static SimpleAttributeSet style(int size, boolean sup, boolean sub, Color fg) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setFontFamily(a, Font.MONOSPACED);
        StyleConstants.setFontSize(a, size);
        StyleConstants.setBold(a, false);
        StyleConstants.setSuperscript(a, sup);
        StyleConstants.setSubscript(a, sub);
        StyleConstants.setForeground(a, fg);
        return a;
    }

    private boolean restylePending = false;

    public ExpressionPane() {
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 22));
        setBackground(LCD);
        setForeground(LCD_FG);
        setCaretColor(LCD_FG);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        setCharacterAttributes(NORMAL, true);

        ExponentCaret caret = new ExponentCaret();
        caret.setBlinkRate(500);
        setCaret(caret);

        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { scheduleRestyle(); }
            @Override public void removeUpdate(DocumentEvent e)  { scheduleRestyle(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });
    }

    /** Không xuống dòng: chỉ cuộn ngang khi biểu thức dài hơn màn hình. */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport
                && getUI().getPreferredSize(this).width <= getParent().getWidth();
    }

    // ================================================================ chỉnh sửa
    /** Xoá một ký tự nhìn thấy (hoặc nguyên một hàm như sin( ) trước con trỏ. */
    public void backspace() {
        int pos = getCaretPosition();
        String text = getText();
        String before = text.substring(0, pos);
        String after = text.substring(pos);
        while (before.endsWith(String.valueOf(END))) before = before.substring(0, before.length() - 1);
        if (before.isEmpty()) {
            setText(after);
            setCaretPosition(0);
            return;
        }
        int remove = 1;
        for (String t : WHOLE_TOKENS) {
            if (before.endsWith(t)) {
                remove = t.length();
                break;
            }
        }
        String newBefore = before.substring(0, before.length() - remove);
        if (newBefore.endsWith(String.valueOf(CARET))) {      // '^' đang bị ẩn nên xoá luôn
            newBefore = newBefore.substring(0, newBefore.length() - 1);
        }
        setText(newBefore + after);
        setCaretPosition(newBefore.length());
    }

    /** Di chuyển con trỏ sang trái (-1) hoặc phải (+1), bỏ qua ký tự ẩn. */
    public void moveCaret(int delta) {
        String t = getText();
        int caret = getCaretPosition();

        if (delta > 0) {
            // đang ở cuối số mũ: ► = thoát khỏi số mũ
            ExpSpan sp = exponentEndingAt(t, caret);
            if (sp != null) {
                String add = null;
                if (sp.group) add = ")";
                else if (sp.end >= t.length() || t.charAt(sp.end) != END) add = String.valueOf(END);
                if (add != null) {
                    try {
                        getDocument().insertString(caret, add, null);
                    } catch (BadLocationException ignored) {
                        return;
                    }
                    setCaretPosition(caret + add.length());
                    return;
                }
            }
        }

        int pos;
        if (delta < 0) {
            pos = Math.max(0, caret - 1);
            while (pos > 0 && isHidden(t.charAt(pos))) pos--;
        } else {
            pos = Math.min(t.length(), caret + 1);
            while (pos < t.length() && isHidden(t.charAt(pos - 1))) pos++;
        }
        setCaretPosition(pos);
    }

    private static boolean isHidden(char c) {
        return c == CARET || c == END;
    }

    // ================================================================ định dạng
    private void scheduleRestyle() {
        if (restylePending) return;
        restylePending = true;
        SwingUtilities.invokeLater(() -> {
            restylePending = false;
            restyle();
        });
    }

    private void restyle() {
        StyledDocument doc = getStyledDocument();
        String t;
        try {
            t = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }
        int n = t.length();
        doc.setCharacterAttributes(0, n, NORMAL, true);

        // số mũ
        boolean[] inExponent = new boolean[n];
        for (int i = 0; i < n; i++) {
            char c = t.charAt(i);
            if (c == END) {
                doc.setCharacterAttributes(i, 1, HIDDEN, true);
            } else if (c == CARET) {
                doc.setCharacterAttributes(i, 1, HIDDEN, true);
                ExpSpan sp = exponentSpan(t, i + 1);
                if (sp.end > i + 1) {
                    doc.setCharacterAttributes(i + 1, sp.end - i - 1, SUPER, true);
                    for (int k = i + 1; k < sp.end; k++) inExponent[k] = true;
                }
            }
        }

        // phân số: tử nâng lên, mẫu hạ xuống
        Matcher m = FRACTION.matcher(t);
        while (m.find()) {
            if (inExponent[m.start()]) continue;
            if (m.group(3) != null) {                     // hỗn số a⁄b⁄c
                set(doc, m.end(1), 1, GAP);
                set(doc, m.start(2), m.end(2) - m.start(2), SUPER);
                set(doc, m.end(2), 1, SLASH);
                set(doc, m.start(3), m.end(3) - m.start(3), SUB);
            } else {                                      // phân số a⁄b
                set(doc, m.start(1), m.end(1) - m.start(1), SUPER);
                set(doc, m.end(1), 1, SLASH);
                set(doc, m.start(2), m.end(2) - m.start(2), SUB);
            }
        }
    }

    private static void set(StyledDocument doc, int start, int len, SimpleAttributeSet a) {
        doc.setCharacterAttributes(start, len, a, true);
    }

    // ================================================================ phân tích số mũ
    /** Phạm vi số mũ: kết thúc tại end; group = dạng (...), closed = đã đóng ngoặc. */
    static final class ExpSpan {
        final int end;
        final boolean group, closed;

        ExpSpan(int end, boolean group, boolean closed) {
            this.end = end;
            this.group = group;
            this.closed = closed;
        }
    }

    /** Số mũ bắt đầu tại p (ngay sau ^): [-]số, [-]số⁄số, [-](...) hoặc π. */
    static ExpSpan exponentSpan(String t, int p) {
        int n = t.length();
        int i = p;
        if (i < n && t.charAt(i) == '-') i++;
        if (i < n && t.charAt(i) == '(') {
            int depth = 0;
            boolean closed = false;
            while (i < n) {
                char c = t.charAt(i++);
                if (c == '(') depth++;
                else if (c == ')' && --depth == 0) {
                    closed = true;
                    break;
                }
            }
            return new ExpSpan(i, true, closed);
        }
        int start = i;
        while (i < n && (Character.isDigit(t.charAt(i)) || t.charAt(i) == '.')) i++;
        if (i == start) {
            if (i < n && t.charAt(i) == 'π') i++;
        } else {
            for (int k = 0; k < 2; k++) {                 // phân số trong số mũ: x^1⁄2
                if (i < n && t.charAt(i) == FRAC) {
                    int j = i + 1;
                    while (j < n && Character.isDigit(t.charAt(j))) j++;
                    if (j == i + 1) break;
                    i = j;
                } else {
                    break;
                }
            }
        }
        return new ExpSpan(i, false, false);
    }

    /** Con trỏ d đang nằm trong một số mũ chưa kết thúc? */
    static boolean caretInExponent(String t, int d) {
        for (int p = 0; p < t.length(); p++) {
            if (t.charAt(p) != CARET) continue;
            ExpSpan sp = exponentSpan(t, p + 1);
            if (d >= p + 1 && d <= sp.end) {
                if (sp.group && sp.closed && d == sp.end) continue;   // đã đóng ngoặc: ra khỏi số mũ
                return true;
            }
        }
        return false;
    }

    /** Số mũ (chưa đóng) có phần cuối đúng tại vị trí d, hoặc null. */
    static ExpSpan exponentEndingAt(String t, int d) {
        for (int p = 0; p < t.length(); p++) {
            if (t.charAt(p) != CARET) continue;
            ExpSpan sp = exponentSpan(t, p + 1);
            if (d >= p + 1 && d == sp.end && !(sp.group && sp.closed)) return sp;
        }
        return null;
    }

    // ================================================================ con trỏ
    /** Con trỏ bình thường; khi đang sửa số mũ thì ngắn lại và nằm ở phía trên. */
    private class ExponentCaret extends DefaultCaret {
        @Override
        public void paint(Graphics g) {
            if (!isVisible()) return;
            JTextComponent c = getComponent();
            if (c == null) return;
            Rectangle2D r;
            try {
                r = c.modelToView2D(getDot());
            } catch (BadLocationException e) {
                return;
            }
            if (r == null) return;
            g.setColor(c.getCaretColor());
            int x = (int) Math.round(r.getX());
            int y = (int) Math.round(r.getY());
            int h = (int) Math.round(r.getHeight());
            if (caretInExponent(c.getText(), getDot())) {
                g.fillRect(x, y + 1, 2, Math.max(8, h * 45 / 100));
            } else {
                g.fillRect(x, y, 2, h);
            }
        }
    }
}
