package com.calculator;

import com.calculator.calculator.Calculator;
import com.calculator.feature.Feature;
import com.calculator.feature.impl.*;
import com.calculator.model.AngleMode;

import java.util.Arrays;

/** Test các chức năng trong menu MODE: chạy bằng `java com.calculator.FeatureTest`. */
public class FeatureTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        Calculator c = new Calculator();
        c.setAngleMode(AngleMode.RAD);

        // Phương trình bậc 1
        eq(c, new EquationFeature(1), "2 -4", "x = 2");
        eq(c, new EquationFeature(1), "3 2", "x = -2/3\n  ≈ -0.6666666667");
        eq(c, new EquationFeature(1), "0 5", "Vô nghiệm");
        eq(c, new EquationFeature(1), "0 0", "Vô số nghiệm (mọi x)");
        // Bất phương trình bậc 1
        eq(c, new InequalityFeature(1, "<"), "2 -4", "Nghiệm: (-∞; 2)");
        eq(c, new InequalityFeature(1, ">="), "2 -4", "Nghiệm: [2; +∞)");
        eq(c, new InequalityFeature(1, ">"), "-1 3", "Nghiệm: (-∞; 3)");
        eq(c, new InequalityFeature(1, "<="), "-1 3", "Nghiệm: [3; +∞)");
        eq(c, new InequalityFeature(1, ">"), "0 5", "Nghiệm: ℝ (mọi x)");
        eq(c, new InequalityFeature(1, "<"), "0 5", "Vô nghiệm");
        eq(c, new InequalityFeature(1, "<="), "0 0", "Nghiệm: ℝ (mọi x)");
        eq(c, new InequalityFeature(1, "<"), "0 0", "Vô nghiệm");
        // bất PT bậc 2 cũ vẫn đúng
        eq(c, new InequalityFeature(2, "<"), "1 -3 2", "Nghiệm: (1; 2)");

        // Ma trận nghịch đảo
        eq(c, new MatrixFeature("inv", 2), "1 2 3 4", "det A = -2\n -2     1\n3/2  -1/2");
        eq(c, new MatrixFeature("inv", 3), "2 0 0 0 3 0 0 0 4", "det A = 24\n1/2    0    0\n  0  1/3    0\n  0    0  1/4");
        eq(c, new MatrixFeature("inv", 2), "1 2 2 4", "Math ERROR (det = 0)");
        // 4×4: A·A⁻¹ = I (kiểm tra bằng cách nhân lại)
        inv4(c);

        System.out.println("\nPassed: " + passed + "  Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    /** Đầu vào dạng "ô1 ô2 ..." (cách nhau bằng dấu cách) hoặc "f(X)|a" cho đạo hàm. */
    private static String run(Calculator c, Feature f, String input) {
        String[] cells = input.contains("|") ? input.split("\\|") : input.split("\\s+");
        try {
            return f.solve(Arrays.asList(cells), c);
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    private static void eq(Calculator c, Feature f, String input, String expected) {
        String got = run(c, f, input);
        report(got.equals(expected), f.getName() + " [" + input + "]", expected, got);
    }

    private static void inv4(Calculator c) {
        double[][] a = {{2, 1, 0, 1}, {1, 3, 1, 0}, {0, 1, 4, 1}, {1, 0, 1, 5}};
        StringBuilder in = new StringBuilder();
        for (double[] r : a) for (double v : r) in.append((int) v).append(' ');
        String out = run(c, new MatrixFeature("inv", 4), in.toString().trim());
        double[][] inv = new double[4][4];
        String[] lines = out.split("\n");
        int found = 0;
        for (int i = 1; i < lines.length; i++) {          // dòng 0 là det A
            String[] p = lines[i].trim().split("\\s+");
            for (int j = 0; j < p.length; j++, found++)
                inv[i - 1][j] = p[j].contains("/")
                        ? Double.parseDouble(p[j].split("/")[0]) / Double.parseDouble(p[j].split("/")[1]) : Double.parseDouble(p[j]);
        }
        boolean ok = found == 16;
        for (int i = 0; ok && i < 4; i++)
            for (int j = 0; j < 4; j++) {
                double s = 0;
                for (int k = 0; k < 4; k++) s += a[i][k] * inv[k][j];
                if (Math.abs(s - (i == j ? 1 : 0)) > 1e-6) ok = false;
            }
        report(ok, "nghịch đảo 4×4: A·A⁻¹ = I", "I", out.replace("\n", " | "));
    }

    private static void report(boolean ok, String name, String expected, String got) {
        if (ok) passed++;
        else {
            failed++;
            System.out.println("FAIL " + name + "\n  mong đợi: " + expected.replace("\n", "⏎") + "\n  nhận được: " + got.replace("\n", "⏎"));
        }
    }
}
