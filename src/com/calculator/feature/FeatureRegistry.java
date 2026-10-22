package com.calculator.feature;

import com.calculator.feature.impl.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Danh sách các chức năng mở rộng hiện trong MENU. Thêm chức năng mới ở đây. */
public final class FeatureRegistry {
    private static final List<Feature> FEATURES = new ArrayList<>();

    static {
        // Thêm chức năng mới: tạo class extends AbstractFeature trong feature/impl rồi register tại đây.
        register(new EquationFeature());
        register(new SystemFeature());
        register(new InequalityFeature());
        register(new ComplexFeature());
        register(new MatrixFeature());
        register(new VectorFeature());
        register(new StatisticsFeature());
        register(new TableFeature());
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
