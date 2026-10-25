package com.calculator;

import com.calculator.calculator.Calculator;
import com.calculator.model.AngleMode;
import com.calculator.model.Symbols;

/** Test các mẫu nhập ∫, d/dx, Σ, Π, log cơ số: chạy bằng `java com.calculator.TemplateTest`. */
public class TemplateTest {
    private static int passed = 0, failed = 0;
    private static final char I = Symbols.INT_OPEN, D = Symbols.DER_OPEN, S = Symbols.SUM_OPEN,
            P = Symbols.PROD_OPEN, L = Symbols.LOG_OPEN, SEP = Symbols.TPL_SEP, C = Symbols.TPL_CLOSE;

    private static String t(char open, String... slots) {
        return open + String.join(String.valueOf(SEP), slots) + C;
    }

    public static void main(String[] args) {
        Calculator c = new Calculator();
        c.setAngleMode(AngleMode.RAD);
        // ∫ (biểu thức, cận dưới, cận trên)
        eq(c, t(I, "0", "3", "X^2"), 9);
        eq(c, t(I, "0", "π", "sin(X)"), 2);
        eq(c, t(I, "1", "e", "1÷X"), 1);
        eq(c, t(I, "3", "0", "X"), -4.5);
        eq(c, "2×" + t(I, "0", "2", "X") + "+1", 5);
        // d/dx (biểu thức, điểm)
        eq(c, t(D, "X^2", "3"), 6);
        eq(c, t(D, "sin(X)", "0"), 1);
        eq(c, t(D, "e^X", "1"), Math.E);
        eq(c, t(D, "X^3-2X", "1"), 1);
        eq(c, t(D, "ln(X)", "2"), 0.5);
        err(c, t(D, "abs(X)", "0"), "Math ERROR");
        // Σ, Π (biểu thức, từ, đến)
        eq(c, t(S, "1", "100", "X"), 5050);
        eq(c, t(S, "1", "10", "X^2"), 385);
        eq(c, t(P, "1", "5", "X"), 120);
        // log cơ số (cơ số, đối số)
        eq(c, t(L, "2", "8"), 3);
        eq(c, t(L, "10", "1000"), 3);
        eq(c, t(L, "3", "81") + "+1", 5);
        err(c, t(L, "1", "5"), "Math ERROR");
        err(c, t(L, "2", "-1"), "Math ERROR");
        // lồng nhau và kết hợp
        eq(c, t(S, "1", "4", t(L, "2", "2^X")), 10);                 // Σ log₂(2^X) = 1+2+3+4
        eq(c, t(I, "0", "1", t(S, "1", "3", "X")), 6);               // ∫ 6 dx
        eq(c, t(I, "0", "1", "X") + "+" + t(L, "2", "4"), 2.5);
        eq(c, "Ans", 2.5);                                            // Ans không bị các ô bên trong làm đổi
        // ô trống / sai cấu trúc
        err(c, t(I, "0", "1", ""), "Syntax ERROR");
        err(c, t(L, "2", ""), "Syntax ERROR");
        err(c, "" + I + "X" + SEP + "0", "Syntax ERROR");
        // thời gian: Σ 100000 số hạng
        long t0 = System.currentTimeMillis();
        eq(c, t(S, "1", "99999", "X"), 4999950000.0);
        System.out.println("  (Σ 99999 số hạng: " + (System.currentTimeMillis() - t0) + " ms)");

        System.out.println("\nPassed: " + passed + "  Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void eq(Calculator c, String expr, double expected) {
        try {
            double v = c.evaluate(expr);
            boolean ok = Math.abs(v - expected) <= 1e-8 * Math.max(1, Math.abs(expected));
            report(ok, show(expr), expected + "", v + "");
        } catch (RuntimeException e) {
            report(false, show(expr), expected + "", e.getMessage());
        }
    }

    private static void err(Calculator c, String expr, String msg) {
        try {
            report(false, show(expr), msg, c.evaluate(expr) + "");
        } catch (RuntimeException e) {
            report(msg.equals(e.getMessage()), show(expr), msg, e.getMessage());
        }
    }

    private static String show(String s) {
        return s.replace(I, '∫').replace(D, 'ð').replace(S, 'Σ').replace(P, 'Π').replace(L, 'λ')
                .replace(SEP, ';').replace(C, '⟩');
    }

    private static void report(boolean ok, String name, String expected, String got) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name + "\n  mong đợi: " + expected + "\n  nhận được: " + got);
        }
    }
}
