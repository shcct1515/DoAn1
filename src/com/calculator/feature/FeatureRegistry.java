package com.calculator.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Danh sách các chức năng mở rộng hiện trong MENU. Thêm chức năng mới ở đây. */
public final class FeatureRegistry {
    private static final List<Feature> FEATURES = new ArrayList<>();

    static {
        register(new PlaceholderFeature("Giải phương trình", "Bậc 2, bậc 3, ..."));
        register(new PlaceholderFeature("Giải hệ phương trình", "Hệ 2 ẩn, 3 ẩn"));
        register(new PlaceholderFeature("Giải bất phương trình", "Bậc 2, bậc 3, ..."));
        register(new PlaceholderFeature("Số phức", "Tính toán với số phức"));
        register(new PlaceholderFeature("Ma trận", "Cộng, nhân, định thức, nghịch đảo"));
        register(new PlaceholderFeature("Vector", "Tích vô hướng, tích có hướng"));
        register(new PlaceholderFeature("Thống kê", "Trung bình, phương sai, hồi quy"));
        register(new PlaceholderFeature("Bảng giá trị hàm", "Bảng giá trị f(x)"));
    }

    private FeatureRegistry() {
    }

    public static void register(Feature feature) {
        FEATURES.add(feature);
    }

    public static List<Feature> getAll() {
        return Collections.unmodifiableList(FEATURES);
    }
}
