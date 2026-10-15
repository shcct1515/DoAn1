package com.calculator.parser;

import com.calculator.model.CalcException;
import com.calculator.model.Token;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Parse chuỗi biểu thức thành danh sách Token.
 * Hỗ trợ: số thập phân, + - × ÷ ^, số âm (neg), ngoặc, hàm, hằng số (pi, e, Ans, M),
 * phép nhân ngầm (2(3), 2π, 3sin(30)), toán tử hậu tố ! và %.
 */
public class Tokenizer {
    private static final Set<String> FUNCTIONS = Set.of(
            "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh",
            "log", "ln", "exp", "sqrt", "cbrt", "abs");
    private static final Set<String> CONSTANTS = Set.of("pi", "e", "Ans", "M");
    private static final Set<String> BINARY_WORDS = Set.of("root", "nPr", "nCr");

    /** Tất cả tên, sắp xếp dài trước để khớp "sinh" trước "sin", "exp" trước "e". */
    private static final List<String> NAMES = new ArrayList<>();

    static {
        NAMES.addAll(FUNCTIONS);
        NAMES.addAll(CONSTANTS);
        NAMES.addAll(BINARY_WORDS);
        NAMES.sort((a, b) -> b.length() - a.length());
    }

    public List<Token> tokenize(String input) {
        String s = normalize(input);
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
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
                implicitMultiply(out);
                out.add(Token.number(Double.parseDouble(num)));
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
