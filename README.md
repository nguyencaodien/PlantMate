<h1 align="center">🌿 PlantMate</h1>

<p align="center">
  <strong>Kết nối cộng đồng yêu cây cảnh — Chăm sóc thông minh với AI</strong>
</p>

<p align="center">
  PlantMate là ứng dụng di động kết nối cộng đồng yêu cây cảnh trong khu vực lân cận — nơi mọi người có thể <strong>cho tặng, trao đổi, bán, và tái định cư (rehome)</strong> cây cảnh, đồng thời <strong>theo dõi &amp; chăm sóc</strong> khu vườn của riêng mình với sự hỗ trợ của AI.
</p>

---

## 👥 Thành viên nhóm

| Họ và tên | MSSV |
|---|---|
| Nguyễn Cao Điền | 31241023473 |
| Hồ Trọng Phúc | 31241023650 |
| Trần Lê Quân | 31241021135 |
| Lê Quân | 31241021233 |
| Phạm Tuấn Đạt | 31241025206 |

---

## 🛠️ Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| **Nền tảng** | Android (Java / XML) |
| **Backend** | Firebase (Auth, Firestore, Storage, Cloud Messaging) |
| **AI / ML** | Google Gemini API |
| **Kiến trúc** | MVVM + Repository Pattern |
| **Build Tool** | Gradle |

---

## 🚀 Bắt đầu nhanh

```bash
# Clone repository
git clone https://github.com/nguyencaodien/PlantMate.git
cd PlantMate/android

# Mở bằng Android Studio và đồng bộ Gradle
```

- 📖 [Hướng dẫn cài đặt Android](ANDROID-SETUP.md)
- 📖 [README ứng dụng Android](android/README.md)

---

## 📱 Tổng quan tính năng

### 1. 🔐 Xác thực (Authentication)
- Màn hình **Đăng nhập / Đăng ký** với email & mật khẩu
- Hỗ trợ đăng nhập nhanh qua **Google Sign-In**
- Giao diện tối giản, tông màu xanh lá chủ đạo

### 2. 🏠 Trang chủ cộng đồng (Community Feed)
- Hiển thị danh sách cây theo khu vực (lọc theo bán kính, ví dụ *"within 5 km"*)
- Bộ lọc theo loại tin đăng: **All / Give Away / Trade / Rehome**
- Mỗi bài đăng gồm: ảnh cây, tên người đăng, khoảng cách, tên cây + loài, mô tả ngắn
- Nút yêu thích (❤️) và **"Ask for this plant"** để liên hệ
- Nút nổi (+) để tạo bài đăng mới

### 3. 🌱 Chi tiết cây (Plant Listing Detail)
- Ảnh lớn dạng carousel
- Nhãn phân loại: `TRADE`, `SELL`, `REHOME`
- Thông tin nhanh: ánh sáng (Sun), tần suất tưới (Water), độ khó chăm (Level)
- Đánh giá người bán/trao đổi (rating ⭐)
- Hai hành động chính: **Save** (lưu) và **Ask for it** (liên hệ)
- Nút **"Share to Community"** để chia sẻ lên bảng tin

### 4. ✍️ Đăng bài lên cộng đồng (Create Post)
- Ô nhập nội dung: chia sẻ tin vui, xin lời khuyên, hỏi đáp...
- Thêm ảnh cây (tuỳ chọn)
- Chọn danh mục: `Plant sharing`, `Plant win`, `Question`, `Tip`, `General`
- Có thể đính kèm một cây cụ thể từ bộ sưu tập cá nhân

### 5. 🪴 Khu vườn của tôi (My Garden)
- Tổng quan số cây cần chăm sóc hôm nay
- Danh sách nhắc việc nhanh: *Water now*, *Needs sun*, *Check soil*
- Lưới thẻ cây với ảnh, tên, loài, và lịch tưới nước tiếp theo (đổi màu theo mức độ khẩn cấp: 🟢 → 🟡 → 🔴)

### 6. 💚 Chi tiết & chăm sóc cây (Plant Detail & Care)
- Trạng thái sức khỏe cây (ví dụ: **Thriving**)
- Thông tin chăm sóc: Water, Light, Last care
- Nút hành động nhanh: **Water now**, **Add care log**
- Sau khi tưới, trạng thái cập nhật tức thì (nút chuyển thành "✓ Watered")
- Nút **"Ask AI about [tên cây]"** để hỏi trợ lý AI
- **Lịch sử chăm sóc (Care History)** dạng timeline theo thời gian

### 7. 📝 Ghi nhật ký chăm sóc (Add Care Log)
- Chọn loại hành động: Watered, Fertilized, Pruned, Rotated, Repotted, Checked soil
- Ghi chú tuỳ chọn
- Lưu log để cập nhật vào lịch sử

### 8. 🔬 Plant Doctor — Chẩn đoán cây bằng AI
- Chụp ảnh hoặc tải ảnh cây lên để phân tích
- Nút **"Analyse plant"** trả về kết quả với độ chính xác (%), tên bệnh nghi ngờ (ví dụ: *Possible Leaf Spot*)
- Danh sách **các bước xử lý đề xuất** theo thứ tự ưu tiên
- Có thể lưu kết quả chẩn đoán vào hồ sơ cây (**Save to plant**) hoặc chụp lại (**Retake**)
- ⚠️ Chỉ mang tính tham khảo, không thay thế tư vấn chuyên gia

### 9. 🤖 Trợ lý PlantMate AI (Chat)
- Giao diện chat dạng bong bóng hội thoại
- Gợi ý câu hỏi nhanh: *Care advice*, *What's wrong with my Monstera?*, *Watering...*
- Cho phép gửi ảnh kèm câu hỏi
- Nhắc nhở: *"AI suggestions may not always be accurate."*

### 10. 💬 Nhắn tin giữa người dùng (Messaging)
- Khung chat 1-1 gắn liền với tin đăng cụ thể (hiển thị ảnh, tên cây, giá ngay đầu khung chat)
- Nút **"View plant"** để quay lại chi tiết tin đăng
- Hỗ trợ gửi ảnh, biểu tượng bong bóng chat có badge số tin nhắn chưa đọc ở trang chủ

---

## 🎨 Ngôn ngữ thiết kế (Design Language)

| Thuộc tính | Mô tả |
|---|---|
| **Màu chủ đạo** | Xanh lá đậm (`#1E4234`) cho nút hành động chính, nền be/kem nhạt |
| **Typography** | Font sans-serif hiện đại, tiêu đề đậm, phụ đề màu xám nhạt |
| **Bo góc** | Card và input đều bo góc lớn (`rounded-2xl`), tạo cảm giác mềm mại |
| **Icon** | Icon line-style tối giản (nhà, lá cây, kính lúp, chat, hồ sơ) |
| **Thanh điều hướng** | 5 tab: Home · Garden · Doctor · Chat · Profile |
| **Trạng thái** | Badge màu (🟢 ổn · 🟡 cảnh báo · 🔴 cần xử lý gấp) |

---

## 🗂️ Cấu trúc luồng người dùng (User Flow)

```
Đăng nhập
   └── Trang chủ (Feed cộng đồng)
         ├── Xem chi tiết tin đăng → Nhắn tin với chủ cây
         ├── Đăng bài mới → Chia sẻ lên cộng đồng
         ├── Khu vườn của tôi
         │      └── Chi tiết cây → Ghi log chăm sóc → Lịch sử chăm sóc
         ├── Plant Doctor
         │      └── Chụp/tải ảnh → Kết quả chẩn đoán → Hỏi AI chi tiết
         └── PlantMate AI Chat
```

---

## 📂 Cấu trúc thư mục

```
PlantMate/
├── android/              # Mã nguồn ứng dụng Android (Java/XML)
├── frontend/             # Tài nguyên frontend
├── docs/                 # Tài liệu dự án
├── qa/                   # Kiểm thử & QA
├── Class/                # Class diagram
├── ERD/                  # Entity-Relationship Diagram
├── FunctionalDiagram/    # Function diagram
├── .firebaserc           # Cấu hình Firebase project
├── .gitignore
├── AGENTS.md
├── ANDROID_SETUP.md      # Hướng dẫn cài đặt Android
└── README.md
```

