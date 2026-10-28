# 🧮 CASIO FX-570 Simulator

Ứng dụng mô phỏng máy tính khoa học **CASIO fx-570** viết bằng **Java Swing**.

Dự án áp dụng **Lập trình hướng đối tượng, Cấu trúc dữ liệu và Giải thuật**, tập trung vào **Stack** và **thuật toán Ba Lan ngược (RPN)** để phân tích, tính biểu thức. Giao diện và cách bấm phím bám theo máy thật: menu **MODE**, nhập từng hệ số `a ?`, `b ?`, `c ?` ngay trên màn LCD, phân số gạch ngang, phím tắt `MODE 9 2 2`...

---

## ✨ Tính năng

### 🔢 Tính toán cơ bản
* Cộng, trừ, nhân, chia, dấu ngoặc, số âm
* Luỹ thừa `x²`, `x^y`, `x⁻¹`, căn `√`, `%`, giai thừa `x!`, giá trị tuyệt đối `|x|`
* `×10^x`, hằng số `π`, `e`, kết quả trước `Ans`
* Tổ hợp `nCr`, chỉnh hợp `nPr`

### 📐 Hàm toán học
* `sin`, `cos`, `tan` và hàm ngược `sin⁻¹`, `cos⁻¹`, `tan⁻¹`
* `sinh`, `cosh`, `tanh`
* `log`, `ln`, `eˣ`, `10ˣ`, `ⁿ√x`
* Đơn vị góc `DEG` / `RAD` / `GRAD`

### 🧾 Hiển thị và bộ nhớ
* Đổi qua lại **phân số ⇔ thập phân** (`S⇔D`), hiển thị hỗn số
* Hiển thị `NORM`, `FIX`, `SCI`, chọn số chữ số
* Hệ cơ số `DEC`, `HEX`, `BIN`, `OCT`
* Bộ nhớ `M+`, `MR`, `MC`, lưu biến `STO` (A–F, X, Y, M)
* Lịch sử biểu thức, ▲ ▼ để xem lại

### 🎛️ Menu MODE (hiện trực tiếp trên LCD)

Bấm `MODE` rồi bấm số để chọn, có thể bấm liền như `MODE 9 2 2` (= Phương trình bậc 3).
`DEL` lùi một cấp, `AC` thoát.

| Phím | Chức năng |
| ---- | --------- |
| `1` | **Tính toán** (COMP, chế độ thường) |
| `2` | **Số phức** (CMPLX): gõ biểu thức có `i`, ví dụ `(1+2i)(3-i)`; kết quả dạng `a+bi`, kèm `r` và góc `∠` |
| `3` | **Hệ cơ số**: DEC, HEX, BIN, OCT |
| `4` | **Ma trận**: định thức, nghịch đảo (2×2 đến 4×4, kết quả dạng phân số), `A+B`, `A×B` (2×2, 3×3) |
| `5` | **Vector**: tích vô hướng, tích có hướng, góc, độ dài, cộng, trừ (2D, 3D) |
| `6` | **Thống kê**: một biến, hồi quy tuyến tính `y = a + bx` |
| `7` | **Hàm số**: `1` Bảng giá trị `f(X)`, `2` Tích phân `∫`, `3` Logarit `logₐ(b)` |
| `8` | **Bất phương trình** `1` bậc 2, `2` bậc 3, `3` bậc 4, `4` bậc 1 (`<`, `>`, `≤`, `≥`) |
| `9` | **Phương trình**: `1` Hệ 2, 3, 4 ẩn; `2` Đa thức: `1` bậc 2, `2` bậc 3, `3` bậc 4, `4` bậc 1 |

**Ma trận và hệ phương trình** được nhập trên **lưới ô vuông** vẽ ngay trên LCD: ô đang sửa được tô đậm, ô chưa nhập hiện mờ `0` (bỏ trống = 0). Gõ số (hoặc phân số, căn, biến...) rồi bấm `=` để sang ô kế; `▲ ▼` đổi hàng, `◄ ►` đổi ô khi con trỏ đã ở mép ô (trong phân số thì `▲ ▼ ◄ ►` vẫn di chuyển như bình thường). `DEL` ở ô trống lùi về ô trước. Bấm `=` ở ô cuối để tính; ở trang kết quả bấm `=` để sửa lại các số cũ.

Cách nhập trong các chức năng khác: máy hỏi từng ô (`a ?`, `b ?`...), nhập bằng phím máy tính (gõ được phân số, căn, biến, `Ans`) rồi bấm `=` để sang ô kế. Nhập xong ô cuối thì kết quả hiện ngay trên LCD, ▲ ▼ để cuộn, `=` để nhập lại bài mới, `AC` để thoát.

### ✍️ Mẫu nhập như máy thật (ô trống hiện □)

Hàng phím trên cùng chèn thẳng vào dòng nhập các mẫu giống CASIO fx-580VN X, giống cách `a b/c` chèn phân số:

| Phím | Mẫu | Thứ tự các ô (bấm `►` để sang ô kế) |
| ---- | --- | ----------------------------------- |
| `∫` | ∫ cận trên, cận dưới, biểu thức, `dx` | cận dưới → cận trên → biểu thức (con trỏ bắt đầu ở biểu thức, `◄` lùi lên các cận) |
| `d/dx` | d/dx (biểu thức) \|x=điểm | biểu thức → điểm |
| `Σ` / `Π` | tổng / tích, `x=` bên dưới, cận trên | từ → đến → biểu thức (con trỏ bắt đầu ở biểu thức) |
| `logₐ` | log cơ số dưới, (đối số) | cơ số → đối số |
| `x` | biến x (hiện `X`) dùng trong biểu thức bên trong | |

`►` `◄` đi qua từng ô rồi ra ngoài (từ đầu ô biểu thức của `∫`, `Σ`, `Π`, `◄` đi lên cận trên rồi cận dưới), `▲` `▼` nhảy giữa cận trên, biểu thức và cận dưới. `DEL` ở đầu một mẫu còn trống thì xoá cả mẫu. Mẫu dùng được như một số hạng bình thường và lồng nhau được, ví dụ `2×∫…dx + logₐ…`. Ô còn trống hoặc sai thì báo `Syntax ERROR` / `Math ERROR` như máy thật.

### ⚙️ Setting
* **Giao diện**: chọn theme, cỡ chữ LCD
* **Phím tắt**: gắn phím bàn phím cho từng phím máy tính
* **Tính toán**: đơn vị góc, kiểu hiển thị, số chữ số, tự động phân số, hỗn số
* **Chung**: luôn nằm trên cùng, số dòng lịch sử
* Cài đặt được lưu lại giữa các lần chạy

### ⌨️ Thao tác trong ô nhập
| Thao tác | Tác dụng |
| -------- | -------- |
| `a b/c` | Chèn phân số; nếu ngay trước có số hoặc nhóm ngoặc thì đưa lên tử số |
| `►` | Đi từ tử xuống mẫu, rồi ra khỏi phân số; ra khỏi số mũ |
| `◄` | Đi vào lại phân số, số mũ |
| `▲` `▼` | Nhảy giữa tử và mẫu (ngoài phân số: xem lịch sử) |
| `DEL` | Xoá một ký tự hoặc nguyên một hàm (`sin(`, `Ans`...) |
| `i` | Số phức, tự bật chế độ CMPLX. `SHIFT` + `i` = MC |

---

## 🧠 Thuật toán Ba Lan ngược (RPN)

Biểu thức được chuyển từ **trung tố** sang **hậu tố (RPN)** bằng thuật toán **Shunting-yard**, rồi tính bằng **Stack**.

```text
2 + 3 × (4 - 1)
        ↓
     Tokenizer          →  [2] [+] [3] [×] [(] [4] [-] [1] [)]
        ↓
   RPN Converter        →  2 3 4 1 - × +
        ↓
  RPN Evaluator         →  11
```

Quá trình tính bằng Stack:

```text
Push 2, 3, 4, 1
Gặp '-'  →  4 - 1 = 3   →  Push 3
Gặp '×'  →  3 × 3 = 9   →  Push 9
Gặp '+'  →  2 + 9 = 11  →  Push 11
```

### 🔍 Parse biểu thức

**Tokenizer** tách chuỗi nhập thành các token: toán hạng (số, hằng số, biến), toán tử, hàm và dấu ngoặc. Tokenizer cũng xử lý nhân ngầm (`2π`, `3(4+1)`), số mũ, và đổi khung phân số `tử ⁄ mẫu` thành `((tử)/(mẫu))` để phân số luôn được tính trước.

### Các thuật toán khác trong project

| Chức năng | Thuật toán |
| --------- | ---------- |
| Số phức (CMPLX) | Bộ phân tích đệ quy (recursive descent), luỹ thừa bằng dạng cực |
| Phương trình bậc 2, 3 | Công thức nghiệm, Cardano / lượng giác |
| Phương trình bậc 4 | Durand–Kerner (nghiệm gần đúng) |
| Hệ phương trình, nghịch đảo ma trận | Gauss–Jordan, chọn phần tử trụ |
| Định thức, hạng ma trận | Khử Gauss |
| Bất phương trình | Xét dấu theo nghiệm, trả về tập nghiệm dạng khoảng |
| Tích phân | Simpson thích nghi |
| Đạo hàm `d/dx` | Sai phân trung tâm + ngoại suy Richardson (Ridders), kiểm tra khả vi hai phía |
| `Σ`, `Π`, `logₐ` | Cộng/nhân dồn từng số hạng; `ln b / ln a` |
| Hồi quy tuyến tính | Bình phương tối thiểu |

---

## 📂 Cấu trúc thư mục

```text
CASIO-FX570/
├── src/com/calculator/
│   ├── Main.java                    # Điểm bắt đầu chạy chương trình
│   ├── CalculatorTest.java          # Kiểm thử nhanh bộ tính toán
│   ├── FeatureTest.java             # Kiểm thử các chức năng MODE (PT, BPT, ma trận)
│   ├── TemplateTest.java            # Kiểm thử mẫu nhập ∫, d/dx, Σ, Π, log cơ số
│   │
│   ├── calculator/
│   │   ├── Calculator.java          # Điều phối tính toán, biến, bộ nhớ, định dạng kết quả
│   │   ├── Templates.java, Calculus.java  # Tính mẫu ∫, d/dx, Σ, Π, log; tích phân và đạo hàm số
│   │   └── ComplexMath.java         # Tính biểu thức số phức (CMPLX)
│   │
│   ├── parser/
│   │   ├── Tokenizer.java           # Tách biểu thức thành Token
│   │   └── RPNConverter.java        # Trung tố → RPN (Shunting-yard)
│   │
│   ├── evaluator/
│   │   └── RPNEvaluator.java        # Tính biểu thức RPN bằng Stack
│   │
│   ├── model/                       # Dữ liệu dùng chung
│   │   ├── Token.java, Symbols.java (ký tự đặc biệt: phân số, số mũ)
│   │   └── AngleMode.java, DisplayMode.java, CalcException.java
│   │
│   ├── settings/
│   │   ├── Settings.java            # Cài đặt, phím tắt, lưu ra file
│   │   └── Theme.java               # Giao diện màu sắc
│   │
│   ├── feature/                     # Các chức năng của menu MODE
│   │   ├── Feature.java             # Giao diện chung của một chức năng
│   │   ├── AbstractFeature.java     # Khung sẵn: chỉ cần viết hàm solve()
│   │   ├── ModeNode.java, ModeTree.java   # Cây menu MODE
│   │   └── impl/                    # Phương trình, hệ PT, bất PT, ma trận, vector,
│   │                                # thống kê, bảng giá trị, tích phân, logarit
│   │
│   └── ui/                          # Giao diện Java Swing
│       ├── CalculatorUI.java        # Cửa sổ chính, bàn phím, xử lý phím
│       ├── ExpressionPane.java      # Ô nhập tự vẽ: phân số, số mũ, con trỏ
│       ├── ResultView.java          # Hiển thị kết quả
│       ├── ModeScreen.java          # Menu MODE, nhập từng ô, trang kết quả trên LCD
│       ├── SettingsDialog.java      # Hộp thoại Setting
│       └── ReplayKey.java, ActionEntry.java
│
└── Readme.md
```

---

## 🔄 Luồng hoạt động

```text
        👤 Người dùng
              │
              ▼
        🖥️ CalculatorUI ─────────────► ModeScreen ──► Feature (solve)
              │                          (MODE)
              ▼
         ⚙️ Calculator
              │
   ┌──────────┴──────────┐
   ▼                     ▼
Tokenizer            ComplexMath
   │                  (CMPLX)
   ▼
RPNConverter
   │
   ▼
RPNEvaluator (Stack)
   │
   ▼
 ✅ Kết quả
```

| Thành phần | Nhiệm vụ |
| ---------- | -------- |
| `CalculatorUI` | Giao diện, nhận phím bấm và phím tắt |
| `ExpressionPane` | Vẽ biểu thức (phân số gạch ngang, số mũ), quản lý con trỏ |
| `ModeScreen` | Menu MODE và các chức năng chạy trực tiếp trên LCD |
| `Calculator` | Điều phối tính toán |
| `Tokenizer` | Parse biểu thức thành Token |
| `RPNConverter` | Infix → RPN |
| `RPNEvaluator` | Tính RPN bằng Stack |
| `ComplexMath` | Tính biểu thức số phức |

---

## ➕ Thêm chức năng mới vào menu MODE

1. Tạo class mới trong `feature/impl` kế thừa `AbstractFeature`:

```java
public class MyFeature extends AbstractFeature {
    public MyFeature() {
        super("Tên hiển thị", "a", "b");          // nhãn các ô nhập: a ?, b ?
    }

    @Override
    public String solve(List<String> in, Calculator calc) {
        double a = val(calc, in, 0), b = val(calc, in, 1);
        return "a + b = " + MathUtil.f(a + b);   // mỗi dòng là một dòng trên LCD
    }
}
```

2. Thêm một dòng vào `feature/ModeTree.java`:

```java
menu.add(ModeNode.leaf("Tên mục", new MyFeature()));
```

Dữ liệu sai thì ném `IllegalArgumentException("...")`, thông báo sẽ hiện trên LCD.
Cần số ô nhập thay đổi (như Thống kê) thì ghi đè hàm `prompt()`.

---

## 🛠️ Công nghệ sử dụng

* ☕ **Java 11** trở lên
* 🎨 **Java Swing** (giao diện, ô nhập tự vẽ bằng `Graphics2D`)
* 📚 **Stack / Deque**
* 🔄 **Reverse Polish Notation**, **Shunting-yard**
* 🧱 **Lập trình hướng đối tượng**

---

## ▶️ Cách chạy

Mở project bằng **Eclipse**, **IntelliJ IDEA** hoặc **VS Code**, đặt thư mục `src` làm source folder (encoding **UTF-8**), rồi chạy:

```text
src/com/calculator/Main.java
```

Phím tắt mặc định: `F3` mở MODE, `F4` mở Setting. Có thể đổi trong **Setting → Phím tắt**.

---

## 🧪 Ví dụ

| Nhập | Kết quả |
| ---- | ------- |
| `2 + 3 × (4 - 1)` | `11` |
| `sin(30)` (DEG) | `0.5` |
| `2^10` | `1024` |
| `8 × ⌈2 / (9+8)⌉` (phân số gạch ngang) | `0.9411...` |
| `(1+2i)(3-i)` (CMPLX) | `5+5i` |
| `MODE 9 2 2`, nhập `1 -6 11 -6` | `x1 = 1, x2 = 2, x3 = 3` |
| `MODE 7 2`, `f(X)=X²`, `0`, `3` | `∫ = 9` |
| `∫` gõ `X²`, `◄` (về đầu ô) `◄` gõ `3`, `◄` gõ `0` | `9` |
| `d/dx` `sin(X)` ► `0` (RAD) | `1` |
| `Σ` gõ `X`, `◄` gõ `100`, `◄` gõ `1` | `5050` |
| `logₐ` `2` ► `8` | `3` |
| `MODE 9 2 4`, `a=3`, `b=2` | `x = -2/3` |
| `MODE 8 4 1`, `a=2`, `b=-4` (2x−4 < 0) | `Nghiệm: (-∞; 2)` |
| `MODE 4 2 3`, ma trận 4×4 | `det A` và `A⁻¹` dạng phân số |

---

## 🎯 Mục tiêu đồ án

1. 🧠 Hiểu và áp dụng **Stack**.
2. 🔄 Triển khai **thuật toán Ba Lan ngược**.
3. 🔍 Xây dựng bộ **Parser/Tokenizer** cho biểu thức toán học.
4. ☕ Áp dụng **OOP trong Java** (kế thừa, đa hình qua `Feature`).
5. 🎨 Xây dựng giao diện bằng **Java Swing**.
6. 🧮 Cài đặt các thuật toán số: nghiệm đa thức, Gauss–Jordan, tích phân số.
7. 🏗️ Tổ chức một project Java theo từng package rõ ràng.

---

## 👨‍💻 Tác giả

**CASIO FX-570 Simulator**

> Dự án được xây dựng nhằm mục đích học tập và nghiên cứu.
