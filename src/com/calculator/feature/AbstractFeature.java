package com.calculator.feature;

import com.calculator.calculator.Calculator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * Khung sẵn cho chức năng dạng "nhập số liệu -> bấm Tính -> xem kết quả".
 * Chỉ cần: khai báo tên + nhãn các ô nhập (constructor) và viết hàm {@link #solve}.
 * Giao diện (ô nhập, nút Tính, vùng kết quả, bắt lỗi) đã được làm sẵn ở đây.
 */
public abstract class AbstractFeature implements Feature {
    private final String name;
    private final String description;
    private final String[] inputLabels;

    protected AbstractFeature(String name, String description, String... inputLabels) {
        this.name = name;
        this.description = description;
        this.inputLabels = inputLabels;
    }

    /**
     * HÀM CẦN VIẾT: nhận giá trị các ô nhập (đúng thứ tự inputLabels), trả về chuỗi kết quả.
     * Dùng {@link #num} để đọc số/biểu thức. Ném exception nếu dữ liệu sai, UI sẽ hiện lỗi.
     */
    protected abstract String solve(String[] inputs, Calculator calc) throws Exception;

    /** Đọc 1 ô nhập thành số (cho phép nhập biểu thức như 1/3, sqrt(2), ...). */
    protected static double num(Calculator calc, String text) {
        return calc.evaluate(text.trim());
    }

    @Override public String getName() { return name; }
    @Override public String getDescription() { return description; }

    @Override
    public JComponent createPanel(Calculator calc) {
        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JTextField[] fields = new JTextField[inputLabels.length];
        for (int i = 0; i < fields.length; i++) {
            JLabel l = new JLabel(inputLabels[i]);
            l.setForeground(new Color(0xcfd3d8));
            fields[i] = new JTextField();
            form.add(l);
            form.add(fields[i]);
        }
        JTextArea out = new JTextArea(6, 10);
        out.setEditable(false);
        out.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        out.setLineWrap(true);
        JButton run = new JButton("Tính");
        run.addActionListener(e -> {
            String[] in = new String[fields.length];
            for (int i = 0; i < in.length; i++) in[i] = fields[i].getText();
            try {
                out.setText(solve(in, calc));
            } catch (Exception ex) {
                out.setText("Lỗi: " + ex.getMessage());
            }
        });
        form.add(run);

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        p.add(form, BorderLayout.NORTH);
        p.add(new JScrollPane(out), BorderLayout.CENTER);
        return p;
    }
}
