package com.calculator.model;

/** Một thành phần (toán hạng, toán tử, hàm, dấu ngoặc...) của biểu thức. */
public class Token {
    public enum Type { NUMBER, CONSTANT, OPERATOR, FUNCTION, POSTFIX, LEFT_PAREN, RIGHT_PAREN }

    private final Type type;
    private final String text;
    private final double value;

    private Token(Type type, String text, double value) {
        this.type = type;
        this.text = text;
        this.value = value;
    }

    public static Token number(double v)       { return new Token(Type.NUMBER, String.valueOf(v), v); }
    public static Token constant(String name)  { return new Token(Type.CONSTANT, name, 0); }
    public static Token operator(String op)    { return new Token(Type.OPERATOR, op, 0); }
    public static Token function(String name)  { return new Token(Type.FUNCTION, name, 0); }
    public static Token postfix(String op)     { return new Token(Type.POSTFIX, op, 0); }
    public static Token leftParen()            { return new Token(Type.LEFT_PAREN, "(", 0); }
    public static Token rightParen()           { return new Token(Type.RIGHT_PAREN, ")", 0); }

    public Type getType()   { return type; }
    public String getText() { return text; }
    public double getValue() { return value; }

    @Override
    public String toString() {
        return type == Type.NUMBER ? (value == Math.rint(value) && Math.abs(value) < 1e15
                ? String.valueOf((long) value) : String.valueOf(value)) : text;
    }
}
