package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

public class ComplexFeature extends AbstractFeature {
    public ComplexFeature() {
        super("Số phức", "Cộng, trừ, nhân, chia, luỹ thừa",
                "z1 (vd 3+4i)", "z2 (vd 1-2i; với ^ nhập số mũ nguyên)", "Phép toán (+, -, *, /, ^)");
    }

    /** Đọc "a+bi" thành {a, b}. */
    private static double[] parse(Calculator calc, String text) {
        String s = text.replace(" ", "");
        if (s.isEmpty()) throw new IllegalArgumentException("Thiếu số phức");
        double re = 0, im = 0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[+-]?[^+-]+").matcher(s);
        while (m.find()) {
            String t = m.group();
            if (t.endsWith("i")) {
                String k = t.substring(0, t.length() - 1);
                if (k.isEmpty() || k.equals("+")) im += 1;
                else if (k.equals("-")) im -= 1;
                else im += calc.evaluate(k.startsWith("+") ? k.substring(1) : k);
            } else {
                re += calc.evaluate(t.startsWith("+") ? t.substring(1) : t);
            }
        }
        return new double[]{re, im};
    }

    @Override
    protected String solve(String[] in, Calculator calc) {
        double[] a = parse(calc, in[0]), b = parse(calc, in[1]);
        double re, im;
        switch (in[2].trim()) {
            case "+": re = a[0] + b[0]; im = a[1] + b[1]; break;
            case "-": re = a[0] - b[0]; im = a[1] - b[1]; break;
            case "*":
                re = a[0] * b[0] - a[1] * b[1];
                im = a[0] * b[1] + a[1] * b[0];
                break;
            case "/": {
                double d = b[0] * b[0] + b[1] * b[1];
                if (d < MathUtil.EPS) throw new IllegalArgumentException("Chia cho 0");
                re = (a[0] * b[0] + a[1] * b[1]) / d;
                im = (a[1] * b[0] - a[0] * b[1]) / d;
                break;
            }
            case "^": {
                double r = Math.pow(Math.hypot(a[0], a[1]), b[0]);
                double th = Math.atan2(a[1], a[0]) * b[0];
                re = r * Math.cos(th);
                im = r * Math.sin(th);
                break;
            }
            default: throw new IllegalArgumentException("Phép toán phải là + - * / ^");
        }
        double mod = Math.hypot(re, im), arg = Math.atan2(im, re);
        return "Kết quả = " + MathUtil.cx(re, im)
                + "\nLiên hợp = " + MathUtil.cx(re, -im)
                + "\n|z| = " + MathUtil.f(mod)
                + "\narg = " + MathUtil.f(arg) + " rad = " + MathUtil.f(Math.toDegrees(arg)) + "°";
    }
}
