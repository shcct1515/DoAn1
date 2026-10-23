package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

public class ComplexFeature extends AbstractFeature {
    private final String op;

    public ComplexFeature(String op) {
        super("Số phức: " + title(op), labels(op));
        this.op = op;
    }

    private static String title(String op) {
        switch (op) {
            case "add": return "z1 + z2";
            case "sub": return "z1 − z2";
            case "mul": return "z1 × z2";
            case "div": return "z1 ÷ z2";
            case "pow": return "z^n";
            default: return "Mô-đun/Arg";
        }
    }

    private static String[] labels(String op) {
        switch (op) {
            case "pow": return new String[]{"Re z", "Im z", "n"};
            case "modarg": return new String[]{"Re z", "Im z"};
            default: return new String[]{"Re z1", "Im z1", "Re z2", "Im z2"};
        }
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double a = val(calc, in, 0), b = val(calc, in, 1);
        double c = in.size() > 2 ? val(calc, in, 2) : 0, d = in.size() > 3 ? val(calc, in, 3) : 0;
        double re, im;
        switch (op) {
            case "add": re = a + c; im = b + d; break;
            case "sub": re = a - c; im = b - d; break;
            case "mul": re = a * c - b * d; im = a * d + b * c; break;
            case "div": {
                double den = c * c + d * d;
                if (den < MathUtil.EPS) throw new IllegalArgumentException("Math ERROR (chia 0)");
                re = (a * c + b * d) / den;
                im = (b * c - a * d) / den;
                break;
            }
            case "pow": {
                double r = Math.pow(Math.hypot(a, b), c), th = Math.atan2(b, a) * c;
                re = r * Math.cos(th);
                im = r * Math.sin(th);
                break;
            }
            default: re = a; im = b;
        }
        double mod = Math.hypot(re, im), arg = Math.atan2(im, re);
        return "= " + MathUtil.cx(re, im) + "\nLiên hợp: " + MathUtil.cx(re, -im)
                + "\n|z| = " + MathUtil.f(mod)
                + "\narg = " + MathUtil.f(Math.toDegrees(arg)) + "° (" + MathUtil.f(arg) + " rad)";
    }
}
