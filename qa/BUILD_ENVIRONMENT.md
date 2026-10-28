# BUILD_ENVIRONMENT — Xác minh môi trường build và bỏ đường dẫn máy cũ

**Issue**: #36  
**Người thực hiện**: lequantr1898  
**Ngày kiểm tra**: 2026-10-28  
**Phụ thuộc**: Issue #30 (đã hoàn thành)

---

## 1. Phiên bản công cụ build

| Công cụ | Phiên bản | Nguồn |
|---|---|---|
| Gradle Wrapper | **9.5.0** | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin (AGP) | **9.3.3** | `gradle/libs.versions.toml` (`agp`) |
| Gradle Daemon JVM (toolchain) | **JVM 25** | `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`) |
| Java source/target compatibility | **Java 11** | `app/build.gradle.kts` (`JavaVersion.VERSION_11`) |

---

## 2. Cấu hình Android SDK

| Tham số | Giá trị | Nguồn |
|---|---|---|
| `compileSdk` | **37** (Android 15) | `app/build.gradle.kts` |
| `minSdk` | **29** (Android 10) | `app/build.gradle.kts` |
| `targetSdk` | **37** | `app/build.gradle.kts` |
| `applicationId` | `com.example.ungdungchiasecaycanh` | `app/build.gradle.kts` |
| `versionCode` | 2 | `app/build.gradle.kts` |
| `versionName` | 1.1 | `app/build.gradle.kts` |

---

## 3. Dependency chính (từ `gradle/libs.versions.toml`)

| Thư viện | Phiên bản |
|---|---|
| Firebase BOM | 34.2.0 |
| Room | 2.7.2 |
| CameraX | 1.5.0 |
| Material | 1.14.0 |
| Credentials (Sign in with Google) | 1.5.0 |
| Facebook Login | 18.3.0 |
| AppCompat | 1.8.0 |
| ConstraintLayout | 2.2.2 |

---

## 4. Cấu trúc Gradle Daemon JVM

File `gradle/gradle-daemon-jvm.properties` dùng **Foojay Toolchain Resolver** để tải JVM 25 tự động cho từng nền tảng:

| Nền tảng | Kiến trúc | Nguồn toolchain |
|---|---|---|
| Windows | x86_64 | Foojay API (JVM 25) |
| Windows | aarch64 | Foojay API (JVM 25) |
| Linux / macOS / FreeBSD | x86_64 / aarch64 | Foojay API (JVM 25) |

> Daemon JVM 25 chỉ dùng để **chạy Gradle**, không phải để compile source. Source vẫn compile bằng Java 11 (`compileOptions` trong `app/build.gradle.kts`). Hai phiên bản này tách biệt nhau.

---

## 5. Kết quả kiểm tra resolve dependency

**Trạng thái**: Phân tích tĩnh — các dependency khai báo trong `libs.versions.toml` đều có trên Maven Central và Google Maven tại thời điểm kiểm tra.

- AGP 9.3.3 tương thích với Gradle 9.5.0 ✅
- Firebase BOM 34.2.0 tương thích với compileSdk 37 ✅
- Room 2.7.2 tương thích với Java 11 source ✅
- `annotationProcessor(libs.androidx.room.compiler)` được khai báo đúng cho Java (không phải `kapt`) ✅

> **Lưu ý**: Chưa chạy `:app:assembleDebug` thực tế vì `google-services.json` không có trong repo (git-ignored theo Issue #29) và môi trường CI chưa được cấu hình. Kết quả trên là phân tích tĩnh.

---

## 6. Thay đổi trong `AGENTS.md`

Dòng cũ (hardcode đường dẫn máy Admin cũ):
```
Scope all work to `C:\Users\Admin\AndroidStudioProjects\UngDungChiaSeCayCanh`.
```

Dòng mới (relocatable — không phụ thuộc máy cụ thể):
```
Scope all work to the project root — the directory that contains `app/`, `gradlew`,
and `settings.gradle.kts`. The project root may be located anywhere on the
contributor's machine; do not hard-code an absolute path.
```

**Lý do**: Dự án hiện đang được làm việc tại `d:\UngDungChiaSeCayCanh\UngDungChiaSeCayCanh\` thay vì đường dẫn Admin cũ. Quy tắc cũ sẽ gây nhầm lẫn cho thành viên mới hoặc khi clone sang máy khác.

---

## 7. Yêu cầu môi trường để build

Thành viên cần chuẩn bị:

| Yêu cầu | Chi tiết |
|---|---|
| Android Studio | Mới nhất (hỗ trợ AGP 9.3.3 trở lên) |
| JDK bên ngoài | Không bắt buộc — Foojay Resolver tải JVM 25 tự động |
| Android SDK | API 29+ đến API 37 |
| `google-services.json` | Lấy từ trưởng nhóm (git-ignored) |
| `keystore.properties` | Lấy từ trưởng nhóm (git-ignored, chỉ cần cho release build) |

---

## 8. Phạm vi và giới hạn

- Kiểm tra này là **phân tích tĩnh** — không thực thi Gradle build trên môi trường CI hoặc máy chuẩn.
- Kết quả `:app:assembleDebug` thực tế cần được xác nhận bởi thành viên có đủ `google-services.json` và Android SDK đã cài.
- Không nâng/hạ phiên bản dependency; giữ nguyên Java 11 và SDK hiện tại theo AGENTS.md.
