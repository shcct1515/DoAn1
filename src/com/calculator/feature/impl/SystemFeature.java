package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

public class SystemFeature extends AbstractFeature {
    private final int n;

    public SystemFeature(int n) {
        super("Hệ " + n + " ẩn", labels(n));
        this.n = n;
    }

    @Override public int[] grid() { return new int[]{n, n + 1, 1}; }

    @Override public boolean gridAugmented() { return true; }

    private static String[] labels(int n) {
        String[] base = Arrays4.names(n + 1);
        String[] r = new String[n * (n + 1)];
        for (int i = 0; i < n; i++)
            for (int j = 0; j <= n; j++) r[i * (n + 1) + j] = base[j] + (i + 1);
        return r;
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double[][] m = new double[n][n + 1], a = new double[n][n], b = new double[n][1];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j <= n; j++) m[i][j] = val(calc, in, i * (n + 1) + j);
            System.arraycopy(m[i], 0, a[i], 0, n);
            b[i][0] = m[i][n];
        }
        double[][] x = MathUtil.gaussJordan(a, b);
        if (x == null) return MathUtil.rank(a, null) == MathUtil.rank(m, null) ? "Vô số nghiệm" : "Vô nghiệm";
        String[] names = {"x", "y", "z", "t"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(names[i]).append(" = ").append(MathUtil.f(x[i][0])).append("\n");
        return sb.toString().trim();
    }
}
