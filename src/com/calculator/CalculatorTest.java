package com.calculator;

import com.calculator.calculator.Calculator;
import com.calculator.model.AngleMode;
import com.calculator.model.CalcException;
import com.calculator.model.DisplayMode;

/** Test đơn giản (không cần JUnit): chạy bằng `java com.calculator.CalculatorTest`. */
public class CalculatorTest {
    private static int passed = 0, failed = 0;

    public static void main(String[] args) {
        Calculator c = new Calculator();
        c.setAngleMode(AngleMode.DEG);

        // Cơ bản
        check(c, "2+3×(4-1)", "11");
        check(c, "2+3*(4-1)", "11");
        check(c, "2^10", "1024");
        check(c, "0.1+0.2", "0.3");
        check(c, "1÷3", "0.3333333333");
        check(c, "10-4-3", "3");
        check(c, "2^3^2", "512");
        // Số âm
        check(c, "-2^2", "-4");
        check(c, "2^-1", "0.5");
        check(c, "2×-3", "-6");
        check(c, "-(3)", "-3");
        check(c, "(-2)^2", "4");
        // Hàm
        check(c, "sin(30)", "0.5");
        check(c, "cos(60)", "0.5");
        check(c, "sin(180)", "0");
        check(c, "asin(1)", "90");
        check(c, "log(1000)", "3");
        check(c, "ln(e)", "1");
        check(c, "√(16)", "4");
        check(c, "3root27", "3");
        check(c, "abs(-5)", "5");
        check(c, "exp(0)", "1");
        check(c, "10^(2)", "100");
        // Giai thừa, %, tổ hợp
        check(c, "5!", "120");
        check(c, "0!", "1");
        check(c, "50%", "0.5");
        check(c, "200×10%", "20");
        check(c, "5nCr2", "10");
        check(c, "5nPr2", "20");
        // Nhân ngầm, hằng số, tự đóng ngoặc
        check(c, "2(3)", "6");
        check(c, "2π", "6.283185307");
        check(c, "3sin(30)", "1.5");
        check(c, "(1+2", "3");
        check(c, "2×(3+4", "14");
        // Số lớn / nhỏ
        check(c, "10^10", "1×10^10");
        check(c, "1÷10^10", "1×10^-10");
        // Ans và bộ nhớ
        c.evaluate("7");
        check(c, "Ans+1", "8");
        c.memoryPlus(5);
        check(c, "M×2", "10");
        // Chế độ RAD
        c.setAngleMode(AngleMode.RAD);
        check(c, "sin(π÷2)", "1");
        c.setAngleMode(AngleMode.DEG);
        // Lỗi
        checkError(c, "1÷0", "Math ERROR");
        checkError(c, "tan(90)", "Math ERROR");
        checkError(c, "√(-1)", "Math ERROR");
        checkError(c, "log(0)", "Math ERROR");
        checkError(c, "(-8)^0.5", "Math ERROR");
        checkError(c, "1+", "Syntax ERROR");
        checkError(c, "×3", "Syntax ERROR");
        checkError(c, "1+2)", "Syntax ERROR");
        checkError(c, "2..3", "Syntax ERROR");
        checkError(c, "abc", "Syntax ERROR");
        checkError(c, "171!", "Math ERROR");
        // Phân số và hỗn số (ký tự ⁄ = \u2044)
        String f = "\u2044";
        check(c, "1" + f + "2+1" + f + "3", "0.8333333333");
        check(c, "3" + f + "4×2", "1.5");
        check(c, "2" + f + "3" + f + "4", "2.75");
        check(c, "-1" + f + "2", "-0.5");
        check(c, "9^1" + f + "2", "3");
        check(c, "2(1" + f + "2)", "1");
        checkError(c, "1" + f + "0", "Math ERROR");
        checkError(c, "1" + f, "Syntax ERROR");
        checkError(c, "1.5" + f + "2", "Syntax ERROR");
        // Ký tự ẩn kết thúc số mũ: x² rồi gõ 3 = x²×3
        String e = "\u200B";
        check(c, "2^2" + e + "3", "12");
        check(c, "2^23", "8388608");
        // Biến A-F, X, Y (STO) và bộ nhớ M
        c.setVariable("A", 5);
        c.setVariable("B", 3);
        c.setVariable("M", 2);
        check(c, "A×2", "10");
        check(c, "2A", "10");
        check(c, "A+B", "8");
        check(c, "A^B", "125");
        check(c, "M+A", "7");
        check(c, "C+1", "1");
        // Kiểu hiển thị Fix / Sci / Norm
        Calculator.setDisplayFormat(DisplayMode.FIX, 2);
        eq("fix 2", "3.14", Calculator.format(3.14159));
        Calculator.setDisplayFormat(DisplayMode.FIX, 0);
        eq("fix 0", "3", Calculator.format(2.6 + 0.4));
        Calculator.setDisplayFormat(DisplayMode.FIX, 2);
        eq("fix neg", "-0.50", Calculator.format(-0.5));
        Calculator.setDisplayFormat(DisplayMode.SCI, 3);
        eq("sci 12345.678", "1.23×10^4", Calculator.format(12345.678));
        eq("sci 0.000123", "1.23×10^-4", Calculator.format(0.000123));
        eq("sci 9.999", "1.00×10^1", Calculator.format(9.9999));
        Calculator.setDisplayFormat(DisplayMode.NORM, 6);
        eq("norm 6", "0.333333", Calculator.format(1.0 / 3));
        Calculator.setDisplayFormat(DisplayMode.NORM, 10);
        eq("norm 10", "0.3333333333", Calculator.format(1.0 / 3));
        // Hiển thị
        long[] fr = Calculator.fraction(2.75);
        eq("fraction parts", "11/4", fr == null ? null : fr[0] + "/" + fr[1]);
        eq("RPN", "2 3 4 1 - × +".replace("×", "*"), c.toRPNString("2+3×(4-1)"));
        eq("fraction 0.75", "3/4", Calculator.toFraction(0.75));
        eq("fraction -1/3", "-1/3", Calculator.toFraction(-1.0 / 3));
        eq("fraction int", null, Calculator.toFraction(4));
        eq("hex", "FF", Calculator.toBase(255, 16));
        eq("bin", "1010", Calculator.toBase(10, 2));
        eq("oct", "17", Calculator.toBase(15, 8));

        System.out.println("\nPassed: " + passed + "  Failed: " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(Calculator c, String expr, String expected) {
        try {
            eq(expr, expected, Calculator.format(c.evaluate(expr)));
        } catch (RuntimeException e) {
            fail(expr, expected, e.getMessage());
        }
    }

    private static void checkError(Calculator c, String expr, String expectedMsg) {
        try {
            double v = c.evaluate(expr);
            fail(expr, expectedMsg, "kết quả " + v);
        } catch (CalcException e) {
            eq(expr, expectedMsg, e.getMessage());
        }
    }

    private static void eq(String name, String expected, String actual) {
        if ((expected == null && actual == null) || (expected != null && expected.equals(actual))) {
            passed++;
        } else {
            fail(name, expected, actual);
        }
    }

    private static void fail(String name, String expected, String actual) {
        failed++;
        System.out.println("FAIL  " + name + "  expected=" + expected + "  actual=" + actual);
    }
}
