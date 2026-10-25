package com.calculator.calculator;

import com.calculator.model.CalcException;

import java.util.function.DoubleUnaryOperator;

/** Tích phân và đạo hàm số: Simpson thích nghi, sai phân trung tâm + ngoại suy Richardson (Ridders). */
public final class Calculus {
    private static final double CON = 1.4, CON2 = CON * CON, SAFE = 2;
    private static final int LEVELS = 10, MAX_EVALS = 60000;

    private Calculus() {
    }

    /** Làm tròn về số nguyên gần nhất khi sai khác chỉ do sai số số học. */
    public static double snap(double v) {
        double r = Math.rint(v);
        return Math.abs(v - r) <= 1e-9 * Math.max(1, Math.abs(v)) ? r : v;
    }

    // ------------------------------------------------------------ tích phân
    /** ∫ từ a đến b của f(x) dx. */
    public static double integrate(DoubleUnaryOperator f, double a, double b) {
        if (a == b) return 0;
        Integrator in = new Integrator(f);
        double lo = Math.min(a, b), hi = Math.max(a, b);
        int parts = 8;                          // chia 8 đoạn để không bỏ sót đặc điểm của hàm
        double sum = 0, h = (hi - lo) / parts;
        for (int i = 0; i < parts; i++) {
            double x0 = lo + i * h, x1 = x0 + h, xm = (x0 + x1) / 2;
            double f0 = in.f(x0), fm = in.f(xm), f1 = in.f(x1);
            sum += in.adaptive(x0, x1, f0, fm, f1, simpson(x0, x1, f0, fm, f1), 1e-10 / parts, 18);
        }
        if (Double.isNaN(sum) || Double.isInfinite(sum)) throw CalcException.math();
        return a > b ? -sum : sum;
    }

    private static double simpson(double a, double b, double fa, double fm, double fb) {
        return (b - a) / 6 * (fa + 4 * fm + fb);
    }

    private static final class Integrator {
        final DoubleUnaryOperator fn;
        int evals;

        Integrator(DoubleUnaryOperator fn) {
            this.fn = fn;
        }

        double f(double x) {
            if (++evals > MAX_EVALS) throw CalcException.math();
            return fn.applyAsDouble(x);
        }

        double adaptive(double a, double b, double fa, double fm, double fb, double whole, double eps, int depth) {
            double m = (a + b) / 2, lm = (a + m) / 2, rm = (m + b) / 2;
            double flm = f(lm), frm = f(rm);
            double left = simpson(a, m, fa, flm, fm), right = simpson(m, b, fm, frm, fb);
            double diff = left + right - whole;
            if (depth <= 0 || Math.abs(diff) <= 15 * eps) return left + right + diff / 15;
            return adaptive(a, m, fa, flm, fm, left, eps / 2, depth - 1)
                    + adaptive(m, b, fm, frm, fb, right, eps / 2, depth - 1);
        }
    }

    // ------------------------------------------------------------ đạo hàm
    /** f'(a). Hàm không khả vi tại a (gấp khúc, gián đoạn) thì báo Math ERROR. */
    public static double derivative(DoubleUnaryOperator f, double a) {
        double scale = Math.max(1, Math.abs(a));
        double fa = check(f.applyAsDouble(a));
        double e = 1e-7 * scale;
        double left = (fa - check(f.applyAsDouble(a - e))) / e;
        double right = (check(f.applyAsDouble(a + e)) - fa) / e;
        if (Math.abs(left - right) > 1e-3 * Math.max(1, Math.max(Math.abs(left), Math.abs(right)))) {
            throw CalcException.math();
        }
        return snap(extrapolate(h -> (check(f.applyAsDouble(a + h)) - check(f.applyAsDouble(a - h))) / (2 * h), 0.05 * scale));
    }

    private static double check(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) throw CalcException.math();
        return v;
    }

    /** Ngoại suy Richardson: stencil(h) có sai số theo luỹ thừa chẵn của h; trả về giá trị tốt nhất. */
    private static double extrapolate(DoubleUnaryOperator stencil, double h) {
        double[][] t = new double[LEVELS][LEVELS];
        double err = Double.MAX_VALUE;
        t[0][0] = stencil.applyAsDouble(h);
        double best = t[0][0];
        for (int i = 1; i < LEVELS; i++) {
            h /= CON;
            t[0][i] = stencil.applyAsDouble(h);
            double fac = CON2;
            for (int j = 1; j <= i; j++) {
                t[j][i] = (t[j - 1][i] * fac - t[j - 1][i - 1]) / (fac - 1);
                fac *= CON2;
                double e = Math.max(Math.abs(t[j][i] - t[j - 1][i]), Math.abs(t[j][i] - t[j - 1][i - 1]));
                if (e <= err) {
                    err = e;
                    best = t[j][i];
                }
            }
            if (Math.abs(t[i][i] - t[i - 1][i - 1]) >= SAFE * err) break;
        }
        return best;
    }
}
