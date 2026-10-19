package com.calculator.calculator;

import com.calculator.evaluator.RPNEvaluator;
import com.calculator.model.AngleMode;
import com.calculator.model.CalcException;
import com.calculator.model.Token;
import com.calculator.parser.RPNConverter;
import com.calculator.parser.Tokenizer;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/**
 * Điều phối quá trình: Tokenizer → RPNConverter → RPNEvaluator.
 * Lưu Ans (kết quả trước), bộ nhớ M và chế độ góc.
 */
public class Calculator {
    private final Tokenizer tokenizer = new Tokenizer();
    private final RPNConverter converter = new RPNConverter();
    private final RPNEvaluator evaluator = new RPNEvaluator();

    private double ans = 0;
    private double memory = 0;

    public void setAngleMode(AngleMode mode) {
        evaluator.setAngleMode(mode);
    }

    public double evaluate(String expression) {
        evaluator.setVariable("Ans", ans);
        evaluator.setVariable("M", memory);
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

    // ---------------------------------------------------------------- bộ nhớ
    public void memoryPlus(double v)  { memory += v; }
    public void memoryMinus(double v) { memory -= v; }
    public void memoryClear()         { memory = 0; }
    public double getMemory()         { return memory; }
    public double getAns()            { return ans; }

    // ---------------------------------------------------------------- hiển thị

    public static String format(double v) {
        if (v == 0 || Double.isNaN(v)) return "0";
        BigDecimal bd = new BigDecimal(v).round(new MathContext(10, RoundingMode.HALF_UP)).stripTrailingZeros();
        double abs = Math.abs(v);
        if (abs >= 1e10 || abs < 1e-9) {
            return bd.toString().replace("E+", "×10^").replace("E-", "×10^-");
        }
        return bd.toPlainString();
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
