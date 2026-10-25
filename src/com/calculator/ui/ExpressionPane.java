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
import java.awt.geom.Path2D;
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

    private static final char TS = Symbols.TPL_SEP;
    private static final char TC = Symbols.TPL_CLOSE;

    private static final int PAD = 3, GAP = 2;

    /** Ký tự khung (ẩn, không có độ rộng): khung phân số và khung các mẫu ∫, d/dx, Σ, Π, log. */
    private static boolean isFrame(char c) {
        return c == FO || c == FM || c == FC || Symbols.isTemplateOpen(c) || c == TS || c == TC;
    }

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

    /** Chèn mẫu ∫ / d/dx / Σ / Π / log cơ số (các ô trống □), con trỏ vào ô đầu tiên. */
    public void insertTemplate(char open) {
        if (insertHook != null) insertHook.accept("" + open);
        StringBuilder sb = new StringBuilder().append(open);
        for (int i = 1; i < Symbols.slotCount(open); i++) sb.append(TS);
        sb.append(TC);
        int pos = getSelectionStart();
        replaceSelection(sb.toString());
        // ∫, Σ, Π: con trỏ vào ô biểu thức (ô cuối, sau hai cận); các mẫu khác vào ô đầu
        setCaretPosition(pos + (Symbols.slotCount(open) == 3 ? 3 : 1));
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
        if (Symbols.isTemplateOpen(last)) {     // đầu ô đầu tiên của mẫu: mẫu còn trống thì xoá cả mẫu
            int[] sc = tplScan(text, p - 1);
            if (sc != null && text.substring(p, sc[sc.length - 1]).replace(String.valueOf(TS), "").isEmpty()) {
                setText(text.substring(0, p - 1) + text.substring(sc[sc.length - 1] + 1));
            }
            setCaretPosition(p - 1);
            return;
        }
        if (last == TS || last == TC) {         // lùi về cuối ô trước / vào lại ô cuối của mẫu
            setCaretPosition(p - 1);
            return;
        }
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
        int[] tp = enclosingTpl(t, caret);
        if (tp != null && (f == null || tp[0] > f[0]) && lastLay != null) {      // trong ∫, d/dx, Σ, Π, log
            moveInTemplate(t, caret, tp, dir);
            return true;
        }
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

    /** ▲ ▼ trong mẫu: nhảy giữa các ô theo đúng vị trí trên màn hình (cận trên, biểu thức, cận dưới...). */
    private void moveInTemplate(String t, int caret, int[] tp, int dir) {
        char kind = t.charAt(tp[0]);
        int[] order;                                            // thứ tự các ô từ trên xuống dưới
        if (kind == Symbols.DER_OPEN) order = new int[]{0, 1};
        else if (kind == Symbols.LOG_OPEN) order = new int[]{1, 0};
        else order = new int[]{1, 2, 0};           // cận trên, biểu thức, cận dưới
        int at = 0;
        for (int k = 0; k < order.length; k++) if (order[k] == tp[1]) at = k;
        int target = order[Math.max(0, Math.min(order.length - 1, at + dir))];
        if (target == tp[1]) return;
        int lo = target == 0 ? tp[0] + 1 : tp[2 + target - 1] + 1, hi = tp[2 + target];
        setCaretPosition(pick(lo, hi, lastLay.cx[Math.min(caret, lastLay.cx.length - 1)]));
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

    /** Vị trí các ký tự ngăn ô rồi ký tự đóng của mẫu mở tại open; null nếu thiếu hoặc sai số ô. */
    static int[] tplScan(String t, int open) {
        int need = Symbols.slotCount(t.charAt(open)) - 1;
        List<Integer> seps = new ArrayList<>();
        int depth = 0;
        for (int i = open; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Symbols.isTemplateOpen(c)) depth++;
            else if (c == TS && depth == 1) seps.add(i);
            else if (c == TC && --depth == 0) {
                if (seps.size() != need) return null;
                int[] r = new int[need + 1];
                for (int k = 0; k < need; k++) r[k] = seps.get(k);
                r[need] = i;
                return r;
            }
        }
        return null;
    }

    /** Mẫu trong cùng chứa con trỏ: {vị trí mở, chỉ số ô, các vị trí ngăn..., vị trí đóng} hoặc null. */
    private static int[] enclosingTpl(String t, int caret) {
        int best = -1;
        for (int i = 0; i < t.length(); i++) {
            if (!Symbols.isTemplateOpen(t.charAt(i))) continue;
            int[] sc = tplScan(t, i);
            if (sc != null && i < caret && caret <= sc[sc.length - 1]) best = i;     // mẫu mở sau = lồng bên trong
        }
        if (best < 0) return null;
        int[] sc = tplScan(t, best);
        int slot = 0;
        while (slot < sc.length - 1 && caret > sc[slot]) slot++;
        int[] r = new int[sc.length + 2];
        r[0] = best;
        r[1] = slot;
        System.arraycopy(sc, 0, r, 2, sc.length);
        return r;
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
            if (isHidden(c) || isFrame(c)) return;
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

    /** Mẫu nhiều ô giống máy Casio: ∫, d/dx, Σ, Π, log cơ số. Ô trống hiện □. */
    private final class Tpl extends Node {
        final char kind;
        final int n;
        final int[] pos;                 // vị trí các ký tự ngăn ô, rồi ký tự đóng
        final Seq[] slot;
        final int[] lv, box;
        final boolean[] emp;
        final float[] sw, sa, sd;
        // hình học tính trong measure()
        float fa, fd, axis, ba, bd, symW, boundW, colW, signW, signH, signBase, pw, subOff, dxW, fracW, lblW;
        float upBase, lowBase, subW;

        Tpl(int start, char kind, int[] pos) {
            this.start = start;
            this.kind = kind;
            this.pos = pos;
            n = pos.length;
            slot = new Seq[n];
            lv = new int[n];
            box = new int[n];
            emp = new boolean[n];
            sw = new float[n];
            sa = new float[n];
            sd = new float[n];
        }

        int slotStart(int i) { return i == 0 ? start + 1 : pos[i - 1] + 1; }

        int slotEnd(int i) { return pos[i]; }

        @Override
        void measure(int level) {
            FontMetrics m = fm(level);
            fa = m.getAscent();
            fd = m.getDescent();
            axis = Math.round(fa * 0.33f);
            for (int i = 0; i < n; i++) {
                boolean small = kind == Symbols.LOG_OPEN ? i == 0 : kind == Symbols.DER_OPEN ? i > 0 : i < 2;   // cận, điểm x, cơ số: chữ nhỏ
                lv[i] = small ? level + 1 : level;
                slot[i].measure(lv[i]);
                emp[i] = slot[i].kids.isEmpty();
                box[i] = Math.max(6, Math.round(fm(lv[i]).getAscent() * 0.8f));
                sw[i] = emp[i] ? box[i] + 2 : slot[i].w;
                sa[i] = emp[i] ? box[i] : slot[i].asc;
                sd[i] = emp[i] ? 0 : slot[i].desc;
            }
            int bi = kind == Symbols.LOG_OPEN ? 1 : kind == Symbols.DER_OPEN ? 0 : 2;     // ô "thân" nằm trên dòng chính
            ba = Math.max(sa[bi], fa);
            bd = Math.max(sd[bi], fd);
            pw = Math.max(4, Math.round((ba + bd) * 0.16f));
            switch (kind) {
                case Symbols.INT_OPEN: {
                    float h = ba + bd + 4;
                    h = Math.max(h, sa[1] + sd[1] + sa[0] + sd[0] + 4);
                    float e = (h - (ba + bd + 4)) / 2;
                    asc = ba + 2 + e;
                    desc = bd + 2 + e;
                    symW = Math.max(7, Math.round(h * 0.30f));
                    boundW = Math.max(sw[0], sw[1]);
                    dxW = m.stringWidth("dx");
                    w = symW + 2 + boundW + 3 + sw[2] + 3 + dxW + 2;
                    break;
                }
                case Symbols.DER_OPEN: {
                    dxW = m.stringWidth("dx");
                    fracW = dxW + 4;
                    lblW = fm(level + 1).stringWidth("x=");
                    subOff = Math.round(Math.max(bd, fd) + sa[1] * 0.35f);
                    float fracAsc = axis + 2 + fd + fa, fracDesc = Math.max(0, -axis + 2 + fa + fd);
                    asc = Math.max(ba, fracAsc) + 1;
                    desc = Math.max(Math.max(bd, fracDesc), subOff + sd[1]) + 1;
                    w = fracW + 3 + pw + 2 + sw[0] + 2 + pw + 4 + 2 + lblW + sw[1] + 2;
                    break;
                }
                case Symbols.LOG_OPEN: {
                    lblW = m.stringWidth("log");
                    subOff = Math.round(fd * 0.6f + sa[0] * 0.45f);
                    asc = ba + 1;
                    desc = Math.max(bd, subOff + sd[0]) + 1;
                    w = lblW + 1 + sw[0] + 2 + pw + 2 + sw[1] + 2 + pw + 1;
                    break;
                }
                default: {                                                       // Σ, Π
                    signH = Math.round(fa * 1.3f);
                    Font sf = font(level).deriveFont(Font.PLAIN, signH / 0.72f);
                    signW = getFontMetrics(sf).stringWidth(kind == Symbols.SUM_OPEN ? "Σ" : "Π");
                    lblW = fm(level + 1).stringWidth("x=");
                    colW = Math.max(signW, Math.max(lblW + sw[0], sw[1]));
                    signBase = -axis + signH / 2;                                // so với đường cơ sở
                    upBase = signBase - signH - 2 - sd[1];
                    lowBase = signBase + 2 + sa[0];
                    asc = Math.max(ba, -(upBase - sa[1])) + 1;
                    desc = Math.max(bd, lowBase + sd[0]) + 1;
                    w = colW + 3 + pw + 2 + sw[2] + 2 + pw + 2;
                }
            }
        }

        private void slotAt(Lay L, int i, float x, float base) {
            L.put(slotStart(i), x, base, lv[i]);
            if (emp[i]) {
                if (L.g != null) {
                    L.g.setColor(fg);
                    L.g.setStroke(new BasicStroke(1f));
                    L.g.drawRect(Math.round(x) + 1, Math.round(base) - box[i], box[i] - 1, box[i] - 1);
                }
            } else {
                slot[i].place(L, x, base, lv[i]);
            }
            L.put(slotEnd(i), x + sw[i], base, lv[i]);
        }

        private void text(Lay L, String s, int level, float x, float base) {
            if (L.g == null) return;
            L.g.setFont(font(level));
            L.g.setColor(fg);
            L.g.drawString(s, x, base);
        }

        private void paren(Lay L, float x, float base, boolean left) {
            if (L.g == null) return;
            float top = base - ba - 1, bot = base + bd + 1, mid = (top + bot) / 2;
            Path2D.Float p = new Path2D.Float();
            if (left) {
                p.moveTo(x + pw, top);
                p.quadTo(x - pw * 0.15f, mid, x + pw, bot);
            } else {
                p.moveTo(x, top);
                p.quadTo(x + pw * 1.15f, mid, x, bot);
            }
            L.g.setColor(fg);
            L.g.setStroke(new BasicStroke(1.3f));
            L.g.draw(p);
        }

        @Override
        void place(Lay L, float x, float base, int level) {
            L.put(start, x, base, level);
            switch (kind) {
                case Symbols.INT_OPEN: {
                    float top = base - asc, bot = base + desc;
                    if (L.g != null) {                                           // dấu tích phân
                        float h = bot - top;
                        Path2D.Float p = new Path2D.Float();
                        p.moveTo(x + symW * 0.95f, top + h * 0.12f);
                        p.curveTo(x + symW * 0.90f, top + h * 0.01f, x + symW * 0.62f, top - h * 0.01f, x + symW * 0.57f, top + h * 0.2f);
                        p.lineTo(x + symW * 0.43f, top + h * 0.8f);
                        p.curveTo(x + symW * 0.38f, top + h * 1.01f, x + symW * 0.10f, top + h * 0.99f, x + symW * 0.05f, top + h * 0.88f);
                        L.g.setColor(fg);
                        L.g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        L.g.draw(p);
                    }
                    float bx = x + symW + 2;
                    slotAt(L, 1, bx, top + sa[1]);              // cận trên
                    slotAt(L, 0, bx, bot - sd[0]);              // cận dưới
                    float cx = bx + boundW + 3;
                    slotAt(L, 2, cx, base);                     // biểu thức
                    text(L, "dx", level, cx + sw[2] + 3, base);
                    break;
                }
                case Symbols.DER_OPEN: {
                    if (L.g != null) {                                           // d/dx dạng phân số nhỏ
                        L.g.setColor(fg);
                        L.g.setStroke(new BasicStroke(1.2f));
                        L.g.draw(new Line2D.Float(x, base - axis, x + fracW, base - axis));
                    }
                    text(L, "d", level, x + (fracW - fm(level).stringWidth("d")) / 2, base - axis - 2 - fd);
                    text(L, "dx", level, x + 2, base - axis + 2 + fa);
                    float cx = x + fracW + 3;
                    paren(L, cx, base, true);
                    slotAt(L, 0, cx + pw + 2, base);
                    float rx = cx + pw + 2 + sw[0] + 2;
                    paren(L, rx, base, false);
                    float bar = rx + pw + 4;
                    if (L.g != null) {
                        L.g.setColor(fg);
                        L.g.setStroke(new BasicStroke(1.3f));
                        L.g.draw(new Line2D.Float(bar, base - ba, bar, base + subOff));
                    }
                    text(L, "x=", level + 1, bar + 2, base + subOff);
                    slotAt(L, 1, bar + 2 + lblW, base + subOff);
                    break;
                }
                case Symbols.LOG_OPEN: {
                    text(L, "log", level, x, base);
                    slotAt(L, 0, x + lblW + 1, base + subOff);
                    float cx = x + lblW + 1 + sw[0] + 2;
                    paren(L, cx, base, true);
                    slotAt(L, 1, cx + pw + 2, base);
                    paren(L, cx + pw + 2 + sw[1] + 2, base, false);
                    break;
                }
                default: {                                                       // Σ, Π
                    if (L.g != null) {
                        Font sf = font(level).deriveFont(Font.PLAIN, signH / 0.72f);
                        L.g.setFont(sf);
                        L.g.setColor(fg);
                        L.g.drawString(kind == Symbols.SUM_OPEN ? "Σ" : "Π", x + (colW - signW) / 2, base + signBase);
                    }
                    slotAt(L, 1, x + (colW - sw[1]) / 2, base + upBase);          // đến
                    float lx = x + (colW - (lblW + sw[0])) / 2;
                    text(L, "x=", level + 1, lx, base + lowBase);
                    slotAt(L, 0, lx + lblW, base + lowBase);                       // từ
                    float cx = x + colW + 3;
                    paren(L, cx, base, true);
                    slotAt(L, 2, cx + pw + 2, base);                               // biểu thức
                    paren(L, cx + pw + 2 + sw[2] + 2, base, false);
                }
            }
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
            if (Symbols.isTemplateOpen(c)) {
                int[] sc = tplScan(t, i);
                if (sc != null && sc[sc.length - 1] < to) {
                    Tpl tp = new Tpl(i, c, sc);
                    for (int k = 0; k < tp.n; k++) tp.slot[k] = parse(t, tp.slotStart(k), tp.slotEnd(k));
                    s.kids.add(tp);
                    i = sc[sc.length - 1] + 1;
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
