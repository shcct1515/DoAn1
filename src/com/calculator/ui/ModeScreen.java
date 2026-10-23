package com.calculator.ui;

import com.calculator.calculator.Calculator;
import com.calculator.feature.Feature;
import com.calculator.feature.ModeNode;
import com.calculator.feature.ModeTree;
import com.calculator.settings.Theme;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
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
    private enum State { OFF, MENU, INPUT, RESULT }

    private final JTextArea view = new JTextArea();
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
    }

    public JComponent getView() { return view; }

    public boolean isActive() { return state != State.OFF; }

    /** Chữ hiện trên dòng trạng thái của LCD. */
    public String label() {
        switch (state) {
            case MENU: return "MODE";
            case INPUT: case RESULT: return feature.getName();
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
        if (state == State.RESULT) {
            top = Math.max(0, Math.min(Math.max(0, lines.length - capacity), top + delta));
            renderResult();
            return true;
        }
        return state != State.OFF;
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
        index = 0;
        state = State.INPUT;
        layout(false, 3);
        prepareField(null);
    }

    private void restart() {
        start(feature);
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
