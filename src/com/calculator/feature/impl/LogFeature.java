package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

/** Logarit cơ số bất kỳ: logₐ(b). */
public class LogFeature extends AbstractFeature {
    public LogFeature() {
        super("logₐ(b)", "a (cơ số)", "b");
    }

    @Override
    public void check(int index, String value, Calculator calc) {
        double v = calc.evaluate(value);
        if (index == 0 && (v <= 0 || v == 1)) throw new IllegalArgumentException("Cơ số phải > 0 và ≠ 1");
        if (index == 1 && v <= 0) throw new IllegalArgumentException("b phải > 0");
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double a = val(calc, in, 0), b = val(calc, in, 1);
        return "logₐ(b) = " + MathUtil.f(Math.log(b) / Math.log(a))
                + "\nln b = " + MathUtil.f(Math.log(b))
                + "\nlog b = " + MathUtil.f(Math.log10(b));
    }
}
