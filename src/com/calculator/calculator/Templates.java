package com.calculator.calculator;

import com.calculator.model.CalcException;
import com.calculator.model.Symbols;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Tính các mẫu nhập nhiều ô (∫, d/dx, Σ, Π, log cơ số) nằm trong biểu thức: mỗi mẫu được tính ra một số
 * rồi thay bằng "(số)", phần còn lại của biểu thức đi qua Tokenizer → RPN như bình thường.
 * Biến của biểu thức bên trong là X.
 */
final class Templates {
    private static final int MAX_TERMS = 100000;

    private Templates() {
    }

    static boolean has(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= Symbols.INT_OPEN && c <= Symbols.TPL_CLOSE) return true;
        }
        return false;
    }

    static String expand(String s, Calculator calc) {
        if (!has(s)) return s;
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Symbols.isTemplateOpen(c)) {
                int[] pos = scan(s, i);
                if (pos == null) throw CalcException.syntax();
                List<String> slots = new ArrayList<>();
                int from = i + 1;
                for (int p : pos) {
                    slots.add(s.substring(from, p));
                    from = p + 1;
                }
                out.append('(').append(plain(value(c, slots, calc))).append(')');
                i = pos[pos.length - 1] + 1;
            } else if (c == Symbols.TPL_SEP || c == Symbols.TPL_CLOSE) {
                throw CalcException.syntax();
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /** Vị trí các ký tự ngăn ô rồi ký tự đóng của mẫu bắt đầu tại open; null nếu thiếu hoặc sai số ô. */
    static int[] scan(String s, int open) {
        int need = Symbols.slotCount(s.charAt(open)) - 1;
        List<Integer> seps = new ArrayList<>();
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Symbols.isTemplateOpen(c)) depth++;
            else if (c == Symbols.TPL_SEP && depth == 1) seps.add(i);
            else if (c == Symbols.TPL_CLOSE && --depth == 0) {
                if (seps.size() != need) return null;
                int[] r = new int[need + 1];
                for (int k = 0; k < need; k++) r[k] = seps.get(k);
                r[need] = i;
                return r;
            }
        }
        return null;
    }

    private static String plain(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) throw CalcException.math();
        return BigDecimal.valueOf(v).toPlainString();
    }

    /** Tính từng ô độc lập với nhau: Ans giữ nguyên, biến X được trả lại như cũ sau khi tính. */
    private static final class Ctx {
        final Calculator calc;
        final double ans, x;

        Ctx(Calculator calc) {
            this.calc = calc;
            this.ans = calc.getAns();
            this.x = calc.getVariable("X");
        }

        double eval(String text) {
            if (text.isBlank()) throw CalcException.syntax();       // còn ô □ chưa nhập
            calc.setAns(ans);
            return calc.evaluate(text);
        }

        double at(String body, double xv) {
            calc.setVariable("X", xv);
            return eval(body);
        }

        void restore() {
            calc.setAns(ans);
            calc.setVariable("X", x);
        }
    }

    private static double value(char kind, List<String> sl, Calculator calc) {
        Ctx ctx = new Ctx(calc);
        try {
            switch (kind) {
                case Symbols.INT_OPEN: {                    // (cận dưới, cận trên, biểu thức)
                    double a = ctx.eval(sl.get(0)), b = ctx.eval(sl.get(1));
                    if (sl.get(2).isBlank()) throw CalcException.syntax();
                    return Calculus.integrate(xv -> ctx.at(sl.get(2), xv), a, b);
                }
                case Symbols.DER_OPEN: {
                    double a = ctx.eval(sl.get(1));
                    if (sl.get(0).isBlank()) throw CalcException.syntax();
                    return Calculus.derivative(xv -> ctx.at(sl.get(0), xv), a);
                }
                case Symbols.SUM_OPEN:
                case Symbols.PROD_OPEN: {
                    double a = ctx.eval(sl.get(0)), b = ctx.eval(sl.get(1));     // (từ, đến, biểu thức)
                    if (sl.get(2).isBlank()) throw CalcException.syntax();
                    if (a != Math.rint(a) || b != Math.rint(b) || b < a || b - a >= MAX_TERMS) throw CalcException.math();
                    boolean sum = kind == Symbols.SUM_OPEN;
                    double r = sum ? 0 : 1;
                    for (long k = (long) a; k <= (long) b; k++) {
                        double t = ctx.at(sl.get(2), k);
                        r = sum ? r + t : r * t;
                    }
                    return r;
                }
                default: {                                  // log cơ số: logₐ(b)
                    double base = ctx.eval(sl.get(0)), arg = ctx.eval(sl.get(1));
                    if (base <= 0 || base == 1 || arg <= 0) throw CalcException.math();
                    double r = Math.log(arg) / Math.log(base);
                    return Math.abs(r - Math.rint(r)) < 1e-12 * Math.max(1, Math.abs(r)) ? Math.rint(r) : r;
                }
            }
        } finally {
            ctx.restore();
        }
    }
}
