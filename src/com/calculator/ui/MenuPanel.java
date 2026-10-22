package com.calculator.ui;

import com.calculator.calculator.Calculator;
import com.calculator.feature.Feature;
import com.calculator.feature.FeatureRegistry;
import com.calculator.settings.Theme;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Bảng MODE (menu tính năng) mở rộng bên phải máy tính: danh sách chức năng, bấm vào để mở.
 * Nội dung lấy từ FeatureRegistry nên thêm chức năng mới không cần sửa file này.
 */
public class MenuPanel extends JPanel {
    public static final int WIDTH = 290;

    private static final String CARD_LIST = "list";
    private static final String CARD_FEATURE = "feature";

    private final Calculator calculator;
    private Theme theme = Theme.classic();
    private CardLayout cards;
    private JPanel cardPanel;
    private Component featureView;

    public MenuPanel(Calculator calculator) {
        super(new BorderLayout(0, 8));
        this.calculator = calculator;
        setPreferredSize(new Dimension(WIDTH, 100));
        rebuild();
    }

    /** Đổi giao diện: dựng lại toàn bộ menu với màu mới. */
    public void applyTheme(Theme t) {
        this.theme = t;
        rebuild();
        revalidate();
        repaint();
    }

    private Color bg()       { return theme.get("body"); }
    private Color item()     { return theme.get("fn"); }
    private Color itemText() { return Theme.textOn(item()); }
    private Color bodyText() { return Theme.textOn(bg()); }

    private void rebuild() {
        removeAll();
        featureView = null;
        setBackground(bg());
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        JLabel title = new JLabel("CHỨC NĂNG MỞ RỘNG", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 14));
        title.setForeground(theme.get("shiftText"));
        title.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        add(title, BorderLayout.NORTH);

        cards = new CardLayout();
        cardPanel = new JPanel(cards);
        cardPanel.setOpaque(false);
        cardPanel.add(buildList(), CARD_LIST);
        add(cardPanel, BorderLayout.CENTER);
    }

    private JComponent buildList() {
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(bg());

        for (Feature f : FeatureRegistry.getAll()) {
            JButton b = styledButton(f.getName());
            b.setToolTipText(f.getDescription());
            b.addActionListener(e -> openFeature(f));

            JLabel desc = new JLabel(f.getDescription());
            desc.setFont(new Font("Dialog", Font.PLAIN, 11));
            desc.setForeground(Theme.blend(bodyText(), bg(), 0.35));
            desc.setAlignmentX(Component.LEFT_ALIGNMENT);
            desc.setBorder(BorderFactory.createEmptyBorder(2, 4, 8, 0));

            list.add(b);
            list.add(desc);
        }

        JScrollPane scroll = new JScrollPane(list, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(bg());
        return scroll;
    }

    private void openFeature(Feature feature) {
        if (featureView != null) cardPanel.remove(featureView);

        JPanel view = new JPanel(new BorderLayout(0, 8));
        view.setBackground(bg());

        JButton back = styledButton("◄  Quay lại");
        back.addActionListener(e -> cards.show(cardPanel, CARD_LIST));

        JLabel name = new JLabel(feature.getName(), SwingConstants.CENTER);
        name.setFont(new Font("Dialog", Font.BOLD, 14));
        name.setForeground(bodyText());

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(back, BorderLayout.NORTH);
        header.add(name, BorderLayout.CENTER);

        JComponent content = feature.createPanel(calculator);
        recolorLabels(content, bodyText());

        view.add(header, BorderLayout.NORTH);
        view.add(content, BorderLayout.CENTER);

        featureView = view;
        cardPanel.add(view, CARD_FEATURE);
        cards.show(cardPanel, CARD_FEATURE);
        cardPanel.revalidate();
    }

    /** Đặt màu chữ cho các nhãn trong giao diện chức năng để hợp với giao diện hiện tại. */
    private static void recolorLabels(Component c, Color color) {
        if (c instanceof JLabel) c.setForeground(color);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) recolorLabels(child, color);
        }
    }

    /** Quay về danh sách chức năng. */
    public void showList() {
        cards.show(cardPanel, CARD_LIST);
    }

    private JButton styledButton(String text) {
        JButton b = new JButton(text);
        b.setFocusable(false);
        b.setOpaque(true);
        b.setBackground(item());
        b.setForeground(itemText());
        b.setFont(new Font("Dialog", Font.BOLD, 14));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(item().darker().darker()),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return b;
    }
}
