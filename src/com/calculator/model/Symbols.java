package com.calculator.model;

/** Các ký tự đặc biệt dùng chung giữa giao diện và bộ phân tích biểu thức. */
public final class Symbols {
    /** Dấu phân số (a b/c): 3⁄4 là ba phần tư, 2⁄3⁄4 là hai và ba phần tư. */
    public static final char FRACTION_SLASH = '\u2044';

    /** Ký tự ẩn đánh dấu kết thúc số mũ, để x² rồi gõ tiếp "3" không thành x^23. */
    public static final char EXPONENT_END = '\u200B';

    private Symbols() {
    }
}
