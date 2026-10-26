# Member List

| Name | MSSV |
|------|------|
| Nguyễn Cao Điền | 31241023473 |
| Hồ Trọng Phúc | 31241023650 |
| Trần Lê Quân | 31241021135 |
| Lê Quân | 31241021233 |
| Phạm Tuấn Đạt | 31241025206 |

# PlantMate

PlantMate là ứng dụng cộng đồng giúp người yêu cây **cho tặng và trao đổi cây tại địa phương**, chăm sóc vườn riêng và thảo luận với nhau. Bán cây là loại tin phụ; phạm vi hiện tại không có giỏ hàng, thanh toán hoặc vận chuyển.

## Tình trạng repository — 26/10/2026

Repository hiện lưu prototype giao diện trong [frontend/](frontend/) và các sơ đồ. Phần Android bên dưới mô tả bộ mã Java/XML đã được rà soát trong thư mục làm việc của nhóm. Mã `app/`, Gradle wrapper và `docs/` của Android chưa được đưa lên trong thay đổi tài liệu này; clone repository hiện tại chưa đủ để build ứng dụng Android.

Việc bàn giao mã Android thực hiện theo phạm vi file trên các issue trong [Project PlantMate](https://github.com/users/nguyencaodien/projects/12). Prototype và các sơ đồ hiện có được giữ nguyên.

## Điều hướng Android

Năm tab chính theo thứ tự cố định: **Home → Garden → Doctor → Community → Profile**. Inbox, Settings, các màn chi tiết/tạo nội dung và trợ lý Mầm là các màn phụ theo ngữ cảnh.

## Chức năng có trong bộ mã Android đã rà soát

- **Tài khoản:** Firebase Authentication, đăng ký/đăng nhập, khôi phục mật khẩu, hồ sơ và đăng xuất; đã có tích hợp Google/Facebook, việc sử dụng phụ thuộc cấu hình dịch vụ.
- **Chia sẻ cây:** tin Give Away, Trade và Sell; tìm kiếm/lọc, lưu tin, nháp, xem trước, đăng tin và quản lý trạng thái. Các màn dùng chung một `listingId`.
- **Garden:** cây cá nhân, ảnh cây, nhật ký chăm sóc, lịch tưới và nhắc việc trên thiết bị; dữ liệu riêng theo tài khoản.
- **Doctor:** nhận dạng cây và gợi ý chẩn đoán từ ảnh qua Firebase AI Logic/Gemini, kiểm tra chất lượng ảnh, lịch sử cục bộ và phản hồi. Phản hồi ảnh chỉ được gửi khi người dùng đồng ý.
- **Community:** bài viết, thích, bình luận/trả lời, chia sẻ tin cây, kết bạn và khoảnh khắc xanh; có thông báo trong ứng dụng.
- **Inbox:** hội thoại một-một giữa các thành viên, hỗ trợ văn bản, ảnh, ghi âm ngắn và thẻ tin cây; có thể mở từ ngữ cảnh listing.
- **Profile, Settings và an toàn:** chỉnh hồ sơ, lựa chọn Việt/Anh, tùy chọn cục bộ, báo cáo/chặn thành viên, đánh giá theo điều kiện tương tác và các màn quản trị đã có source.
- **Mầm:** hướng dẫn chăm sóc bằng nội dung cục bộ; luồng này tách với phân tích ảnh AI của Doctor.

Danh sách trên xác nhận sự hiện diện trong source, không thay thế kết quả kiểm thử hoặc chứng nhận dịch vụ Firebase đang hoạt động.

## Kiến trúc và công nghệ

```text
Activity / Fragment → ViewModel → Repository → DataSource / DAO → Firebase / Room
```

Ứng dụng Android dùng Java 11, XML, Material Components, RecyclerView, LiveData/ViewModel, Firebase Authentication, Cloud Firestore, Firebase AI Logic, Room, SharedPreferences, AlarmManager và Android Notifications. Cấu hình hiện tại: namespace/applicationId `com.example.ungdungchiasecaycanh`, minSdk 29, compileSdk/targetSdk 37.

## Nguồn dữ liệu

| Dữ liệu | Nơi lưu |
|---|---|
| Phiên đăng nhập | Firebase Authentication |
| Hồ sơ, listing, bài viết, hội thoại, đánh giá và dữ liệu cộng đồng/an toàn | Cloud Firestore |
| Ảnh chia sẻ nén và thumbnail; ảnh/voice của chat | Firestore qua các lớp dữ liệu ảnh/media; cấu hình hiện tại không dùng Firebase Storage |
| Cây, care log, lịch sử Doctor, ảnh cây, nháp listing và dấu đã xem story | Room, **schema phiên bản 10**, truy cập theo tài khoản/chủ cây |
| Tùy chọn và dấu trạng thái cá nhân | SharedPreferences theo UID; ngôn ngữ ứng dụng dùng cơ chế locale của AppCompat |

Ảnh chia sẻ dùng reference như `pmimg://` để trỏ tới dữ liệu ảnh trong Firestore. URI ảnh cục bộ không được coi là ảnh đã công bố. Garden, nháp và lịch sử Doctor vẫn là dữ liệu trên thiết bị; chưa có đồng bộ Garden nhiều thiết bị.

## Chạy và kiểm tra

Prototype hiện có nằm trong [frontend/](frontend/). Với bộ mã Android đã được bàn giao đủ `app/` và Gradle wrapper, chạy từ gốc dự án trên Windows:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Các lệnh này chưa áp dụng cho bản clone chỉ có prototype. Hướng dẫn chuẩn bị môi trường Android/Firebase đầy đủ thuộc [issue #37](https://github.com/nguyencaodien/TreeShare/issues/37). Không đưa mật khẩu, private key, keystore hoặc App Check debug token vào Git.

## Giới hạn và tình trạng xác minh

- Doctor cung cấp gợi ý AI, phụ thuộc cấu hình AI Logic/App Check, kết nối và quota; kết quả có thể sai.
- Media hiện dành cho ảnh nén và ghi âm ngắn; không có video, group chat, cart, payment hoặc shipping.
- Commit tài liệu này không chạy build/test/runtime. Trạng thái triển khai rules, nâng cấp dữ liệu trên thiết bị và nhắc tưới dài ngày cần được kiểm chứng riêng.
- Một số tài liệu Android còn mô tả baseline cũ; việc hợp nhất theo source và quyết định đã duyệt nằm trong các issue tài liệu. Việc cập nhật README không tự phê duyệt tính năng mới.

## Làm việc nhóm và tài liệu

- [Kế hoạch 30 issue và phạm vi file](ISSUES_PLAN.md)
- [Quy trình branch, commit và Pull Request](CONTRIBUTING.md)

Các tài liệu sau đã có trong bộ mã Android cục bộ, chưa được đưa lên repository trong thay đổi này: `docs/FINAL_DEMO_SCRIPT.md`, `docs/DEMO_DATA_PLAN.md`, `docs/SCREENSHOT_CHECKLIST.md`, `docs/COURSE_CONCEPT_MAPPING.md` và `docs/FINAL_PROJECT_SUMMARY.md`.
