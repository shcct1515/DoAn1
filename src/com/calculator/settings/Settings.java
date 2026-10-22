package com.calculator.settings;

import com.calculator.model.AngleMode;
import com.calculator.model.DisplayMode;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Toàn bộ cài đặt của máy tính (giao diện, phím tắt, cách tính, ...).
 * Tự lưu vào file .casio-fx570.properties trong thư mục người dùng và tự đọc lại lần sau.
 */
public class Settings {
    private static final File FILE = new File(System.getProperty("user.home"), ".casio-fx570.properties");

    /** Phím tắt mặc định (id hành động → phím). Các phím nhập số/toán tử gõ thẳng bằng bàn phím nên không cần. */
    private static final Map<String, KeyStroke> DEFAULT_SHORTCUTS = new LinkedHashMap<>();

    static {
        DEFAULT_SHORTCUTS.put("equals", KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
        DEFAULT_SHORTCUTS.put("ac", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        DEFAULT_SHORTCUTS.put("del", KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));
        DEFAULT_SHORTCUTS.put("left", KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
        DEFAULT_SHORTCUTS.put("right", KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
        DEFAULT_SHORTCUTS.put("up", KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
        DEFAULT_SHORTCUTS.put("down", KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
        DEFAULT_SHORTCUTS.put("shift", KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        DEFAULT_SHORTCUTS.put("alpha", KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        DEFAULT_SHORTCUTS.put("mode", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        DEFAULT_SHORTCUTS.put("settings", KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        DEFAULT_SHORTCUTS.put("angle", KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        DEFAULT_SHORTCUTS.put("base", KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        DEFAULT_SHORTCUTS.put("sto", KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
    }

    private final Theme theme = Theme.classic();
    private AngleMode angleMode = AngleMode.DEG;
    private DisplayMode displayMode = DisplayMode.NORM;
    private int digitsNorm = 10, digitsFix = 2, digitsSci = 4;
    private boolean autoFraction = true;
    private boolean mixedFraction = true;
    private boolean alwaysOnTop = false;
    private int historySize = 30;
    private int lcdFontSize = 22;
    /** Phím tắt hiện tại; giá trị null = đã xoá phím. */
    private final Map<String, KeyStroke> shortcuts = new LinkedHashMap<>(DEFAULT_SHORTCUTS);

    private final List<Runnable> listeners = new ArrayList<>();

    public Settings() {
        load();
    }

    // ================================================================ getter / setter
    public Theme getTheme() { return theme; }

    public AngleMode getAngleMode() { return angleMode; }
    public void setAngleMode(AngleMode m) { angleMode = m; }

    public DisplayMode getDisplayMode() { return displayMode; }
    public void setDisplayMode(DisplayMode m) { displayMode = m; }

    /** Số chữ số của chế độ hiển thị hiện tại. */
    public int getDigits() {
        switch (displayMode) {
            case FIX: return digitsFix;
            case SCI: return digitsSci;
            default:  return digitsNorm;
        }
    }

    public void setDigits(int d) {
        switch (displayMode) {
            case FIX: digitsFix = clamp(d, 0, 12); break;
            case SCI: digitsSci = clamp(d, 1, 12); break;
            default:  digitsNorm = clamp(d, 4, 15); break;
        }
    }

    public boolean isAutoFraction() { return autoFraction; }
    public void setAutoFraction(boolean b) { autoFraction = b; }

    public boolean isMixedFraction() { return mixedFraction; }
    public void setMixedFraction(boolean b) { mixedFraction = b; }

    public boolean isAlwaysOnTop() { return alwaysOnTop; }
    public void setAlwaysOnTop(boolean b) { alwaysOnTop = b; }

    public int getHistorySize() { return historySize; }
    public void setHistorySize(int n) { historySize = clamp(n, 5, 200); }

    public int getLcdFontSize() { return lcdFontSize; }
    public void setLcdFontSize(int n) { lcdFontSize = clamp(n, 16, 30); }

    public void applyPreset(Theme preset) {
        theme.copyFrom(preset);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ================================================================ phím tắt
    public KeyStroke getShortcut(String actionId) {
        return shortcuts.get(actionId);
    }

    public KeyStroke getDefaultShortcut(String actionId) {
        return DEFAULT_SHORTCUTS.get(actionId);
    }

    /**
     * Gán phím tắt cho hành động (null = xoá phím).
     * @return id của hành động đã bị lấy mất phím này (nếu trùng), hoặc null.
     */
    public String setShortcut(String actionId, KeyStroke ks) {
        String lost = null;
        if (ks != null) {
            for (Map.Entry<String, KeyStroke> e : shortcuts.entrySet()) {
                if (!e.getKey().equals(actionId) && ks.equals(e.getValue())) {
                    e.setValue(null);
                    lost = e.getKey();
                }
            }
        }
        shortcuts.put(actionId, ks);
        return lost;
    }

    public void resetShortcuts() {
        shortcuts.clear();
        shortcuts.putAll(DEFAULT_SHORTCUTS);
    }

    /** Mô tả phím, ví dụ "Ctrl+S", "F1". */
    public static String describe(KeyStroke ks) {
        if (ks == null) return "—";
        String mods = InputEvent.getModifiersExText(ks.getModifiers());
        String key = KeyEvent.getKeyText(ks.getKeyCode());
        return mods.isEmpty() ? key : mods + "+" + key;
    }

    // ================================================================ khôi phục / thông báo
    public void resetAll() {
        theme.copyFrom(Theme.classic());
        angleMode = AngleMode.DEG;
        displayMode = DisplayMode.NORM;
        digitsNorm = 10;
        digitsFix = 2;
        digitsSci = 4;
        autoFraction = true;
        mixedFraction = true;
        alwaysOnTop = false;
        historySize = 30;
        lcdFontSize = 22;
        resetShortcuts();
    }

    public void addListener(Runnable r) {
        listeners.add(r);
    }

    /** Lưu file rồi báo cho giao diện cập nhật. */
    public void fireChanged() {
        save();
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }

    public File getFile() {
        return FILE;
    }

    // ================================================================ lưu / đọc file
    public void save() {
        Properties p = new Properties();
        p.setProperty("theme.name", theme.getName());
        for (String k : Theme.KEYS) p.setProperty("theme." + k, Theme.toHex(theme.get(k)));
        p.setProperty("angle", angleMode.name());
        p.setProperty("display.mode", displayMode.name());
        p.setProperty("display.digitsNorm", String.valueOf(digitsNorm));
        p.setProperty("display.digitsFix", String.valueOf(digitsFix));
        p.setProperty("display.digitsSci", String.valueOf(digitsSci));
        p.setProperty("fraction.auto", String.valueOf(autoFraction));
        p.setProperty("fraction.mixed", String.valueOf(mixedFraction));
        p.setProperty("window.alwaysOnTop", String.valueOf(alwaysOnTop));
        p.setProperty("history.size", String.valueOf(historySize));
        p.setProperty("lcd.fontSize", String.valueOf(lcdFontSize));
        for (Map.Entry<String, KeyStroke> e : shortcuts.entrySet()) {
            KeyStroke ks = e.getValue();
            p.setProperty("shortcut." + e.getKey(), ks == null ? "none" : ks.getKeyCode() + "," + ks.getModifiers());
        }
        try (OutputStream out = Files.newOutputStream(FILE.toPath())) {
            p.store(out, "CASIO fx-570 Simulator settings");
        } catch (IOException | RuntimeException ignored) {
            // không lưu được thì thôi, chương trình vẫn chạy bình thường
        }
    }

    private void load() {
        if (!FILE.isFile()) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE.toPath())) {
            p.load(in);
        } catch (IOException | RuntimeException e) {
            return;
        }
        theme.setName(p.getProperty("theme.name", theme.getName()));
        for (String k : Theme.KEYS) {
            String hex = p.getProperty("theme." + k);
            if (hex != null) {
                try {
                    theme.set(k, new java.awt.Color(Integer.parseInt(hex, 16)));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        try { angleMode = AngleMode.valueOf(p.getProperty("angle", angleMode.name())); } catch (IllegalArgumentException ignored) { }
        try { displayMode = DisplayMode.valueOf(p.getProperty("display.mode", displayMode.name())); } catch (IllegalArgumentException ignored) { }
        digitsNorm = readInt(p, "display.digitsNorm", digitsNorm, 4, 15);
        digitsFix = readInt(p, "display.digitsFix", digitsFix, 0, 12);
        digitsSci = readInt(p, "display.digitsSci", digitsSci, 1, 12);
        autoFraction = Boolean.parseBoolean(p.getProperty("fraction.auto", String.valueOf(autoFraction)));
        mixedFraction = Boolean.parseBoolean(p.getProperty("fraction.mixed", String.valueOf(mixedFraction)));
        alwaysOnTop = Boolean.parseBoolean(p.getProperty("window.alwaysOnTop", String.valueOf(alwaysOnTop)));
        historySize = readInt(p, "history.size", historySize, 5, 200);
        lcdFontSize = readInt(p, "lcd.fontSize", lcdFontSize, 16, 30);
        for (String id : new ArrayList<>(shortcuts.keySet())) {
            String v = p.getProperty("shortcut." + id);
            if (v == null) continue;
            if (v.equals("none")) {
                shortcuts.put(id, null);
            } else {
                try {
                    String[] parts = v.split(",");
                    shortcuts.put(id, KeyStroke.getKeyStroke(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
                } catch (RuntimeException ignored) {
                }
            }
        }
        // phím tắt của các phím máy tính khác (không nằm trong mặc định)
        for (String name : p.stringPropertyNames()) {
            if (!name.startsWith("shortcut.")) continue;
            String id = name.substring("shortcut.".length());
            if (shortcuts.containsKey(id)) continue;
            String v = p.getProperty(name);
            try {
                if (v.equals("none")) {
                    shortcuts.put(id, null);
                } else {
                    String[] parts = v.split(",");
                    shortcuts.put(id, KeyStroke.getKeyStroke(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
                }
            } catch (RuntimeException ignored) {
            }
        }
    }

    private static int readInt(Properties p, String key, int def, int lo, int hi) {
        try {
            return clamp(Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim()), lo, hi);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
