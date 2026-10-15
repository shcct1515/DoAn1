package com.calculator.parser;

import com.calculator.model.CalcException;
import com.calculator.model.Token;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Chuyển biểu thức trung tố (infix) sang hậu tố (RPN) bằng thuật toán Shunting-yard.
 *
 * Độ ưu tiên: + - (1) < × ÷ (2) < số âm (3) < nPr nCr (4) < ^ root (5).
 * Số âm có độ ưu tiên thấp hơn ^ nên -2^2 = -4 giống máy Casio.
 * Dấu ")" thiếu ở cuối biểu thức được tự động đóng.
 */
public class RPNConverter {

    public List<Token> toRPN(List<Token> tokens) {
        List<Token> out = new ArrayList<>();
        Deque<Token> stack = new ArrayDeque<>();

        for (Token t : tokens) {
            switch (t.getType()) {
                case NUMBER:
                case CONSTANT:
                case POSTFIX:           // ! và % áp dụng ngay cho toán hạng đứng trước
                    out.add(t);
                    break;
                case FUNCTION:
                case LEFT_PAREN:
                    stack.push(t);
                    break;
                case OPERATOR:
                    if (t.getText().equals("neg")) {   // toán tử tiền tố: không pop gì cả
                        stack.push(t);
                        break;
                    }
                    while (!stack.isEmpty()) {
                        Token top = stack.peek();
                        if (top.getType() == Token.Type.FUNCTION) {      // hàm không có ngoặc: sin30
                            out.add(stack.pop());
                        } else if (top.getType() == Token.Type.OPERATOR && shouldPop(top, t)) {
                            out.add(stack.pop());
                        } else {
                            break;
                        }
                    }
                    stack.push(t);
                    break;
                case RIGHT_PAREN:
                    while (!stack.isEmpty() && stack.peek().getType() != Token.Type.LEFT_PAREN) {
                        out.add(stack.pop());
                    }
                    if (stack.isEmpty()) throw CalcException.syntax();   // thừa ")"
                    stack.pop();                                          // bỏ "("
                    if (!stack.isEmpty() && stack.peek().getType() == Token.Type.FUNCTION) {
                        out.add(stack.pop());
                    }
                    break;
            }
        }
        while (!stack.isEmpty()) {
            Token t = stack.pop();
            if (t.getType() != Token.Type.LEFT_PAREN) out.add(t);   // tự đóng ngoặc
        }
        return out;
    }

    private static boolean shouldPop(Token top, Token cur) {
        int pt = precedence(top.getText());
        int pc = precedence(cur.getText());
        return pt > pc || (pt == pc && !rightAssociative(cur.getText()));
    }

    static int precedence(String op) {
        switch (op) {
            case "+": case "-":        return 1;
            case "*": case "/":        return 2;
            case "neg":                return 3;
            case "nPr": case "nCr":    return 4;
            case "^": case "root":     return 5;
            default:                   return 0;
        }
    }

    static boolean rightAssociative(String op) {
        return op.equals("^") || op.equals("root") || op.equals("neg");
    }
}
