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

    /**
     * Mẫu nhập nhiều ô giống máy thật (ô trống hiện □): ∫, d/dx, Σ, Π, log cơ số.
     * Dạng lưu trong ô nhập: [ký tự mở] ô1 [TPL_SEP] ô2 [TPL_SEP] ô3 [TPL_CLOSE]
     * Thứ tự ô (trái → phải trên màn hình): ∫, Σ, Π = (cận dưới, cận trên, biểu thức); d/dx = (biểu thức, điểm x); log = (cơ số, đối số).
     */
    public static final char INT_OPEN = '\uE010';
    public static final char DER_OPEN = '\uE011';
    public static final char SUM_OPEN = '\uE012';
    public static final char PROD_OPEN = '\uE013';
    public static final char LOG_OPEN = '\uE014';
    public static final char TPL_SEP = '\uE015';
    public static final char TPL_CLOSE = '\uE016';

    public static boolean isTemplateOpen(char c) {
        return c >= INT_OPEN && c <= LOG_OPEN;
    }

    /** Số ô của mẫu bắt đầu bằng ký tự mở c (0 nếu c không phải ký tự mở). */
    public static int slotCount(char c) {
        switch (c) {
            case INT_OPEN: case SUM_OPEN: case PROD_OPEN: return 3;
            case DER_OPEN: case LOG_OPEN: return 2;
            default: return 0;
        }
    }

    private Symbols() {
    }
}
