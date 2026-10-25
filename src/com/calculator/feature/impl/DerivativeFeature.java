package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Đạo hàm tại một điểm: f(a), f'(a), f''(a) và phương trình tiếp tuyến.
 * Dùng sai phân trung tâm kết hợp ngoại suy Richardson (phương pháp Ridders).
 */
public class DerivativeFeature extends AbstractFeature {
    private static final double CON = 1.4, CON2 = CON * CON, SAFE = 2;
    private static final int LEVELS = 10;

    private Calculator calc;
    private String expr;

    public DerivativeFeature() {
        super("d/dX f(X) tại a", "f(X)", "a (tính tại X=a)");
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
        calc.setVariable("X", x);
        double y = calc.evaluate(expr);
        if (Double.isNaN(y) || Double.isInfinite(y)) throw new IllegalArgumentException("Math ERROR");
        return y;
    }

    /** Ngoại suy Richardson: stencil(h) có sai số theo luỹ thừa chẵn của h; trả về giá trị tốt nhất. */
    private static double extrapolate(DoubleUnaryOperator stencil, double h) {
        double[][] t = new double[LEVELS][LEVELS];
        double best = 0, err = Double.MAX_VALUE;
        t[0][0] = stencil.applyAsDouble(h);
        best = t[0][0];
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

    @Override
    public String solve(List<String> in, Calculator calc) {
        this.calc = calc;
        this.expr = in.get(0).trim();
        double a = val(calc, in, 1);
        double old = calc.getVariable("X");
        try {
            double scale = Math.max(1, Math.abs(a));
            double fa = f(a);

            // hàm không khả vi (gấp khúc như |X|, gián đoạn): hai đạo hàm một phía khác nhau
            double e = 1e-7 * scale;
            double left = (fa - f(a - e)) / e, right = (f(a + e) - fa) / e;
            if (Math.abs(left - right) > 1e-3 * Math.max(1, Math.max(Math.abs(left), Math.abs(right)))) {
                throw new IllegalArgumentException("Math ERROR (không khả vi tại X = " + MathUtil.f(a) + ")");
            }

            double d1 = MathUtil.snap(extrapolate(h -> (f(a + h) - f(a - h)) / (2 * h), 0.05 * scale));
            double d2 = MathUtil.snap(extrapolate(h -> (f(a + h) - 2 * fa + f(a - h)) / (h * h), 0.05 * scale));
            double n = MathUtil.snap(fa - d1 * a);

            StringBuilder sb = new StringBuilder();
            sb.append("f(a)   = ").append(MathUtil.f(fa)).append("\n");
            sb.append("f'(a)  = ").append(MathUtil.f(d1)).append("\n");
            sb.append("f''(a) = ").append(MathUtil.f(d2)).append("\n");
            sb.append("Tiếp tuyến: y = ");
            if (Math.abs(d1) < MathUtil.EPS) sb.append(MathUtil.f(fa));
            else {
                sb.append(Math.abs(d1 - 1) < MathUtil.EPS ? "" : Math.abs(d1 + 1) < MathUtil.EPS ? "-" : MathUtil.f(d1)).append("x");
                if (Math.abs(n) >= MathUtil.EPS) sb.append(n < 0 ? " - " : " + ").append(MathUtil.f(Math.abs(n)));
            }
            sb.append("\n(giá trị gần đúng)");
            return sb.toString();
        } finally {
            calc.setVariable("X", old);
        }
    }
}
