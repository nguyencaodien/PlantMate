# Kế hoạch GitHub Issues — PlantMate

Ngày lập: **26/10/2026** (Asia/Saigon). Trạng thái: **ĐÃ TẠO 30/30 GITHUB ISSUES VÀ THÊM ĐỦ VÀO PROJECT 12 — 0 thất bại**.

Phạm vi phân tích là bộ mã Android PlantMate trong thư mục làm việc, có `app/`, `gradlew` và `settings.gradle.kts`. Repository TreeShare hiện có prototype `frontend/` và sơ đồ; bộ mã Android được bàn giao riêng theo phạm vi issue. Đường dẫn tuyệt đối cũ trong AGENTS.md đã được ghi thành việc cần sửa.

## A. Tóm tắt project — 7 dòng

- PlantMate là ứng dụng cộng đồng Android để tặng/trao đổi cây, chăm sóc vườn riêng và thảo luận; bán cây là phụ, không có cart/payment/shipping trong phạm vi này.
- Mã ứng dụng dùng Java 11 và XML, Material/RecyclerView, ViewModel/LiveData, Room và Firebase; Gradle dùng Kotlin DSL, không đồng nghĩa ứng dụng viết bằng Kotlin.
- Có một module app, namespace com.example.ungdungchiasecaycanh, minSdk 29, compile/targetSdk 37; wrapper Gradle 9.5.0 và JVM daemon được cấu hình 25.
- Đã có năm tab Home/Garden/Doctor/Community/Profile, Auth, listing/nháp/preview, chat text/ảnh/voice/thẻ listing, cộng đồng, friends/stories, notifications, admin và safety.
- Garden/care/drafts/history dùng Room v10 theo UID; dữ liệu chung và ảnh/media dùng Firestore; Doctor đã có Firebase AI Logic, không có cấu hình Firebase Storage.
- Lượt rà soát ban đầu phát hiện README và nhiều tài liệu còn baseline Room v2/Doctor unavailable/ảnh chưa hỗ trợ. README được cập nhật ở #28 theo source hiện tại; các tài liệu còn lại và rủi ro lifecycle/callback/quyền dữ liệu giữ nguyên phạm vi issue đã giao.
- Tại lượt phân tích ban đầu, thư mục local không có .git; cache/build/artifact và cấu hình client cần rà soát. Đã xác minh TreeShare, nhánh main, quyền ADMIN và năm người nhận trên GitHub; bằng chứng build/runtime mới vẫn cần hoàn tất trước bàn giao.

## Thông tin nhóm, đích GitHub và nguyên tắc phân công

- Repository **nguyencaodien/TreeShare**: https://github.com/nguyencaodien/TreeShare.git đã được xác minh qua gh, nhánh chính **main**, tài khoản **nguyencaodien** có quyền **ADMIN**. Đã xác minh và gán issue thành công cho cả năm người: nguyencaodien, lequantr1898, trongphuccc, td22042006, pxshyn.
- Chưa có thế mạnh từng người nên **mapping theo vai trò vẫn tạm thời**: trưởng nhóm nguyencaodien; thành viên 2 lequantr1898; thành viên 3 trongphuccc; thành viên 4 td22042006; thành viên 5 pxshyn. Vai trò chuyên môn là đề xuất, chưa phải kết luận về năng lực; giữ ranh giới file khi điều chỉnh phân công.
- Cần email liên hệ và ngày hiệu lực chính sách cho issue #24; cần thiết bị/emulator, tài khoản thử và quyền Firebase phù hợp cho các ca runtime. Thiếu điều kiện thì ghi chưa xác minh, không bịa kết quả.
- Đã đọc tài liệu canonical và source liên quan, cấu hình build, manifest, rules/indexes, scripts, tài liệu và metadata tài nguyên; không đọc nội dung dependency/node_modules hoặc binary không liên quan. Không sửa docs/reference.
- Phát hiện code dưới đây là **kết luận phân tích tĩnh**, chưa phải lỗi đã tái hiện trên thiết bị. Kết quả QA/lint cũ trong project là lịch sử, không chứng nhận bản hiện tại. Lượt phân tích ban đầu **không chạy build/test, deploy hoặc thao tác dữ liệu remote**; việc tạo GitHub Issues sau đó không phải bằng chứng build/runtime đạt.
- Đã tạo **30/30 issue, 0 thất bại**, trên TreeShare bằng gh và đối chiếu lại tiêu đề, người nhận, label, hạn và phụ thuộc; số GitHub thật là **#28–#57**. Đã thêm đủ vào Project **PlantMate #12**: https://github.com/users/nguyencaodien/projects/12 và xác minh trạng thái Todo, Target date cùng Estimate theo kế hoạch. Lượt tạo issue chỉ chuẩn bị tài liệu, không sửa source hoặc khởi tạo Git; yêu cầu tiếp theo của nguyencaodien ngày 26/10 cho phép nộp các tài liệu thuộc #28 và #31 trên nhánh riêng.
- Yêu cầu mới nhất cho phép dùng **GitHub Project 12** để quản lý các issue, thay cho hạn chế GitHub Projects ban đầu. Tiếp tục không dùng Actions/CI/CD, Docker, tag hoặc GitHub Release. Không thêm kiến trúc/tính năng, không giao viết bộ test mới; tái sử dụng kiểm tra đã có khi cần.

## B. Bảng phân công

| STT kế hoạch | Tiêu đề Issue | Người phụ trách | Hạn | Giờ | GitHub |
|---|---|---|---|---:|---|
| 1 | Cập nhật giới thiệu và chức năng trong README | nguyencaodien | 26/10 | 1 | [#28](https://github.com/nguyencaodien/TreeShare/issues/28) |
| 2 | Bổ sung quy tắc bỏ qua cache và cấu hình máy cá nhân | lequantr1898 | 26/10 | 1 | [#29](https://github.com/nguyencaodien/TreeShare/issues/29) |
| 3 | Kiểm tra cấu hình Firebase, đăng nhập và thông tin nhạy cảm | lequantr1898 | 26/10 | 2 | [#30](https://github.com/nguyencaodien/TreeShare/issues/30) |
| 4 | Chuẩn bị Git repository độc lập đúng đích | nguyencaodien | 26/10 | 1 | [#31](https://github.com/nguyencaodien/TreeShare/issues/31) |
| 5 | Giữ khóa đăng bài khi màn chia sẻ listing bị tạo lại | trongphuccc | 26/10 | 2 | [#32](https://github.com/nguyencaodien/TreeShare/issues/32) |
| 6 | Giữ ảnh và giờ tưới đang sửa khi xoay form Garden | td22042006 | 26/10 | 2 | [#33](https://github.com/nguyencaodien/TreeShare/issues/33) |
| 7 | Đồng nhất kiểm tra avatar và quyền upload chat media | pxshyn | 26/10 | 2 | [#34](https://github.com/nguyencaodien/TreeShare/issues/34) |
| 8 | Rà soát tài nguyên và bằng chứng trước khi công khai | lequantr1898 | 27/10 | 2 | [#35](https://github.com/nguyencaodien/TreeShare/issues/35) |
| 9 | Xác minh môi trường build và bỏ đường dẫn máy cũ | lequantr1898 | 27/10 | 1 | [#36](https://github.com/nguyencaodien/TreeShare/issues/36) |
| 10 | Hoàn thiện hướng dẫn cài đặt và chạy từ bản clone | nguyencaodien | 27/10 | 2 | [#37](https://github.com/nguyencaodien/TreeShare/issues/37) |
| 11 | Giữ trạng thái gửi chat khi xoay màn hình | trongphuccc | 27/10 | 2 | [#38](https://github.com/nguyencaodien/TreeShare/issues/38) |
| 12 | Đưa lỗi form Community vào resource Việt/Anh | trongphuccc | 27/10 | 1 | [#39](https://github.com/nguyencaodien/TreeShare/issues/39) |
| 13 | Đưa callback Doctor về main thread nhất quán | td22042006 | 27/10 | 2 | [#40](https://github.com/nguyencaodien/TreeShare/issues/40) |
| 14 | Cho tải lại gợi ý cây sau request thất bại | td22042006 | 27/10 | 1 | [#41](https://github.com/nguyencaodien/TreeShare/issues/41) |
| 15 | Hợp nhất Data Model với schema và ID hiện tại | pxshyn | 27/10 | 2 | [#42](https://github.com/nguyencaodien/TreeShare/issues/42) |
| 16 | Chia batch khi admin xóa bài hoặc bình luận nhiều con | pxshyn | 27/10 | 2 | [#43](https://github.com/nguyencaodien/TreeShare/issues/43) |
| 17 | Hợp nhất phạm vi MVP và mô tả hệ thống đã bị mâu thuẫn | nguyencaodien | 28/10 | 2 | [#44](https://github.com/nguyencaodien/TreeShare/issues/44) |
| 18 | Xác minh luồng listing, nháp, preview và Back | trongphuccc | 28/10 | 2 | [#45](https://github.com/nguyencaodien/TreeShare/issues/45) |
| 19 | Đồng bộ tài liệu điều hướng và cấu trúc Android | trongphuccc | 28/10 | 2 | [#46](https://github.com/nguyencaodien/TreeShare/issues/46) |
| 20 | Bỏ callback Profile thuộc tài khoản cũ | td22042006 | 28/10 | 2 | [#47](https://github.com/nguyencaodien/TreeShare/issues/47) |
| 21 | Cập nhật tài liệu Doctor AI và ngôn ngữ theo source | td22042006 | 28/10 | 1 | [#48](https://github.com/nguyencaodien/TreeShare/issues/48) |
| 22 | Dọn dữ liệu con của bài viết khi xóa tài khoản | pxshyn | 28/10 | 2 | [#49](https://github.com/nguyencaodien/TreeShare/issues/49) |
| 23 | Gắn thao tác xóa tài khoản với UID ban đầu | pxshyn | 28/10 | 1 | [#50](https://github.com/nguyencaodien/TreeShare/issues/50) |
| 24 | Hoàn thiện thông tin liên hệ quyền riêng tư còn để mẫu | lequantr1898 | 29/10 | 1 | [#51](https://github.com/nguyencaodien/TreeShare/issues/51) |
| 25 | Hậu kiểm Home, Community và Inbox trên bản mới | trongphuccc | 29/10 | 2 | [#52](https://github.com/nguyencaodien/TreeShare/issues/52) |
| 26 | Xác minh Room, quyền sở hữu và nhắc tưới | td22042006 | 29/10 | 2 | [#53](https://github.com/nguyencaodien/TreeShare/issues/53) |
| 27 | Kiểm tra lại dung lượng ảnh sau nén dự phòng | pxshyn | 29/10 | 1 | [#54](https://github.com/nguyencaodien/TreeShare/issues/54) |
| 28 | Chạy lại bộ kiểm tra sẵn có và lint của bản bàn giao | lequantr1898 | 29/10 | 2 | [#55](https://github.com/nguyencaodien/TreeShare/issues/55) |
| 29 | Cập nhật kịch bản demo và tổng kết theo bản hiện tại | nguyencaodien | 29/10 | 2 | [#56](https://github.com/nguyencaodien/TreeShare/issues/56) |
| 30 | Kiểm tra clone về máy sạch vẫn chạy | nguyencaodien | 30/10 | 2 | [#57](https://github.com/nguyencaodien/TreeShare/issues/57) |

### Phạm vi theo thành viên

| Thành viên tạm | Chuyên môn đề xuất | Issue | Tổng giờ |
|---|---|---|---:|
| nguyencaodien — Trưởng nhóm | Git/GitHub, README, hợp nhất tài liệu, kiểm tra bàn giao | #1, #4, #10, #17, #29, #30 | 10 |
| lequantr1898 — Thành viên 2 | Môi trường build, file công bố, cấu hình, quyền riêng tư | #2, #3, #8, #9, #24, #28 | 9 |
| trongphuccc — Thành viên 3 | UI Community/Inbox, điều hướng và kiểm tra luồng chia sẻ | #5, #11, #12, #18, #19, #25 | 11 |
| td22042006 — Thành viên 4 | Garden/Doctor/Profile, dữ liệu cá nhân và lifecycle | #6, #13, #14, #20, #21, #26 | 10 |
| pxshyn — Thành viên 5 | Firestore rules, remote data, schema và xóa dữ liệu | #7, #15, #16, #22, #23, #27 | 10 |

## C. Nội dung chi tiết 30 issue

Đường dẫn dưới đây tính từ gốc project. Các đường dẫn hiện có đã được kiểm tra tồn tại; file chưa có luôn ghi **Tạo mới**. **Chỉ đọc** không cấp quyền sửa file đó. Số # trong tiêu đề chi tiết và bảng phụ thuộc là **số nội bộ kế hoạch**; cột GitHub ở bảng B chứa số/link thật tương ứng. Đã ánh xạ phụ thuộc trên các issue GitHub, với số thật bằng số kế hoạch + 27; không dùng số kế hoạch trong `Closes #...`.

Theo yêu cầu bổ sung, cả **30 issue GitHub #28–#57** đã có mục **File cần đưa lên GitHub trong commit/PR**: checklist đường dẫn đầy đủ cho file cần sửa/tạo mới, danh sách chỉ đọc riêng và cách ghi `Closes #...`. Chỉ nộp các file thực sự thay đổi thuộc nhiệm vụ; `.git/` chỉ tồn tại cục bộ, không upload. Issue GitHub [#31](https://github.com/nguyencaodien/TreeShare/issues/31) bổ sung `CONTRIBUTING.md` đã chuẩn bị để trưởng nhóm đưa kèm khi repository chưa có. Đã đọc lại 30/30 issue, không có ghi chú thiếu/trùng hoặc thay đổi ngoài phần ghi chú.

### Issue #1: Cập nhật giới thiệu và chức năng trong README

- **Mô tả:** README còn ghi Room v2, Doctor provider-unavailable và chat text-only. Đối chiếu mã hiện tại để giới thiệu đúng ứng dụng Java/XML, Room v10, Firebase AI Logic và các chức năng đang có.
- **File/thư mục liên quan:** Sửa `README.md`.
- **Người phụ trách:** nguyencaodien — Trưởng nhóm (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 26/10/2026.
- **Label:** docs.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Giới thiệu ưu tiên Give Away/Trade và đủ năm tab theo thứ tự quy định.
- [ ] Nêu đúng Doctor, ảnh Firestore, chat media, Garden và các giới hạn đã xác minh.
- [ ] Tách chức năng có trong source khỏi trạng thái cấu hình/dịch vụ chưa được kiểm chứng; không nhận là đã build hoặc deploy thành công.

### Issue #2: Bổ sung quy tắc bỏ qua cache và cấu hình máy cá nhân

- **Mô tả:** .gitignore đã có build, local.properties, keystore và exports; firestore-tests cũng đã bỏ qua node_modules/log. Bổ sung khoảng trống thực tế như .firebase và metadata máy cá nhân, không tạo lại file đã có.
- **File/thư mục liên quan:** Sửa `.gitignore`; Sửa `firestore-tests/.gitignore`; Sửa `.idea/.gitignore`.
- **Người phụ trách:** lequantr1898 — Thành viên 2 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 26/10/2026.
- **Label:** cleanup.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Cache/build/node_modules/log và cấu hình cá nhân không vào danh sách công bố, gồm .firebase và launcher trỏ máy cũ khi phù hợp.
- [ ] Giữ Gradle wrapper, source, resources, package-lock và các cấu hình mẫu cần chạy dự án.
- [ ] Xác minh quy tắc bằng danh sách file cục bộ; trưởng nhóm xác nhận lại ignore khi khởi tạo Git trong #4. Không xóa dữ liệu hoặc dùng dọn Git diện rộng.

### Issue #3: Kiểm tra cấu hình Firebase, đăng nhập và thông tin nhạy cảm

- **Mô tả:** Có google-services.json với một Firebase API key và facebook.xml có giá trị. Đây là cấu hình client cần phân loại, chưa có bằng chứng đó là private secret. Kiểm tra cấu hình ký, App Check, log và dữ liệu demo trước khi công khai.
- **File/thư mục liên quan:** Chỉ đọc `app/google-services.json`; Chỉ đọc `app/src/main/res/values/facebook.xml`; Chỉ đọc `keystore.properties.example`; Chỉ đọc `app/src/debug/`; Chỉ đọc `local.properties`; Chỉ đọc `.firebaserc`; **Tạo mới** `qa/CONFIG_AUDIT.md`.
- **Người phụ trách:** lequantr1898 — Thành viên 2 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 26/10/2026.
- **Label:** setup.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Xác nhận package/project khớp source, nêu cấu hình nào công khai, riêng tư hoặc cần chủ dự án xác nhận; không in giá trị key/token/password.
- [ ] Không công bố keystore, service-account key, mật khẩu demo hoặc App Check debug token nếu tìm thấy; báo rõ phạm vi đã quét và giới hạn.
- [ ] Ghi điều kiện đăng nhập/AI và quyền dự án cần cho thành viên; không tự thay project Firebase hay xóa cấu hình bắt buộc.

### Issue #4: Chuẩn bị Git repository độc lập đúng đích

- **Mô tả:** Bản hiện tại không có .git; mọi lệnh Git kiểm tra đều báo không phải repository. Chuẩn bị repository đúng thư mục này, xác định nhánh chính và đích GitHub theo thông tin nhóm cung cấp.
- **File/thư mục liên quan:** **Tạo mới** `.git/` (chỉ cục bộ, không commit/upload); Sửa `ISSUES_PLAN.md`; Sửa `CONTRIBUTING.md` (tài liệu đã chuẩn bị, đưa kèm khi repository chưa có).
- **Người phụ trách:** nguyencaodien — Trưởng nhóm (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 26/10/2026.
- **Label:** setup.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** #2, #3; owner/repo, năm username và quyền truy cập GitHub đã được xác minh.

**Acceptance Criteria:**

- [ ] Xác nhận repository đích và thành viên thật; không tự chọn repository thay thế.
- [ ] Git root trùng thư mục project hiện tại; không thao tác repository cha hoặc thư mục Admin cũ.
- [ ] Kiểm tra danh sách file dự kiến đưa lên sau #2/#3; commit/push do người được nhóm giao thực hiện, không chạy tự động trong bước lập kế hoạch.

### Issue #5: Giữ khóa đăng bài khi màn chia sẻ listing bị tạo lại

- **Mô tả:** CreatePostActivity gọi initializeShare mỗi onCreate; CreatePostViewModel.initializeShare luôn đặt submitting=false và callback có thể ghi đè trạng thái đang gửi. Chặn đường đi có thể cho phép đăng lặp khi xoay lúc request còn chạy.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/community/CreatePostActivity.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/community/CreatePostViewModel.java`.
- **Người phụ trách:** trongphuccc — Thành viên 3 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 26/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Khởi tạo lại cùng listingId không reset request/submitting đang thực hiện; callback tải listing không ghi đè kết quả publish.
- [ ] Xác minh request chậm rồi xoay vẫn khóa gửi và chỉ tạo một bài trên dữ liệu thử.
- [ ] Thành công thoát form đúng Back stack; lỗi giữ nội dung và cho thử lại; chạy kiểm tra sẵn có phù hợp.

### Issue #6: Giữ ảnh và giờ tưới đang sửa khi xoay form Garden

- **Mô tả:** BasePlantFormActivity giữ selectedImageUri trong field và chỉ lưu trạng thái xin quyền; renderPlant nạp lại giờ tưới từ DB mỗi Activity mới. Giữ dữ liệu form qua tái tạo theo ViewModel/saved state hiện có.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/garden/BasePlantFormActivity.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/garden/PlantFormViewModel.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/garden/EditPlantActivity.java`.
- **Người phụ trách:** td22042006 — Thành viên 4 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 26/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Add/Edit chọn hoặc bỏ ảnh, đổi giờ tưới rồi xoay vẫn giữ bản đang sửa.
- [ ] Lần mở đầu vẫn nạp đúng cây theo plantId/UID; dữ liệu gốc không ghi đè form đang chỉnh.
- [ ] Lưu ra Room đúng ảnh/lịch đã chọn và permission-denied vẫn dùng được; không đổi schema hoặc viết lại Garden.

### Issue #7: Đồng nhất kiểm tra avatar và quyền upload chat media

- **Mô tả:** users create chỉ kiểm avatarUrl là string, còn update dùng validAvatar; chatMedia create kiểm thành viên nhưng thiếu kiểm block hai chiều đang áp dụng cho Message. Siết hai khoảng trống trong rules hiện có.
- **File/thư mục liên quan:** Sửa `firestore.rules`; Chỉ đọc `firestore-tests/rules.test.mjs`; Chỉ đọc `firestore-tests/run.mjs`; **Tạo mới** `qa/FIRESTORE_ACCESS_CHECK.md`.
- **Người phụ trách:** pxshyn — Thành viên 5 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 26/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Tạo profile chấp nhận avatar rỗng/preset/ảnh thuộc chủ và từ chối định dạng/ảnh không thuộc chủ theo contract hiện hành.
- [ ] Upload chatMedia bị chặn khi hai người có block; đọc lịch sử hợp lệ vẫn giữ quyền như trước.
- [ ] Chạy bộ rules có sẵn và kiểm tra allow/deny cần thiết trên emulator demo-plantmate; ghi kết quả thật, không deploy vào Firebase đang dùng.

### Issue #8: Rà soát tài nguyên và bằng chứng trước khi công khai

- **Mô tả:** Project có nhiều ảnh QA/evidence, log kỹ thuật, cache và exports; ZIP giao diện 27,79 MB đã được ignore. Lập danh sách tài nguyên thật cần giữ và file chỉ dùng nội bộ, rà thông tin riêng tư trước khi công khai.
- **File/thư mục liên quan:** Chỉ đọc `qa/screenshots/`; Chỉ đọc `docs/evidence/`; Chỉ đọc `docs/sample_images/`; Chỉ đọc `exports/`; Chỉ đọc `.claude/launch.json`; Sửa `.gitignore`; **Tạo mới** `qa/PUBLICATION_FILES_CHECK.md`.
- **Người phụ trách:** lequantr1898 — Thành viên 2 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** cleanup.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** #2.

**Acceptance Criteria:**

- [ ] Báo cáo chỉ rõ ảnh/tài nguyên cần cho app hoặc tài liệu và artifact không cần công bố; không coi toàn bộ evidence là rác.
- [ ] Nếu phát hiện thông tin nhạy cảm, ghi đường dẫn và loại thông tin, không chép giá trị vào báo cáo; thống nhất cách loại khỏi bản công bố.
- [ ] Không xóa ảnh được source/tài liệu tham chiếu, không sửa docs/reference; ZIP exports tiếp tục được loại khỏi commit.

### Issue #9: Xác minh môi trường build và bỏ đường dẫn máy cũ

- **Mô tả:** AGENTS.md vẫn giới hạn đường dẫn Admin cũ trong khi bản đang làm ở Downloads; Gradle dùng daemon JVM 25 và Java source 11. Kiểm tra khả năng resolve/build mà không nâng phiên bản dependency.
- **File/thư mục liên quan:** Sửa `AGENTS.md`; Chỉ đọc `build.gradle.kts`; Chỉ đọc `settings.gradle.kts`; Chỉ đọc `app/build.gradle.kts`; Chỉ đọc `gradle/libs.versions.toml`; Chỉ đọc `gradle/gradle-daemon-jvm.properties`; Chỉ đọc `gradle/wrapper/gradle-wrapper.properties`; **Tạo mới** `qa/BUILD_ENVIRONMENT.md`.
- **Người phụ trách:** lequantr1898 — Thành viên 2 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** setup.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** #3.

**Acceptance Criteria:**

- [ ] Đổi chỉ dẫn đường dẫn cũ thành phạm vi thư mục gốc project có thể di chuyển; giữ các quy tắc bảo vệ repository cha.
- [ ] Ghi JVM/SDK cần thiết và kết quả thực của :app:assembleDebug trên môi trường chuẩn bị được.
- [ ] Giữ Java 11, SDK và dependency hiện tại; nếu resolve thất bại ghi đúng nguyên nhân, không nâng/hạ phiên bản hàng loạt.

### Issue #10: Hoàn thiện hướng dẫn cài đặt và chạy từ bản clone

- **Mô tả:** README có lệnh build nhưng thiếu điều kiện môi trường và cách chuẩn bị Firebase/App Check. Dùng kết quả kiểm tra cấu hình để bổ sung một đường dẫn cài đặt có thể làm lại.
- **File/thư mục liên quan:** Sửa `README.md`.
- **Người phụ trách:** nguyencaodien — Trưởng nhóm (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** docs.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** #1, #3, #9.

**Acceptance Criteria:**

- [ ] Ghi đúng minSdk 29, compile/target 37, Gradle wrapper 9.5.0, JVM daemon 25; phân biệt với mức ngôn ngữ Java 11.
- [ ] Hướng dẫn Android SDK/local.properties, Firebase đúng package, cấu hình công khai và App Check riêng cho thiết bị; không đưa token vào Git.
- [ ] Có lệnh Windows cho build/chạy, unit test và rules emulator hiện có; hướng dẫn dùng tài khoản thử và các trạng thái thiếu dịch vụ.

### Issue #11: Giữ trạng thái gửi chat khi xoay màn hình

- **Mô tả:** ConversationActivity luôn loadConversation khi onCreate; ViewModel tăng generation và xóa sending, khiến callback gửi cũ bị bỏ qua. Làm khởi tạo cùng conversationId an toàn khi Activity được tạo lại.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/inbox/ConversationActivity.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/inbox/ConversationViewModel.java`.
- **Người phụ trách:** trongphuccc — Thành viên 3 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Cùng conversationId không hủy trạng thái gửi đang chờ; đổi ID và Retry vẫn tải đúng cuộc trò chuyện.
- [ ] Xác minh gửi chậm rồi xoay không làm nút gửi bật lại sớm, không gửi trùng, chỉ xóa ô nhập sau thành công.
- [ ] Giữ subscription theo lifecycle và quyền thành viên; không thêm loại tin nhắn hoặc thay ID/schema.

### Issue #12: Đưa lỗi form Community vào resource Việt/Anh

- **Mô tả:** CreatePostViewModel còn trả literal tiếng Anh cho lỗi thiếu nội dung/category, quá 2000 ký tự và listing không khả dụng; Activity hiển thị nguyên chuỗi. Chuyển các thông báo này sang resource.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/community/CreatePostViewModel.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/community/CreatePostActivity.java`; **Tạo mới** `app/src/main/res/values/strings_community_errors.xml`; **Tạo mới** `app/src/main/res/values-vi/strings_community_errors.xml`.
- **Người phụ trách:** trongphuccc — Thành viên 3 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** #5.

**Acceptance Criteria:**

- [ ] Các lỗi nêu trên hiện đúng ngôn ngữ Việt/Anh và giữ nguyên giới hạn validation.
- [ ] Dùng ID/mã lỗi phù hợp tầng UI, không truy cập Firebase trực tiếp từ Activity.
- [ ] Kiểm tra hiển thị sáng/tối và sau xoay; resource mới không trùng tên hoặc sửa strings.xml chung.

### Issue #13: Đưa callback Doctor về main thread nhất quán

- **Mô tả:** DoctorViewModel gọi send từ QUALITY executor nhưng nhánh lỗi sớm DoctorRepository gọi callback trực tiếp; callback lại dùng LiveData.setValue. Loại đường đi có thể cập nhật UI sai luồng khi URI sai hoặc hết session.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/data/repository/DoctorRepository.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/doctor/DoctorViewModel.java`.
- **Người phụ trách:** td22042006 — Thành viên 4 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Callback thành công/lỗi tới UI luôn được chuyển đúng main thread, gồm lỗi validation và chưa đăng nhập.
- [ ] Ca session đổi trong lúc kiểm tra ảnh và URI lỗi hiển thị trạng thái lỗi, không crash do LiveData.
- [ ] Decode, AI và Room tiếp tục chạy nền; chạy kiểm tra Doctor sẵn có phù hợp, không thêm backend hoặc secret.

### Issue #14: Cho tải lại gợi ý cây sau request thất bại

- **Mô tả:** DoctorViewModel.loadGivers gán giversFor trước request và không xử lý failure, khiến những lần gọi lại cùng tên bị bỏ qua. Chỉ khóa kết quả thành công hoặc mở lại quyền retry khi lỗi.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/doctor/DoctorViewModel.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/doctor/DoctorResultActivity.java`.
- **Người phụ trách:** td22042006 — Thành viên 4 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** #13.

**Acceptance Criteria:**

- [ ] Tải khi offline rồi gọi lại cùng tên lúc có mạng có thể nhận danh sách.
- [ ] Thành công không tạo vòng tải lặp; callback cũ không ghi đè kết quả của tên cây mới.
- [ ] Dùng cơ chế tải/lifecycle phù hợp màn hiện có; không thêm hệ gợi ý hoặc query ngoài phạm vi.

### Issue #15: Hợp nhất Data Model với schema và ID hiện tại

- **Mô tả:** DATA_MODEL đồng thời ghi ảnh remote chưa có và ảnh Firestore/thumb, profile đúng sáu field và verification bổ sung, contract chat cũ và media mới. Đối chiếu model, Room v10 và rules, nêu rõ sai khác chưa có quyết định.
- **File/thư mục liên quan:** Sửa `docs/DATA_MODEL.md`.
- **Người phụ trách:** pxshyn — Thành viên 5 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** docs.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Bảng dữ liệu hiện hành nhất quán về Room/entities/migrations, nguồn ảnh và các field profile; phân biệt mốc lịch sử.
- [ ] Mô tả đúng ID conversation theo code hiện tại, direct/listing context và giới hạn bằng chứng review; không bịa exchange/accepted participant.
- [ ] Phần vượt hoặc mâu thuẫn contract cũ được ghi rõ để nhóm xác nhận trước triển khai; không tự đổi schema/source-of-truth.

### Issue #16: Chia batch khi admin xóa bài hoặc bình luận nhiều con

- **Mô tả:** FirebaseAdminDataSource.deletePost/deleteComment gom toàn bộ comments/likes/replies, parent và audit vào một WriteBatch. Chia thao tác có giới hạn như cơ chế đã có ở xóa tài khoản.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/data/remote/FirebaseAdminDataSource.java`.
- **Người phụ trách:** pxshyn — Thành viên 5 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 27/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Mỗi batch giữ trong giới hạn đang dùng của project, kể cả khi dữ liệu con vượt một batch.
- [ ] Chỉ xóa parent/ghi hoàn tất sau khi phần con thành công; lỗi giữa chừng được trả về rõ và có thể thử lại.
- [ ] Xác minh bằng dữ liệu fixture trên emulator, giữ quyền admin/audit và không mở rộng màn quản trị.

### Issue #17: Hợp nhất phạm vi MVP và mô tả hệ thống đã bị mâu thuẫn

- **Mô tả:** Overview và MVP_SCOPE đồng thời giữ baseline cũ và các mở rộng được ghi phê duyệt: Doctor AI, chat media, friends/stories, notifications/admin. Loại mâu thuẫn, giữ lịch sử quyết định và đánh dấu chỗ thiếu căn cứ phê duyệt.
- **File/thư mục liên quan:** Sửa `docs/PLANTMATE_SYSTEM_DESIGN.md`; Sửa `docs/MVP_SCOPE.md`.
- **Người phụ trách:** nguyencaodien — Trưởng nhóm (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 28/10/2026.
- **Label:** docs.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** #15.

**Acceptance Criteria:**

- [ ] Không còn đồng thời khẳng định Doctor chỉ unavailable và đã tích hợp AI, hoặc chat chỉ TEXT và có media.
- [ ] Mô tả nhất quán với DATA_MODEL và source về Room v10, ảnh Firestore và conversation; phần lệch chưa có căn cứ được nêu để nhóm chốt.
- [ ] Giữ năm tab, Garden riêng theo UID, ưu tiên chia sẻ/trao đổi; không tự phê duyệt tính năng hoặc thay kiến trúc.

### Issue #18: Xác minh luồng listing, nháp, preview và Back

- **Mô tả:** Luồng listing đã có nhưng báo cáo QA cũ không chứng nhận thao tác ghi/publish và toàn bộ khôi phục form. Kiểm tra Give Away/Trade/Sell, nháp, preview và ID handoff trên tài khoản thử.
- **File/thư mục liên quan:** Chỉ đọc `app/src/main/java/com/example/ungdungchiasecaycanh/ui/listing/`; **Tạo mới** `qa/VERIFICATION_LISTINGS.md`.
- **Người phụ trách:** trongphuccc — Thành viên 3 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 28/10/2026.
- **Label:** qa.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** #9.

**Acceptance Criteria:**

- [ ] Ghi kết quả cho lưu/mở nháp, preview, publish và mở chi tiết bằng listingId; xác minh ảnh/placeholder đúng hành vi hiện tại.
- [ ] Thành công publish không để form dùng lại trong Back stack; yêu cầu email SELL được kiểm tra đúng.
- [ ] Mỗi ca có phiên bản/môi trường và đạt/lỗi/chưa chạy; không dùng dữ liệu thật hoặc sửa source ngoài phạm vi đã giao.

### Issue #19: Đồng bộ tài liệu điều hướng và cấu trúc Android

- **Mô tả:** NAVIGATION và ANDROID_STRUCTURE có các đoạn planned/baseline cũ cạnh mô tả mới. Đối chiếu các lớp, ID và luồng đang có, nhất là conversation, ảnh, Doctor và Room.
- **File/thư mục liên quan:** Sửa `docs/NAVIGATION.md`; Sửa `docs/ANDROID_STRUCTURE.md`.
- **Người phụ trách:** trongphuccc — Thành viên 3 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 28/10/2026.
- **Label:** docs.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Giữ chính xác năm tab và secondary Activities; mô tả ID handoff, Up/Back, kết thúc form publish theo source đã kiểm tra.
- [ ] Các lớp đã triển khai không còn bị ghi là chỉ planned; đường dẫn package/resource đều tồn tại hoặc được đánh dấu kế hoạch.
- [ ] Nêu khác biệt chưa có căn cứ phê duyệt để trưởng nhóm chốt; không sửa source cho khớp tài liệu lỗi thời.

### Issue #20: Bỏ callback Profile thuộc tài khoản cũ

- **Mô tả:** ProfileViewModel.loadStats/loadProfile/refreshProfile có callback thiếu kiểm tra UID, trong khi callback friendRequests đã có guard. Giữ dữ liệu và tác vụ async gắn với phiên bắt đầu.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/profile/ProfileViewModel.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/ui/profile/ProfileFragment.java`.
- **Người phụ trách:** td22042006 — Thành viên 4 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 28/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Callback bắt đầu ở A không cập nhật Profile khi đã sign-out hoặc đăng nhập B.
- [ ] Sign-out xóa state hiển thị gồm thống kê/danh sách/reviewer; phiên mới tự nạp đúng hồ sơ.
- [ ] Kiểm tra request chậm và đổi UID bằng dữ liệu thử; giữ hành vi xóa lịch sử điều hướng khi sign-out.

### Issue #21: Cập nhật tài liệu Doctor AI và ngôn ngữ theo source

- **Mô tả:** DOCTOR_AI_AND_LANGUAGE đã ghi AI được duyệt nhưng còn mô tả camera/Home cũ và giới hạn parser chưa nhất quán với các cập nhật. Đồng bộ phần Doctor/ngôn ngữ, dẫn sang tài liệu giao diện cho Home.
- **File/thư mục liên quan:** Sửa `docs/DOCTOR_AI_AND_LANGUAGE.md`.
- **Người phụ trách:** td22042006 — Thành viên 4 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 28/10/2026.
- **Label:** docs.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** #13, #14.

**Acceptance Criteria:**

- [ ] Mô tả đúng camera, kiểm tra ảnh, analyzer/parser, lịch sử và phản hồi theo mã hiện tại; v3 được ghi là mốc migration, không là phiên bản DB hiện tại.
- [ ] Giữ hướng dẫn App Check không chứa token và các trạng thái lỗi/không cấu hình trung thực.
- [ ] Đoạn Home cũ được cập nhật hoặc thay bằng dẫn chiếu phù hợp; không cam kết model/quota chưa kiểm chứng trực tiếp.

### Issue #22: Dọn dữ liệu con của bài viết khi xóa tài khoản

- **Mô tả:** FirebaseAccountDataSource.findMemberData tìm posts của UID và comments do UID viết, chưa thu thập comments/likes của người khác trong chính posts sắp xóa. Tránh để subcollection mồ côi.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/data/remote/FirebaseAccountDataSource.java`.
- **Người phụ trách:** pxshyn — Thành viên 5 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 28/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Thu thập đủ comments/replies/likes dưới các bài của tài khoản bị xóa, khử trùng document references trước khi xóa.
- [ ] Xóa con trước parent, giữ batch giới hạn và profile cuối; chỉ bắt đầu sau khi các truy vấn cần thiết thành công.
- [ ] Xác minh trên fixture/emulator và kiểm tra quyền owner sẵn có; không xóa nội dung của người khác ngoài dữ liệu con thuộc bài đang xóa.

### Issue #23: Gắn thao tác xóa tài khoản với UID ban đầu

- **Mô tả:** AccountRepository chụp UID lúc đầu nhưng bước cuối gọi FirebaseAuthDataSource.deleteCurrentUser theo current user lúc đó. Ngăn trường hợp session đổi giữa chuỗi thao tác gây tác động sang tài khoản mới.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/data/repository/AccountRepository.java`; Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/data/remote/FirebaseAuthDataSource.java`.
- **Người phụ trách:** pxshyn — Thành viên 5 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 28/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Các bước xóa gắn với UID đã xác thực ban đầu; đổi session thì dừng an toàn hoặc tiếp tục đúng đối tượng đã chụp, không xóa B thay A.
- [ ] Local cleanup chỉ nhắm UID ban đầu; lỗi/re-authentication vẫn được báo về UI.
- [ ] Kiểm chứng session đổi giữa các bước bằng fixture hoặc kiểm tra sẵn có; giữ hành vi đăng nhập hiện tại và không dùng tài khoản thật để thử xóa.

### Issue #24: Hoàn thiện thông tin liên hệ quyền riêng tư còn để mẫu

- **Mô tả:** privacy.html còn [NGÀY CÓ HIỆU LỰC], [EMAIL LIÊN HỆ]; PLAY_STORE_LISTING cũng chưa có email. Chuẩn bị nội dung hoàn chỉnh cho các file đang có trong repository.
- **File/thư mục liên quan:** Sửa `hosting/public/privacy.html`; Sửa `docs/PLAY_STORE_LISTING.md`.
- **Người phụ trách:** lequantr1898 — Thành viên 2 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 29/10/2026.
- **Label:** docs.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue; Cần nhóm cung cấp email liên hệ và ngày chính sách có hiệu lực.

**Acceptance Criteria:**

- [ ] Chỉ điền email/ngày do nhóm xác nhận, không tự bịa người liên hệ hoặc ngày cam kết.
- [ ] Đối chiếu mô tả ảnh/AI và xóa tài khoản với hành vi đang có; nêu điểm chưa xác minh.
- [ ] Rà hiển thị sáng/tối và các liên kết nội bộ của trang; không deploy hosting hoặc mở quy trình phát hành Play.

### Issue #25: Hậu kiểm Home, Community và Inbox trên bản mới

- **Mô tả:** Dùng các ca trọng tâm từ qa/BUG_REPORT.md nhưng không coi bug lịch sử là lỗi đang mở. Kiểm tra tìm kiếm không dấu/rỗng, filter, loading/error/Retry, lifecycle gửi/đăng và hai theme.
- **File/thư mục liên quan:** Chỉ đọc `app/src/main/java/com/example/ungdungchiasecaycanh/ui/home/`; Chỉ đọc `app/src/main/java/com/example/ungdungchiasecaycanh/ui/community/`; Chỉ đọc `app/src/main/java/com/example/ungdungchiasecaycanh/ui/inbox/`; Chỉ đọc `qa/BUG_REPORT.md`; **Tạo mới** `qa/VERIFICATION_SHARED_SCREENS.md`.
- **Người phụ trách:** trongphuccc — Thành viên 3 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 29/10/2026.
- **Label:** qa.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** #5, #7, #11, #12.

**Acceptance Criteria:**

- [ ] Chạy ca gửi/đăng chậm rồi xoay để xác minh #5/#11, tìm kiếm/filter và lỗi tải/Retry trên dữ liệu thử.
- [ ] Kiểm tra màn bị sửa ở sáng/tối và font lớn 130%/200%; ghi ảnh cần thiết đã rà riêng tư.
- [ ] Phân biệt kết quả runtime mới, kết luận từ code và ca thiếu môi trường; không tự mở lại lỗi đã sửa chỉ từ báo cáo cũ.

### Issue #26: Xác minh Room, quyền sở hữu và nhắc tưới

- **Mô tả:** Room v10 và kiểm tra migration/cascade đã tồn tại. Chạy các ca sẵn có cùng kiểm tra tài khoản A/B, quyền thông báo và khôi phục lịch; ghi kết quả mới thay vì xây lại chức năng.
- **File/thư mục liên quan:** Chỉ đọc `app/src/test/java/com/example/ungdungchiasecaycanh/data/repository/GardenRepositoryTest.java`; Chỉ đọc `app/src/test/java/com/example/ungdungchiasecaycanh/data/repository/GardenCareContractTest.java`; Chỉ đọc `app/src/test/java/com/example/ungdungchiasecaycanh/data/repository/WateringPlanTest.java`; Chỉ đọc `app/src/test/java/com/example/ungdungchiasecaycanh/notification/CareReminderSchedulerTest.java`; Chỉ đọc `app/src/test/java/com/example/ungdungchiasecaycanh/data/preferences/SettingsRepositoryTest.java`; Chỉ đọc `app/src/androidTest/java/com/example/ungdungchiasecaycanh/data/local/db/PlantMateMigrationTest.java`; Chỉ đọc `app/src/androidTest/java/com/example/ungdungchiasecaycanh/data/local/db/PlantDeleteTest.java`; **Tạo mới** `qa/PERSONAL_DATA_CHECK.md`.
- **Người phụ trách:** td22042006 — Thành viên 4 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 29/10/2026.
- **Label:** qa.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** #6, #13, #14, #20.

**Acceptance Criteria:**

- [ ] Chạy kiểm tra phù hợp đã có và ghi lệnh/kết quả; migration/cascade và Doctor history SET NULL được xác minh khi có thiết bị.
- [ ] Garden/settings/reminder không lẫn dữ liệu A/B; từ chối thông báo vẫn CRUD cây và xem lịch.
- [ ] Ghi ca nhắc tưới chưa thể kiểm tra dài ngày thay vì ghi đạt; không gỡ app chứa dữ liệu thật để chạy kiểm tra.

### Issue #27: Kiểm tra lại dung lượng ảnh sau nén dự phòng

- **Mô tả:** FirebaseImageDataSource.encode kiểm MAX_BASE64_CHARS trong vòng nén nhưng sau fallback resize/nén một lần lại trả ngay. Bảo đảm ảnh trả về không vượt giới hạn đã định trước khi gọi Firestore.
- **File/thư mục liên quan:** Sửa `app/src/main/java/com/example/ungdungchiasecaycanh/data/remote/FirebaseImageDataSource.java`.
- **Người phụ trách:** pxshyn — Thành viên 5 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 29/10/2026.
- **Label:** bug.
- **Thời gian dự kiến:** 1 giờ.
- **Phụ thuộc:** Không có phụ thuộc issue.

**Acceptance Criteria:**

- [ ] Đầu ra ảnh chính luôn trong MAX_BASE64_CHARS hoặc trả lỗi rõ trước khi upload; kích thước ảnh không về 0.
- [ ] Thumbnail cũng được kiểm tra theo giới hạn rules hiện hành; không đổi schema/định dạng reference.
- [ ] Kiểm chứng ảnh lớn/nhiều chi tiết, ảnh nhỏ và URI lỗi; encode tiếp tục chạy nền và luồng báo lỗi hiện có vẫn hoạt động.

### Issue #28: Chạy lại bộ kiểm tra sẵn có và lint của bản bàn giao

- **Mô tả:** qa/BUG_REPORT.md và lint XML chứa kết quả lịch sử 15–16/10, không chứng nhận source hiện tại. Chạy lại JVM tests/lint hiện có sau các sửa, tập trung lỗi ảnh hưởng build, không yêu cầu bộ test mới.
- **File/thư mục liên quan:** Chỉ đọc `app/src/test/`; Chỉ đọc `qa/BUG_REPORT.md`; **Tạo mới** `qa/BUILD_VERIFICATION.md`.
- **Người phụ trách:** lequantr1898 — Thành viên 2 (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 29/10/2026.
- **Label:** qa.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** #5, #6, #9, #11, #12, #13, #14, #16, #20, #22, #23, #27.

**Acceptance Criteria:**

- [ ] Ghi lệnh :app:testDebugUnitTest và :app:lintDebug, thời điểm, kết quả và số test từ báo cáo thực; phân biệt UP-TO-DATE với đã chạy.
- [ ] Phân loại lỗi chặn bàn giao và warning có thể giữ; không sửa layout/source thuộc người khác hoặc dọn warning hàng loạt.
- [ ] Nếu môi trường chặn thì báo rõ, không ghi pass; lỗi được trả về đúng chủ file để xử lý trước #30.

### Issue #29: Cập nhật kịch bản demo và tổng kết theo bản hiện tại

- **Mô tả:** Kịch bản demo, checklist và tổng kết vẫn mô tả Doctor unavailable, Room v2 và ảnh chưa hỗ trợ. Cập nhật lời dẫn cùng giới hạn bằng chứng từ các báo cáo kiểm tra của đợt này.
- **File/thư mục liên quan:** Sửa `docs/FINAL_DEMO_SCRIPT.md`; Sửa `docs/DEMO_DAY_CHECKLIST.md`; Sửa `docs/FINAL_PROJECT_SUMMARY.md`.
- **Người phụ trách:** nguyencaodien — Trưởng nhóm (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 29/10/2026.
- **Label:** docs.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** #17, #19, #21, #28.

**Acceptance Criteria:**

- [ ] Các bước demo dùng chức năng đã có và dữ liệu mẫu được nhận diện rõ.
- [ ] Mô tả Doctor/ảnh/chat/Room khớp các tài liệu đã đồng bộ; có phương án khi dịch vụ không hoạt động.
- [ ] Mọi kết quả build/rules/runtime đều có nguồn, thời điểm và giới hạn; không tái sử dụng kết quả tháng 10 cũ như chứng nhận bản mới.

### Issue #30: Kiểm tra clone về máy sạch vẫn chạy

- **Mô tả:** Trưởng nhóm kiểm tra bản bàn giao từ repository đã thống nhất ở môi trường sạch, dùng README và cấu hình riêng của nhóm. Ngày 30/10 chỉ dành kiểm tra, rà soát và hoàn tất.
- **File/thư mục liên quan:** Chỉ đọc `README.md`; Sửa `ISSUES_PLAN.md`; **Tạo mới** `qa/CLEAN_CLONE_CHECK.md`.
- **Người phụ trách:** nguyencaodien — Trưởng nhóm (vai trò chuyên môn tạm).
- **Hạn hoàn thành:** 30/10/2026.
- **Label:** qa.
- **Thời gian dự kiến:** 2 giờ.
- **Phụ thuộc:** Các issue #1–#29 cần thiết cho bản bàn giao.

**Acceptance Criteria:**

- [ ] Clone thành công đúng repository, chuẩn bị dependencies/cấu hình theo README và build/cài/chạy app; kiểm tra đủ năm tab cùng luồng cốt lõi đã bàn giao.
- [ ] Không thiếu wrapper/resource/cấu hình cần thiết; không có credential riêng tư trong file công bố; phân biệt Firebase client config với private secret.
- [ ] Đối chiếu toàn bộ issue cần thiết đã xử lý, ghi phiên bản và kết quả; nếu lỗi báo cụ thể, giao lại đúng owner và đề xuất xử lý, không mở rộng phạm vi.

## D. Thống kê và lịch thực hiện

- **30 issue**, mỗi người **6 issue**, tất cả từ 1–2 giờ; tổng **50 giờ công**, ước tính sơ bộ khi chưa biết thế mạnh nhóm.
- Khối lượng: trưởng nhóm 10 giờ; thành viên 2: 9 giờ; thành viên 3: 11 giờ; thành viên 4: 10 giờ; thành viên 5: 10 giờ. Chênh lệch tối đa 2 giờ; nếu cần đổi người theo thế mạnh thực, cập nhật đồng thời người nhận trên GitHub và ranh giới file trong kế hoạch.
- Công việc chính hoàn tất trước hết **29/10/2026**. **30/10/2026** chỉ có issue cuối kiểm tra clone, rà soát và xác nhận bàn giao.

| Ngày | nguyencaodien | lequantr1898 | trongphuccc | td22042006 | pxshyn | Tổng giờ |
|---|---:|---:|---:|---:|---:|---:|
| 26/10/2026 | 2 | 3 | 2 | 2 | 2 | 11 |
| 27/10/2026 | 2 | 3 | 3 | 3 | 4 | 15 |
| 28/10/2026 | 2 | 0 | 4 | 3 | 3 | 12 |
| 29/10/2026 | 2 | 3 | 2 | 2 | 1 | 10 |
| 30/10/2026 | 2 | 0 | 0 | 0 | 0 | 2 |

### Phụ thuộc cần theo dõi

| Issue | Phụ thuộc |
|---|---|
| #4 | #2, #3 |
| #8 | #2 |
| #9 | #3 |
| #10 | #1, #3, #9 |
| #12 | #5 |
| #14 | #13 |
| #17 | #15 |
| #18 | #9 |
| #21 | #13, #14 |
| #25 | #5, #7, #11, #12 |
| #26 | #6, #13, #14, #20 |
| #28 | #5, #6, #9, #11, #12, #13, #14, #16, #20, #22, #23, #27 |
| #29 | #17, #19, #21, #28 |
| #30 | #1–#29 |

Các issue đã được đánh số theo thứ tự phụ thuộc: không có cạnh ngược hoặc vòng lặp. Phụ thuộc cùng ngày phải hoàn tất theo thứ tự: #2/#3 → #4 ngày 26; #9 → #10, #13 → #14 ngày 27; #27 → #28 → #29 ngày 29. Ưu tiên #27 đầu ngày 29; trưởng nhóm cập nhật báo cáo sau khi nhận kết quả #28. Hai người không chạy Gradle đồng thời trên cùng checkout; ở máy/branch riêng có thể kiểm tra độc lập.

## E. Kiểm tra xung đột và rủi ro

- Đã kiểm tra toàn bộ danh sách file **Sửa/Tạo mới**: **không có file nào giao cho hai người khác nhau**. Một người có thể sửa cùng file trong nhiều issue, thực hiện tuần tự trên phạm vi của mình.
- Trưởng nhóm sở hữu README, overview/MVP, demo/tổng kết, ISSUES_PLAN và metadata Git. Thành viên 2 sở hữu .gitignore, AGENTS, báo cáo cấu hình/build/file công bố, privacy.html và PLAY_STORE_LISTING.
- Thành viên 3 sở hữu CreatePost/Conversation UI+ViewModel, hai resource lỗi Community mới, tài liệu NAVIGATION/ANDROID_STRUCTURE và báo cáo màn chung. Thành viên 4 sở hữu form Garden, DoctorRepository/DoctorViewModel/result UI, Profile UI/ViewModel, tài liệu Doctor và báo cáo dữ liệu cá nhân.
- Thành viên 5 sở hữu DATA_MODEL, firestore.rules, FirebaseAdminDataSource/FirebaseAccountDataSource/AccountRepository/FirebaseAuthDataSource/FirebaseImageDataSource và báo cáo quyền Firestore. Hai sửa rules nằm cùng issue #7, cùng owner.
- Đọc source/test/tài liệu của người khác để kiểm chứng không phải quyền sửa. Các resource dùng chung như strings.xml, colors.xml, themes.xml chưa được giao sửa. Nếu cần file ngoài danh sách, trưởng nhóm phải cập nhật quyền sở hữu trước; không tự mở rộng phạm vi.
- Rủi ro còn lại là phụ thuộc contract giữa DATA_MODEL và overview/README, kiểm tra cùng thiết bị/tài khoản, hoặc lỗi ngoài phạm vi phát hiện khi kiểm thử. Xử lý bằng thứ tự ở trên, fixture riêng, trả lỗi cho đúng owner và ghi giới hạn. Bất đồng code–contract chưa có căn cứ phê duyệt phải được nêu rõ, không coi việc đồng bộ tài liệu là phê duyệt tính năng mới.
- Lượt audit local ban đầu không có Git metadata nên chưa kiểm tra được lịch sử hay file tracked của bản local. Sau đó đã xác minh repository TreeShare, nhánh main, quyền ADMIN và năm người nhận trên GitHub. Không có căn cứ kết luận private secret đã lộ; quét mẫu cục bộ không bảo đảm mọi file/lịch sử đều an toàn. Chưa xác nhận deployed rules hoặc build/runtime ở máy này.
- Nếu nguồn ảnh do nhóm sưu tầm chưa có bằng chứng quyền sử dụng, ghi vào #8 để nhóm quyết định bản công bố; không tự bịa license hay xóa hàng loạt ảnh đang được app/tài liệu dùng.

## F. Trạng thái triển khai theo yêu cầu mới nhất

Đã tạo thành công **30/30 issue, 0 thất bại**, bằng gh trên **nguyencaodien/TreeShare**, tương ứng **#28–#57**. Đã xác minh nguyencaodien có quyền ADMIN, nhánh chính main và cả năm người nhận. Đối chiếu sau tạo tìm đủ 30 issue, không có sai khác về tiêu đề, người nhận, label, hạn hoặc phụ thuộc.

Body issue bắt đầu bằng `Hạn: DD/MM/2026`; phụ thuộc trên GitHub dùng số/link thật. Bảng B ghi ánh xạ từng số kế hoạch sang issue đã tạo; các mục #1–#30 trong tài liệu này vẫn là số kế hoạch để giữ nguyên cấu trúc công việc.

**Project PlantMate #12 đã có đủ 30 issue mới**, tại https://github.com/users/nguyencaodien/projects/12 . Đối chiếu sau cập nhật: 37 mục tổng cộng, gồm 30 issue mới và 7 mục cũ; không có issue mới trùng hoặc thiếu. Nội dung, người nhận và trạng thái của 7 mục cũ được giữ nguyên.

Mỗi người nhận 6 issue; các mục mới ở **Todo**, **Target date** khớp hạn 26–30/10/2026 và **Estimate** ghi số giờ 1–2, tổng **50 giờ**. Issue kiểm tra clone cuối cùng là [#57](https://github.com/nguyencaodien/TreeShare/issues/57), nguyencaodien phụ trách, hạn 30/10/2026. Đã đọc lại dữ liệu Project và danh sách issue, không có sai khác. Đợt nộp ngày 26/10 chỉ gồm README.md, ISSUES_PLAN.md và CONTRIBUTING.md; mã Android chưa được upload.

CONTRIBUTING.md dưới 25 dòng và tối đa 8 lệnh Git, do nguyencaodien sở hữu; dùng số issue GitHub thực cho branch và `Closes #<issue-number>`. Các thành viên chỉ thực hiện công việc được giao; yêu cầu tạo issue không tự động cho phép thực thi toàn bộ 30 việc hoặc commit/push source.


## G. Phần nguyencaodien phụ trách ngày 26/10/2026

- [#28](https://github.com/nguyencaodien/TreeShare/issues/28): cập nhật README theo bộ mã Android đã rà soát, giữ danh sách thành viên và dẫn chiếu prototype hiện có. Không coi mô tả chức năng trong source là kết quả kiểm thử runtime.
- [#31](https://github.com/nguyencaodien/TreeShare/issues/31): nộp ISSUES_PLAN.md và CONTRIBUTING.md để nhóm có phân công, checklist file, quy trình branch/PR. Nhánh tài liệu: `issue-28-31-nguyencaodien-2026-10-26`.
- Phạm vi đợt này chỉ có ba file tài liệu, tách hai commit. Prototype, sơ đồ, source Android và file của các thành viên khác giữ nguyên trên GitHub.
- #31 chưa được coi là hoàn tất: #29/#30 còn mở, chưa có báo cáo kiểm tra cấu hình/ignore và thư mục Android cục bộ chưa có Git metadata. Các bước còn lại theo đúng owner/phụ thuộc; không đóng #31 chỉ vì đã nộp tài liệu.
- Chỉ kiểm tra nội dung, đường dẫn, diff và thông tin nhạy cảm của ba tài liệu; không chạy build/test/deploy trong đợt này. Hướng dẫn cài đặt đầy đủ của #37 và công việc ngày sau không được triển khai ở đây.

