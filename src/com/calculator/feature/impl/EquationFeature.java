package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

public class EquationFeature extends AbstractFeature {
    public EquationFeature() {
        super("Giải phương trình", "Bậc 1, 2, 3", "Hệ số từ cao xuống thấp (vd bậc 2: 1 -3 2)");
    }

    @Override
    protected String solve(String[] in, Calculator calc) {
        double[] c = MathUtil.nums(calc, in[0]);
        List<double[]> r = MathUtil.roots(c);
        StringBuilder sb = new StringBuilder("Bậc " + r.size() + "\n");
        if (r.size() == 2) {
            double d = c[1] * c[1] - 4 * c[0] * c[2];
            sb.append("Δ = ").append(MathUtil.f(d)).append("\n");
        }
        for (int i = 0; i < r.size(); i++) {
            sb.append("x").append(i + 1).append(" = ").append(MathUtil.cx(r.get(i)[0], r.get(i)[1])).append("\n");
        }
        return sb.toString().trim();
    }
}
