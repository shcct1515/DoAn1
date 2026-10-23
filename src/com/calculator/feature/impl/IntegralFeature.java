package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

/** Tích phân xác định ∫ f(X) dX từ a đến b (Simpson thích nghi). */
public class IntegralFeature extends AbstractFeature {
    private Calculator calc;
    private String expr;
    private int evals;

    public IntegralFeature() {
        super("∫ f(X) dX", "f(X)", "a (cận dưới)", "b (cận trên)");
    }

    @Override
    public void check(int index, String value, Calculator calc) {
        if (index == 0) {
            if (value.isBlank()) throw new IllegalArgumentException("Chưa nhập f(X)");
            return;
        }
        super.check(index, value, calc);
    }

    private double f(double x) {
        if (++evals > 60000) throw new IllegalArgumentException("Math ERROR (không hội tụ)");
        calc.setVariable("X", x);
        return calc.evaluate(expr);
    }

    private double simpson(double a, double b, double fa, double fm, double fb) {
        return (b - a) / 6 * (fa + 4 * fm + fb);
    }

    private double adaptive(double a, double b, double fa, double fm, double fb, double whole, double eps, int depth) {
        double m = (a + b) / 2, lm = (a + m) / 2, rm = (m + b) / 2;
        double flm = f(lm), frm = f(rm);
        double left = simpson(a, m, fa, flm, fm), right = simpson(m, b, fm, frm, fb);
        double diff = left + right - whole;
        if (depth <= 0 || Math.abs(diff) <= 15 * eps) return left + right + diff / 15;
        return adaptive(a, m, fa, flm, fm, left, eps / 2, depth - 1)
                + adaptive(m, b, fm, frm, fb, right, eps / 2, depth - 1);
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        this.calc = calc;
        this.expr = in.get(0).trim();
        this.evals = 0;
        double a = val(calc, in, 1), b = val(calc, in, 2);
        double old = calc.getVariable("X");
        try {
            if (a == b) return "∫ = 0";
            double lo = Math.min(a, b), hi = Math.max(a, b);
            // chia 8 đoạn để không bỏ sót đặc điểm của hàm
            int parts = 8;
            double sum = 0, h = (hi - lo) / parts;
            for (int i = 0; i < parts; i++) {
                double x0 = lo + i * h, x1 = x0 + h, xm = (x0 + x1) / 2;
                double f0 = f(x0), fm = f(xm), f1 = f(x1);
                sum += adaptive(x0, x1, f0, fm, f1, simpson(x0, x1, f0, fm, f1), 1e-10 / parts, 18);
            }
            if (Double.isNaN(sum) || Double.isInfinite(sum)) throw new IllegalArgumentException("Math ERROR");
            if (a > b) sum = -sum;
            return "∫ = " + MathUtil.f(sum) + "\n(giá trị gần đúng)";
        } finally {
            calc.setVariable("X", old);
        }
    }
}
