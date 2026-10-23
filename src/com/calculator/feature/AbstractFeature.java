package com.calculator.feature;

import com.calculator.calculator.Calculator;

import java.util.List;

/**
 * Khung sẵn cho chức năng có số ô nhập cố định. Chỉ cần: truyền tên + nhãn các ô nhập
 * vào constructor và viết hàm {@link #solve}. (Cần số ô nhập thay đổi thì ghi đè {@link #prompt}.)
 */
public abstract class AbstractFeature implements Feature {
    private final String name;
    private final String[] prompts;

    protected AbstractFeature(String name, String... prompts) {
        this.name = name;
        this.prompts = prompts;
    }

    @Override public String getName() { return name; }

    @Override
    public String prompt(int index, List<String> values) {
        return index < prompts.length ? prompts[index] : null;
    }

    /** Mặc định: ô nhập phải tính được ra số (cho phép nhập biểu thức như 1÷3, √2...). */
    @Override
    public void check(int index, String value, Calculator calc) {
        calc.evaluate(value);
    }

    /** Giá trị số của ô nhập thứ i. */
    protected static double val(Calculator calc, List<String> values, int i) {
        return calc.evaluate(values.get(i).trim());
    }
}
