package com.calculator.ui;

import com.calculator.calculator.Calculator;
import com.calculator.feature.Feature;
import com.calculator.feature.ModeNode;
import com.calculator.feature.ModeTree;
import com.calculator.model.Symbols;
import com.calculator.settings.Theme;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình chức năng kiểu Casio: menu MODE, nhập từng ô (a?, b?, ...) và trang kết quả,
 * tất cả hiện ngay trên LCD. Ô nhập dùng chính dòng nhập của máy tính nên gõ được phân số, căn, biến...
 */
public class ModeScreen {
    private enum State { OFF, MENU, INPUT, GRID, RESULT }

    private final JTextArea view = new JTextArea() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (state == State.GRID) paintGrid((Graphics2D) g);
        }
    };
    private final ExpressionPane input;
    private final JScrollPane inputScroll;
    private final ResultView result;
    private final Calculator calc;
    private final Consumer<String> actionHandler;   // "comp", "base:n"
    private final ModeNode root = ModeTree.root();

    private State state = State.OFF;
    private final Deque<ModeNode> path = new ArrayDeque<>();
    private Feature feature;
    private List<String> values = new ArrayList<>();
    private int index;
    private String[] lines = new String[0];
    private int top, capacity = 5;
    private int hInput = 40, hResult = 66;

    // nhập theo lưới ô vuông (ma trận, hệ phương trình)
    private int[] shape;                 // {hàng, cột, số lưới}
    private String[] cells = new String[0];   // nội dung gõ vào từng ô ("" = chưa nhập, coi như 0)
    private String[] shown = new String[0];   // giá trị đã tính của ô (hiện trong lưới)
    private int cur;
    private String gridError;

    public ModeScreen(ExpressionPane input, JScrollPane inputScroll, ResultView result,
                      Calculator calc, Consumer<String> actionHandler) {
        this.input = input;
        this.inputScroll = inputScroll;
        this.result = result;
        this.calc = calc;
        this.actionHandler = actionHandler;
        view.setEditable(false);
        view.setFocusable(false);
        view.setHighlighter(null);
        view.setTabSize(6);
        view.setOpaque(true);
        view.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        view.setBorder(null);
        view.setVisible(false);
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { liveRepaint(); }
            @Override public void removeUpdate(DocumentEvent e) { liveRepaint(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });
    }

    private void liveRepaint() {
        if (state == State.GRID) view.repaint();
    }

    public JComponent getView() { return view; }

    public boolean isActive() { return state != State.OFF; }

    /** Chữ hiện trên dòng trạng thái của LCD. */
    public String label() {
        switch (state) {
            case MENU: return "MODE";
            case INPUT: case GRID: case RESULT: return feature.getName();
            default: return "";
        }
    }

    public void applyTheme(Theme t, int size) {
        view.setBackground(t.get("lcd"));
        view.setForeground(t.get("lcdText"));
        view.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(12, size - 6)));
    }

    // ================================================================ vào / ra
    /** Phím MODE: mở menu gốc (bấm lần nữa khi đang ở menu gốc thì thoát). */
    public void openMenu() {
        if (state == State.MENU && path.size() == 1) {
            exit();
            return;
        }
        enter();
        path.clear();
        path.push(root);
        state = State.MENU;
        showMenu();
    }

    private void enter() {
        if (state != State.OFF) return;
        if (inputScroll.getHeight() > 0) hInput = inputScroll.getHeight();
        if (result.getHeight() > 0) hResult = result.getHeight();
    }

    public void exit() {
        state = State.OFF;
        view.setVisible(false);
        inputScroll.setVisible(true);
        result.setVisible(true);
        relayout();
    }

    private void layout(boolean full, int minLines) {
        FontMetrics fm = view.getFontMetrics(view.getFont());
        int lh = fm.getHeight();
        int h = Math.max(full ? hInput + hResult : hResult, lh * minLines + 6);
        capacity = Math.max(3, h / lh - 1);
        Dimension d = new Dimension(100, h);
        view.setPreferredSize(d);
        view.setMinimumSize(d);
        view.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        view.setVisible(true);
        inputScroll.setVisible(!full);
        result.setVisible(false);
        relayout();
    }

    private void relayout() {
        if (view.getParent() != null) {
            view.getParent().revalidate();
            view.getParent().repaint();
        }
    }

    // ================================================================ phím bấm
    /** Trả về true nếu phím đã được màn hình chức năng xử lý (không chạy hành động thường nữa). */
    public boolean handleKey(String id) {
        if (state == State.OFF) return false;
        if (id.equals("mode") || id.equals("settings") || id.equals("angle") || id.equals("base")) return false;
        switch (state) {
            case MENU:
                if (id.matches("d[1-9]")) select(id.charAt(1) - '0');
                else if (id.equals("ac")) exit();
                else if (id.equals("del")) up();
                return true;
            case GRID:
                if (id.equals("equals")) {
                    submitGrid();
                    return true;
                }
                if (id.equals("ac")) {
                    if (input.getText().isEmpty()) exit();
                    else input.setText("");
                    return true;
                }
                if (id.equals("del") && input.getText().isEmpty()) {
                    if (cur > 0) goCell(cur - 1);
                    return true;
                }
                return false;
            case INPUT:
                if (id.equals("equals")) {
                    submit();
                    return true;
                }
                if (id.equals("ac")) {
                    if (input.getText().isEmpty()) exit();
                    else input.setText("");
                    return true;
                }
                if (id.equals("del") && input.getText().isEmpty() && index > 0) {
                    index--;
                    prepareField(null);
                    return true;
                }
                return false;
            default:    // RESULT
                if (id.equals("equals")) restart();
                else if (id.equals("ac")) exit();
                return true;
        }
    }

    /** Phím ▲ ▼: cuộn trang kết quả. Trả về true nếu đã xử lý. */
    public boolean scroll(int delta) {
        if (state == State.GRID) {
            if (!input.moveVertical(delta)) moveRow(delta);     // trong phân số thì nhảy tử/mẫu trước
            return true;
        }
        if (state == State.RESULT) {
            top = Math.max(0, Math.min(Math.max(0, lines.length - capacity), top + delta));
            renderResult();
            return true;
        }
        return state != State.OFF;
    }

    /** Phím ◄ ►: con trỏ đã ở mép ô nhập thì sang ô kế bên. Trả về true nếu đã chuyển ô. */
    public boolean horizontal(int delta) {
        if (state != State.GRID) return false;
        goCell(cur + delta);
        return true;
    }

    // ================================================================ menu
    private void up() {
        if (path.size() > 1) path.pop();
        else {
            exit();
            return;
        }
        showMenu();
    }

    private void select(int n) {
        ModeNode node = path.peek();
        if (n > node.children.size()) return;
        ModeNode child = node.children.get(n - 1);
        if (child.action != null) {
            exit();
            actionHandler.accept(child.action);
        } else if (child.feature != null) {
            values = new ArrayList<>();
            start(child.feature);
        } else {
            path.push(child);
            showMenu();
        }
    }

    private void showMenu() {
        StringBuilder crumb = new StringBuilder();
        List<ModeNode> p = new ArrayList<>(path);
        for (int i = p.size() - 1; i >= 0; i--) crumb.append(i < p.size() - 1 ? " › " : "").append(p.get(i).title);
        List<ModeNode> items = path.peek().children;
        StringBuilder sb = new StringBuilder(crumb).append("\n");
        for (int i = 0; i < items.size(); i += 2) {
            sb.append(String.format("%-22s", (i + 1) + ":" + items.get(i).title));
            if (i + 1 < items.size()) sb.append(i + 2).append(":").append(items.get(i + 1).title);
            sb.append("\n");
        }
        layout(true, items.size() / 2 + 3);
        view.setText(sb.toString());
    }

    // ================================================================ nhập từng ô
    private void start(Feature f) {
        feature = f;
        if (f.grid() != null) {
            startGrid(false);
            return;
        }
        index = 0;
        state = State.INPUT;
        layout(false, 3);
        prepareField(null);
    }

    // ================================================================ nhập theo lưới
    private int total() { return shape[0] * shape[1] * shape[2]; }

    private void startGrid(boolean keep) {
        shape = feature.grid();
        if (!keep || cells.length != total()) {
            cells = new String[total()];
            shown = new String[total()];
            Arrays.fill(cells, "");
        }
        values = new ArrayList<>();
        index = 0;
        cur = 0;
        gridError = null;
        state = State.GRID;
        FontMetrics fm = view.getFontMetrics(view.getFont());
        int lh = fm.getHeight();
        int need = 2 * lh + shape[0] * (lh + 8) + (feature.gridNames() != null ? lh : 0) + 16;
        layout(false, (need + lh - 1) / lh);
        loadCell();
    }

    private void loadCell() {
        view.setText(feature.getName() + "\n" + feature.prompt(cur, values) + " ?"
                + (gridError == null ? "" : "  " + gridError));
        input.setText(cells[cur]);
        input.setCaretPosition(input.getText().length());
        view.repaint();
    }

    /** Lưu ô đang sửa (kiểm tra hợp lệ). Sai thì báo lỗi trên LCD và ở lại ô đó. */
    private boolean commitCell() {
        String t = input.getText().trim();
        if (t.isEmpty()) {
            cells[cur] = "";
            shown[cur] = null;
            return true;
        }
        try {
            feature.check(cur, t, calc);
            double v = calc.evaluate(t);
            String fr = Calculator.toFraction(v);
            cells[cur] = t;
            shown[cur] = fr != null ? fr : Calculator.format(v);
            return true;
        } catch (Exception ex) {
            gridError = ex.getMessage() == null ? "Syntax ERROR" : ex.getMessage();
            view.setText(feature.getName() + "\n" + feature.prompt(cur, values) + " ?  " + gridError);
            view.repaint();
            return false;
        }
    }

    private void goCell(int target) {
        if (target < 0 || target >= total() || target == cur) return;
        if (!commitCell()) return;
        cur = target;
        gridError = null;
        loadCell();
    }

    /** ▲ ▼: sang hàng trên / dưới trong cùng một lưới. */
    private void moveRow(int d) {
        int per = shape[0] * shape[1], local = cur % per, row = local / shape[1];
        int nr = row + d;
        if (nr < 0 || nr >= shape[0]) return;
        goCell(cur + d * shape[1]);
    }

    private void submitGrid() {
        if (!commitCell()) return;
        gridError = null;
        if (cur + 1 < total()) {
            cur++;
            loadCell();
            return;
        }
        values = new ArrayList<>();
        for (String c : cells) values.add(c.isEmpty() ? "0" : c);
        index = values.size();
        finish();
    }

    /** Chữ hiện trong ô khi đang gõ: bỏ ký tự ẩn, khung phân số thành a/b. */
    private static String plain(String s) {
        StringBuilder sb = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (ch == Symbols.FRAC_MID || ch == Symbols.FRACTION_SLASH) sb.append('/');
            else if (ch == Symbols.FRAC_OPEN || ch == Symbols.FRAC_CLOSE || ch == Symbols.EXPONENT_END) continue;
            else if (Symbols.isTemplateOpen(ch)) sb.append('(');
            else if (ch == Symbols.TPL_SEP) sb.append(';');
            else if (ch == Symbols.TPL_CLOSE) sb.append(')');
            else sb.append(ch);
        }
        return sb.toString();
    }

    private void paintGrid(Graphics2D g) {
        if (shape == null || cells.length != total()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font base = view.getFont();
        FontMetrics fm = g2.getFontMetrics(base);
        int lh = fm.getHeight(), rows = shape[0], cols = shape[1], count = shape[2];
        int ch = lh + 8, gap = 16, margin = 6;
        int avail = view.getWidth() - 2 * margin - gap * (count - 1);
        int cw = Math.max(24, Math.min(Math.max(ch, 58), avail / (cols * count)));
        int totalW = cw * cols * count + gap * (count - 1);
        int x0 = Math.max(margin, (view.getWidth() - totalW) / 2);
        String[] names = feature.gridNames();
        int top = 2 * lh + 10 + (names != null ? lh : 0);
        Color fg = view.getForeground(), bg = view.getBackground();
        Color dim = new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 110);
        int per = rows * cols;

        for (int gi = 0; gi < count; gi++) {
            int gx = x0 + gi * (cols * cw + gap);
            if (names != null && gi < names.length) {
                g2.setFont(base);
                g2.setColor(fg);
                g2.drawString(names[gi], gx + (cols * cw - fm.stringWidth(names[gi])) / 2, top - 6);
            }
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int idx = gi * per + r * cols + c;
                    int x = gx + c * cw, y = top + r * ch;
                    boolean active = idx == cur;
                    if (active) {
                        g2.setColor(fg);
                        g2.fillRect(x + 1, y + 1, cw - 2, ch - 2);
                    }
                    g2.setColor(active ? fg : dim);
                    g2.setStroke(new BasicStroke(active ? 2f : 1.2f));
                    g2.drawRect(x, y, cw, ch);

                    String text;
                    boolean empty = false;
                    if (active) text = plain(input.getText());
                    else if (shown[idx] != null) text = shown[idx];
                    else {
                        text = "0";
                        empty = true;
                    }
                    Font f = base;
                    FontMetrics tf = fm;
                    while (tf.stringWidth(text) > cw - 8 && f.getSize() > 8) {
                        f = f.deriveFont((float) f.getSize() - 1);
                        tf = g2.getFontMetrics(f);
                    }
                    g2.setFont(f);
                    g2.setColor(active ? bg : empty ? dim : fg);
                    g2.drawString(text, x + (cw - tf.stringWidth(text)) / 2, y + (ch + tf.getAscent() - tf.getDescent()) / 2);
                }
            }
            if (feature.gridAugmented() && cols > 1) {          // vạch ngăn trước cột hệ số tự do
                g2.setColor(fg);
                g2.setStroke(new BasicStroke(3f));
                int vx = gx + (cols - 1) * cw;
                g2.drawLine(vx, top - 3, vx, top + rows * ch + 3);
            }
        }
        g2.dispose();
    }

    private void restart() {
        if (feature.grid() != null) startGrid(true);      // giữ lại số đã nhập để sửa
        else start(feature);
    }

    private void prepareField(String error) {
        String p = feature.prompt(index, values);
        if (p == null) {
            finish();
            return;
        }
        view.setText(feature.getName() + "\n" + p + " ?" + (error == null ? "" : "\n" + error));
        input.setText(index < values.size() ? values.get(index) : "");
        input.setCaretPosition(input.getText().length());
    }

    private void submit() {
        String t = input.getText().trim();
        if (t.isEmpty()) return;
        try {
            feature.check(index, t, calc);
        } catch (RuntimeException ex) {
            prepareFieldKeep(t, ex.getMessage());
            return;
        } catch (Exception ex) {
            prepareFieldKeep(t, ex.getMessage());
            return;
        }
        if (index < values.size()) values.set(index, t);
        else values.add(t);
        index++;
        prepareField(null);
    }

    private void prepareFieldKeep(String text, String error) {
        String p = feature.prompt(index, values);
        view.setText(feature.getName() + "\n" + p + " ?\n" + (error == null ? "Syntax ERROR" : error));
        input.setText(text);
        input.setCaretPosition(text.length());
    }

    // ================================================================ kết quả
    private void finish() {
        while (values.size() > index) values.remove(values.size() - 1);
        String out;
        try {
            out = feature.solve(values, calc);
        } catch (Exception ex) {
            out = ex.getMessage() == null ? "Math ERROR" : ex.getMessage();
        }
        lines = out.split("\n");
        top = 0;
        state = State.RESULT;
        input.setText("");
        layout(true, 6);
        renderResult();
    }

    private void renderResult() {
        boolean more = top + capacity < lines.length;
        String arrows = (top > 0 ? " ▲" : "") + (more ? " ▼" : "");
        StringBuilder sb = new StringBuilder(feature.getName()).append(arrows).append("\n");
        sb.append(String.join("\n", Arrays.copyOfRange(lines, top, Math.min(lines.length, top + capacity))));
        view.setText(sb.toString());
    }
}
