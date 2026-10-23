package com.calculator.model;

/** Các ký tự đặc biệt dùng chung giữa giao diện và bộ phân tích biểu thức. */
public final class Symbols {
    public static final char FRACTION_SLASH = '\u2044';

    /** Ký tự ẩn đánh dấu kết thúc số mũ */
    public static final char EXPONENT_END = '\u200B';

    /** Khung phân số */
    public static final char FRAC_OPEN = '\uE000';
    public static final char FRAC_MID = '\uE001';
    public static final char FRAC_CLOSE = '\uE002';

    private Symbols() {
    }
}
