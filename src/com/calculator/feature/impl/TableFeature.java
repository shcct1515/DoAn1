package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

public class TableFeature extends AbstractFeature {
    public TableFeature() {
        super("Bảng giá trị hàm", "Bảng giá trị f(x)", "f(x) (dùng biến X, vd X^2+1)", "Từ x", "Đến x", "Bước");
    }

    @Override
    protected String solve(String[] in, Calculator calc) {
        String expr = in[0].trim();
        if (expr.isEmpty()) throw new IllegalArgumentException("Chưa nhập f(x)");
        double from = calc.evaluate(in[1].trim()), to = calc.evaluate(in[2].trim()), step = calc.evaluate(in[3].trim());
        if (step <= 0 || to < from) throw new IllegalArgumentException("Cần Từ ≤ Đến và Bước > 0");
        if ((to - from) / step > 200) throw new IllegalArgumentException("Tối đa 200 dòng, hãy tăng bước");
        double old = calc.getVariable("X");
        StringBuilder sb = new StringBuilder("x\tf(x)\n");
        try {
            for (int i = 0; from + i * step <= to + 1e-12; i++) {
                double x = from + i * step;
                calc.setVariable("X", x);
                String y;
                try {
                    y = MathUtil.f(calc.evaluate(expr));
                } catch (Exception e) {
                    y = "lỗi";
                }
                sb.append(MathUtil.f(x)).append("\t").append(y).append("\n");
            }
        } finally {
            calc.setVariable("X", old);
        }
        return sb.toString().trim();
    }
}
