# PUBLICATION_FILES_CHECK — Rà soát tài nguyên và bằng chứng trước khi công khai

**Issue**: #35  
**Người thực hiện**: lequantr1898  
**Ngày kiểm tra**: 2026-10-28  
**Phụ thuộc**: Issue #29 (đã hoàn thành — .gitignore đã bổ sung quy tắc ignore cache/exports)

---

## 1. `qa/screenshots/` — Ảnh chụp màn hình UI

**Tổng số**: 147 file PNG  
**Kích thước**: ~85 MB (ước tính)

### Phân loại nội dung

| Nhóm | Số file | Mô tả |
|---|---|---|
| `01–90` | 90 | Ảnh QA gốc: home, garden, doctor, community, profile, listing, v.v. |
| `91–147` | 57 | Ảnh upgrade + retest: inbox, search, dark mode, font scale 130/200 |

### Kết luận

- **Không phát hiện thông tin nhạy cảm** từ tên file (không có tên người dùng thật, số điện thoại, email trong tên file).
- Ảnh là bằng chứng QA cần thiết cho báo cáo và tài liệu dự án.
- **Khuyến nghị**: Giữ trong repo khi commit tài liệu QA. Tuy nhiên, vì tổng kích thước lớn (~85 MB), cần trưởng nhóm xác nhận chính sách trước khi push toàn bộ.
- Nếu có ảnh chứa dữ liệu người dùng thật (tên, ảnh đại diện demo), cần kiểm tra thủ công từng file trước khi publish.

---

## 2. `docs/evidence/` — Bằng chứng kiểm thử

**Cấu trúc**:
```
docs/evidence/
├── CAPTURE_NOTES.md
├── EVIDENCE_INDEX.md
├── FIDELITY_DELTAS.md
├── SECONDARY_SCREENS.md
├── pixel7_admin/
├── pixel7_before/
├── pixel7_fidelity/
├── pixel7_final/
├── pixel7_i18n_doctor/
├── pixel7_leaf/
├── pixel7_prompt3/
├── pixel7_secondary/
├── pixel7_vivid/
├── screenshots/
└── technical/
```

### Kết luận

- 4 file markdown (`CAPTURE_NOTES.md`, `EVIDENCE_INDEX.md`, `FIDELITY_DELTAS.md`, `SECONDARY_SCREENS.md`): **An toàn để commit** — tài liệu mô tả quy trình QA, không chứa credential.
- 11 thư mục `pixel7_*`: Chứa ảnh chụp màn hình từ thiết bị thật Pixel 7 — **cần kiểm tra thủ công** xem có ảnh hiển thị tên/email người dùng demo thật không trước khi publish.
- **Khuyến nghị**: Commit markdown files; trưởng nhóm xác nhận pixel7 folders trước khi push công khai.

---

## 3. `docs/sample_images/` — Ảnh mẫu cho dữ liệu demo

**Nội dung**:

| File | Kích thước | Nguồn |
|---|---|---|
| `aloe_vera.jpg` | 170 KB | Ảnh thực vật demo |
| `fiddle_leaf_fig.jpg` | 250 KB | Ảnh thực vật demo |
| `jade_plant.jpg` | 385 KB | Ảnh thực vật demo |
| `mint.jpg` | 324 KB | Ảnh thực vật demo |
| `orchid.jpg` | 177 KB | Ảnh thực vật demo |
| `peace_lily.jpg` | 408 KB | Ảnh thực vật demo |
| `snake_plant.jpg` | 262 KB | Ảnh thực vật demo |
| `spider_plant.jpg` | 250 KB | Ảnh thực vật demo |
| `zz_plant.jpg` | 371 KB | Ảnh thực vật demo |
| `ATTRIBUTION.md` | 4.4 KB | Ghi nguồn/license ảnh |

### Kết luận

- `ATTRIBUTION.md` có mặt — **tốt**: ghi rõ nguồn gốc và license.
- Ảnh phục vụ seed data demo, **không chứa thông tin người dùng thật**.
- **Khuyến nghị**: ✅ An toàn để commit toàn bộ. Đây là tài nguyên cần thiết cho ứng dụng.

---

## 4. `exports/` — Thư mục xuất file

**Trạng thái**: Thư mục `/exports/` đã được thêm vào `.gitignore` trong Issue #29 (`/exports/`).

**Kết luận**: ✅ Đúng — ZIP exports (27,79 MB đã đề cập trong issue) sẽ không được commit.

---

## 5. `.claude/launch.json` — Cấu hình Claude AI tool

**Nội dung phát hiện**:
- Chứa đường dẫn tuyệt đối đến thư mục `AppData\Local\Temp` trên máy cá nhân của developer.
- Đây là cấu hình **máy-cụ thể** do Claude IDE extension tạo ra tự động.

**Đánh giá**:
- Không chứa credential hay token.
- Tuy nhiên, tiết lộ cấu trúc thư mục máy local và username Windows.
- `AppData/Local/Temp` path lộ ra có thể giúp đoán tên user Windows.

**Khuyến nghị**: Thêm `.claude/` vào `.gitignore` (**đã thực hiện trong PR này**).

---

## 6. Cập nhật `.gitignore` (Issue #35)

Thêm quy tắc mới để loại trừ cấu hình Claude IDE tự động:

```
# Claude AI IDE local config (machine-specific, auto-generated)
.claude/
```

---

## 7. Tổng kết — Bảng phân loại tài nguyên

| Tài nguyên | Loại | Nhạy cảm? | Hành động |
|---|---|---|---|
| `qa/screenshots/*.png` (147 files) | Ảnh QA UI | ⚠️ Cần kiểm tra thủ công | Kiểm tra trước khi publish |
| `docs/evidence/*.md` (4 files) | Tài liệu QA | Không | ✅ Commit |
| `docs/evidence/pixel7_*/` | Ảnh thiết bị thật | ⚠️ Cần kiểm tra | Trưởng nhóm xác nhận |
| `docs/sample_images/*.jpg` (9 files) | Ảnh demo có Attribution | Không | ✅ Commit |
| `docs/sample_images/ATTRIBUTION.md` | License info | Không | ✅ Commit |
| `exports/` | ZIP builds, exports lớn | N/A | ✅ Git-ignored (Issue #29) |
| `.claude/launch.json` | Config local machine | Có (path local) | ✅ Thêm vào .gitignore |

---

## 8. Phạm vi và giới hạn

- Kiểm tra này là **phân tích tĩnh** tên file và nội dung file cấu hình.
- Không mở từng file ảnh trong `qa/screenshots/` và `docs/evidence/pixel7_*/` để kiểm tra nội dung hình ảnh (cần kiểm tra thủ công nếu lo ngại PII trong ảnh).
- Không kiểm tra lịch sử Git commit trước đó.
