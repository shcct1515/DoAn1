package com.calculator.feature;

import com.calculator.calculator.Calculator;

import java.util.List;

/** Một chức năng chạy trực tiếp trên màn LCD: hỏi từng ô nhập (a?, b?, ...) rồi hiện kết quả. */
public interface Feature {
    String getName();

    /** Nhãn của ô nhập thứ index (từ 0); trả về null khi đã đủ. values = các ô đã nhập trước đó. */
    String prompt(int index, List<String> values);

    /** Kiểm tra giá trị vừa nhập; ném exception (kèm thông báo) nếu sai. */
    void check(int index, String value, Calculator calc) throws Exception;

    /** Tính kết quả từ các ô đã nhập; mỗi dòng của chuỗi trả về là một dòng trên LCD. */
    String solve(List<String> values, Calculator calc) throws Exception;
}
