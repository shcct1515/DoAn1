package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;
import com.calculator.feature.AbstractFeature;

import java.util.Arrays;

public class StatisticsFeature extends AbstractFeature {
    public StatisticsFeature() {
        super("Thống kê", "Trung bình, độ lệch chuẩn, hồi quy tuyến tính",
                "Dãy x (cách nhau bởi dấu cách)", "Dãy y (tuỳ chọn, để hồi quy y = a + bx)");
    }

    @Override
    protected String solve(String[] in, Calculator calc) {
        double[] x = MathUtil.nums(calc, in[0]), y = MathUtil.nums(calc, in[1]);
        int n = x.length;
        if (n == 0) throw new IllegalArgumentException("Chưa nhập dữ liệu");
        double sum = 0, sum2 = 0;
        for (double v : x) {
            sum += v;
            sum2 += v * v;
        }
        double mean = sum / n, ss = sum2 - n * mean * mean;
        double[] s = x.clone();
        Arrays.sort(s);
        double med = n % 2 == 1 ? s[n / 2] : (s[n / 2 - 1] + s[n / 2]) / 2;
        StringBuilder sb = new StringBuilder();
        sb.append("n = ").append(n).append("\nΣx = ").append(MathUtil.f(sum))
                .append("\nΣx² = ").append(MathUtil.f(sum2))
                .append("\nTrung bình = ").append(MathUtil.f(mean))
                .append("\nTrung vị = ").append(MathUtil.f(med))
                .append("\nMin = ").append(MathUtil.f(s[0])).append(", Max = ").append(MathUtil.f(s[n - 1]))
                .append("\nσ (tổng thể) = ").append(MathUtil.f(Math.sqrt(Math.max(0, ss) / n)));
        if (n > 1) sb.append("\ns (mẫu) = ").append(MathUtil.f(Math.sqrt(Math.max(0, ss) / (n - 1))));
        if (y.length > 0) {
            if (y.length != n || n < 2) throw new IllegalArgumentException("x và y phải cùng số phần tử (≥ 2)");
            double my = 0;
            for (double v : y) my += v;
            my /= n;
            double sxx = 0, syy = 0, sxy = 0;
            for (int i = 0; i < n; i++) {
                sxx += (x[i] - mean) * (x[i] - mean);
                syy += (y[i] - my) * (y[i] - my);
                sxy += (x[i] - mean) * (y[i] - my);
            }
            if (sxx < MathUtil.EPS) throw new IllegalArgumentException("Mọi x bằng nhau, không hồi quy được");
            double b = sxy / sxx, a = my - b * mean;
            sb.append("\n\nHồi quy y = a + bx\na = ").append(MathUtil.f(a)).append("\nb = ").append(MathUtil.f(b));
            if (syy >= MathUtil.EPS) sb.append("\nr = ").append(MathUtil.f(sxy / Math.sqrt(sxx * syy)));
        }
        return sb.toString();
    }
}
