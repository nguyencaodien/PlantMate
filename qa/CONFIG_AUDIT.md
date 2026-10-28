# CONFIG_AUDIT — Kiểm tra cấu hình Firebase, đăng nhập và thông tin nhạy cảm

**Issue**: #30  
**Người thực hiện**: lequantr1898  
**Ngày kiểm tra**: 2026-10-28  
**Phạm vi**: Phân tích tĩnh trên mã nguồn local. Không thực thi runtime, không kết nối Firebase Console.

---

## 1. Tổng quan dự án Firebase

| Trường | Giá trị |
|---|---|
| Project ID | `plantmate-be289` |
| Package name | `com.example.ungdungchiasecaycanh` |
| Nguồn | `app/google-services.json`, `.firebaserc` |

---

## 2. `app/google-services.json`

### Loại thông tin có trong file

| Trường | Loại | Mức độ nhạy cảm |
|---|---|---|
| `project_number` | Số định danh dự án Firebase | Công khai (có trong APK) |
| `project_id` | ID dự án Firebase | Công khai |
| `storage_bucket` | URL Storage | Công khai |
| `mobilesdk_app_id` | App ID của SDK | Công khai (có trong APK) |
| `package_name` | Package Android | Công khai |
| `client_id` (type 1) | OAuth client ID Android — gắn SHA1 certificate | Client-side, hạn chế bởi SHA1 |
| `client_id` (type 3) | OAuth client ID Web | Client-side |
| `certificate_hash` | SHA-1 của keystore debug/upload | Công khai (debug cert) |
| `current_key` (api_key) | Firebase Android Browser API Key | **Xem ghi chú bên dưới** |

### Kết luận về `api_key.current_key`

- Đây là **Android Restricted API Key** — được nhúng vào mọi APK phát hành bởi Google/Firebase theo thiết kế.
- Key này bị giới hạn bởi `package_name` + `certificate_hash` ở phía Google Console (không dùng được trên package/cert khác).
- **Không phải** service account private key hay App Check secret.
- **Không in giá trị key** trong báo cáo này theo yêu cầu issue.

### Khuyến nghị

- File hiện đã được thêm vào `.gitignore` (theo Issue #29). **Giữ nguyên** chính sách này.
- Nếu nhóm quyết định commit, cần xác nhận Firebase Console đã bật **Android App restriction** cho key này.
- Chưa tìm thấy bằng chứng key bị lộ qua kênh khác (log, hardcode trong Java, v.v.).

---

## 3. `app/src/main/res/values/facebook.xml`

| Trường | Loại | Mức độ nhạy cảm |
|---|---|---|
| `facebook_app_id` | Facebook App ID (số) | Client-side, thiết kế để ship trong APK |
| `facebook_client_token` | Client Token | Client-side (không phải App Secret) |
| `fb_login_protocol_scheme` | URL scheme đăng nhập Facebook | Công khai |

### Kết luận

- **App Secret** của Facebook **không có** trong file này (theo comment trong file, Secret chỉ lưu trong Firebase Console → Authentication).
- `facebook_client_token` là client-side token, Meta thiết kế để embed trong ứng dụng — an toàn để commit theo Meta docs.
- Không cần git-ignore file này nếu nhóm đồng ý.

---

## 4. `keystore.properties.example`

| Trường | Giá trị trong file |
|---|---|
| `storeFile` | Đường dẫn tương đối đến `.jks` |
| `storePassword` | **Trống** (placeholder) |
| `keyAlias` | `plantmate` |
| `keyPassword` | **Trống** (placeholder) |

### Kết luận

- File example **không chứa mật khẩu thật**. An toàn để commit.
- `keystore.properties` (bản thật) đã được git-ignored — **đúng**.
- File `.jks` thật chưa tìm thấy trong thư mục dự án → không bị commit nhầm.

---

## 5. `app/src/debug/`

Nội dung:
- `AndroidManifest.xml`: chỉ khai báo `SampleDataActivity` (debug-only, không export ra release build).
- `java/`: chứa code tạo dữ liệu mẫu cho debug.

### Kết luận

- Không tìm thấy token, secret hoặc credential cứng trong debug manifest.
- `SampleDataActivity` không ảnh hưởng bản release (debug sourceSet không merge vào release).

---

## 6. `local.properties`

- Chỉ chứa đường dẫn Android SDK trên máy local (`sdk.dir=...`).
- Đã được git-ignored (cả file gốc lẫn quy tắc trong `.gitignore`). **Đúng**.
- Không chứa credential.

---

## 7. `.firebaserc`

- Chứa `"default": "plantmate-be289"` — project ID công khai.
- Không chứa thông tin nhạy cảm. **An toàn** nếu nhóm muốn commit.

---

## 8. Điều kiện đăng nhập / phân quyền dự án

Để thành viên mới tham gia build và chạy dự án:

| Điều kiện | Nơi lấy |
|---|---|
| `google-services.json` | Trưởng nhóm cung cấp ngoài Git |
| `keystore.properties` (có mật khẩu thật) | Trưởng nhóm cung cấp riêng |
| File `.jks` keystore | Trưởng nhóm cung cấp riêng |
| Firebase Console access | Trưởng nhóm thêm email vào project |
| Facebook Developer access | Trưởng nhóm thêm tester |

---

## 9. Tổng kết — Bảng phân loại

| File | Loại | Hiện trạng Git | Khuyến nghị |
|---|---|---|---|
| `app/google-services.json` | Client config (có API key Android) | **Git-ignored** (Issue #29) | Giữ ignored; cung cấp ngoài Git |
| `app/src/main/res/values/facebook.xml` | Client-side Facebook IDs | Chưa ignored | Có thể commit (không có App Secret) |
| `keystore.properties.example` | Placeholder trống | Nên commit | ✅ Commit |
| `app/src/debug/AndroidManifest.xml` | Debug manifest | Nên commit | ✅ Commit |
| `local.properties` | SDK path máy local | **Git-ignored** | ✅ Đúng |
| `.firebaserc` | Firebase project ID | Chưa ignored | Có thể commit (thông tin công khai) |

---

## 10. Phạm vi và giới hạn

- Kiểm tra này là **phân tích tĩnh** trên mã nguồn local tại thời điểm 2026-10-28.
- Không truy cập Firebase Console, không chạy emulator, không kiểm tra lịch sử Git từ các commit trước.
- Cần trưởng nhóm xác nhận Firebase Console đã bật Android App restriction cho API key.
- Cần trưởng nhóm xác nhận không có service account key nào được lưu ngoài phạm vi đã quét.
