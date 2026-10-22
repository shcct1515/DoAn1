package com.calculator.calculator;

import com.calculator.evaluator.RPNEvaluator;
import com.calculator.model.AngleMode;
import com.calculator.model.CalcException;
import com.calculator.model.DisplayMode;
import com.calculator.model.Token;
import com.calculator.parser.RPNConverter;
import com.calculator.parser.Tokenizer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Điều phối quá trình: Tokenizer → RPNConverter → RPNEvaluator.
 * Lưu Ans (kết quả trước), bộ nhớ M và chế độ góc.
 */
public class Calculator {
    private final Tokenizer tokenizer = new Tokenizer();
    private final RPNConverter converter = new RPNConverter();
    private final RPNEvaluator evaluator = new RPNEvaluator();

    /** Tên các biến người dùng có thể lưu giá trị (STO). M là bộ nhớ. */
    public static final String[] VARIABLES = {"A", "B", "C", "D", "E", "F", "X", "Y", "M"};

    private double ans = 0;
    private double memory = 0;
    private final Map<String, Double> variables = new LinkedHashMap<>();

    // Cách hiển thị kết quả (dùng chung cho toàn chương trình)
    private static DisplayMode displayMode = DisplayMode.NORM;
    private static int displayDigits = 10;

    public void setAngleMode(AngleMode mode) {
        evaluator.setAngleMode(mode);
    }

    public double evaluate(String expression) {
        evaluator.setVariable("Ans", ans);
        evaluator.setVariable("M", memory);
        for (Map.Entry<String, Double> e : variables.entrySet()) {
            evaluator.setVariable(e.getKey(), e.getValue());
        }
        List<Token> tokens = tokenizer.tokenize(expression);
        List<Token> rpn = converter.toRPN(tokens);
        double result = evaluator.evaluate(rpn);
        ans = result;
        return result;
    }

    /** Trả về chuỗi RPN của biểu thức (để minh hoạ/debug). */
    public String toRPNString(String expression) {
        StringBuilder sb = new StringBuilder();
        for (Token t : converter.toRPN(tokenizer.tokenize(expression))) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(t);
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- biến
    /** Lưu giá trị vào biến A-F, X, Y hoặc M (bộ nhớ). */
    public void setVariable(String name, double value) {
        if (name.equals("M")) memory = value;
        else variables.put(name, value);
    }

    public double getVariable(String name) {
        if (name.equals("M")) return memory;
        return variables.getOrDefault(name, 0.0);
    }

    // ---------------------------------------------------------------- bộ nhớ
    public void memoryPlus(double v)  { memory += v; }
    public void memoryMinus(double v) { memory -= v; }
    public void memoryClear()         { memory = 0; }
    public double getMemory()         { return memory; }
    public double getAns()            { return ans; }

    // ---------------------------------------------------------------- hiển thị
    /** Chọn cách hiển thị: NORM/SCI = số chữ số có nghĩa, FIX = số chữ số thập phân. */
    public static void setDisplayFormat(DisplayMode mode, int digits) {
        displayMode = mode;
        displayDigits = digits;
    }

    /** Định dạng kết quả theo chế độ hiển thị hiện tại (mặc định Norm, 10 chữ số có nghĩa). */
    public static String format(double v) {
        if (Double.isNaN(v)) return "0";
        switch (displayMode) {
            case FIX: return formatFix(v, displayDigits);
            case SCI: return formatSci(v, displayDigits);
            default:  return formatNorm(v, displayDigits);
        }
    }

    private static String formatNorm(double v, int digits) {
        if (v == 0) return "0";
        BigDecimal bd = new BigDecimal(v).round(new MathContext(digits, RoundingMode.HALF_UP)).stripTrailingZeros();
        double abs = Math.abs(v);
        if (abs >= Math.pow(10, digits) || abs < 1e-9) {
            return bd.toString().replace("E+", "×10^").replace("E-", "×10^-");
        }
        return bd.toPlainString();
    }

    private static String formatFix(double v, int decimals) {
        return new BigDecimal(v).setScale(decimals, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatSci(double v, int digits) {
        if (v == 0) return "0";
        BigDecimal bd = new BigDecimal(v).round(new MathContext(Math.max(1, digits), RoundingMode.HALF_UP));
        int exp = bd.precision() - bd.scale() - 1;
        String mantissa = bd.movePointLeft(exp).setScale(Math.max(1, digits) - 1, RoundingMode.HALF_UP).toPlainString();
        return mantissa + "×10^" + exp;
    }

    /**
     * Đổi |v| thành phân số tối giản (liên phân số, mẫu tối đa 100000).
     * @return {tử, mẫu}, hoặc null nếu v là số nguyên hoặc không biểu diễn được.
     */
    public static long[] fraction(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-12) return null;
        double x = Math.abs(v);
        long h1 = 1, h0 = 0, k1 = 0, k0 = 1;
        double b = x;
        for (int i = 0; i < 40; i++) {
            long a = (long) Math.floor(b);
            long h2 = a * h1 + h0;
            long k2 = a * k1 + k0;
            if (k2 > 100000) return null;
            h0 = h1; h1 = h2; k0 = k1; k1 = k2;
            if (Math.abs(x - (double) h1 / k1) < 1e-10 * Math.max(1, x)) {
                return new long[]{h1, k1};
            }
            double frac = b - a;
            if (frac < 1e-12) break;
            b = 1 / frac;
        }
        return null;
    }

    /** Dạng chữ "a/b" của phân số (có dấu âm), null nếu không đổi được. */
    public static String toFraction(double v) {
        long[] f = fraction(v);
        return f == null ? null : (v < 0 ? "-" : "") + f[0] + "/" + f[1];
    }

    /** Hiển thị số nguyên ở hệ cơ số 2, 8, 10, 16 (số âm dùng bù 2, 32 bit như Casio). */
    public static String toBase(double v, int base) {
        if (base == 10) return format(v);
        if (v != Math.rint(v) || v > Integer.MAX_VALUE || v < Integer.MIN_VALUE) {
            throw CalcException.math();
        }
        return Integer.toUnsignedString((int) v, base).toUpperCase();
    }
}
