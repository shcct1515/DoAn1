package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InequalityFeature extends AbstractFeature {
    private final String op;

    public InequalityFeature(int degree, String op) {
        super(Arrays4.form(degree) + op.replace("<=", "≤").replace(">=", "≥") + "0", Arrays4.names(degree + 1));
        this.op = op;
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double[] c = new double[in.size()];
        for (int k = 0; k < c.length; k++) c[k] = val(calc, in, k);
        boolean less = op.startsWith("<"), eq = op.endsWith("=");

        // mọi hệ số đứng trước hệ số tự do đều bằng 0 -> chỉ còn hằng số c (vd 0x + 5 > 0)
        boolean constant = true;
        for (int k = 0; k < c.length - 1; k++) if (Math.abs(c[k]) >= MathUtil.EPS) constant = false;
        if (constant) {
            double v = c[c.length - 1];
            boolean zero = Math.abs(v) < MathUtil.EPS;
            boolean holds = zero ? eq : (less ? v < 0 : v > 0);
            return holds ? "Nghiệm: ℝ (mọi x)" : "Vô nghiệm";
        }

        List<Double> rt = new ArrayList<>();
        for (double[] z : MathUtil.roots(c)) if (Math.abs(z[1]) < 1e-7) rt.add(z[0]);
        Collections.sort(rt);
        List<Double> roots = new ArrayList<>();
        for (double v : rt) if (roots.isEmpty() || v - roots.get(roots.size() - 1) > 1e-7) roots.add(v);
        int n = roots.size();

        // phần tử xen kẽ: khoảng 0, nghiệm 0, khoảng 1, nghiệm 1, ..., khoảng n
        boolean[] ok = new boolean[2 * n + 1];
        for (int k = 0; k <= n; k++) {
            double s = n == 0 ? 0 : k == 0 ? roots.get(0) - 1 : k == n ? roots.get(n - 1) + 1
                    : (roots.get(k - 1) + roots.get(k)) / 2;
            double v = MathUtil.horner(c, s);
            ok[2 * k] = less ? v < 0 : v > 0;
            if (k < n) ok[2 * k + 1] = eq;
        }
        List<String> parts = new ArrayList<>();
        int i = 0;
        while (i < ok.length) {
            if (!ok[i]) {
                i++;
                continue;
            }
            int s = i;
            while (i + 1 < ok.length && ok[i + 1]) i++;
            int e = i++;
            if (s == 0 && e == 2 * n) return "Nghiệm: ℝ (mọi x)";
            if (s == e && s % 2 == 1) {
                parts.add("{" + MathUtil.f(roots.get((s - 1) / 2)) + "}");
                continue;
            }
            String l = s == 0 ? "(-∞" : s % 2 == 1 ? "[" + MathUtil.f(roots.get((s - 1) / 2))
                    : "(" + MathUtil.f(roots.get(s / 2 - 1));
            String r = e == 2 * n ? "+∞)" : e % 2 == 1 ? MathUtil.f(roots.get((e - 1) / 2)) + "]"
                    : MathUtil.f(roots.get(e / 2)) + ")";
            parts.add(l + "; " + r);
        }
        return parts.isEmpty() ? "Vô nghiệm" : "Nghiệm: " + String.join(" ∪ ", parts);
    }
}
