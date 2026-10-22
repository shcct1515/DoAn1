package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InequalityFeature extends AbstractFeature {
    public InequalityFeature() {
        super("Giải bất phương trình", "Bậc 1, 2, 3",
                "Hệ số từ cao xuống thấp (vd: 1 -3 2)", "Dấu so sánh với 0 (<, <=, >, >=)");
    }

    @Override
    protected String solve(String[] in, Calculator calc) {
        double[] c = MathUtil.nums(calc, in[0]);
        String op = in[1].trim().replace("≤", "<=").replace("≥", ">=");
        if (!op.matches("<=?|>=?")) throw new IllegalArgumentException("Dấu phải là <, <=, > hoặc >=");
        boolean less = op.startsWith("<"), eq = op.endsWith("=");

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
