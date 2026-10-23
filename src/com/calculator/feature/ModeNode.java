package com.calculator.feature;

import java.util.ArrayList;
import java.util.List;

/** Một mục trong menu MODE: menu con, chức năng (feature), hoặc hành động của máy ("comp", "base:n"). */
public final class ModeNode {
    public final String title;
    public final List<ModeNode> children = new ArrayList<>();
    public final Feature feature;
    public final String action;

    private ModeNode(String title, Feature feature, String action) {
        this.title = title;
        this.feature = feature;
        this.action = action;
    }

    public static ModeNode menu(String title) { return new ModeNode(title, null, null); }
    public static ModeNode leaf(String title, Feature f) { return new ModeNode(title, f, null); }
    public static ModeNode action(String title, String id) { return new ModeNode(title, null, id); }

    public ModeNode add(ModeNode child) {
        children.add(child);
        return this;
    }
}
