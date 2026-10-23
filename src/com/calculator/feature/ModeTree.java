package com.calculator.feature;

import com.calculator.feature.impl.*;

/**
 * Cây menu MODE. Bấm MODE rồi bấm số theo từng cấp, vd MODE 9 2 2 = Phương trình > Đa thức > Bậc 3.
 * Thêm chức năng mới: tạo class extends AbstractFeature rồi thêm một dòng ModeNode.leaf(...) vào đây.
 */
public final class ModeTree {
    private ModeTree() {
    }

    public static ModeNode root() {
        ModeNode r = ModeNode.menu("MODE");
        r.add(ModeNode.action("Tính toán", "comp"));

        r.add(ModeNode.action("Số phức (CMPLX)", "cmplx"));

        ModeNode base = ModeNode.menu("Hệ cơ số");
        String[] bases = {"DEC (10)", "HEX (16)", "BIN (2)", "OCT (8)"};
        for (int i = 0; i < bases.length; i++) base.add(ModeNode.action(bases[i], "base:" + i));
        r.add(base);

        ModeNode mat = ModeNode.menu("Ma trận");
        String[][] mOps = {{"det", "Định thức"}, {"inv", "Nghịch đảo"}, {"add", "A + B"}, {"mul", "A × B"}};
        for (String[] o : mOps) {
            ModeNode m = ModeNode.menu(o[1]);
            for (int n = 2; n <= 3; n++) m.add(ModeNode.leaf("Cấp " + n + "×" + n, new MatrixFeature(o[0], n)));
            mat.add(m);
        }
        r.add(mat);

        ModeNode vec = ModeNode.menu("Vector");
        String[][] vOps = {{"dot", "Tích vô hướng"}, {"cross", "Tích có hướng"}, {"angle", "Góc"},
                {"norm", "Độ dài |u|"}, {"add", "u + v"}, {"sub", "u − v"}};
        for (String[] o : vOps) {
            if (o[0].equals("cross")) {
                vec.add(ModeNode.leaf(o[1], new VectorFeature(o[0], 3)));
                continue;
            }
            ModeNode m = ModeNode.menu(o[1]);
            m.add(ModeNode.leaf("2 chiều", new VectorFeature(o[0], 2)));
            m.add(ModeNode.leaf("3 chiều", new VectorFeature(o[0], 3)));
            vec.add(m);
        }
        r.add(vec);

        ModeNode st = ModeNode.menu("Thống kê");
        st.add(ModeNode.leaf("Một biến", new StatisticsFeature(false)));
        st.add(ModeNode.leaf("Hồi quy y=a+bx", new StatisticsFeature(true)));
        r.add(st);

        ModeNode fn = ModeNode.menu("Hàm số");
        fn.add(ModeNode.leaf("Bảng giá trị", new TableFeature()));
        fn.add(ModeNode.leaf("Tích phân", new IntegralFeature()));
        fn.add(ModeNode.leaf("Logarit", new LogFeature()));
        r.add(fn);

        ModeNode ineq = ModeNode.menu("Bất phương trình");
        for (int deg = 2; deg <= 4; deg++) {
            ModeNode d = ModeNode.menu("Bậc " + deg);
            String[] ops = {"<", ">", "<=", ">="};
            for (String op : ops) d.add(ModeNode.leaf(op.replace("<=", "≤").replace(">=", "≥") + " 0",
                    new InequalityFeature(deg, op)));
            ineq.add(d);
        }
        r.add(ineq);

        ModeNode eq = ModeNode.menu("Phương trình");
        ModeNode sys = ModeNode.menu("Hệ phương trình");
        sys.add(ModeNode.leaf("2 ẩn", new SystemFeature(2)));
        sys.add(ModeNode.leaf("3 ẩn", new SystemFeature(3)));
        sys.add(ModeNode.leaf("4 ẩn", new SystemFeature(4)));
        ModeNode poly = ModeNode.menu("Đa thức");
        poly.add(ModeNode.leaf("Bậc 2", new EquationFeature(2)));
        poly.add(ModeNode.leaf("Bậc 3", new EquationFeature(3)));
        poly.add(ModeNode.leaf("Bậc 4", new EquationFeature(4)));
        eq.add(sys).add(poly);
        r.add(eq);
        return r;
    }
}
