# Member List

| Name | MSSV |
|------|------|
| Nguy?n Cao Ði?n | 31241023473 |
| H? Tr?ng Phúc | 31241023650 |
| Tr?n Lê Quân | 31241021135 |
| Lê Quân | 31241021233 |
| Ph?m Tu?n Ð?t | 31241025206 |
# ?? PlantMate

PlantMate là ?ng d?ng di ð?ng k?t n?i c?ng ð?ng yêu cây c?nh trong khu v?c lân c?n — nõi m?i ngý?i có th? **cho t?ng, trao ð?i, bán, và tái ð?nh cý (rehome)** cây c?nh, ð?ng th?i **theo d?i & chãm sóc** khu vý?n c?a riêng m?nh v?i s? h? tr? c?a AI.

> M? ngu?n ?ng d?ng Android (Java/XML) n?m trong thý m?c `android/`. Xem cách ch?y t?i [android/README.md](android/README.md) và [ANDROID-SETUP.md](ANDROID-SETUP.md).

----------------------------------------------------

## ?? T?ng quan tính nãng

### 1. Xác th?c (Authentication)
- Màn h?nh **Ðãng nh?p / Ðãng k?** v?i email & m?t kh?u
- H? tr? ðãng nh?p nhanh qua Google
- Giao di?n t?i gi?n, tông màu xanh lá ch? ð?o

### 2. Trang ch? c?ng ð?ng (Community Feed)
- Hi?n th? danh sách cây theo khu v?c (l?c theo bán kính, ví d? *"within 5 km"*)
- B? l?c theo lo?i tin ðãng: **All / Give Away / Trade / Rehome**
- M?i bài ðãng g?m: ?nh cây, tên ngý?i ðãng, kho?ng cách, tên cây + loài, mô t? ng?n
- Nút yêu thích (??) và **"Ask for this plant"** ð? liên h?
- Nút n?i (+) ð? t?o bài ðãng m?i

### 3. Chi ti?t cây (Plant Listing Detail)
- ?nh l?n d?ng carousel
- Nh?n phân lo?i: `TRADE`, `SELL`, `REHOME`
- Thông tin nhanh: ánh sáng (Sun), t?n su?t tý?i (Water), ð? khó chãm (Level)
- Ðánh giá ngý?i bán/trao ð?i (rating ?)
- Hai hành ð?ng chính: **Save** (lýu) và **Ask for it** (liên h?)
- Nút **"Share to Community"** ð? chia s? lên b?ng tin

### 4. Ðãng bài lên c?ng ð?ng (Create Post)
- Ô nh?p n?i dung: chia s? tin vui, xin l?i khuyên, h?i ðáp...
- Thêm ?nh cây (tu? ch?n)
- Ch?n danh m?c: `Plant sharing`, `Plant win`, `Question`, `Tip`, `General`
- Có th? ðính kèm m?t cây c? th? t? b? sýu t?p cá nhân

### 5. Khu vý?n c?a tôi (My Garden)
- T?ng quan s? cây c?n chãm sóc hôm nay
- Danh sách nh?c vi?c nhanh: *Water now*, *Needs sun*, *Check soil*
- Lý?i th? cây v?i ?nh, tên, loài, và l?ch tý?i ný?c ti?p theo (ð?i màu theo m?c ð? kh?n c?p: xanh lá ? vàng ? ð?)

### 6. Chi ti?t & chãm sóc cây (Plant Detail & Care)
- Tr?ng thái s?c kh?e cây (ví d?: **Thriving**)
- Thông tin chãm sóc: Water, Light, Last care
- Nút hành ð?ng nhanh: **Water now**, **Add care log**
- Sau khi tý?i, tr?ng thái c?p nh?t t?c th? (nút chuy?n thành "? Watered")
- Nút **"Ask AI about [tên cây]"** ð? h?i tr? l? AI
- **L?ch s? chãm sóc (Care History)** d?ng timeline theo th?i gian

### 7. Ghi nh?t k? chãm sóc (Add Care Log)
- Ch?n lo?i hành ð?ng: Watered, Fertilized, Pruned, Rotated, Repotted, Checked soil
- Ghi chú tu? ch?n
- Lýu log ð? c?p nh?t vào l?ch s?

### 8. Plant Doctor — Ch?n ðoán cây b?ng AI
- Ch?p ?nh ho?c t?i ?nh cây lên ð? phân tích
- Nút **"Analyse plant"** tr? v? k?t qu? v?i ð? chính xác (%), tên b?nh nghi ng? (ví d?: *Possible Leaf Spot*)
- Danh sách **các bý?c x? l? ð? xu?t** theo th? t? ýu tiên
- Có th? lýu k?t qu? ch?n ðoán vào h? sõ cây (**Save to plant**) ho?c ch?p l?i (**Retake**)
- Ghi chú mi?n tr? trách nhi?m: ch? mang tính tham kh?o, không thay th? tý v?n chuyên gia

### 9. Tr? l? PlantMate AI (Chat)
- Giao di?n chat d?ng bong bóng h?i tho?i
- G?i ? câu h?i nhanh: *Care advice*, *What's wrong with my Monstera?*, *Watering...*
- Cho phép g?i ?nh kèm câu h?i
- Nh?c nh?: *"AI suggestions may not always be accurate."*

### 10. Nh?n tin gi?a ngý?i dùng (Messaging)
- Khung chat 1-1 g?n li?n v?i tin ðãng c? th? (hi?n th? ?nh, tên cây, giá ngay ð?u khung chat)
- Nút **"View plant"** ð? quay l?i chi ti?t tin ðãng
- H? tr? g?i ?nh, bi?u tý?ng bong bóng chat có badge s? tin nh?n chýa ð?c ? trang ch?

---

## ?? Ngôn ng? thi?t k? (Design Language)

| Thu?c tính | Mô t? |
|---|---|
| **Màu ch? ð?o** | Xanh lá ð?m (`#1E4234`-ish) cho nút hành ð?ng chính, n?n be/kem nh?t |
| **Typography** | Font sans-serif hi?n ð?i, tiêu ð? ð?m, ph? ð? màu xám nh?t |
| **Bo góc** | Card và input ð?u bo góc l?n (rounded-2xl), t?o c?m giác m?m m?i |
| **Icon** | Icon line-style t?i gi?n (nhà, lá cây, kính lúp, chat, h? sõ) |
| **Thanh ði?u hý?ng dý?i** | 5 tab: Home · Garden · Doctor · Chat · Profile |
| **Tr?ng thái** | Dùng badge màu (xanh = ?n, vàng = c?nh báo, ð? = c?n x? l? g?p) |

---

## ??? C?u trúc lu?ng ngý?i dùng (User Flow)

```
Ðãng nh?p
   ??? Trang ch? (Feed c?ng ð?ng)
         ??? Xem chi ti?t tin ðãng ? Nh?n tin v?i ch? cây
         ??? Ðãng bài m?i ? Chia s? lên c?ng ð?ng
         ??? Khu vý?n c?a tôi
         ?      ??? Chi ti?t cây ? Ghi log chãm sóc ? L?ch s? chãm sóc
         ??? Plant Doctor
         ?      ??? Ch?p/t?i ?nh ? K?t qu? ch?n ðoán ? H?i AI chi ti?t
         ??? PlantMate AI Chat
```

---

## ?? Ghi chú

Ðây là repo lýu tr? các màn h?nh UI (mockup) ph?c v? m?c ðích tr?nh bày ? tý?ng s?n ph?m / portfolio thi?t k?. Chýa bao g?m m? ngu?n tri?n khai th?c t?.

