package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

public class EquationFeature extends AbstractFeature {
    private final int degree;

    public EquationFeature(int degree) {
        super(Arrays4.form(degree) + "=0", Arrays4.names(degree + 1));
        this.degree = degree;
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double[] c = new double[in.size()];
        for (int i = 0; i < c.length; i++) c[i] = val(calc, in, i);
        if (degree == 1) return solveLinear(c[0], c[1]);
        List<double[]> r = MathUtil.roots(c);
        StringBuilder sb = new StringBuilder();
        if (c.length == 3) sb.append("Δ = ").append(MathUtil.f(c[1] * c[1] - 4 * c[0] * c[2])).append("\n");
        for (int i = 0; i < r.size(); i++) {
            sb.append("x").append(i + 1).append(" = ").append(MathUtil.cx(r.get(i)[0], r.get(i)[1])).append("\n");
        }
        return sb.toString().trim();
    }

    /** ax + b = 0. */
    private static String solveLinear(double a, double b) {
        if (Math.abs(a) < MathUtil.EPS) return Math.abs(b) < MathUtil.EPS ? "Vô số nghiệm (mọi x)" : "Vô nghiệm";
        double x = -b / a;
        String d = MathUtil.f(x), q = MathUtil.fr(x);
        return d.equals(q) ? "x = " + d : "x = " + q + "\n  ≈ " + d;
    }
}
