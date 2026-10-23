package com.calculator.feature.impl;

/** Tên hệ số a, b, c, d... và dạng đa thức hiển thị trên LCD. */
final class Arrays4 {
    private Arrays4() {
    }

    static String[] names(int n) {
        String[] r = new String[n];
        for (int i = 0; i < n; i++) r[i] = String.valueOf((char) ('a' + i));
        return r;
    }

    /** deg 3 -> "ax³+bx²+cx+d". */
    static String form(int deg) {
        String[] sup = {"", "", "²", "³", "⁴"};
        String[] c = names(deg + 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= deg; i++) {
            int e = deg - i;
            if (i > 0) sb.append("+");
            sb.append(c[i]);
            if (e >= 1) sb.append("x");
            if (e >= 2) sb.append(sup[e]);
        }
        return sb.toString();
    }
}
