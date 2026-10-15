package com.calculator.model;

/** Lỗi khi tính toán, thông điệp giống máy Casio: "Math ERROR" / "Syntax ERROR". */
public class CalcException extends RuntimeException {
    public CalcException(String message) {
        super(message);
    }

    public static CalcException math() {
        return new CalcException("Math ERROR");
    }

    public static CalcException syntax() {
        return new CalcException("Syntax ERROR");
    }
}
