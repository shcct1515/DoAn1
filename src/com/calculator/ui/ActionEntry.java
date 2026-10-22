package com.calculator.ui;

import java.util.function.Supplier;

/** Một hành động của máy tính (một phím hoặc một thao tác) có thể gán phím tắt. */
public final class ActionEntry {
    private final String id;
    private final Supplier<String> name;
    private final Runnable run;

    public ActionEntry(String id, Supplier<String> name, Runnable run) {
        this.id = id;
        this.name = name;
        this.run = run;
    }

    /** Mã định danh ổn định, dùng khi lưu phím tắt. */
    public String getId() { return id; }

    /** Tên hiển thị trong bảng phím tắt. */
    public String getName() { return name.get(); }

    public void run() { run.run(); }
}
