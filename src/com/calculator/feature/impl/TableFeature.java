package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.List;

public class TableFeature extends AbstractFeature {
    public TableFeature() {
        super("Bảng giá trị f(X)", "f(X)", "Start", "End", "Step");
    }

    @Override
    public void check(int index, String value, Calculator calc) {
        if (index == 0) {
            if (value.isBlank()) throw new IllegalArgumentException("Chưa nhập f(X)");
            return;     // f(X) chứa biến X nên không tính thử ở đây
        }
        super.check(index, value, calc);
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        String expr = in.get(0).trim();
        double from = val(calc, in, 1), to = val(calc, in, 2), step = val(calc, in, 3);
        if (step <= 0 || to < from) throw new IllegalArgumentException("Start ≤ End và Step > 0");
        if ((to - from) / step > 200) throw new IllegalArgumentException("Tối đa 200 dòng, tăng Step");
        double old = calc.getVariable("X");
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 0; from + i * step <= to + 1e-12; i++) {
                double x = from + i * step;
                calc.setVariable("X", x);
                String y;
                try {
                    y = MathUtil.f(calc.evaluate(expr));
                } catch (Exception e) {
                    y = "ERROR";
                }
                sb.append("X=").append(MathUtil.f(x)).append("  f=").append(y).append("\n");
            }
        } finally {
            calc.setVariable("X", old);
        }
        return sb.toString().trim();
    }
}
