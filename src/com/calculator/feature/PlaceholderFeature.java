package com.calculator.feature;

import com.calculator.calculator.Calculator;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/** Chức năng giữ chỗ: hiện thông báo "đang phát triển" cho đến khi được viết thật. */
public class PlaceholderFeature implements Feature {
    private final String name;
    private final String description;

    public PlaceholderFeature(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public JComponent createPanel(Calculator calculator) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        JLabel label = new JLabel("<html><div style='text-align:center'>" + name
                + "<br><br><font size='-1'>Chức năng đang được phát triển</font></div></html>",
                SwingConstants.CENTER);
        label.setFont(new Font("Dialog", Font.BOLD, 14));
        label.setForeground(new Color(0xcfd3d8));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
