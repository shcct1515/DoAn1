package com.calculator.feature;

import com.calculator.calculator.Calculator;

import javax.swing.JComponent;

/**
 * Một chức năng mở rộng của máy tính (giải phương trình, hệ phương trình, ...).
 */
public interface Feature {
    /** Tên hiển thị trong menu. */
    String getName();

    /** Mô tả ngắn hiển thị dưới tên. */
    String getDescription();

    /** Tạo giao diện của chức năng (được gọi mỗi lần người dùng mở chức năng). */
    JComponent createPanel(Calculator calculator);
}
