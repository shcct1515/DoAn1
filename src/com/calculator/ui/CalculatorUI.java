package com.calculator.ui;

import com.calculator.calculator.Calculator;
import com.calculator.calculator.ComplexMath;
import com.calculator.model.CalcException;
import com.calculator.model.DisplayMode;
import com.calculator.model.Symbols;
import com.calculator.settings.Settings;
import com.calculator.settings.Theme;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Giao diện máy tính CASIO FX-570 bằng Java Swing.
 *
 * Bố cục: màn hình LCD / hàng điều khiển [SHIFT][ALPHA] (REPLAY) [MODE][SETTING] / bàn phím.
 * MODE mở menu tính năng mở rộng, SETTING mở cài đặt (giao diện, phím tắt, ...).
 */
public class CalculatorUI extends JFrame {

    /** Loại phím, quyết định màu theo giao diện. */
    private enum Kind {
        FN("fn"), NUM("num"), OP("op"), CLEAR("clear"), EQUAL("equal"), SHIFT("shift"), ALPHA("alpha");

        final String themeKey;

        Kind(String themeKey) {
            this.themeKey = themeKey;
        }
    }

    private static final String FRAC = String.valueOf(Symbols.FRACTION_SLASH);
    private static final String EXP_END = String.valueOf(Symbols.EXPONENT_END);
    private static final String[] BASE_NAMES = {"DEC", "HEX", "BIN", "OCT"};
    private static final int[] BASE_VALUES = {10, 16, 2, 8};

    private final Settings settings = new Settings();
    private final Calculator calc = new Calculator();
    private final ExpressionPane input = new ExpressionPane();
    private final ResultView result = new ResultView();
    private final JLabel status = new JLabel(" ");
    private final List<KeyButton> keys = new ArrayList<>();
    private final Map<String, ActionEntry> actions = new LinkedHashMap<>();
    private final List<KeyStroke> installedShortcuts = new ArrayList<>();
    private final List<String> history = new ArrayList<>();

    private JPanel root, keypad, lcdPanel;
    private JScrollPane inputScroll;
    private ReplayKey replay;
    private KeyButton shiftKey, alphaKey, modeKey, settingsKey, angleKey, baseKey;
    private SettingsDialog settingsDialog;
    private ModeScreen screen;

    private boolean shift = false;
    private boolean alpha = false;
    private boolean pendingStore = false;
    private double storeValue = 0;
    private boolean justEvaluated = false;
    private boolean fractionView = false;
    private boolean complexMode = false;
    private double[] lastComplex = null;
    private int baseIndex = 0;
    private int historyPos = 0;
    private Double lastResult = null;
    private String statusNote = "";

    public CalculatorUI() {
        super("CASIO fx-570 Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(buildScreen(), BorderLayout.CENTER);
        top.add(buildControlRow(), BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);
        root.add(buildKeypad(), BorderLayout.CENTER);
        setContentPane(root);

        input.setInsertHook(this::beforeInsert);
        settings.addListener(this::applySettings);
        applySettings();

        setMinimumSize(new Dimension(420, 700));
        setSize(460, 800);
        setLocationRelativeTo(null);
    }

    // ================================================================ màn hình LCD
    private JPanel buildScreen() {
        lcdPanel = new JPanel();
        lcdPanel.setLayout(new BoxLayout(lcdPanel, BoxLayout.Y_AXIS));

        status.setFont(new Font("Dialog", Font.BOLD, 11));
        status.setAlignmentX(LEFT_ALIGNMENT);

        inputScroll = new JScrollPane(input,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.setAlignmentX(LEFT_ALIGNMENT);

        result.setAlignmentX(LEFT_ALIGNMENT);

        screen = new ModeScreen(input, inputScroll, result, calc, this::modeAction);
        lcdPanel.add(status);
        lcdPanel.add(screen.getView());
        lcdPanel.add(inputScroll);
        lcdPanel.add(result);
        return lcdPanel;
    }

    // ================================================================ hàng điều khiển (theo bản phác thảo)
    private JPanel buildControlRow() {
        shiftKey = createKey("shift", "SHIFT", Kind.SHIFT, null).title("SHIFT");
        alphaKey = createKey("alpha", "ALPHA", Kind.ALPHA, null).title("ALPHA");
        modeKey = createKey("mode", "MODE", Kind.FN, screen::openMenu).title("MODE (menu tính năng)");
        settingsKey = createKey("settings", "SETTING", Kind.FN, this::openSettings).title("SETTING (cài đặt)");

        addAction("left", "◄  Con trỏ sang trái", () -> moveCaretOrCell(-1));
        addAction("right", "►  Con trỏ sang phải", () -> moveCaretOrCell(1));
        addAction("up", "▲  Biểu thức trước (lịch sử)", this::historyUp);
        addAction("down", "▼  Biểu thức sau (lịch sử)", this::historyDown);

        replay = new ReplayKey(d -> {
            switch (d) {
                case LEFT:  actions.get("left").run(); break;
                case RIGHT: actions.get("right").run(); break;
                case UP:    actions.get("up").run(); break;
                default:    actions.get("down").run(); break;
            }
        });

        JPanel leftPair = new JPanel(new GridLayout(1, 2, 6, 0));
        leftPair.setOpaque(false);
        leftPair.add(shiftKey.button);
        leftPair.add(alphaKey.button);

        JPanel rightPair = new JPanel(new GridLayout(1, 2, 6, 0));
        rightPair.setOpaque(false);
        rightPair.add(modeKey.button);
        rightPair.add(settingsKey.button);

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1;
        row.add(leftPair, c);
        c.gridx = 1;
        c.weightx = 0;
        c.insets = new Insets(0, 12, 0, 12);
        row.add(replay, c);
        c.gridx = 2;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        row.add(rightPair, c);
        row.setPreferredSize(new Dimension(100, 48));
        return row;
    }

    // ================================================================ bàn phím
    private JPanel buildKeypad() {
        keypad = new JPanel(new GridLayout(9, 6, 5, 5));
        JPanel p = keypad;

        // Hàng 0: mẫu nhập nhiều ô như máy thật (ô trống hiện □), biến x
        add(p, "integral", "∫", Kind.FN, () -> input.insertTemplate(Symbols.INT_OPEN))
                .title("Tích phân ∫ (biểu thức, cận dưới, cận trên) dx — ► để sang ô kế, ▲ ▼ đổi ô");
        add(p, "deriv", "d/dx", Kind.FN, () -> input.insertTemplate(Symbols.DER_OPEN))
                .title("Đạo hàm tại một điểm: d/dx (biểu thức) |x= điểm");
        add(p, "sigma", "Σ", Kind.FN, () -> input.insertTemplate(Symbols.SUM_OPEN))
                .title("Tổng Σ (biểu thức, từ x=, đến)");
        add(p, "prod", "Π", Kind.FN, () -> input.insertTemplate(Symbols.PROD_OPEN))
                .title("Tích Π (biểu thức, từ x=, đến)");
        add(p, "logbase", "logₐ", Kind.FN, () -> input.insertTemplate(Symbols.LOG_OPEN))
                .title("Logarit cơ số bất kỳ: log (cơ số) (đối số)");
        add(p, "varx", "x", Kind.FN, ins("X")).title("Biến x (hiện là X) dùng trong ∫, d/dx, Σ, Π");

        // Hàng 1
        add(p, "abc", "a b/c", Kind.FN, () -> input.insertFraction());
        angleKey = add(p, "angle", "DEG", Kind.FN, this::cycleAngle).title("Đơn vị góc (DEG / RAD / GRAD)");
        baseKey = add(p, "base", "DEC", Kind.FN, this::cycleBase).title("Hệ cơ số (DEC / HEX / BIN / OCT)");
        add(p, "fact", "x!", Kind.FN, ins("!"));
        add(p, "ncr", "nCr", Kind.FN, ins("nCr")).shift("nPr", ins("nPr"));
        add(p, "abs", "|x|", Kind.FN, ins("abs("));

        // Hàng 2: lượng giác, log, nhớ
        add(p, "sin", "sin", Kind.FN, ins("sin(")).shift("sin⁻¹", ins("asin(")).alpha("D");
        add(p, "cos", "cos", Kind.FN, ins("cos(")).shift("cos⁻¹", ins("acos(")).alpha("E");
        add(p, "tan", "tan", Kind.FN, ins("tan(")).shift("tan⁻¹", ins("atan(")).alpha("F");
        add(p, "log", "log", Kind.FN, ins("log(")).shift("10^x", ins("10^("));
        add(p, "ln", "ln", Kind.FN, ins("ln(")).shift("e^x", ins("exp("));
        add(p, "mplus", "M+", Kind.FN, () -> memoryAdd(true)).shift("M-", () -> memoryAdd(false)).alpha("M");

        // Hàng 3: hyperbolic, lũy thừa, căn
        add(p, "sinh", "sinh", Kind.FN, ins("sinh(")).alpha("B");
        add(p, "cosh", "cosh", Kind.FN, ins("cosh(")).alpha("C");
        add(p, "tanh", "tanh", Kind.FN, ins("tanh("));
        add(p, "square", "x²", Kind.FN, ins("^2" + EXP_END)).shift("x³", ins("^3" + EXP_END));
        add(p, "pow", "x^y", Kind.FN, ins("^(")).shift("n√x", ins("root"));
        add(p, "sqrt", "√", Kind.FN, ins("√(")).shift("³√x", ins("cbrt("));

        // Hàng 4: hằng số, nghịch đảo, STO, bộ nhớ
        add(p, "pi", "π", Kind.FN, ins("π"));
        add(p, "e", "e", Kind.FN, ins("e"));
        add(p, "inv", "x⁻¹", Kind.FN, ins("^-1" + EXP_END));
        add(p, "sto", "S⇔D", Kind.FN, this::toggleFraction).shift("STO", this::storeKey).title("S⇔D (đổi thập phân ⇔ phân số); SHIFT + phím này = STO (lưu vào biến, rồi bấm chữ cái)");
        add(p, "mr", "MR", Kind.FN, ins("M"));
        add(p, "cplx", "i", Kind.FN, this::insertI).shift("MC", calc::memoryClear)
                .title("i (số phức, tự bật chế độ CMPLX); SHIFT + i = MC");
        addAction("mc", "MC (xoá bộ nhớ)", calc::memoryClear);

        // Hàng 5 - 8: bàn phím số
        add(p, "d7", "7", Kind.NUM, ins("7"));
        add(p, "d8", "8", Kind.NUM, ins("8"));
        add(p, "d9", "9", Kind.NUM, ins("9"));
        add(p, "lparen", "(", Kind.OP, ins("(")).alpha("Y");
        add(p, "rparen", ")", Kind.OP, ins(")")).alpha("X");
        add(p, "del", "DEL", Kind.CLEAR, this::backspace);

        add(p, "d4", "4", Kind.NUM, ins("4"));
        add(p, "d5", "5", Kind.NUM, ins("5"));
        add(p, "d6", "6", Kind.NUM, ins("6"));
        add(p, "mul", "×", Kind.OP, ins("×"));
        add(p, "div", "÷", Kind.OP, ins("÷"));
        add(p, "ac", "AC", Kind.CLEAR, this::clearAll);

        add(p, "d1", "1", Kind.NUM, ins("1"));
        add(p, "d2", "2", Kind.NUM, ins("2"));
        add(p, "d3", "3", Kind.NUM, ins("3"));
        add(p, "add", "+", Kind.OP, ins("+"));
        add(p, "sub", "−", Kind.OP, ins("-"));
        add(p, "percent", "%", Kind.OP, ins("%"));

        add(p, "d0", "0", Kind.NUM, ins("0"));
        add(p, "dot", ".", Kind.NUM, ins("."));
        add(p, "exp10", "×10^x", Kind.OP, ins("×10^("));
        add(p, "ans", "Ans", Kind.OP, ins("Ans"));
        add(p, "neg", "(-)", Kind.OP, ins("-")).alpha("A");
        add(p, "equals", "=", Kind.EQUAL, this::calculate);
        return p;
    }

    private KeyButton add(JPanel panel, String id, String label, Kind kind, Runnable action) {
        KeyButton k = createKey(id, label, kind, action);
        panel.add(k.button);
        return k;
    }

    private Runnable ins(String text) {
        return () -> insert(text);
    }

    private KeyButton createKey(String id, String label, Kind kind, Runnable action) {
        KeyButton k = new KeyButton(id, label, kind, action);
        k.button.addActionListener(e -> press(k));
        keys.add(k);
        actions.put(id, new ActionEntry(id, k::displayName, () -> k.button.doClick(40)));
        return k;
    }

    private void addAction(String id, String name, Runnable run) {
        actions.put(id, new ActionEntry(id, () -> name, run));
    }

    /** Xử lý một lần bấm phím, có tính đến SHIFT / ALPHA / STO. */
    private void press(KeyButton k) {
        statusNote = "";
        if (k.kind == Kind.SHIFT) {
            shift = !shift;
            afterKey();
            return;
        }
        if (k.kind == Kind.ALPHA) {
            alpha = !alpha;
            if (!alpha) pendingStore = false;
            afterKey();
            return;
        }

        if (screen.isActive() && screen.handleKey(k.id)) {
            shift = false;
            alpha = false;
            pendingStore = false;
            justEvaluated = false;
            afterKey();
            return;
        }

        boolean alphaPress = alpha && k.alphaLetter != null;
        boolean shiftPress = !alphaPress && shift && k.shiftAction != null;
        Runnable run = shiftPress ? k.shiftAction : k.action;
        boolean store = alphaPress && pendingStore;

        shift = false;
        alpha = false;
        if (!alphaPress) pendingStore = false;     // bấm phím khác thì huỷ STO

        if (store) storeVariable(k.alphaLetter);
        else if (alphaPress) insert(k.alphaLetter);
        else run.run();
        afterKey();
    }

    private void afterKey() {
        refreshKeys();
        updateStatus();
        input.requestFocusInWindow();
    }

    private void refreshKeys() {
        Theme t = settings.getTheme();
        shiftKey.active = shift;
        alphaKey.active = alpha;
        modeKey.highlight = screen.isActive();
        for (KeyButton k : keys) k.refresh(t, shift, alpha);
    }

    // ================================================================ nút bấm
    private static final class KeyButton {
        final String id;
        final Kind kind;
        final JButton button = new JButton();
        final Runnable action;
        String label;
        String title;
        String shiftLabel;
        Runnable shiftAction;
        String alphaLetter;
        boolean highlight;
        boolean active;

        KeyButton(String id, String label, Kind kind, Runnable action) {
            this.id = id;
            this.label = label;
            this.kind = kind;
            this.action = action;
            button.setFocusable(false);                 // giữ con trỏ trong ô nhập
            button.setOpaque(true);
            button.setFont(new Font("Dialog", Font.BOLD, 14));
        }

        KeyButton shift(String label, Runnable action) {
            this.shiftLabel = label;
            this.shiftAction = action;
            return this;
        }

        KeyButton alpha(String letter) {
            this.alphaLetter = letter;
            return this;
        }

        KeyButton title(String t) {
            this.title = t;
            return this;
        }

        /** Tên hiển thị trong bảng phím tắt. */
        String displayName() {
            if (title != null) return title;
            StringBuilder sb = new StringBuilder(label);
            if (shiftLabel != null) sb.append("    SHIFT: ").append(shiftLabel);
            if (alphaLetter != null) sb.append("    ALPHA: ").append(alphaLetter);
            return sb.toString();
        }

        void refresh(Theme t, boolean shiftOn, boolean alphaOn) {
            Color bg = highlight ? t.get("shift") : t.get(kind.themeKey);
            Color fg = Theme.textOn(bg);
            String text = label;
            if (alphaOn && alphaLetter != null) {
                text = alphaLetter;
                fg = t.get("alphaText");
            } else if (shiftOn && shiftLabel != null) {
                text = shiftLabel;
                fg = t.get("shiftText");
            }
            button.setText(text);
            button.setBackground(bg);
            button.setForeground(fg);
            button.setBorder(active
                    ? BorderFactory.createLineBorder(Theme.textOn(bg), 3)
                    : BorderFactory.createLineBorder(Theme.blend(bg, Color.BLACK, 0.55)));
        }
    }

    // ================================================================ áp dụng cài đặt
    /** Được gọi mỗi khi cài đặt thay đổi (và lúc khởi động). */
    private void applySettings() {
        calc.setAngleMode(settings.getAngleMode());
        angleKey.label = settings.getAngleMode().name();
        Calculator.setDisplayFormat(settings.getDisplayMode(), settings.getDigits());
        applyTheme();
        applyShortcuts();
        try {
            setAlwaysOnTop(settings.isAlwaysOnTop());
        } catch (SecurityException ignored) {
        }
        while (history.size() > settings.getHistorySize()) history.remove(0);
        historyPos = Math.min(historyPos, history.size());
        refreshResult();
        updateStatus();
    }

    private void applyTheme() {
        Theme t = settings.getTheme();
        int size = settings.getLcdFontSize();
        Color body = t.get("body");
        Color lcd = t.get("lcd");

        root.setBackground(body);
        keypad.setBackground(body);

        lcdPanel.setBackground(lcd);
        lcdPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.blend(body, Color.BLACK, 0.55), 3),
                BorderFactory.createEmptyBorder(6, 10, 8, 10)));
        status.setForeground(t.get("lcdText"));

        inputScroll.getViewport().setBackground(lcd);
        inputScroll.setBackground(lcd);
        Dimension d = new Dimension(100, size * 5 / 2);
        inputScroll.setPreferredSize(d);
        inputScroll.setMinimumSize(d);
        inputScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, size * 5 / 2));

        input.applyTheme(t, size);
        result.applyTheme(t, size);
        replay.applyColors(t.get("op"), Theme.textOn(t.get("op")));
        screen.applyTheme(t, size);
        refreshKeys();
        root.revalidate();
        root.repaint();
    }

    /** Gắn phím tắt theo cài đặt (cả khi con trỏ đang trong ô nhập lẫn khi ở cửa sổ chính). */
    private void applyShortcuts() {
        InputMap inputMap = input.getInputMap(JComponent.WHEN_FOCUSED);
        InputMap windowMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        for (KeyStroke ks : installedShortcuts) {
            inputMap.remove(ks);
            windowMap.remove(ks);
        }
        installedShortcuts.clear();

        for (ActionEntry a : actions.values()) {
            KeyStroke ks = settings.getShortcut(a.getId());
            if (ks == null) continue;
            String name = "act:" + a.getId();
            AbstractAction action = new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    a.run();
                }
            };
            inputMap.put(ks, name);
            windowMap.put(ks, name);
            input.getActionMap().put(name, action);
            getRootPane().getActionMap().put(name, action);
            installedShortcuts.add(ks);
        }
    }

    // ================================================================ MODE (menu) và SETTING
    /** Hành động từ menu MODE: "comp" (về tính toán thường), "base:n" (đổi hệ cơ số). */
    private void insertI() {
        complexMode = true;
        insert("i");
    }

    private void modeAction(String id) {
        if (id.equals("cmplx") || id.equals("comp")) {
            complexMode = id.equals("cmplx");
            lastComplex = null;
            refreshResult();
        }
        if (id.startsWith("base:")) {
            baseIndex = Integer.parseInt(id.substring(5));
            baseKey.label = BASE_NAMES[baseIndex];
            refreshResult();
        }
        justEvaluated = false;
    }

    private void openSettings() {
        if (settingsDialog == null) {
            settingsDialog = new SettingsDialog(this, settings, new ArrayList<>(actions.values()));
        }
        settingsDialog.open();
    }

    // ================================================================ nhập liệu
    private void insert(String text) {
        input.replaceSelection(text);
    }

    /** Gọi ngay trước mỗi lần chèn chữ: sau "=" thì toán tử dùng Ans, số bắt đầu biểu thức mới. */
    private void beforeInsert(String text) {
        if (!justEvaluated) return;
        justEvaluated = false;
        input.setText(startsWithBinaryOperator(text) ? "Ans" : "");
        input.setCaretPosition(input.getText().length());
    }

    private static boolean startsWithBinaryOperator(String s) {
        return s.startsWith("+") || s.startsWith("-") || s.startsWith("×") || s.startsWith("÷")
                || s.startsWith("^") || s.startsWith("!") || s.startsWith("%")
                || s.startsWith("root") || s.startsWith("nPr") || s.startsWith("nCr");
    }

    private void backspace() {
        justEvaluated = false;
        input.backspace();
    }

    private void clearAll() {
        input.setText("");
        result.showText("0");
        lastResult = null;
        justEvaluated = false;
        fractionView = false;
        pendingStore = false;
    }

    private void setExpression(String s) {
        justEvaluated = false;
        input.setText(s);
        input.setCaretPosition(input.getText().length());
    }

    // ---- lịch sử (phím ▲ ▼)
    private void addHistory(String expr) {
        if (history.isEmpty() || !history.get(history.size() - 1).equals(expr)) history.add(expr);
        while (history.size() > settings.getHistorySize()) history.remove(0);
        historyPos = history.size();
    }

    /** ◄ ►: di chuyển con trỏ; trong lưới ma trận, con trỏ đã ở mép ô thì sang ô kế bên. */
    private void moveCaretOrCell(int delta) {
        String before = input.getText();
        int pos = input.getCaretPosition();
        input.moveCaret(delta);
        if (screen.isActive() && input.getCaretPosition() == pos && input.getText().equals(before)) {
            screen.horizontal(delta);
        }
    }

    private void historyUp() {
        if (screen.scroll(-1) || input.moveVertical(-1)) return;
        if (history.isEmpty()) return;
        historyPos = Math.max(0, historyPos - 1);
        setExpression(history.get(historyPos));
    }

    private void historyDown() {
        if (screen.scroll(1) || input.moveVertical(1)) return;
        if (history.isEmpty()) return;
        if (historyPos < history.size() - 1) {
            historyPos++;
            setExpression(history.get(historyPos));
        } else {
            historyPos = history.size();
            setExpression("");
        }
    }

    // ================================================================ tính toán
    private void calculate() {
        String expr = input.getText().trim();
        if (expr.isEmpty()) return;
        calc.setAngleMode(settings.getAngleMode());
        try {
            if (complexMode) {
                double[] prev = lastComplex != null ? lastComplex
                        : new double[]{lastResult == null ? 0 : lastResult, 0};
                lastComplex = ComplexMath.evaluate(expr, prev);
                lastResult = lastComplex[0];
                double mod = Math.hypot(lastComplex[0], lastComplex[1]);
                double arg = Math.atan2(lastComplex[1], lastComplex[0]);
                String unit = "";
                switch (settings.getAngleMode()) {
                    case DEG: arg = Math.toDegrees(arg); unit = "°"; break;
                    case GRAD: arg = Math.toDegrees(arg) * 200 / 180; unit = "g"; break;
                    default: unit = " rad";
                }
                statusNote = Math.abs(lastComplex[1]) < 1e-12 ? "" : "r=" + Calculator.format(mod) + "  ∠" + Calculator.format(arg) + unit;
            } else {
                lastComplex = null;
                lastResult = calc.evaluate(expr);
            }
            justEvaluated = true;
            addHistory(expr);
            // nhập bằng phân số thì kết quả cũng hiện dạng phân số (nếu được)
            fractionView = settings.isAutoFraction() && (expr.indexOf(Symbols.FRACTION_SLASH) >= 0 || expr.indexOf(Symbols.FRAC_OPEN) >= 0);
            refreshResult();
        } catch (CalcException ex) {
            lastResult = null;
            result.showText(ex.getMessage());
        } catch (RuntimeException ex) {
            lastResult = null;
            result.showText("Syntax ERROR");
        }
        updateStatus();
    }

    /** Giá trị hiện tại: tính biểu thức đang nhập (nếu có) hoặc lấy kết quả vừa rồi. */
    private Double currentValue() {
        if (!justEvaluated && !input.getText().isBlank()) calculate();
        return lastResult;
    }

    private void refreshResult() {
        if (lastResult == null) return;
        if (complexMode && lastComplex != null && Math.abs(lastComplex[1]) >= 1e-12) {
            result.showText(ComplexMath.format(lastComplex));
            return;
        }
        try {
            int base = BASE_VALUES[baseIndex];
            if (base != 10) {
                result.showText(Calculator.toBase(lastResult, base));
                return;
            }
            long[] f = fractionView ? Calculator.fraction(lastResult) : null;
            if (f == null) {
                result.showText(Calculator.format(lastResult));
                return;
            }
            long whole = f[0] / f[1];
            long rest = f[0] % f[1];
            if (settings.isMixedFraction() && whole > 0 && rest != 0) {
                result.showFraction(lastResult < 0, whole, rest, f[1]);       // hỗn số
            } else {
                result.showFraction(lastResult < 0, 0, f[0], f[1]);           // phân số
            }
        } catch (CalcException ex) {
            result.showText(ex.getMessage());
        }
    }

    private void cycleAngle() {
        settings.setAngleMode(settings.getAngleMode().next());
        settings.fireChanged();
    }

    private void toggleFraction() {
        fractionView = !fractionView;
        refreshResult();
    }

    private void cycleBase() {
        baseIndex = (baseIndex + 1) % BASE_VALUES.length;
        baseKey.label = BASE_NAMES[baseIndex];
        refreshResult();
    }

    // ---- bộ nhớ và biến
    private void memoryAdd(boolean add) {
        Double v = currentValue();
        if (v == null) return;
        if (add) calc.memoryPlus(v);
        else calc.memoryMinus(v);
    }

    /** Phím STO: lấy giá trị hiện tại rồi chờ người dùng bấm ALPHA + chữ cái của biến. */
    private void storeKey() {
        Double v = currentValue();
        if (v == null) return;
        storeValue = v;
        pendingStore = true;
        alpha = true;
    }

    private void storeVariable(String name) {
        calc.setVariable(name, storeValue);
        pendingStore = false;
        statusNote = name + " = " + Calculator.format(storeValue);
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        if (shift) sb.append("S  ");
        if (alpha) sb.append(pendingStore ? "STO→  " : "A  ");
        if (screen.isActive()) sb.append(screen.label()).append("  ");
        if (complexMode) sb.append("CMPLX  ");
        sb.append(settings.getAngleMode()).append("  ").append(BASE_NAMES[baseIndex]);
        if (settings.getDisplayMode() != DisplayMode.NORM) {
            sb.append("  ").append(settings.getDisplayMode().name()).append(settings.getDigits());
        }
        if (calc.getMemory() != 0) sb.append("  M");
        if (fractionView) sb.append("  a/b");
        if (!statusNote.isEmpty()) sb.append("     ").append(statusNote);
        status.setText(sb.toString());
    }
}
