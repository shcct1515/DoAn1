package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

public class VectorFeature extends AbstractFeature {
    private final String op;
    private final int dim;

    public VectorFeature(String op, int dim) {
        super("Vector " + dim + "D: " + op, labels(op, dim));
        this.op = op;
        this.dim = dim;
    }

    private static String[] labels(String op, int dim) {
        String[] axes = {"x", "y", "z"};
        int cnt = op.equals("norm") ? dim : 2 * dim;
        String[] r = new String[cnt];
        for (int i = 0; i < cnt; i++) r[i] = (i < dim ? "u" : "v") + axes[i % dim];
        return r;
    }

    private static String vec(double[] v) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < v.length; i++) sb.append(i > 0 ? "; " : "").append(MathUtil.f(v[i]));
        return sb.append(")").toString();
    }

    private static double dot(double[] a, double[] b) {
        double s = 0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double[] u = new double[dim], v = new double[dim];
        for (int i = 0; i < dim; i++) {
            u[i] = val(calc, in, i);
            if (in.size() > dim) v[i] = val(calc, in, dim + i);
        }
        switch (op) {
            case "norm": return "|u| = " + MathUtil.f(Math.sqrt(dot(u, u)));
            case "dot": return "u·v = " + MathUtil.f(dot(u, v));
            case "add": case "sub": {
                double s = op.equals("add") ? 1 : -1;
                double[] r = new double[dim];
                for (int i = 0; i < dim; i++) r[i] = u[i] + s * v[i];
                return vec(r);
            }
            case "cross":
                return "u×v = " + vec(new double[]{u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2],
                        u[0] * v[1] - u[1] * v[0]});
            default: {
                double d = Math.sqrt(dot(u, u) * dot(v, v));
                if (d < MathUtil.EPS) throw new IllegalArgumentException("Math ERROR (vector 0)");
                double ang = Math.acos(Math.max(-1, Math.min(1, dot(u, v) / d)));
                return "Góc = " + MathUtil.f(Math.toDegrees(ang)) + "°\n    = " + MathUtil.f(ang) + " rad";
            }
        }
    }
}
