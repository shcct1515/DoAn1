package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

public class MatrixFeature extends AbstractFeature {
    public MatrixFeature() {
        super("Ma trận", "Cộng, nhân, định thức, nghịch đảo",
                "Ma trận A (hàng cách nhau ;  vd 1 2;3 4)", "Ma trận B (chỉ cần cho + - *)",
                "Phép toán (+, -, *, det, inv, T, rank)");
    }

    @Override
    protected String solve(String[] in, Calculator calc) {
        double[][] a = MathUtil.matrix(calc, in[0]);
        String op = in[2].trim().toLowerCase();
        int n = a.length, m = a[0].length;
        switch (op) {
            case "det": {
                if (n != m) throw new IllegalArgumentException("Cần ma trận vuông");
                double[] d = new double[1];
                MathUtil.rank(a, d);
                return "det = " + MathUtil.f(d[0]);
            }
            case "inv": {
                if (n != m) throw new IllegalArgumentException("Cần ma trận vuông");
                double[][] id = new double[n][n];
                for (int i = 0; i < n; i++) id[i][i] = 1;
                double[][] r = MathUtil.gaussJordan(a, id);
                if (r == null) throw new IllegalArgumentException("Ma trận không khả nghịch (det = 0)");
                return MathUtil.show(r);
            }
            case "t": {
                double[][] r = new double[m][n];
                for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) r[j][i] = a[i][j];
                return MathUtil.show(r);
            }
            case "rank":
                return "Hạng = " + MathUtil.rank(a, null);
            case "+": case "-": {
                double[][] b = MathUtil.matrix(calc, in[1]);
                if (b.length != n || b[0].length != m) throw new IllegalArgumentException("Hai ma trận khác cỡ");
                double s = op.equals("+") ? 1 : -1;
                double[][] r = new double[n][m];
                for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) r[i][j] = a[i][j] + s * b[i][j];
                return MathUtil.show(r);
            }
            case "*": {
                double[][] b = MathUtil.matrix(calc, in[1]);
                if (b.length != m) throw new IllegalArgumentException("Số cột A phải bằng số hàng B");
                double[][] r = new double[n][b[0].length];
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < b[0].length; j++)
                        for (int k = 0; k < m; k++) r[i][j] += a[i][k] * b[k][j];
                return MathUtil.show(r);
            }
            default:
                throw new IllegalArgumentException("Phép toán: + - * det inv T rank");
        }
    }
}
