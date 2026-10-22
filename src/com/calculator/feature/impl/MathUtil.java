package com.calculator.feature.impl;

import com.calculator.calculator.Calculator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Hàm dùng chung cho các chức năng mở rộng: đọc số, định dạng, nghiệm đa thức, ma trận. */
final class MathUtil {
    static final double EPS = 1e-9;

    private MathUtil() {
    }

    static String f(double v) {
        if (Math.abs(v) < EPS) v = 0;
        return Calculator.format(v);
    }

    /** "1 2.5, 1/3" -> {1, 2.5, 0.333...}. */
    static double[] nums(Calculator calc, String text) {
        String t = text.trim();
        if (t.isEmpty()) return new double[0];
        String[] p = t.split("[\\s,]+");
        double[] r = new double[p.length];
        for (int i = 0; i < p.length; i++) r[i] = calc.evaluate(p[i]);
        return r;
    }

    static String cx(double re, double im) {
        if (Math.abs(re) < EPS) re = 0;
        if (Math.abs(im) < EPS) im = 0;
        if (im == 0) return f(re);
        String mag = Math.abs(Math.abs(im) - 1) < EPS ? "" : f(Math.abs(im));
        if (re == 0) return (im < 0 ? "-" : "") + mag + "i";
        return f(re) + (im < 0 ? " - " : " + ") + mag + "i";
    }

    /** Nghiệm đa thức bậc 1..3 (hệ số từ cao xuống thấp). Mỗi nghiệm là {phần thực, phần ảo}. */
    static List<double[]> roots(double[] c) {
        int s = 0;
        while (s < c.length && Math.abs(c[s]) < EPS) s++;
        int deg = c.length - s - 1;
        if (deg < 1 || deg > 3) throw new IllegalArgumentException("Chỉ hỗ trợ bậc 1 đến 3");
        double[] a = Arrays.copyOfRange(c, s, c.length);
        List<double[]> r = new ArrayList<>();
        if (deg == 1) {
            r.add(new double[]{-a[1] / a[0], 0});
        } else if (deg == 2) {
            double d = a[1] * a[1] - 4 * a[0] * a[2];
            double x = -a[1] / (2 * a[0]);
            if (Math.abs(d) < EPS) {
                r.add(new double[]{x, 0});
                r.add(new double[]{x, 0});
            } else if (d > 0) {
                double sq = Math.sqrt(d) / (2 * a[0]);
                r.add(new double[]{x - sq, 0});
                r.add(new double[]{x + sq, 0});
            } else {
                double sq = Math.sqrt(-d) / (2 * Math.abs(a[0]));
                r.add(new double[]{x, -sq});
                r.add(new double[]{x, sq});
            }
        } else {
            double p = a[1] / a[0], q = a[2] / a[0], rr = a[3] / a[0];
            double P = q - p * p / 3;
            double Q = 2 * p * p * p / 27 - p * q / 3 + rr;
            double shift = -p / 3;
            double disc = Q * Q / 4 + P * P * P / 27;
            if (Math.abs(disc) < EPS) {
                if (Math.abs(P) < EPS && Math.abs(Q) < EPS) {
                    for (int i = 0; i < 3; i++) r.add(new double[]{shift, 0});
                } else {
                    r.add(new double[]{3 * Q / P + shift, 0});
                    r.add(new double[]{-3 * Q / (2 * P) + shift, 0});
                    r.add(new double[]{-3 * Q / (2 * P) + shift, 0});
                }
            } else if (disc > 0) {
                double sd = Math.sqrt(disc);
                double u = Math.cbrt(-Q / 2 + sd), v = Math.cbrt(-Q / 2 - sd);
                r.add(new double[]{u + v + shift, 0});
                r.add(new double[]{-(u + v) / 2 + shift, Math.sqrt(3) / 2 * Math.abs(u - v)});
                r.add(new double[]{-(u + v) / 2 + shift, -Math.sqrt(3) / 2 * Math.abs(u - v)});
            } else {
                double m = 2 * Math.sqrt(-P / 3);
                double arg = Math.max(-1, Math.min(1, 3 * Q / (P * m)));
                double th = Math.acos(arg) / 3;
                for (int k = 0; k < 3; k++) {
                    r.add(new double[]{m * Math.cos(th - 2 * Math.PI * k / 3) + shift, 0});
                }
            }
        }
        return r;
    }

    static double horner(double[] c, double x) {
        double v = 0;
        for (double k : c) v = v * x + k;
        return v;
    }

    // ---------------------------------------------------------------- ma trận
    /** "1 2;3 4" -> ma trận 2x2. */
    static double[][] matrix(Calculator calc, String text) {
        String[] rows = text.trim().split(";");
        double[][] m = new double[rows.length][];
        for (int i = 0; i < rows.length; i++) {
            m[i] = nums(calc, rows[i]);
            if (m[i].length == 0 || m[i].length != m[0].length) {
                throw new IllegalArgumentException("Ma trận sai dạng (hàng cách nhau bằng ;)");
            }
        }
        return m;
    }

    static String show(double[][] m) {
        StringBuilder sb = new StringBuilder();
        for (double[] row : m) {
            for (double v : row) sb.append(f(v)).append("\t");
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    static double[][] copy(double[][] m) {
        double[][] r = new double[m.length][];
        for (int i = 0; i < m.length; i++) r[i] = m[i].clone();
        return r;
    }

    /** Khử Gauss, trả về hạng; nếu det != null thì det[0] = định thức (ma trận vuông). */
    static int rank(double[][] src, double[] det) {
        double[][] m = copy(src);
        int rows = m.length, cols = m[0].length, rank = 0;
        double d = 1;
        for (int c = 0; c < cols && rank < rows; c++) {
            int piv = rank;
            for (int i = rank + 1; i < rows; i++) if (Math.abs(m[i][c]) > Math.abs(m[piv][c])) piv = i;
            if (Math.abs(m[piv][c]) < EPS) {
                d = 0;
                continue;
            }
            if (piv != rank) {
                double[] t = m[piv];
                m[piv] = m[rank];
                m[rank] = t;
                d = -d;
            }
            d *= m[rank][c];
            for (int i = rank + 1; i < rows; i++) {
                double k = m[i][c] / m[rank][c];
                for (int j = c; j < cols; j++) m[i][j] -= k * m[rank][j];
            }
            rank++;
        }
        if (det != null) det[0] = (rank == rows && rows == cols) ? d : 0;
        return rank;
    }

    /** Gauss-Jordan trên ma trận mở rộng [A | B]; trả về phần bên phải sau khi khử. Null nếu A suy biến. */
    static double[][] gaussJordan(double[][] a, double[][] b) {
        int n = a.length, k = b[0].length;
        double[][] m = new double[n][n + k];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], 0, m[i], 0, n);
            System.arraycopy(b[i], 0, m[i], n, k);
        }
        for (int c = 0; c < n; c++) {
            int piv = c;
            for (int i = c + 1; i < n; i++) if (Math.abs(m[i][c]) > Math.abs(m[piv][c])) piv = i;
            if (Math.abs(m[piv][c]) < EPS) return null;
            double[] t = m[piv];
            m[piv] = m[c];
            m[c] = t;
            double p = m[c][c];
            for (int j = 0; j < n + k; j++) m[c][j] /= p;
            for (int i = 0; i < n; i++) {
                if (i == c) continue;
                double q = m[i][c];
                for (int j = 0; j < n + k; j++) m[i][j] -= q * m[c][j];
            }
        }
        double[][] r = new double[n][k];
        for (int i = 0; i < n; i++) System.arraycopy(m[i], n, r[i], 0, k);
        return r;
    }
}
