package com.calculator.model;

/** Cách hiển thị kết quả (giống SETUP của máy Casio). */
public enum DisplayMode {
    /** Tự động: tối đa N chữ số có nghĩa. */
    NORM("Norm (tự động)"),
    /** Cố định N chữ số thập phân. */
    FIX("Fix (số chữ số thập phân)"),
    /** Dạng khoa học a×10^n với N chữ số có nghĩa. */
    SCI("Sci (khoa học)");

    private final String label;

    DisplayMode(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
