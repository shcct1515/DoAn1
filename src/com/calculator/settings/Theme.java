package com.calculator.settings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bộ màu của giao diện máy tính. */
public class Theme {
    /** Tên (khoá) của từng màu, dùng khi lưu file. */
    public static final String[] KEYS = {
            "body", "lcd", "lcdText", "fn", "num", "op", "clear", "equal",
            "shift", "alpha", "shiftText", "alphaText"};
    /** Tên hiển thị tương ứng với KEYS. */
    public static final String[] LABELS = {
            "Vỏ máy", "Màn hình LCD", "Chữ màn hình", "Phím chức năng", "Phím số", "Phím toán tử",
            "Phím DEL / AC", "Phím =", "Phím SHIFT", "Phím ALPHA", "Chữ phụ (SHIFT)", "Chữ phụ (ALPHA)"};

    public static final String CUSTOM = "Tùy chỉnh";

    private final Map<String, Color> colors = new LinkedHashMap<>();
    private String name;

    /** hex theo đúng thứ tự KEYS, dạng "rrggbb". */
    public Theme(String name, String... hex) {
        this.name = name;
        for (int i = 0; i < KEYS.length; i++) {
            colors.put(KEYS[i], new Color(Integer.parseInt(hex[i], 16)));
        }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Color get(String key) { return colors.get(key); }
    public void set(String key, Color c) { colors.put(key, c); }

    public void copyFrom(Theme other) {
        for (String k : KEYS) colors.put(k, other.colors.get(k));
        name = other.name;
    }

    // ---------------------------------------------------------------- tiện ích màu
    public static boolean isLight(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 > 150;
    }

    /** Màu chữ (đen/trắng) dễ đọc trên nền bg. */
    public static Color textOn(Color bg) {
        return isLight(bg) ? Color.BLACK : Color.WHITE;
    }

    public static Color blend(Color a, Color b, double t) {
        int r = (int) Math.round(a.getRed() * (1 - t) + b.getRed() * t);
        int g = (int) Math.round(a.getGreen() * (1 - t) + b.getGreen() * t);
        int bl = (int) Math.round(a.getBlue() * (1 - t) + b.getBlue() * t);
        return new Color(r, g, bl);
    }

    public static String toHex(Color c) {
        return String.format("%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ---------------------------------------------------------------- giao diện có sẵn
    public static Theme classic() {
        return new Theme("Casio cổ điển",
                "2b2f36", "c5d0b5", "1d2318", "4a505a", "e6e6e6", "6b7380", "c0392b", "e67e22",
                "d4a017", "e57373", "ffd54f", "ff8a80");
    }

    public static List<Theme> presets() {
        List<Theme> list = new ArrayList<>();
        list.add(classic());
        list.add(new Theme("Sáng",
                "e3e6ea", "dfe8d5", "1b1f16", "b9c2cd", "ffffff", "ced5de", "e74c3c", "f39c12",
                "f1c40f", "ff8a80", "a05a00", "c62828"));
        list.add(new Theme("Xanh đại dương",
                "12263a", "bfe3f0", "0b2533", "1f4e79", "e8f1f8", "2e75b6", "c0392b", "f39c12",
                "ffb300", "e57373", "ffe082", "ff8a80"));
        list.add(new Theme("Hồng sakura",
                "3b2a35", "f3d9e3", "3b1d2c", "7a4b63", "fbeef3", "a06a86", "d1495b", "ff8fab",
                "f6c177", "e05d7a", "ffd6a5", "ffb3c1"));
        list.add(new Theme("Tối (OLED)",
                "000000", "14171a", "7cfc00", "222222", "3a3a3a", "2d2d2d", "8e1b1b", "d35400",
                "b8860b", "a03a3a", "ffd54f", "ff8a80"));
        return list;
    }
}
