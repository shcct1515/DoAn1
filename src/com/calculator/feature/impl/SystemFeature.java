package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

public class SystemFeature extends AbstractFeature {
    public SystemFeature() {
        super("Giải hệ phương trình", "Hệ 2 ẩn, 3 ẩn",
                "PT1 (2 ẩn: a b c, 3 ẩn: a b c d)", "PT2", "PT3 (bỏ trống nếu hệ 2 ẩn)");
    }

    @Override
    protected String solve(String[] in, Calculator calc) {
        int n = in[2].trim().isEmpty() ? 2 : 3;
        double[][] m = new double[n][];
        for (int i = 0; i < n; i++) {
            m[i] = MathUtil.nums(calc, in[i]);
            if (m[i].length != n + 1) throw new IllegalArgumentException("Mỗi PT cần " + (n + 1) + " số");
        }
        double[][] a = new double[n][n], b = new double[n][1];
        for (int i = 0; i < n; i++) {
            System.arraycopy(m[i], 0, a[i], 0, n);
            b[i][0] = m[i][n];
        }
        double[][] x = MathUtil.gaussJordan(a, b);
        if (x == null) {
            int ra = MathUtil.rank(a, null), rm = MathUtil.rank(m, null);
            return ra == rm ? "Hệ có vô số nghiệm" : "Hệ vô nghiệm";
        }
        String[] names = {"x", "y", "z"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(names[i]).append(" = ").append(MathUtil.f(x[i][0])).append("\n");
        return sb.toString().trim();
    }
}
