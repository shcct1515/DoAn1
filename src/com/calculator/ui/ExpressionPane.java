package com.calculator.ui;

import com.calculator.model.Symbols;
import com.calculator.settings.Theme;

import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ô nhập biểu thức trên màn hình LCD (tự vẽ).
 * - Số mũ (sau dấu ^) hiện nhỏ, nâng lên; dấu ^ được ẩn.
 * - Phân số hiện dạng gạch ngang: tử ở trên, mẫu ở dưới; phần còn trống hiện ô vuông □.
 * - Con trỏ thu nhỏ khi ở trong số mũ / tử / mẫu; ► ◄ đi vào, đi ra từng phần (tử → mẫu → ra ngoài).
 */
public class ExpressionPane extends JTextPane {
    private static final char CARET = '^';
    private static final char FRAC = Symbols.FRACTION_SLASH;
    private static final char END = Symbols.EXPONENT_END;
    private static final char FO = Symbols.FRAC_OPEN;
    private static final char FM = Symbols.FRAC_MID;
    private static final char FC = Symbols.FRAC_CLOSE;

    /** Các chuỗi cần xoá nguyên khối khi bấm DEL (dài trước, ngắn sau). */
    private static final String[] WHOLE_TOKENS = {
            "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "sqrt(", "cbrt(",
            "sin(", "cos(", "tan(", "log(", "exp(", "abs(", "ln(", "√(",
            "Ans", "root", "nPr", "nCr"};

    private static final int PAD = 3, GAP = 2;

    private Color bg = Color.WHITE, fg = Color.BLACK;
    private int size = 22;
    private Consumer<String> insertHook;
    private boolean caretOn = true;
    private boolean scrollPending = false;
    private final Timer blink;
    private Lay lastLay;
    private String cacheText;
    private Seq cacheRoot;
    private final Font[] fonts = new Font[3];

    public ExpressionPane() {
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        setOpaque(true);
        applyTheme(Theme.classic(), 22);

        setCaret(new DefaultCaret() {
            @Override public void paint(Graphics g) { }
            @Override protected void damage(Rectangle r) { }
            @Override protected void adjustVisibility(Rectangle r) { }
            @Override public void mousePressed(MouseEvent e) { }
            @Override public void mouseReleased(MouseEvent e) { }
            @Override public void mouseClicked(MouseEvent e) { }
            @Override public void mouseDragged(MouseEvent e) { }
        });
        getCaret().setBlinkRate(0);
        blink = new Timer(530, e -> {
            caretOn = !caretOn;
            repaint();
        });
        blink.start();

        getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { changed(); }
            @Override public void removeUpdate(DocumentEvent e) { changed(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });
        addCaretListener(e -> {
            caretOn = true;
            blink.restart();
            scrollPending = true;
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                setCaretPosition(hitTest(e.getX(), e.getY()));
            }
        });
    }

    private void changed() {
        cacheText = null;
        revalidate();
        repaint();
    }

    /** Đổi màu và cỡ chữ theo giao diện. */
    public void applyTheme(Theme t, int size) {
        this.bg = t.get("lcd");
        this.fg = t.get("lcdText");
        this.size = size;
        float f = size;
        for (int i = 0; i < fonts.length; i++) {
            fonts[i] = new Font(Font.MONOSPACED, Font.PLAIN, Math.max(9, Math.round(f)));
            f *= 0.72f;
        }
        setBackground(bg);
        setForeground(fg);
        setCaretColor(fg);
        setFont(fonts[0]);
        cacheText = null;
        revalidate();
        repaint();
    }

    /** Hàm được gọi ngay trước khi chèn chữ (từ phím bấm hoặc bàn phím), để xử lý sau dấu "=". */
    public void setInsertHook(Consumer<String> hook) {
        this.insertHook = hook;
    }

    /** Chèn chữ tại con trỏ; không cho xuống dòng. */
    @Override
    public void replaceSelection(String content) {
        if (content == null) return;
        content = content.replace("\n", "").replace("\r", "");
        if (content.isEmpty()) return;
        if (insertHook != null) insertHook.accept(content);
        super.replaceSelection(content);
    }

    /** Chèn khung phân số □/□ và đặt con trỏ vào tử số. */
    public void insertFraction() {
        // cho CalculatorUI xử lý trạng thái sau dấu "=" (xoá biểu thức cũ) trước khi lấy số đứng trước
        if (insertHook != null) insertHook.accept("" + FO);
        int pos = getCaretPosition();
        String text = getText();
        int start = getSelectionStart() == getSelectionEnd() ? operandStart(text, pos) : -1;
        if (start < 0 || start >= pos) {
            replaceSelection("" + FO + FM + FC);
            setCaretPosition(Math.max(0, getCaretPosition() - 2));
            return;
        }
        // có số hoặc nhóm ( … ) ngay trước con trỏ: đưa lên tử số, con trỏ xuống mẫu số
        String num = text.substring(start, pos);
        setText(text.substring(0, start) + FO + num + FM + FC + text.substring(pos));
        setCaretPosition(start + 1 + num.length() + 1);
    }

    /** Đầu của số hoặc nhóm ngoặc (kèm tên hàm) đứng ngay trước vị trí pos; -1 nếu không có. */
    private static int operandStart(String t, int pos) {
        if (pos <= 0 || pos > t.length()) return -1;
        char c = t.charAt(pos - 1);
        if (c == ')') {
            int depth = 0, k = pos - 1;
            for (; k >= 0; k--) {
                char d = t.charAt(k);
                if (d == ')') depth++;
                else if (d == '(' && --depth == 0) break;
            }
            if (k < 0) return -1;
            while (k > 0 && Character.isLetter(t.charAt(k - 1))) k--;
            return k;
        }
        if (Character.isDigit(c) || c == '.') {
            int st = pos;
            while (st > 0 && (Character.isDigit(t.charAt(st - 1)) || t.charAt(st - 1) == '.')) st--;
            if (st > 0 && t.charAt(st - 1) == FRAC) return -1;     // đang là mẫu của dạng a⁄b
            return st;
        }
        return -1;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport && getPreferredSize().width <= getParent().getWidth();
    }

    @Override
    public Dimension getPreferredSize() {
        Seq r = tree();
        int h = Math.round(r.asc + r.desc) + 10;
        return new Dimension(Math.round(r.w) + 10, Math.max(h, size * 2));
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
        char last = before.charAt(before.length() - 1);
        int p = before.length();
        if (last == FO) {                       // đầu tử số: xoá cả khung nếu còn rỗng, không thì ra ngoài
            int[] f = fracAt(text, p - 1);
            if (f != null && (f[0] == p || f[1] == f[0] + 1)) {
                // tử số rỗng hoặc mẫu số rỗng: bỏ khung phân số, trả nội dung về như cũ
                String num = text.substring(p, f[0]);
                String den = text.substring(f[0] + 1, f[1]);
                setText(text.substring(0, p - 1) + num + den + text.substring(f[1] + 1));
                setCaretPosition(p - 1 + num.length());
            } else {
                setCaretPosition(p - 1);
            }
            return;
        }
        if (last == FM) {                       // đầu mẫu số: nếu mẫu còn rỗng thì bỏ khung, số về lại như cũ
            int[] f = enclosing(text, p);
            if (f != null && f[1] == p - 1 && f[2] == p) {
                String num = text.substring(f[0] + 1, f[1]);
                setText(text.substring(0, f[0]) + num + text.substring(f[2] + 1));
                setCaretPosition(f[0] + num.length());
                return;
            }
            setCaretPosition(p - 1);
            return;
        }
        if (last == FC) {         // đầu mẫu số / vừa ra ngoài phân số: đi vào lại, chưa xoá
            setCaretPosition(p - 1);
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

    /** ▲ ▼ khi con trỏ đang trong phân số: nhảy giữa tử và mẫu. Trả về true nếu đã xử lý. */
    public boolean moveVertical(int dir) {
        String t = getText();
        int caret = getCaretPosition();
        int[] f = enclosing(t, caret);
        if (f == null || lastLay == null) return false;
        if (dir < 0 && caret > f[1]) {
            setCaretPosition(pick(f[0] + 1, f[1], lastLay.cx[Math.min(caret, lastLay.cx.length - 1)]));
            return true;
        }
        if (dir > 0 && caret <= f[1]) {
            setCaretPosition(pick(f[1] + 1, f[2], lastLay.cx[Math.min(caret, lastLay.cx.length - 1)]));
            return true;
        }
        return false;
    }

    private int pick(int lo, int hi, float x) {
        int best = lo;
        float bd = Float.MAX_VALUE;
        for (int i = lo; i <= hi && i < lastLay.cx.length; i++) {
            if (!lastLay.set[i]) continue;
            float d = Math.abs(lastLay.cx[i] - x);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }

    private static boolean isHidden(char c) {
        return c == CARET || c == END;
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

    /** Số mũ (chưa đóng) có phần cuối đúng tại vị trí d, hoặc null. */
    static ExpSpan exponentEndingAt(String t, int d) {
        for (int p = 0; p < t.length(); p++) {
            if (t.charAt(p) != CARET) continue;
            ExpSpan sp = exponentSpan(t, p + 1);
            if (d >= p + 1 && d == sp.end && !(sp.group && sp.closed)) return sp;
        }
        return null;
    }

    /** Khung phân số bắt đầu tại open: {vị trí FRAC_MID, vị trí FRAC_CLOSE}, null nếu thiếu. */
    static int[] fracAt(String t, int open) {
        int depth = 0, mid = -1;
        for (int i = open; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == FO) depth++;
            else if (c == FM && depth == 1 && mid < 0) mid = i;
            else if (c == FC && --depth == 0) return mid < 0 ? null : new int[]{mid, i};
        }
        return null;
    }

    /** Phân số trong cùng chứa con trỏ: {open, mid, close} hoặc null. */
    private static int[] enclosing(String t, int caret) {
        Deque<Integer> st = new ArrayDeque<>();
        int[] best = null;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == FO) st.push(i);
            else if (c == FC && !st.isEmpty()) {
                int open = st.pop();
                if (open < caret && caret <= i) {
                    int[] f = fracAt(t, open);
                    if (f != null && (best == null || open > best[0])) best = new int[]{open, f[0], f[1]};
                }
            }
        }
        return best;
    }

    // ================================================================ bố cục và vẽ
    private final class Lay {
        final float[] cx, cy;
        final int[] lv;
        final boolean[] set;
        Graphics2D g;

        Lay(int n) {
            cx = new float[n + 1];
            cy = new float[n + 1];
            lv = new int[n + 1];
            set = new boolean[n + 1];
        }

        /** Vị trí con trỏ tại chỉ số i; lần gán đầu tiên được giữ. */
        void put(int i, float x, float y, int level) {
            if (i < 0 || i >= cx.length || set[i]) return;
            set[i] = true;
            cx[i] = x;
            cy[i] = y;
            lv[i] = level;
        }
    }

    private Font font(int level) {
        return fonts[Math.min(level, fonts.length - 1)];
    }

    private FontMetrics fm(int level) {
        return getFontMetrics(font(level));
    }

    private abstract class Node {
        int start;
        float w, asc, desc;

        abstract void measure(int level);

        abstract void place(Lay L, float x, float base, int level);
    }

    private final class Ch extends Node {
        final char c;

        Ch(int i, char c) {
            this.start = i;
            this.c = c;
        }

        @Override
        void measure(int level) {
            if (isHidden(c) || c == FO || c == FM || c == FC) return;
            FontMetrics m = fm(level);
            w = m.charWidth(c);
            asc = m.getAscent();
            desc = m.getDescent();
        }

        @Override
        void place(Lay L, float x, float base, int level) {
            L.put(start, x, base, level);
            if (L.g != null && w > 0) {
                L.g.setFont(font(level));
                L.g.setColor(fg);
                L.g.drawString(String.valueOf(c), x, base);
            }
        }
    }

    private final class Seq extends Node {
        final List<Node> kids = new ArrayList<>();

        @Override
        void measure(int level) {
            w = asc = desc = 0;
            for (Node n : kids) {
                n.measure(level);
                w += n.w;
                asc = Math.max(asc, n.asc);
                desc = Math.max(desc, n.desc);
            }
        }

        @Override
        void place(Lay L, float x, float base, int level) {
            for (Node n : kids) {
                n.place(L, x, base, level);
                x += n.w;
            }
        }
    }

    private final class Exp extends Node {
        int end;
        boolean closedGroup;
        Seq body;
        float raise;

        @Override
        void measure(int level) {
            body.measure(level + 1);
            raise = Math.round(fm(level).getAscent() * 0.45f);
            w = body.w;
            asc = raise + body.asc;
            desc = Math.max(0, body.desc - raise);
        }

        @Override
        void place(Lay L, float x, float base, int level) {
            L.put(start, x, base, level);
            L.put(start + 1, x, base - raise, level + 1);
            body.place(L, x, base - raise, level + 1);
            if (!closedGroup) L.put(end, x + w, base - raise, level + 1);
        }
    }

    private final class Frac extends Node {
        int mid, close;
        Seq num, den;
        boolean numEmpty, denEmpty;
        float numW, numAsc, numDesc, denW, denAsc, denDesc, axis;
        int box;

        @Override
        void measure(int level) {
            num.measure(level + 1);
            den.measure(level + 1);
            FontMetrics m = fm(level);
            axis = Math.round(m.getAscent() * 0.33f);
            box = Math.max(6, Math.round(fm(level + 1).getAscent() * 0.8f));
            numEmpty = num.kids.isEmpty();
            denEmpty = den.kids.isEmpty();
            numW = numEmpty ? box + 2 : num.w;
            numAsc = numEmpty ? box : num.asc;
            numDesc = numEmpty ? 0 : num.desc;
            denW = denEmpty ? box + 2 : den.w;
            denAsc = denEmpty ? box : den.asc;
            denDesc = denEmpty ? 0 : den.desc;
            w = Math.max(numW, denW) + 2 * PAD;
            asc = axis + GAP + numDesc + numAsc;
            desc = Math.max(0, GAP + denAsc - axis + denDesc);
        }

        private void drawBox(Lay L, float x, float base) {
            if (L.g == null) return;
            L.g.setColor(fg);
            L.g.setStroke(new BasicStroke(1f));
            L.g.drawRect(Math.round(x) + 1, Math.round(base) - box, box - 1, box - 1);
        }

        @Override
        void place(Lay L, float x, float base, int level) {
            float inner = Math.max(numW, denW);
            if (L.g != null) {
                L.g.setColor(fg);
                L.g.setStroke(new BasicStroke(1.4f));
                L.g.draw(new Line2D.Float(x + 1, base - axis, x + w - 1, base - axis));
            }
            float nx = x + PAD + (inner - numW) / 2, nb = base - axis - GAP - numDesc;
            float dx = x + PAD + (inner - denW) / 2, db = base - axis + GAP + denAsc;
            L.put(start, x, base, level);
            L.put(start + 1, nx, nb, level + 1);
            if (numEmpty) drawBox(L, nx, nb);
            else num.place(L, nx, nb, level + 1);
            L.put(mid, nx + numW, nb, level + 1);
            L.put(mid + 1, dx, db, level + 1);
            if (denEmpty) drawBox(L, dx, db);
            else den.place(L, dx, db, level + 1);
            L.put(close, dx + denW, db, level + 1);
        }
    }

    private Seq parse(String t, int from, int to) {
        Seq s = new Seq();
        s.start = from;
        int i = from;
        while (i < to) {
            char c = t.charAt(i);
            if (c == FO) {
                int[] f = fracAt(t, i);
                if (f != null && f[1] < to) {
                    Frac fr = new Frac();
                    fr.start = i;
                    fr.mid = f[0];
                    fr.close = f[1];
                    fr.num = parse(t, i + 1, f[0]);
                    fr.den = parse(t, f[0] + 1, f[1]);
                    s.kids.add(fr);
                    i = f[1] + 1;
                    continue;
                }
            }
            if (c == CARET) {
                ExpSpan sp = exponentSpan(t, i + 1);
                int e = Math.min(sp.end, to);
                Exp ex = new Exp();
                ex.start = i;
                ex.end = e;
                ex.closedGroup = sp.group && sp.closed && sp.end <= to;
                ex.body = parse(t, i + 1, e);
                s.kids.add(ex);
                i = e;
                continue;
            }
            s.kids.add(new Ch(i, c));
            i++;
        }
        return s;
    }

    private Seq tree() {
        String t = getText();
        if (cacheRoot == null || !t.equals(cacheText)) {
            cacheRoot = parse(t, 0, t.length());
            cacheRoot.measure(0);
            cacheText = t;
        }
        return cacheRoot;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), getHeight());

        String t = getText();
        Seq root = tree();
        float base = (getHeight() - (root.asc + root.desc)) / 2f + root.asc;
        Lay L = new Lay(t.length());
        L.g = g;
        root.place(L, 2, base, 0);
        L.put(t.length(), 2 + root.w, base, 0);
        lastLay = L;

        int dot = Math.min(getCaretPosition(), t.length());
        if (caretOn && L.set[dot]) {
            FontMetrics m = fm(L.lv[dot]);
            g.setColor(getCaretColor());
            g.fillRect(Math.round(L.cx[dot]), Math.round(L.cy[dot]) - m.getAscent(), 2, m.getAscent() + m.getDescent());
        }
        if (scrollPending && L.set[dot]) {
            scrollPending = false;
            final int x = Math.round(L.cx[dot]);
            SwingUtilities.invokeLater(() -> scrollRectToVisible(new Rectangle(x - 24, 0, 48, Math.max(1, getHeight()))));
        }
        g.dispose();
    }

    /** Chỉ số con trỏ gần điểm bấm chuột nhất. */
    private int hitTest(int x, int y) {
        Lay L = lastLay;
        int best = Math.min(getCaretPosition(), getText().length());
        if (L == null) return best;
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < L.cx.length; i++) {
            if (!L.set[i]) continue;
            float mid = L.cy[i] - fm(L.lv[i]).getAscent() / 2f;
            float d = Math.abs(L.cx[i] - x) + 2 * Math.abs(mid - y);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return best;
    }
}
