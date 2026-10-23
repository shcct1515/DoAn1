package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

public class EquationFeature extends AbstractFeature {
    public EquationFeature(int degree) {
        super(Arrays4.form(degree) + "=0", Arrays4.names(degree + 1));
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double[] c = new double[in.size()];
        for (int i = 0; i < c.length; i++) c[i] = val(calc, in, i);
        List<double[]> r = MathUtil.roots(c);
        StringBuilder sb = new StringBuilder();
        if (c.length == 3) sb.append("Δ = ").append(MathUtil.f(c[1] * c[1] - 4 * c[0] * c[2])).append("\n");
        for (int i = 0; i < r.size(); i++) {
            sb.append("x").append(i + 1).append(" = ").append(MathUtil.cx(r.get(i)[0], r.get(i)[1])).append("\n");
        }
        return sb.toString().trim();
    }
}
