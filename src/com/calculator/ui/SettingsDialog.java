package com.calculator.ui;

import com.calculator.model.AngleMode;
import com.calculator.model.DisplayMode;
import com.calculator.settings.Settings;
import com.calculator.settings.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.KeyStroke;

/** Hộp thoại Cài đặt: giao diện, phím tắt, cách tính, chung. Thay đổi có hiệu lực ngay và được lưu lại. */
public class SettingsDialog extends JDialog {
    private final Settings settings;
    private final List<ActionEntry> actions;
    private boolean updating = false;

    // Giao diện
    private JComboBox<String> themeCombo;
    private final Map<String, JButton> swatches = new LinkedHashMap<>();
    private JSpinner fontSize;
    // Phím tắt
    private DefaultTableModel tableModel;
    private JTable table;
    // Tính toán
    private JComboBox<AngleMode> angleCombo;
    private JComboBox<DisplayMode> displayCombo;
    private JSpinner digits;
    private JLabel digitsLabel;
    private JCheckBox autoFraction, mixedFraction;
    // Chung
    private JCheckBox onTop;
    private JSpinner historySize;

    public SettingsDialog(JFrame owner, Settings settings, List<ActionEntry> actions) {
        super(owner, "Cài đặt", false);
        this.settings = settings;
        this.actions = actions;

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Giao diện", buildAppearance());
        tabs.addTab("Phím tắt", buildShortcuts());
        tabs.addTab("Tính toán", buildCalculation());
        tabs.addTab("Chung", buildGeneral());

        JButton close = new JButton("Đóng");
        close.addActionListener(e -> setVisible(false));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(close);

        getContentPane().add(tabs, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);

        settings.addListener(this::refresh);
        refresh();
        setSize(560, 580);
        setLocationRelativeTo(owner);
    }

    // ================================================================ tab Giao diện
    private JComponent buildAppearance() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // giao diện có sẵn
        JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        presetRow.add(new JLabel("Giao diện có sẵn:"));
        themeCombo = new JComboBox<>();
        for (Theme t : Theme.presets()) themeCombo.addItem(t.getName());
        themeCombo.addItem(Theme.CUSTOM);
        themeCombo.addActionListener(e -> {
            if (updating) return;
            String name = (String) themeCombo.getSelectedItem();
            for (Theme t : Theme.presets()) {
                if (t.getName().equals(name)) {
                    settings.applyPreset(t);
                    settings.fireChanged();
                    return;
                }
            }
        });
        presetRow.add(themeCombo);
        presetRow.setAlignmentX(LEFT_ALIGNMENT);
        p.add(presetRow);

        // màu từng phần
        JPanel colors = new JPanel(new GridLayout(0, 2, 10, 6));
        colors.setBorder(BorderFactory.createTitledBorder("Màu sắc (bấm vào ô màu để đổi)"));
        for (int i = 0; i < Theme.KEYS.length; i++) {
            final String key = Theme.KEYS[i];
            final String label = Theme.LABELS[i];
            colors.add(new JLabel(label));
            JButton swatch = new JButton();
            swatch.setOpaque(true);
            swatch.setFocusable(false);
            swatch.addActionListener(e -> {
                Color c = JColorChooser.showDialog(this, label, settings.getTheme().get(key));
                if (c != null) {
                    settings.getTheme().set(key, c);
                    settings.getTheme().setName(Theme.CUSTOM);
                    settings.fireChanged();
                }
            });
            swatches.put(key, swatch);
            colors.add(swatch);
        }
        colors.setAlignmentX(LEFT_ALIGNMENT);
        p.add(colors);

        // cỡ chữ
        JPanel sizeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sizeRow.add(new JLabel("Cỡ chữ màn hình:"));
        fontSize = new JSpinner(new SpinnerNumberModel(22, 16, 30, 1));
        fontSize.addChangeListener(e -> {
            if (updating) return;
            settings.setLcdFontSize((Integer) fontSize.getValue());
            settings.fireChanged();
        });
        sizeRow.add(fontSize);
        sizeRow.setAlignmentX(LEFT_ALIGNMENT);
        p.add(sizeRow);
        return p;
    }

    // ================================================================ tab Phím tắt
    private JComponent buildShortcuts() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel hint = new JLabel("<html>Chọn một dòng rồi bấm <b>Gán phím...</b> và nhấn phím (hoặc tổ hợp phím) muốn dùng.<br>"
                + "Các phím số và toán tử vẫn gõ trực tiếp từ bàn phím như bình thường.</html>");
        p.add(hint, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new Object[]{"Phím trên máy", "Phím tắt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(22);
        table.getColumnModel().getColumn(0).setPreferredWidth(300);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) assignSelected();
            }
        });
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton assign = new JButton("Gán phím...");
        assign.addActionListener(e -> assignSelected());
        JButton clear = new JButton("Xóa phím");
        clear.addActionListener(e -> {
            ActionEntry a = selectedAction();
            if (a == null) return;
            settings.setShortcut(a.getId(), null);
            settings.fireChanged();
        });
        JButton defaultOne = new JButton("Mặc định");
        defaultOne.addActionListener(e -> {
            ActionEntry a = selectedAction();
            if (a == null) return;
            settings.setShortcut(a.getId(), settings.getDefaultShortcut(a.getId()));
            settings.fireChanged();
        });
        JButton resetAll = new JButton("Khôi phục tất cả");
        resetAll.addActionListener(e -> {
            settings.resetShortcuts();
            settings.fireChanged();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(assign);
        buttons.add(clear);
        buttons.add(defaultOne);
        buttons.add(resetAll);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private ActionEntry selectedAction() {
        int row = table.getSelectedRow();
        return row < 0 ? null : actions.get(row);
    }

    private void assignSelected() {
        ActionEntry a = selectedAction();
        if (a == null) {
            JOptionPane.showMessageDialog(this, "Hãy chọn một dòng trong bảng trước.");
            return;
        }
        KeyStroke ks = captureKey(a.getName());
        if (ks == null) return;
        String lostId = settings.setShortcut(a.getId(), ks);
        if (lostId != null) {
            String lostName = lostId;
            for (ActionEntry other : actions) {
                if (other.getId().equals(lostId)) lostName = other.getName();
            }
            JOptionPane.showMessageDialog(this, "Phím " + Settings.describe(ks) + " đang dùng cho \"" + lostName
                    + "\".\nPhím đó đã được chuyển sang \"" + a.getName() + "\".");
        }
        int row = table.getSelectedRow();
        settings.fireChanged();
        if (row >= 0 && row < table.getRowCount()) table.setRowSelectionInterval(row, row);
    }

    /** Hiện hộp thoại chờ người dùng nhấn một phím; trả về null nếu huỷ. */
    private KeyStroke captureKey(String actionName) {
        final JDialog d = new JDialog(this, "Gán phím tắt", true);
        final KeyStroke[] result = new KeyStroke[1];

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JLabel label = new JLabel("<html><div style='text-align:center'>Nhấn phím (hoặc tổ hợp phím) cho:<br><b>"
                + actionName + "</b></div></html>", SwingConstants.CENTER);
        label.setFont(new Font("Dialog", Font.PLAIN, 14));
        content.add(label, BorderLayout.CENTER);

        JButton cancel = new JButton("Hủy");
        cancel.setFocusable(false);
        cancel.addActionListener(e -> d.dispose());
        JPanel south = new JPanel();
        south.add(cancel);
        content.add(south, BorderLayout.SOUTH);

        content.setFocusable(true);
        content.setFocusTraversalKeysEnabled(false);
        content.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int c = e.getKeyCode();
                if (c == KeyEvent.VK_SHIFT || c == KeyEvent.VK_CONTROL || c == KeyEvent.VK_ALT
                        || c == KeyEvent.VK_META || c == KeyEvent.VK_ALT_GRAPH || c == KeyEvent.VK_UNDEFINED) {
                    return;     // chờ phím chính
                }
                result[0] = KeyStroke.getKeyStroke(c, e.getModifiersEx());
                e.consume();
                d.dispose();
            }
        });
        d.setContentPane(content);
        d.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                content.requestFocusInWindow();
            }
        });
        d.setSize(340, 170);
        d.setLocationRelativeTo(this);
        d.setVisible(true);
        return result[0];
    }

    // ================================================================ tab Tính toán
    private JComponent buildCalculation() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel angleRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        angleRow.add(new JLabel("Đơn vị góc:"));
        angleCombo = new JComboBox<>(AngleMode.values());
        angleCombo.addActionListener(e -> {
            if (updating) return;
            settings.setAngleMode((AngleMode) angleCombo.getSelectedItem());
            settings.fireChanged();
        });
        angleRow.add(angleCombo);
        angleRow.add(new JLabel("(DEG = độ, RAD = radian, GRAD = grad)"));
        angleRow.setAlignmentX(LEFT_ALIGNMENT);
        p.add(angleRow);

        JPanel fmt = new JPanel(new GridLayout(0, 2, 10, 8));
        fmt.setBorder(BorderFactory.createTitledBorder("Cách hiển thị kết quả"));
        fmt.add(new JLabel("Kiểu hiển thị:"));
        displayCombo = new JComboBox<>(DisplayMode.values());
        displayCombo.addActionListener(e -> {
            if (updating) return;
            settings.setDisplayMode((DisplayMode) displayCombo.getSelectedItem());
            settings.fireChanged();
        });
        fmt.add(displayCombo);
        digitsLabel = new JLabel("Số chữ số:");
        fmt.add(digitsLabel);
        digits = new JSpinner(new SpinnerNumberModel(10, 0, 15, 1));
        digits.addChangeListener(e -> {
            if (updating) return;
            settings.setDigits((Integer) digits.getValue());
            settings.fireChanged();
        });
        fmt.add(digits);
        fmt.setAlignmentX(LEFT_ALIGNMENT);
        p.add(fmt);

        JPanel frac = new JPanel();
        frac.setLayout(new BoxLayout(frac, BoxLayout.Y_AXIS));
        frac.setBorder(BorderFactory.createTitledBorder("Phân số"));
        autoFraction = new JCheckBox("Kết quả hiện dạng phân số khi biểu thức có nhập phân số");
        autoFraction.addActionListener(e -> {
            if (updating) return;
            settings.setAutoFraction(autoFraction.isSelected());
            settings.fireChanged();
        });
        mixedFraction = new JCheckBox("Hiện hỗn số (2 3/4) thay vì phân số (11/4)");
        mixedFraction.addActionListener(e -> {
            if (updating) return;
            settings.setMixedFraction(mixedFraction.isSelected());
            settings.fireChanged();
        });
        frac.add(autoFraction);
        frac.add(mixedFraction);
        frac.setAlignmentX(LEFT_ALIGNMENT);
        p.add(frac);
        return p;
    }

    // ================================================================ tab Chung
    private JComponent buildGeneral() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        onTop = new JCheckBox("Luôn hiển thị phía trên các cửa sổ khác");
        onTop.addActionListener(e -> {
            if (updating) return;
            settings.setAlwaysOnTop(onTop.isSelected());
            settings.fireChanged();
        });
        onTop.setAlignmentX(LEFT_ALIGNMENT);
        p.add(onTop);

        JPanel historyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        historyRow.add(new JLabel("Số biểu thức lưu để xem lại (phím ▲ ▼):"));
        historySize = new JSpinner(new SpinnerNumberModel(30, 5, 200, 5));
        historySize.addChangeListener(e -> {
            if (updating) return;
            settings.setHistorySize((Integer) historySize.getValue());
            settings.fireChanged();
        });
        historyRow.add(historySize);
        historyRow.setAlignmentX(LEFT_ALIGNMENT);
        p.add(historyRow);

        p.add(Box.createVerticalStrut(16));
        JButton reset = new JButton("Khôi phục toàn bộ cài đặt mặc định");
        reset.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Khôi phục giao diện, phím tắt và mọi cài đặt về mặc định?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                settings.resetAll();
                settings.fireChanged();
            }
        });
        reset.setAlignmentX(LEFT_ALIGNMENT);
        p.add(reset);

        p.add(Box.createVerticalStrut(16));
        JLabel file = new JLabel("<html>Cài đặt được lưu tại:<br>" + settings.getFile().getAbsolutePath() + "</html>");
        file.setForeground(Color.GRAY);
        file.setAlignmentX(LEFT_ALIGNMENT);
        p.add(file);
        return p;
    }

    // ================================================================ đồng bộ giao diện với Settings
    private void refresh() {
        updating = true;
        try {
            Theme t = settings.getTheme();
            boolean known = false;
            for (int i = 0; i < themeCombo.getItemCount(); i++) {
                if (themeCombo.getItemAt(i).equals(t.getName())) known = true;
            }
            themeCombo.setSelectedItem(known ? t.getName() : Theme.CUSTOM);
            for (Map.Entry<String, JButton> e : swatches.entrySet()) {
                Color c = t.get(e.getKey());
                e.getValue().setBackground(c);
                e.getValue().setForeground(Theme.textOn(c));
                e.getValue().setText("#" + Theme.toHex(c));
            }
            fontSize.setValue(settings.getLcdFontSize());

            angleCombo.setSelectedItem(settings.getAngleMode());
            displayCombo.setSelectedItem(settings.getDisplayMode());
            digits.setValue(settings.getDigits());
            digitsLabel.setText(settings.getDisplayMode() == DisplayMode.FIX
                    ? "Số chữ số thập phân:" : "Số chữ số có nghĩa:");
            autoFraction.setSelected(settings.isAutoFraction());
            mixedFraction.setSelected(settings.isMixedFraction());

            onTop.setSelected(settings.isAlwaysOnTop());
            historySize.setValue(settings.getHistorySize());

            int selected = table == null ? -1 : table.getSelectedRow();
            tableModel.setRowCount(0);
            for (ActionEntry a : actions) {
                tableModel.addRow(new Object[]{a.getName(), Settings.describe(settings.getShortcut(a.getId()))});
            }
            if (selected >= 0 && selected < tableModel.getRowCount()) table.setRowSelectionInterval(selected, selected);
        } finally {
            updating = false;
        }
    }

    /** Hiện hộp thoại (hoặc đưa lên trước nếu đang mở). */
    public void open() {
        SwingUtilities.invokeLater(() -> {
            refresh();
            setVisible(true);
            toFront();
        });
    }
}
