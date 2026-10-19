package com.calculator.ui;

import com.calculator.calculator.Calculator;
import com.calculator.model.AngleMode;
import com.calculator.model.CalcException;
import com.calculator.model.Symbols;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/** Giao diện máy tính CASIO FX-570 bằng Java Swing. */
public class CalculatorUI extends JFrame {

    // ---- màu sắc
    private static final Color BODY   = new Color(0x2b2f36);
    private static final Color LCD    = ExpressionPane.LCD;
    private static final Color LCD_FG = ExpressionPane.LCD_FG;
    private static final Color FN     = new Color(0x4a505a);
    private static final Color NUM    = new Color(0xe6e6e6);
    private static final Color OP     = new Color(0x6b7380);
    private static final Color CLEAR  = new Color(0xc0392b);
    private static final Color EQUAL  = new Color(0xe67e22);
    private static final Color SHIFT  = new Color(0xd4a017);
    private static final Color GOLD   = new Color(0xffd54f);

    private static final String FRAC = String.valueOf(Symbols.FRACTION_SLASH);
    private static final String EXP_END = String.valueOf(Symbols.EXPONENT_END);

    private static final String MENU_CLOSED = "MENU CHỨC NĂNG  ►";
    private static final String MENU_OPEN = "MENU CHỨC NĂNG  ◄";
    private static final String[] BASE_NAMES = {"DEC", "HEX", "BIN", "OCT"};
    private static final int[] BASE_VALUES = {10, 16, 2, 8};

    private final Calculator calc = new Calculator();
    private final ExpressionPane input = new ExpressionPane();
    private final ResultView result = new ResultView();
    private final JLabel status = new JLabel(" ");
    private final List<KeyButton> keys = new ArrayList<>();
    private final MenuPanel menuPanel = new MenuPanel(calc);
    private final JButton menuButton = new JButton(MENU_CLOSED);

    private boolean shift = false;
    private boolean justEvaluated = false;
    private boolean fractionView = false;
    private boolean menuOpen = false;
    private AngleMode angleMode = AngleMode.DEG;
    private int baseIndex = 0;
    private Double lastResult = null;

    public CalculatorUI() {
        super("CASIO fx-570 Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(BODY);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(buildTop(), BorderLayout.NORTH);
        root.add(buildKeypad(), BorderLayout.CENTER);
        menuPanel.setVisible(false);
        root.add(menuPanel, BorderLayout.EAST);
        setContentPane(root);

        updateStatus();
        setMinimumSize(new Dimension(420, 600));
        setSize(460, 700);
        setLocationRelativeTo(null);
    }

    // ================================================================ màn hình + thanh MENU
    private JPanel buildTop() {
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        top.add(buildScreen(), BorderLayout.CENTER);

        menuButton.setFocusable(false);
        menuButton.setOpaque(true);
        menuButton.setBackground(FN);
        menuButton.setForeground(GOLD);
        menuButton.setFont(new Font("Dialog", Font.BOLD, 13));
        menuButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x15171a)),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        menuButton.addActionListener(e -> {
            toggleMenu();
            input.requestFocusInWindow();
        });
        top.add(menuButton, BorderLayout.SOUTH);
        return top;
    }

    private JPanel buildScreen() {
        JPanel lcd = new JPanel();
        lcd.setLayout(new BoxLayout(lcd, BoxLayout.Y_AXIS));
        lcd.setBackground(LCD);
        lcd.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x111111), 3),
                BorderFactory.createEmptyBorder(6, 10, 8, 10)));

        status.setFont(new Font("Dialog", Font.BOLD, 11));
        status.setForeground(LCD_FG);
        status.setAlignmentX(LEFT_ALIGNMENT);

        // phím Enter = "="
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "calculate");
        input.getActionMap().put("calculate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculate();
            }
        });

        JScrollPane inputScroll = new JScrollPane(input,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.getViewport().setBackground(LCD);
        inputScroll.setBackground(LCD);
        inputScroll.setPreferredSize(new Dimension(100, 44));
        inputScroll.setMinimumSize(new Dimension(100, 44));
        inputScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        inputScroll.setAlignmentX(LEFT_ALIGNMENT);

        result.setAlignmentX(LEFT_ALIGNMENT);

        lcd.add(status);
        lcd.add(inputScroll);
        lcd.add(result);
        return lcd;
    }

    // ================================================================ bàn phím
    private JPanel buildKeypad() {
        JPanel p = new JPanel(new GridLayout(8, 6, 5, 5));
        p.setBackground(BODY);

        // Hàng 1: điều khiển (SHIFT + a b/c = S⇔D như máy thật)
        addShiftKey(p);
        addKey(p, "MODE", null, FN, this::cycleAngleMode, null);
        addKey(p, "a b/c", "S⇔D", FN, () -> insert(FRAC), this::toggleFraction);
        addKey(p, "◄", null, FN, () -> input.moveCaret(-1), null);
        addKey(p, "►", null, FN, () -> input.moveCaret(1), null);
        addKey(p, "DEC", null, FN, this::cycleBase, null);

        // Hàng 2: lượng giác, log
        addKey(p, "sin", "sin⁻¹", FN, () -> insert("sin("), () -> insert("asin("));
        addKey(p, "cos", "cos⁻¹", FN, () -> insert("cos("), () -> insert("acos("));
        addKey(p, "tan", "tan⁻¹", FN, () -> insert("tan("), () -> insert("atan("));
        addKey(p, "log", "10^x", FN, () -> insert("log("), () -> insert("10^("));
        addKey(p, "ln", "e^x", FN, () -> insert("ln("), () -> insert("exp("));
        addKey(p, "M+", "M-", FN, () -> memoryAdd(true), () -> memoryAdd(false));

        // Hàng 3: hyperbolic, lũy thừa, căn
        addKey(p, "sinh", null, FN, () -> insert("sinh("), null);
        addKey(p, "cosh", null, FN, () -> insert("cosh("), null);
        addKey(p, "tanh", null, FN, () -> insert("tanh("), null);
        addKey(p, "x²", "x³", FN, () -> insert("^2" + EXP_END), () -> insert("^3" + EXP_END));
        addKey(p, "x^y", "n√x", FN, () -> insert("^("), () -> insert("root"));
        addKey(p, "√", "³√x", FN, () -> insert("√("), () -> insert("cbrt("));

        // Hàng 4: giai thừa, tổ hợp, hằng số
        addKey(p, "x!", null, FN, () -> insert("!"), null);
        addKey(p, "nCr", "nPr", FN, () -> insert("nCr"), () -> insert("nPr"));
        addKey(p, "|x|", null, FN, () -> insert("abs("), null);
        addKey(p, "π", null, FN, () -> insert("π"), null);
        addKey(p, "e", null, FN, () -> insert("e"), null);
        addKey(p, "MR", "MC", FN, () -> insert("M"), this::memoryClear);

        // Hàng 5 - 8: bàn phím số
        addKey(p, "7", null, NUM, () -> insert("7"), null);
        addKey(p, "8", null, NUM, () -> insert("8"), null);
        addKey(p, "9", null, NUM, () -> insert("9"), null);
        addKey(p, "(", null, OP, () -> insert("("), null);
        addKey(p, ")", null, OP, () -> insert(")"), null);
        addKey(p, "DEL", null, CLEAR, this::backspace, null);

        addKey(p, "4", null, NUM, () -> insert("4"), null);
        addKey(p, "5", null, NUM, () -> insert("5"), null);
        addKey(p, "6", null, NUM, () -> insert("6"), null);
        addKey(p, "×", null, OP, () -> insert("×"), null);
        addKey(p, "÷", null, OP, () -> insert("÷"), null);
        addKey(p, "AC", null, CLEAR, this::clearAll, null);

        addKey(p, "1", null, NUM, () -> insert("1"), null);
        addKey(p, "2", null, NUM, () -> insert("2"), null);
        addKey(p, "3", null, NUM, () -> insert("3"), null);
        addKey(p, "+", null, OP, () -> insert("+"), null);
        addKey(p, "−", null, OP, () -> insert("-"), null);
        addKey(p, "%", null, OP, () -> insert("%"), null);

        addKey(p, "0", null, NUM, () -> insert("0"), null);
        addKey(p, ".", null, NUM, () -> insert("."), null);
        addKey(p, "×10^x", null, OP, () -> insert("×10^("), null);
        addKey(p, "Ans", null, OP, () -> insert("Ans"), null);
        addKey(p, "(-)", null, OP, () -> insert("-"), null);
        addKey(p, "=", null, EQUAL, this::calculate, null);
        return p;
    }

    private void addShiftKey(JPanel p) {
        KeyButton k = new KeyButton("SHIFT", null, SHIFT, null, null);
        k.button.addActionListener(e -> {
            shift = !shift;
            refreshKeys();
            updateStatus();
            input.requestFocusInWindow();
        });
        keys.add(k);
        p.add(k.button);
    }

    private void addKey(JPanel p, String label, String shiftLabel, Color bg,
                        Runnable action, Runnable shiftAction) {
        KeyButton k = new KeyButton(label, shiftLabel, bg, action, shiftAction);
        k.button.addActionListener(e -> {
            boolean useShift = shift && k.shiftAction != null;
            shift = false;
            (useShift ? k.shiftAction : k.action).run();
            refreshKeys();
            updateStatus();
            input.requestFocusInWindow();
        });
        keys.add(k);
        p.add(k.button);
    }

    private void refreshKeys() {
        for (KeyButton k : keys) k.refresh(shift);
    }

    /** Một nút bấm với nhãn thường và nhãn SHIFT. */
    private static class KeyButton {
        final JButton button;
        final String label, shiftLabel;
        final Color bg;
        final Runnable action, shiftAction;

        KeyButton(String label, String shiftLabel, Color bg, Runnable action, Runnable shiftAction) {
            this.label = label;
            this.shiftLabel = shiftLabel;
            this.bg = bg;
            this.action = action;
            this.shiftAction = shiftAction;
            button = new JButton(label);
            button.setFocusable(false);                 // giữ con trỏ trong ô nhập
            button.setOpaque(true);
            button.setBackground(bg);
            button.setFont(new Font("Dialog", Font.BOLD, 14));
            button.setForeground(isLight(bg) ? Color.BLACK : Color.WHITE);
            button.setBorder(BorderFactory.createLineBorder(new Color(0x15171a)));
        }

        void refresh(boolean shift) {
            boolean showShift = shift && shiftLabel != null;
            button.setText(showShift ? shiftLabel : label);
            if (!isLight(bg) && bg != SHIFT) {
                button.setForeground(showShift ? GOLD : Color.WHITE);
            }
        }

        private static boolean isLight(Color c) {
            return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 > 150;
        }
    }

    // ================================================================ MENU mở rộng
    private void toggleMenu() {
        menuOpen = !menuOpen;
        menuPanel.setVisible(menuOpen);
        if (menuOpen) menuPanel.showList();
        menuButton.setText(menuOpen ? MENU_OPEN : MENU_CLOSED);
        int delta = MenuPanel.WIDTH + 10;
        setSize(getWidth() + (menuOpen ? delta : -delta), getHeight());
        revalidate();
        repaint();
    }

    // ================================================================ thao tác
    private void insert(String text) {
        if (justEvaluated) {
            justEvaluated = false;
            // Sau "=": bấm toán tử thì dùng kết quả trước (Ans), bấm số thì bắt đầu mới
            input.setText(startsWithBinaryOperator(text) ? "Ans" : "");
            input.setCaretPosition(input.getText().length());
        }
        input.replaceSelection(text);
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
    }

    private void calculate() {
        String expr = input.getText().trim();
        if (expr.isEmpty()) return;
        calc.setAngleMode(angleMode);
        try {
            lastResult = calc.evaluate(expr);
            justEvaluated = true;
            // nhập bằng phân số thì kết quả cũng hiện dạng phân số (nếu được)
            fractionView = expr.indexOf(Symbols.FRACTION_SLASH) >= 0;
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

    private void refreshResult() {
        if (lastResult == null) return;
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
            if (whole > 0 && rest != 0) {
                result.showFraction(lastResult < 0, whole, rest, f[1]);       // hỗn số
            } else {
                result.showFraction(lastResult < 0, 0, f[0], f[1]);           // phân số
            }
        } catch (CalcException ex) {
            result.showText(ex.getMessage());
        }
    }

    private void cycleAngleMode() {
        angleMode = angleMode.next();
    }

    private void toggleFraction() {
        fractionView = !fractionView;
        refreshResult();
    }

    private void cycleBase() {
        baseIndex = (baseIndex + 1) % BASE_VALUES.length;
        for (KeyButton k : keys) {
            if (k.label.equals("DEC") || k.label.equals("HEX")
                    || k.label.equals("BIN") || k.label.equals("OCT")) {
                k.button.setText(BASE_NAMES[baseIndex]);
            }
        }
        refreshResult();
    }

    private void memoryAdd(boolean add) {
        if (!justEvaluated && !input.getText().isBlank()) calculate();
        if (lastResult == null) return;
        if (add) calc.memoryPlus(lastResult);
        else calc.memoryMinus(lastResult);
    }

    private void memoryClear() {
        calc.memoryClear();
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        if (shift) sb.append("S  ");
        sb.append(angleMode).append("  ").append(BASE_NAMES[baseIndex]);
        if (calc.getMemory() != 0) sb.append("  M");
        if (fractionView) sb.append("  a/b");
        status.setText(sb.toString());
    }
}
