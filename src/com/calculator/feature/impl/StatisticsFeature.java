package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.Arrays;
import java.util.List;

public class StatisticsFeature extends AbstractFeature {
    private static final int MAX_N = 40;
    private final boolean regression;

    public StatisticsFeature(boolean regression) {
        super(regression ? "Hồi quy y=a+bx" : "Thống kê 1 biến");
        this.regression = regression;
    }

    private int count(List<String> values, Calculator calc) {
        return (int) Math.round(calc.evaluate(values.get(0).trim()));
    }

    @Override
    public String prompt(int index, List<String> values) {
        if (index == 0) return "n (số mẫu)";
        int n = count(values, calc0());
        if (regression) return index <= 2 * n ? (index % 2 == 1 ? "x" : "y") + (index + 1) / 2 : null;
        return index <= n ? "x" + index : null;
    }

    private static Calculator calc0() {
        return new Calculator();
    }

    @Override
    public void check(int index, String value, Calculator calc) {
        double v = calc.evaluate(value);
        if (index == 0 && (v != Math.floor(v) || v < (regression ? 2 : 1) || v > MAX_N)) {
            throw new IllegalArgumentException("n phải nguyên, " + (regression ? 2 : 1) + " đến " + MAX_N);
        }
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        int n = count(in, calc);
        double[] x = new double[n], y = new double[n];
        for (int i = 0; i < n; i++) {
            if (regression) {
                x[i] = val(calc, in, 1 + 2 * i);
                y[i] = val(calc, in, 2 + 2 * i);
            } else {
                x[i] = val(calc, in, 1 + i);
            }
        }
        double sum = 0, sum2 = 0;
        for (double v : x) {
            sum += v;
            sum2 += v * v;
        }
        double mean = sum / n, ss = Math.max(0, sum2 - n * mean * mean);
        double[] s = x.clone();
        Arrays.sort(s);
        double med = n % 2 == 1 ? s[n / 2] : (s[n / 2 - 1] + s[n / 2]) / 2;
        StringBuilder sb = new StringBuilder();
        sb.append("n = ").append(n).append("   x̄ = ").append(MathUtil.f(mean))
                .append("\nΣx = ").append(MathUtil.f(sum)).append("   Σx² = ").append(MathUtil.f(sum2))
                .append("\nσx = ").append(MathUtil.f(Math.sqrt(ss / n)));
        if (n > 1) sb.append("   sx = ").append(MathUtil.f(Math.sqrt(ss / (n - 1))));
        sb.append("\nMin = ").append(MathUtil.f(s[0])).append("   Max = ").append(MathUtil.f(s[n - 1]))
                .append("   Med = ").append(MathUtil.f(med));
        if (regression) {
            double my = 0;
            for (double v : y) my += v;
            my /= n;
            double sxx = 0, syy = 0, sxy = 0;
            for (int i = 0; i < n; i++) {
                sxx += (x[i] - mean) * (x[i] - mean);
                syy += (y[i] - my) * (y[i] - my);
                sxy += (x[i] - mean) * (y[i] - my);
            }
            if (sxx < MathUtil.EPS) throw new IllegalArgumentException("Math ERROR (mọi x bằng nhau)");
            double b = sxy / sxx;
            sb.append("\na = ").append(MathUtil.f(my - b * mean)).append("   b = ").append(MathUtil.f(b));
            if (syy >= MathUtil.EPS) sb.append("   r = ").append(MathUtil.f(sxy / Math.sqrt(sxx * syy)));
        }
        return sb.toString();
    }
}
