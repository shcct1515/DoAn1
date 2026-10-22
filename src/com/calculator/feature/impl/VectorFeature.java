package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

public class VectorFeature extends AbstractFeature {
    public VectorFeature() {
        super("Vector", "Tích vô hướng, tích có hướng, góc",
                "Vector u (vd 1 2 3)", "Vector v (bỏ trống nếu chỉ tính độ dài)",
                "Phép toán (dot, cross, angle, add, sub, norm)");
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
    protected String solve(String[] in, Calculator calc) {
        double[] u = MathUtil.nums(calc, in[0]), v = MathUtil.nums(calc, in[1]);
        String op = in[2].trim().toLowerCase();
        if (u.length == 0) throw new IllegalArgumentException("Thiếu vector u");
        if (op.equals("norm")) return "|u| = " + MathUtil.f(Math.sqrt(dot(u, u)));
        if (v.length != u.length) throw new IllegalArgumentException("Hai vector phải cùng số chiều");
        switch (op) {
            case "dot": return "u·v = " + MathUtil.f(dot(u, v));
            case "add": case "sub": {
                double s = op.equals("add") ? 1 : -1;
                double[] r = new double[u.length];
                for (int i = 0; i < r.length; i++) r[i] = u[i] + s * v[i];
                return vec(r);
            }
            case "cross": {
                if (u.length != 3) throw new IllegalArgumentException("Tích có hướng cần vector 3 chiều");
                double[] r = {u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]};
                return "u×v = " + vec(r);
            }
            case "angle": {
                double d = Math.sqrt(dot(u, u) * dot(v, v));
                if (d < MathUtil.EPS) throw new IllegalArgumentException("Có vector bằng 0");
                double ang = Math.acos(Math.max(-1, Math.min(1, dot(u, v) / d)));
                return "Góc = " + MathUtil.f(Math.toDegrees(ang)) + "° (" + MathUtil.f(ang) + " rad)";
            }
            default: throw new IllegalArgumentException("Phép toán: dot cross angle add sub norm");
        }
    }
}
