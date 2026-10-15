package com.calculator.ui;

import com.calculator.calculator.Calculator;
import com.calculator.model.AngleMode;
import com.calculator.model.CalcException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.SwingConstants;
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
    private static final Color LCD    = new Color(0xc5d0b5);
    private static final Color LCD_FG = new Color(0x1d2318);
    private static final Color FN     = new Color(0x4a505a);
    private static final Color NUM    = new Color(0xe6e6e6);
    private static final Color OP     = new Color(0x6b7380);
    private static final Color CLEAR  = new Color(0xc0392b);
    private static final Color EQUAL  = new Color(0xe67e22);
    private static final Color SHIFT  = new Color(0xd4a017);
    private static final Color GOLD   = new Color(0xffd54f);

    /** Các chuỗi cần xoá nguyên khối khi bấm DEL (dài trước, ngắn sau). */
    private static final String[] WHOLE_TOKENS = {
            "asin(", "acos(", "atan(", "sinh(", "cosh(", "tanh(", "sqrt(", "cbrt(",
            "sin(", "cos(", "tan(", "log(", "exp(", "abs(", "ln(", "√(",
            "Ans", "root", "nPr", "nCr"};
    private static final String[] BASE_NAMES = {"DEC", "HEX", "BIN", "OCT"};
    private static final int[] BASE_VALUES = {10, 16, 2, 8};

    private final Calculator calc = new Calculator();
    /** Ô nhập biểu thức: JTextPane để hiển thị số mũ nhỏ, nâng lên trên (không xuống dòng). */
    private final JTextPane input = new JTextPane() {
        @Override
        public boolean getScrollableTracksViewportWidth() {
            return false;
        }
    };
    private boolean restylePending = false;

    // Kiểu chữ cho ô nhập
    private static final SimpleAttributeSet STYLE_NORMAL = new SimpleAttributeSet();
    private static final SimpleAttributeSet STYLE_SUPER = new SimpleAttributeSet();
    private static final SimpleAttributeSet STYLE_HIDDEN = new SimpleAttributeSet();

    static {
        StyleConstants.setFontFamily(STYLE_NORMAL, Font.MONOSPACED);
        StyleConstants.setFontSize(STYLE_NORMAL, 22);
        StyleConstants.setBold(STYLE_NORMAL, false);
        StyleConstants.setSuperscript(STYLE_NORMAL, false);
        StyleConstants.setForeground(STYLE_NORMAL, LCD_FG);

        // số mũ: nhỏ hơn và nâng lên trên
        StyleConstants.setFontFamily(STYLE_SUPER, Font.MONOSPACED);
        StyleConstants.setFontSize(STYLE_SUPER, 18);
        StyleConstants.setBold(STYLE_SUPER, false);
        StyleConstants.setSuperscript(STYLE_SUPER, true);
        StyleConstants.setForeground(STYLE_SUPER, LCD_FG);

        // ký tự '^' được ẩn đi (cùng màu nền, cỡ rất nhỏ)
        StyleConstants.setFontFamily(STYLE_HIDDEN, Font.MONOSPACED);
        StyleConstants.setFontSize(STYLE_HIDDEN, 2);
        StyleConstants.setSuperscript(STYLE_HIDDEN, false);
        StyleConstants.setForeground(STYLE_HIDDEN, LCD);
    }
    private final JLabel result = new JLabel("0", SwingConstants.RIGHT);
    private final JLabel status = new JLabel(" ");
    private final List<KeyButton> keys = new ArrayList<>();

    private boolean shift = false;
    private boolean justEvaluated = false;
    private boolean fractionView = false;
    private AngleMode angleMode = AngleMode.DEG;
    private int baseIndex = 0;
    private Double lastResult = null;

    public CalculatorUI() {
        super("CASIO fx-570 Simulator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(BODY);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(buildScreen(), BorderLayout.NORTH);
        root.add(buildKeypad(), BorderLayout.CENTER);
        setContentPane(root);

        updateStatus();
        setMinimumSize(new Dimension(420, 560));
        setSize(460, 640);
        setLocationRelativeTo(null);
    }

    // ================================================================ màn hình
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

        input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 22));
        input.setBackground(LCD);
        input.setForeground(LCD_FG);
        input.setCaretColor(LCD_FG);
        input.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        input.setCharacterAttributes(STYLE_NORMAL, true);
        // phím Enter = "="
        input.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "calculate");
        input.getActionMap().put("calculate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculate();
            }
        });
        // mỗi lần nội dung đổi thì định dạng lại số mũ
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { scheduleRestyle(); }
            @Override public void removeUpdate(DocumentEvent e)  { scheduleRestyle(); }
            @Override public void changedUpdate(DocumentEvent e) { }
        });

        JScrollPane inputScroll = new JScrollPane(input,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        inputScroll.setBorder(BorderFactory.createEmptyBorder());
        inputScroll.getViewport().setBackground(LCD);
        inputScroll.setBackground(LCD);
        inputScroll.setPreferredSize(new Dimension(100, 40));
        inputScroll.setMinimumSize(new Dimension(100, 40));
        inputScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        inputScroll.setAlignmentX(LEFT_ALIGNMENT);

        result.setFont(new Font(Font.MONOSPACED, Font.BOLD, 32));
        result.setForeground(LCD_FG);
        result.setAlignmentX(LEFT_ALIGNMENT);
        result.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        result.setPreferredSize(new Dimension(100, 44));

        lcd.add(status);
        lcd.add(inputScroll);
        lcd.add(result);
        return lcd;
    }

    // ================================================================ bàn phím
    private JPanel buildKeypad() {
        JPanel p = new JPanel(new GridLayout(8, 6, 5, 5));
        p.setBackground(BODY);

        // Hàng 1: điều khiển
        addShiftKey(p);
        addKey(p, "MODE", null, FN, this::cycleAngleMode, null);
        addKey(p, "S⇔D", null, FN, this::toggleFraction, null);
        addKey(p, "◄", null, FN, () -> moveCaret(-1), null);
        addKey(p, "►", null, FN, () -> moveCaret(1), null);
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
        addKey(p, "x²", "x³", FN, () -> insert("^2"), () -> insert("^3"));
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
        int pos = input.getCaretPosition();
        if (pos == 0) return;
        String text = input.getText();
        String before = text.substring(0, pos);
        int remove = 1;
        for (String t : WHOLE_TOKENS) {
            if (before.endsWith(t)) {
                remove = t.length();
                break;
            }
        }
        String newBefore = text.substring(0, pos - remove);
        if (newBefore.endsWith("^")) {                 // '^' đang bị ẩn nên xoá luôn
            newBefore = newBefore.substring(0, newBefore.length() - 1);
        }
        input.setText(newBefore + text.substring(pos));
        input.setCaretPosition(newBefore.length());
    }

    private void moveCaret(int delta) {
        String t = input.getText();
        int pos = Math.max(0, Math.min(t.length(), input.getCaretPosition() + delta));
        // bỏ qua ký tự '^' đang bị ẩn
        if (delta < 0 && pos > 0 && t.charAt(pos - 1) == '^') pos--;
        else if (delta > 0 && pos > 0 && pos < t.length() && t.charAt(pos - 1) == '^') pos++;
        input.setCaretPosition(pos);
    }

    // ================================================================ hiển thị số mũ
    private void scheduleRestyle() {
        if (restylePending) return;
        restylePending = true;
        SwingUtilities.invokeLater(() -> {
            restylePending = false;
            restyle();
        });
    }

    /** Đặt kiểu chữ: số mũ (sau dấu ^) nhỏ và nâng lên, dấu ^ được ẩn. */
    private void restyle() {
        StyledDocument doc = input.getStyledDocument();
        String t;
        try {
            t = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }
        doc.setCharacterAttributes(0, t.length(), STYLE_NORMAL, true);
        int i = 0;
        while (i < t.length()) {
            if (t.charAt(i) == '^') {
                int end = exponentEnd(t, i + 1);
                doc.setCharacterAttributes(i, 1, STYLE_HIDDEN, true);
                if (end > i + 1) doc.setCharacterAttributes(i + 1, end - i - 1, STYLE_SUPER, true);
                i = Math.max(end, i + 1);
            } else {
                i++;
            }
        }
    }

    /** Vị trí kết thúc của số mũ bắt đầu tại p: [-]số, [-](...) hoặc π. */
    static int exponentEnd(String t, int p) {
        int n = t.length();
        int i = p;
        if (i < n && t.charAt(i) == '-') i++;
        if (i < n && t.charAt(i) == '(') {
            int depth = 0;
            while (i < n) {
                char c = t.charAt(i++);
                if (c == '(') depth++;
                else if (c == ')' && --depth == 0) break;
            }
            return i;
        }
        int start = i;
        while (i < n && (Character.isDigit(t.charAt(i)) || t.charAt(i) == '.')) i++;
        if (i == start && i < n && t.charAt(i) == 'π') i++;
        return i;
    }

    private void clearAll() {
        input.setText("");
        result.setText("0");
        lastResult = null;
        justEvaluated = false;
    }

    private void calculate() {
        String expr = input.getText().trim();
        if (expr.isEmpty()) return;
        calc.setAngleMode(angleMode);
        try {
            lastResult = calc.evaluate(expr);
            justEvaluated = true;
            refreshResult();
        } catch (CalcException ex) {
            lastResult = null;
            result.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            lastResult = null;
            result.setText("Syntax ERROR");
        }
        updateStatus();
    }

    private void refreshResult() {
        if (lastResult == null) return;
        try {
            int base = BASE_VALUES[baseIndex];
            String text;
            if (base != 10) {
                text = Calculator.toBase(lastResult, base);
            } else if (fractionView) {
                String frac = Calculator.toFraction(lastResult);
                text = frac != null ? frac : Calculator.format(lastResult);
            } else {
                text = Calculator.format(lastResult);
            }
            result.setText(text);
        } catch (CalcException ex) {
            result.setText(ex.getMessage());
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
