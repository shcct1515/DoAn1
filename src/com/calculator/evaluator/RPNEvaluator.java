package com.calculator.evaluator;

import com.calculator.model.AngleMode;
import com.calculator.model.CalcException;
import com.calculator.model.Token;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tính giá trị biểu thức RPN bằng Stack. */
public class RPNEvaluator {
    private AngleMode angleMode = AngleMode.DEG;
    private final Map<String, Double> variables = new HashMap<>();

    public RPNEvaluator() {
        variables.put("pi", Math.PI);
        variables.put("e", Math.E);
        variables.put("Ans", 0.0);
        variables.put("M", 0.0);
        for (String v : new String[]{"A", "B", "C", "D", "E", "F", "X", "Y"}) variables.put(v, 0.0);
    }

    public void setAngleMode(AngleMode mode) { this.angleMode = mode; }

    public void setVariable(String name, double value) { variables.put(name, value); }

    public double evaluate(List<Token> rpn) {
        Deque<Double> stack = new ArrayDeque<>();
        for (Token t : rpn) {
            switch (t.getType()) {
                case NUMBER:
                    stack.push(t.getValue());
                    break;
                case CONSTANT:
                    stack.push(variables.get(t.getText()));
                    break;
                case OPERATOR:
                    if (t.getText().equals("neg")) {
                        stack.push(-pop(stack));
                    } else {
                        double b = pop(stack);
                        double a = pop(stack);
                        stack.push(binary(t.getText(), a, b));
                    }
                    break;
                case FUNCTION:
                case POSTFIX:
                    stack.push(unary(t.getText(), pop(stack)));
                    break;
                default:
                    throw CalcException.syntax();
            }
        }
        if (stack.size() != 1) throw CalcException.syntax();
        double result = stack.pop();
        if (Double.isNaN(result) || Double.isInfinite(result)) throw CalcException.math();
        return result;
    }

    private static double pop(Deque<Double> stack) {
        if (stack.isEmpty()) throw CalcException.syntax();
        return stack.pop();
    }

    // ---------------------------------------------------------------- toán tử 2 ngôi
    private double binary(String op, double a, double b) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw CalcException.math();
                return a / b;
            case "^":
                if (a == 0 && b == 0) throw CalcException.math();
                return Math.pow(a, b);
            case "root":                     // a root b = căn bậc a của b
                if (a == 0) throw CalcException.math();
                if (b < 0) {
                    if (a == Math.rint(a) && Math.abs(a) % 2 == 1) return -Math.pow(-b, 1 / a);
                    throw CalcException.math();
                }
                return Math.pow(b, 1 / a);
            case "nPr": return permutation(a, b);
            case "nCr": return combination(a, b);
            default:    throw CalcException.syntax();
        }
    }

    // ---------------------------------------------------------------- hàm 1 ngôi
    private double unary(String name, double x) {
        switch (name) {
            case "sin":  return snap(Math.sin(toRadians(x)));
            case "cos":  return snap(Math.cos(toRadians(x)));
            case "tan": {
                double r = toRadians(x);
                if (Math.abs(Math.cos(r)) < 1e-12) throw CalcException.math();
                return snap(Math.tan(r));
            }
            case "asin":
                if (Math.abs(x) > 1) throw CalcException.math();
                return fromRadians(Math.asin(x));
            case "acos":
                if (Math.abs(x) > 1) throw CalcException.math();
                return fromRadians(Math.acos(x));
            case "atan": return fromRadians(Math.atan(x));
            case "sinh": return Math.sinh(x);
            case "cosh": return Math.cosh(x);
            case "tanh": return Math.tanh(x);
            case "log":
                if (x <= 0) throw CalcException.math();
                return Math.log10(x);
            case "ln":
                if (x <= 0) throw CalcException.math();
                return Math.log(x);
            case "exp":  return Math.exp(x);
            case "sqrt":
                if (x < 0) throw CalcException.math();
                return Math.sqrt(x);
            case "cbrt": return Math.cbrt(x);
            case "abs":  return Math.abs(x);
            case "!":    return factorial(x);
            case "%":    return x / 100;
            default:     throw CalcException.syntax();
        }
    }

    private double toRadians(double x) {
        switch (angleMode) {
            case DEG:  return Math.toRadians(x);
            case GRAD: return x * Math.PI / 200;
            default:   return x;
        }
    }

    private double fromRadians(double r) {
        switch (angleMode) {
            case DEG:  return Math.toDegrees(r);
            case GRAD: return r * 200 / Math.PI;
            default:   return r;
        }
    }

    /** Loại nhiễu số học, ví dụ sin(180°) = 1.2e-16 → 0. */
    private static double snap(double v) {
        return Math.abs(v) < 1e-14 ? 0 : v;
    }

    private static void requireNonNegativeInt(double x) {
        if (x < 0 || x != Math.rint(x)) throw CalcException.math();
    }

    private static double factorial(double n) {
        requireNonNegativeInt(n);
        if (n > 170) throw CalcException.math();
        double r = 1;
        for (int i = 2; i <= (int) n; i++) r *= i;
        return r;
    }

    private static double permutation(double n, double r) {
        requireNonNegativeInt(n);
        requireNonNegativeInt(r);
        if (r > n) throw CalcException.math();
        double res = 1;
        for (int i = 0; i < (int) r; i++) res *= (n - i);
        return res;
    }

    private static double combination(double n, double r) {
        requireNonNegativeInt(n);
        requireNonNegativeInt(r);
        if (r > n) throw CalcException.math();
        r = Math.min(r, n - r);
        double res = 1;
        for (int i = 1; i <= (int) r; i++) res = res * (n - r + i) / i;
        return Math.rint(res);
    }
}
