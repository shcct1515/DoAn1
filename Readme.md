# 🧮 CASIO FX-570 Simulator

Ứng dụng mô phỏng máy tính khoa học **CASIO FX-570** được xây dựng bằng **Java**.

Dự án tập trung vào việc áp dụng **Lập trình hướng đối tượng, Cấu trúc dữ liệu và Giải thuật**, đặc biệt là **Stack** và **thuật toán Ba Lan ngược (Reverse Polish Notation - RPN)** để phân tích và tính toán biểu thức toán học.

---

## ✨ Tính năng

### 🔢 Tính toán cơ bản

* Cộng, trừ, nhân, chia
* Dấu ngoặc
* Số âm
* Lũy thừa
* Căn bậc hai
* Phần trăm
* Giai thừa
* Giá trị tuyệt đối

### 📐 Hàm toán học

* `sin`, `cos`, `tan`
* `sin⁻¹`, `cos⁻¹`, `tan⁻¹`
* `sinh`, `cosh`, `tanh`
* `log`, `ln`
* `eˣ`, `10ˣ`
* `xʸ`
* `ⁿ√x`

### 🧮 Các chức năng nâng cao

* Phân số ↔ số thập phân
* Số phức
* Ma trận
* Vector
* Thống kê
* Tổ hợp và chỉnh hợp
* Hệ cơ số `BIN`, `OCT`, `DEC`, `HEX`
* Bộ nhớ tính toán
* Bảng giá trị hàm

### 🎨 Giao diện

* Mô phỏng giao diện máy tính CASIO.
* Các phím chức năng được tổ chức trực quan.
* Hiển thị biểu thức và kết quả tính toán.

---

## 🧠 Thuật toán Ba Lan ngược (RPN)

Một trong những phần quan trọng nhất của project là xử lý biểu thức toán học bằng **Reverse Polish Notation**.

Ví dụ người dùng nhập:

```text
2 + 3 × (4 - 1)
```

Biểu thức sẽ được chuyển thành:

```text
2 3 4 1 - × +
```

Sau đó sử dụng **Stack** để tính toán.

```text
2 + 3 × (4 - 1)
        ↓
     Tokenizer
        ↓
[2] [3] [4] [1] [-] [×] [+]
        ↓
   RPN Converter
        ↓
2 3 4 1 - × +
        ↓
  RPN Evaluator
        ↓
      11
```

### 🔄 Quá trình tính RPN

```text
Push 2
Push 3
Push 4
Push 1

Gặp '-'
→ 4 - 1 = 3
→ Push 3

Gặp '×'
→ 3 × 3 = 9
→ Push 9

Gặp '+'
→ 2 + 9 = 11
→ Push 11
```

Kết quả cuối cùng:

```text
11
```

---

## 🔍 Parse biểu thức

**Parse** là quá trình phân tích biểu thức người dùng nhập vào để chương trình có thể hiểu được các thành phần bên trong.

Ví dụ:

```text
2 + 3 × (4 - 1)
```

được phân tích thành:

```text
[2] [+] [3] [×] [(] [4] [-] [1] [)]
```

Trong đó:

```text
2, 3, 4, 1  → Operand (toán hạng)
+, -, ×      → Operator (toán tử)
(, )         → Parentheses (dấu ngoặc)
```

Quá trình này giúp chương trình biết **cái gì là số, cái gì là phép toán và thứ tự xử lý như thế nào**.

---

## 📂 Cấu trúc thư mục

```text
CASIO-FX570/
│
├── 📁 src/                              # 💻 Source code của chương trình
│   └── 📁 main/
│       └── 📁 java/
│           └── 📁 com/
│               └── 📁 calculator/
│
│                   ├── 🚀 Main.java
│                   │   # Điểm bắt đầu chạy chương trình
│                   │
│                   ├── 📁 calculator/
│                   │   # ⚙️ Chứa logic chính của máy tính
│                   │   └── Calculator.java
│                   │       # Điều phối quá trình tính toán
│                   │
│                   ├── 📁 parser/
│                   │   # 🔍 Phân tích và xử lý biểu thức đầu vào
│                   │   ├── Tokenizer.java
│                   │   │   # Tách chuỗi biểu thức thành các Token
│                   │   │
│                   │   └── RPNConverter.java
│                   │       # Chuyển biểu thức trung tố → RPN
│                   │
│                   ├── 📁 evaluator/
│                   │   # 🧮 Thực thi biểu thức RPN
│                   │   └── RPNEvaluator.java
│                   │       # Dùng Stack để tính toán biểu thức
│                   │
│                   ├── 📁 model/
│                   │   # 📦 Các class đại diện cho dữ liệu
│                   │   └── Token.java
│                   │       # Đại diện cho một Token trong biểu thức
│                   │
│                   └── 📁 ui/
│                       # 🎨 Giao diện người dùng
│                       └── CalculatorUI.java
│                           # Xây dựng giao diện bằng Java Swing
│
├── 📄 README.md                         # 📖 Tài liệu mô tả project
│
├── 📄 .gitignore                        # 🚫 Các file Git không theo dõi
│
└── 📄 pom.xml                           # 📦 Cấu hình Maven & dependencies
```

---

## 🔄 Luồng hoạt động

Toàn bộ quá trình tính toán:

```text
       👤 Người dùng
             │
             ▼
     🖥️ CalculatorUI
             │
             ▼
       ⚙️ Calculator
             │
             ▼
       🔍 Tokenizer
             │
             ▼
      🔄 RPNConverter
             │
             ▼
      📚 Stack / RPN
             │
             ▼
      🧮 RPNEvaluator
             │
             ▼
        ✅ Kết quả
```

### Vai trò của từng thành phần

| Thành phần     | Nhiệm vụ                                      |
| -------------- | --------------------------------------------- |
| `CalculatorUI` | 🎨 Giao diện và nhận input                    |
| `Calculator`   | ⚙️ Điều phối quá trình tính toán              |
| `Tokenizer`    | 🔍 Parse và tách biểu thức thành Token        |
| `RPNConverter` | 🔄 Chuyển Infix → Postfix/RPN                 |
| `RPNEvaluator` | 🧮 Tính biểu thức RPN bằng Stack              |
| `Token`        | 📦 Đại diện cho từng thành phần của biểu thức |

---

## 🛠️ Công nghệ sử dụng

* ☕ **Java**
* 🎨 **Java Swing**
* 📚 **Stack / Deque**
* 🔄 **Reverse Polish Notation (RPN)**
* 🌳 **Shunting-yard Algorithm**
* 📦 **Maven**
* 🧱 **Object-Oriented Programming (OOP)**

---

## ▶️ Cách chạy

### 💻 Chạy bằng IDE

Có thể mở project bằng:

* IntelliJ IDEA
* Eclipse
* Visual Studio Code

Sau đó chạy:

```text
src/main/java/com/calculator/Main.java
```

### 📦 Chạy bằng Maven

Build project:

```bash
mvn clean package
```

Sau khi build thành công:

```bash
java -jar target/CASIO-FX570.jar
```

---

## 🧪 Ví dụ

### Phép tính cơ bản

```text
Input:
2 + 3 * (4 - 1)

Output:
11
```

### Hàm lượng giác

```text
Input:
sin(30)

Mode:
DEG

Output:
0.5
```

### Lũy thừa

```text
Input:
2 ^ 10

Output:
1024
```

---

## 🎯 Mục tiêu đồ án

Dự án được xây dựng nhằm:

1. 🧠 Hiểu và áp dụng **Stack**.
2. 🔄 Triển khai **thuật toán Ba Lan ngược**.
3. 🔍 Xây dựng bộ **Parser/Tokenizer** cho biểu thức toán học.
4. ☕ Áp dụng **OOP trong Java**.
5. 🎨 Xây dựng giao diện bằng **Java Swing**.
6. 🧮 Xử lý các phép toán và hàm toán học.
7. 🏗️ Rèn luyện cách tổ chức một project Java thực tế.

---

## 👨‍💻 Tác giả

**CASIO FX-570 Simulator**

> Dự án được xây dựng nhằm mục đích học tập và nghiên cứu.
