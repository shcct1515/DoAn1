package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

public class MatrixFeature extends AbstractFeature {
    private final String op;
    private final int n;

    public MatrixFeature(String op, int n) {
        super("Ma trận " + n + "×" + n + ": " + op.replace("add", "A+B").replace("mul", "A×B")
                .replace("det", "det A").replace("inv", "A⁻¹"), labels(op, n));
        this.op = op;
        this.n = n;
    }

    private static String[] labels(String op, int n) {
        boolean two = op.equals("add") || op.equals("mul");
        String[] r = new String[two ? 2 * n * n : n * n];
        for (int k = 0; k < r.length; k++) {
            int cell = k % (n * n);
            r[k] = (k < n * n ? "A" : "B") + (cell / n + 1) + (cell % n + 1);
        }
        return r;
    }

    private double[][] read(Calculator calc, List<String> in, int offset) {
        double[][] m = new double[n][n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) m[i][j] = val(calc, in, offset + i * n + j);
        return m;
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double[][] a = read(calc, in, 0);
        switch (op) {
            case "det": {
                double[] d = new double[1];
                MathUtil.rank(a, d);
                return "det A = " + MathUtil.f(d[0]);
            }
            case "inv": {
                double[][] id = new double[n][n];
                for (int i = 0; i < n; i++) id[i][i] = 1;
                double[][] r = MathUtil.gaussJordan(a, id);
                if (r == null) throw new IllegalArgumentException("Math ERROR (det = 0)");
                return MathUtil.show(r);
            }
            case "add": {
                double[][] b = read(calc, in, n * n), r = new double[n][n];
                for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) r[i][j] = a[i][j] + b[i][j];
                return MathUtil.show(r);
            }
            default: {
                double[][] b = read(calc, in, n * n), r = new double[n][n];
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < n; j++)
                        for (int k = 0; k < n; k++) r[i][j] += a[i][k] * b[k][j];
                return MathUtil.show(r);
            }
        }
    }
}
