package com.calculator.parser;

import com.calculator.model.CalcException;
import com.calculator.model.Symbols;
import com.calculator.model.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parse chuỗi biểu thức thành danh sách Token.
 * Hỗ trợ: số thập phân, + - × ÷ ^, số âm (neg), ngoặc, hàm, hằng số và biến (pi, e, Ans, M, A-F, X, Y),
 * phép nhân ngầm (2(3), 2π, 3sin(30)), toán tử hậu tố ! và %,
 * phân số 3⁄4 (ba phần tư) và hỗn số 2⁄3⁄4 (hai và ba phần tư).
 */
public class Tokenizer {
    private static final Set<String> FUNCTIONS = Set.of(
            "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
            "log", "ln", "exp", "sqrt", "cbrt", "abs");
    private static final Set<String> CONSTANTS = Set.of("pi", "e", "Ans", "M",
            "A", "B", "C", "D", "E", "F", "X", "Y");
    private static final Set<String> BINARY_WORDS = Set.of("root", "nPr", "nCr");

    /** Tất cả tên, sắp xếp dài trước để khớp "sinh" trước "sin", "exp" trước "e". */
    private static final List<String> NAMES = new ArrayList<>();

    static {
        NAMES.addAll(FUNCTIONS);
        NAMES.addAll(CONSTANTS);
        NAMES.addAll(BINARY_WORDS);
        NAMES.sort((a, b) -> b.length() - a.length());
    }

    private static final char FRAC = Symbols.FRACTION_SLASH;
    private static final char END = Symbols.EXPONENT_END;

    private static boolean fracOperandChar(char c) {
        return Character.isDigit(c) || c == '.' || Character.isLetter(c);
    }

    /** {đầu tử, vị trí dấu ⁄, đầu mẫu, cuối mẫu} của phân số tại vị trí f; null nếu thiếu tử hoặc mẫu. */
    static int[] fractionSpans(String s, int f) {
        int len = s.length();
        int e = f;
        while (e > 0 && s.charAt(e - 1) == END) e--;
        if (e == 0) return null;
        int st;
        if (s.charAt(e - 1) == ')') {
            int depth = 0, k = e - 1;
            for (; k >= 0; k--) {
                char c = s.charAt(k);
                if (c == ')') depth++;
                else if (c == '(' && --depth == 0) break;
            }
            if (k < 0) return null;
            st = k;
            while (st > 0 && Character.isLetter(s.charAt(st - 1))) st--;
        } else {
            st = e;
            while (st > 0 && fracOperandChar(s.charAt(st - 1))) st--;
            if (st == e) return null;
        }

        int ds = f + 1;
        if (ds >= len) return null;
        int k = ds;
        if (s.charAt(k) == '-') k++;
        int begin = k;
        while (k < len && Character.isLetter(s.charAt(k))) k++;
        int de;
        if (k < len && s.charAt(k) == '(') {
            int depth = 0;
            de = len;
            for (int m = k; m < len; m++) {
                char c = s.charAt(m);
                if (c == '(') depth++;
                else if (c == ')' && --depth == 0) {
                    de = m + 1;
                    break;
                }
            }
        } else {
            while (k < len && fracOperandChar(s.charAt(k))) k++;
            if (k == begin) return null;
            de = k;
        }
        return new int[]{st, f, ds, de};
    }

    /** Đổi các phân số tổng quát thành ((tử)/(mẫu)) để bộ tính đọc được. Dạng số⁄số giữ nguyên. */
    public static String rewriteFractions(String s) {
        s = s.replace(String.valueOf(Symbols.FRAC_OPEN), "((").replace(String.valueOf(Symbols.FRAC_MID), ")/(")
                .replace(String.valueOf(Symbols.FRAC_CLOSE), "))");
        while (true) {
            int f = -1;
            for (int k = 0; k < s.length(); k++) {
                if (s.charAt(k) != FRAC) continue;
                boolean left = k > 0 && Character.isDigit(s.charAt(k - 1));
                boolean right = k + 1 < s.length() && Character.isDigit(s.charAt(k + 1));
                if (left && right) continue;
                f = k;
                break;
            }
            if (f < 0) return s;
            int[] sp = fractionSpans(s, f);
            if (sp == null) throw CalcException.syntax();
            String den = s.substring(sp[2], sp[3]);
            int open = 0;
            for (int i = 0; i < den.length(); i++) {
                if (den.charAt(i) == '(') open++;
                else if (den.charAt(i) == ')') open--;
            }
            StringBuilder fix = new StringBuilder(den);
            for (; open > 0; open--) fix.append(')');
            s = s.substring(0, sp[0]) + "((" + s.substring(sp[0], f) + ")/(" + fix + "))" + s.substring(sp[3]);
        }
    }

    public List<Token> tokenize(String input) {
        String s = normalize(rewriteFractions(input));
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c) || c == Symbols.EXPONENT_END) {
                i++;
            } else if (Character.isDigit(c) || c == '.') {
                int j = i;
                boolean dot = false;
                while (j < s.length() && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                    if (s.charAt(j) == '.') {
                        if (dot) throw CalcException.syntax();
                        dot = true;
                    }
                    j++;
                }
                String num = s.substring(i, j);
                if (num.equals(".")) throw CalcException.syntax();
                double value = Double.parseDouble(num);
                if (j < s.length() && s.charAt(j) == Symbols.FRACTION_SLASH) {
                    if (num.indexOf('.') >= 0) throw CalcException.syntax();
                    // phân số a⁄b hoặc hỗn số a⁄b⁄c
                    int k = j + 1;
                    int e = skipDigits(s, k);
                    if (e == k) throw CalcException.syntax();
                    double second = Double.parseDouble(s.substring(k, e));
                    if (e < s.length() && s.charAt(e) == Symbols.FRACTION_SLASH) {
                        int m = e + 1;
                        int f = skipDigits(s, m);
                        if (f == m) throw CalcException.syntax();
                        double den = Double.parseDouble(s.substring(m, f));
                        if (den == 0) throw CalcException.math();
                        value = value + second / den;
                        j = f;
                    } else {
                        if (second == 0) throw CalcException.math();
                        value = value / second;
                        j = e;
                    }
                }
                implicitMultiply(out);
                out.add(Token.number(value));
                i = j;
            } else if (Character.isLetter(c)) {
                String name = matchName(s, i);
                if (name == null) throw CalcException.syntax();
                i += name.length();
                if (FUNCTIONS.contains(name)) {
                    implicitMultiply(out);
                    out.add(Token.function(name));
                } else if (CONSTANTS.contains(name)) {
                    implicitMultiply(out);
                    out.add(Token.constant(name));
                } else {
                    requireOperand(out);
                    out.add(Token.operator(name));
                }
            } else {
                i++;
                switch (c) {
                    case '+':
                        if (endsOperand(out)) out.add(Token.operator("+")); // '+' đơn ngôi: bỏ qua
                        break;
                    case '-':
                        out.add(Token.operator(endsOperand(out) ? "-" : "neg"));
                        break;
                    case '*': case '/': case '^':
                        requireOperand(out);
                        out.add(Token.operator(String.valueOf(c)));
                        break;
                    case '!': case '%':
                        requireOperand(out);
                        out.add(Token.postfix(String.valueOf(c)));
                        break;
                    case '(':
                        implicitMultiply(out);
                        out.add(Token.leftParen());
                        break;
                    case ')':
                        out.add(Token.rightParen());
                        break;
                    default:
                        throw CalcException.syntax();
                }
            }
        }
        return out;
    }

    private static int skipDigits(String s, int from) {
        int i = from;
        while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
        return i;
    }

    private static String normalize(String s) {
        return s.replace("×", "*").replace("÷", "/").replace("−", "-")
                .replace("π", "pi").replace("√", "sqrt");
    }

    private static String matchName(String s, int pos) {
        for (String name : NAMES) {
            if (s.startsWith(name, pos)) return name;
        }
        return null;
    }

    /** Token cuối cùng là một toán hạng (kết thúc một giá trị)? */
    private static boolean endsOperand(List<Token> out) {
        if (out.isEmpty()) return false;
        Token.Type t = out.get(out.size() - 1).getType();
        return t == Token.Type.NUMBER || t == Token.Type.CONSTANT
                || t == Token.Type.RIGHT_PAREN || t == Token.Type.POSTFIX;
    }

    private static void implicitMultiply(List<Token> out) {
        if (endsOperand(out)) out.add(Token.operator("*"));
    }

    private static void requireOperand(List<Token> out) {
        if (!endsOperand(out)) throw CalcException.syntax();
    }
}
