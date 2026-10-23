package com.calculator.calculator;

import com.calculator.model.CalcException;
import com.calculator.model.Symbols;

/** Tính biểu thức số phức (chế độ CMPLX): + − × ÷, ^, √, abs, i, π, e, Ans, ngoặc, nhân ngầm (2i, 3(1+i)). */
public final class ComplexMath {
    private static final char FRAC = Symbols.FRACTION_SLASH;
    private static final char END = Symbols.EXPONENT_END;

    private final String s;
    private final double[] ans;
    private int p = 0;

    private ComplexMath(String s, double[] ans) {
        this.s = s;
        this.ans = ans == null ? new double[]{0, 0} : ans;
    }

    /** Trả về {phần thực, phần ảo}. ans = kết quả trước (cho Ans), có thể null. */
    public static double[] evaluate(String text, double[] ans) {
        ComplexMath m = new ComplexMath(com.calculator.parser.Tokenizer.rewriteFractions(text), ans);
        double[] r = m.expr();
        if (m.peek() != '\0') throw CalcException.syntax();
        return r;
    }

    public static String format(double[] z) {
        double re = Math.abs(z[0]) < 1e-12 ? 0 : z[0], im = Math.abs(z[1]) < 1e-12 ? 0 : z[1];
        if (im == 0) return Calculator.format(re);
        String mag = Math.abs(im) == 1 ? "" : Calculator.format(Math.abs(im));
        if (re == 0) return (im < 0 ? "-" : "") + mag + "i";
        return Calculator.format(re) + (im < 0 ? "-" : "+") + mag + "i";
    }

    // ---------------------------------------------------------------- phân tích
    private char peek() {
        while (p < s.length() && (s.charAt(p) == ' ' || s.charAt(p) == END)) p++;
        return p < s.length() ? s.charAt(p) : '\0';
    }

    private boolean startsPrimary() {
        char c = peek();
        return Character.isDigit(c) || c == '.' || c == '(' || c == 'i' || c == 'π' || c == 'e'
                || c == '√' || s.startsWith("Ans", p) || s.startsWith("abs(", p);
    }

    private double[] expr() {
        double[] v = term();
        while (true) {
            char c = peek();
            if (c == '+') { p++; v = add(v, term(), 1); }
            else if (c == '-' || c == '−') { p++; v = add(v, term(), -1); }
            else return v;
        }
    }

    private double[] term() {
        double[] v = unary();
        while (true) {
            char c = peek();
            if (c == '×' || c == '*') { p++; v = mul(v, unary()); }
            else if (c == '÷' || c == '/') { p++; v = div(v, unary()); }
            else if (startsPrimary()) v = mul(v, unary());
            else return v;
        }
    }

    private double[] unary() {
        char c = peek();
        if (c == '-' || c == '−') { p++; double[] v = unary(); return new double[]{-v[0], -v[1]}; }
        if (c == '+') { p++; return unary(); }
        double[] b = primary();
        if (peek() == '^') {
            p++;
            double[] e;
            if (peek() == '(') e = group();
            else {
                boolean neg = false;
                char sg = peek();
                if (sg == '-' || sg == '+') { neg = sg == '-'; p++; }
                double d = digits();
                e = new double[]{neg ? -d : d, 0};
            }
            return pow(b, e);
        }
        return b;
    }

    private double[] group() {
        p++;                                   // '('
        double[] v = expr();
        if (peek() == ')') p++;               // cho phép thiếu ngoặc đóng ở cuối
        return v;
    }

    private double digits() {
        int st = p;
        while (p < s.length() && (Character.isDigit(s.charAt(p)) || s.charAt(p) == '.')) p++;
        if (st == p) throw CalcException.syntax();
        try {
            return Double.parseDouble(s.substring(st, p));
        } catch (NumberFormatException e) {
            throw CalcException.syntax();
        }
    }

    private double[] primary() {
        char c = peek();
        if (c == '(') return group();
        if (Character.isDigit(c) || c == '.') {
            double a = digits();
            if (p < s.length() && s.charAt(p) == FRAC) {
                p++;
                double b = digits();
                if (p < s.length() && s.charAt(p) == FRAC) {      // hỗn số a⁄b⁄c
                    p++;
                    double d = digits();
                    if (d == 0) throw CalcException.math();
                    return new double[]{a + b / d, 0};
                }
                if (b == 0) throw CalcException.math();
                return new double[]{a / b, 0};
            }
            return new double[]{a, 0};
        }
        if (c == 'i') { p++; return new double[]{0, 1}; }
        if (c == 'π') { p++; return new double[]{Math.PI, 0}; }
        if (c == 'e') { p++; return new double[]{Math.E, 0}; }
        if (s.startsWith("Ans", p)) { p += 3; return ans.clone(); }
        if (s.startsWith("abs(", p)) {
            p += 3;
            double[] v = group();
            return new double[]{Math.hypot(v[0], v[1]), 0};
        }
        if (c == '√') {
            p++;
            if (peek() != '(') throw CalcException.syntax();
            return sqrt(group());
        }
        throw CalcException.syntax();
    }

    // ---------------------------------------------------------------- phép toán
    private static double[] add(double[] a, double[] b, int sign) {
        return new double[]{a[0] + sign * b[0], a[1] + sign * b[1]};
    }

    private static double[] mul(double[] a, double[] b) {
        return new double[]{a[0] * b[0] - a[1] * b[1], a[0] * b[1] + a[1] * b[0]};
    }

    private static double[] div(double[] a, double[] b) {
        double d = b[0] * b[0] + b[1] * b[1];
        if (d == 0) throw CalcException.math();
        return new double[]{(a[0] * b[0] + a[1] * b[1]) / d, (a[1] * b[0] - a[0] * b[1]) / d};
    }

    private static double[] sqrt(double[] z) {
        double r = Math.hypot(z[0], z[1]);
        return new double[]{Math.sqrt((r + z[0]) / 2), (z[1] < 0 ? -1 : 1) * Math.sqrt((r - z[0]) / 2)};
    }

    private static double[] pow(double[] b, double[] e) {
        if (Math.abs(e[1]) < 1e-12 && e[0] == Math.rint(e[0]) && Math.abs(e[0]) <= 64) {
            int n = (int) Math.abs(e[0]);
            double[] r = {1, 0};
            for (int i = 0; i < n; i++) r = mul(r, b);
            return e[0] < 0 ? div(new double[]{1, 0}, r) : r;
        }
        double m = Math.hypot(b[0], b[1]);
        if (m == 0) {
            if (e[0] > 0) return new double[]{0, 0};
            throw CalcException.math();
        }
        double lnR = Math.log(m), th = Math.atan2(b[1], b[0]);
        double re = e[0] * lnR - e[1] * th, im = e[0] * th + e[1] * lnR;       // w·ln z
        double k = Math.exp(re);
        return new double[]{k * Math.cos(im), k * Math.sin(im)};
    }
}
