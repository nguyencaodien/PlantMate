# Chạy ứng dụng Android PlantMate

> Trạng thái: mã nguồn Android đã có trên nhánh `main`, trong thư mục `android/`.

## Mở và chạy ứng dụng

1. Cài Android Studio và Android SDK theo phiên bản yêu cầu trong cấu hình dự án.
2. Clone repository và mở thư mục `android/` (nơi có `settings.gradle.kts`) trong Android Studio.
3. Đợi Gradle Sync hoàn tất.
4. Cấu hình Firebase như phần dưới.
5. Chọn thiết bị Android hoặc máy ảo, sau đó chạy cấu hình `app`.

## Cấu hình Firebase

Ứng dụng Android dùng package/application ID:

```
com.example.ungdungchiasecaycanh
```

Để dùng các tính năng Firebase, người chạy cần được cấp quyền phù hợp trong Firebase project của nhóm và đặt file `google-services.json` do Firebase Console cấp vào thư mục module Android `app/`. Không chia sẻ file này công khai nếu nhóm quyết định giữ cấu hình Firebase riêng tư; tuyệt đối không đưa service-account JSON, khóa riêng, mật khẩu hay keystore lên GitHub.

Tùy tính năng được bật, Firebase project cần cấu hình Authentication providers, Firestore rules/indexes, Storage, App Check và các dịch vụ AI tương ứng. Chỉ có file cấu hình trên máy không cấp quyền truy cập vào dữ liệu hay dịch vụ của nhóm.

Nếu chưa được cấp quyền Firebase hoặc chưa cấu hình dịch vụ, một số chức năng trực tuyến sẽ không hoạt động. README nên nêu rõ những chức năng nào cần quyền truy cập.

## Những thứ không commit

Không commit các file/mục máy cá nhân hoặc bí mật như:

- `local.properties`, `.idea/`, `.gradle/`, `build/`
- `keystore.properties`, `*.jks`, `*.keystore`
- Service account keys, mật khẩu hoặc token

Giữ Gradle Wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) và các file cấu hình build cần thiết trong repository để người khác có thể build.

## Kiểm tra trước khi công bố

Sau khi mã Android được đưa lên, hãy xác nhận bằng một bản clone sạch rằng Android Studio mở đúng thư mục gốc, Gradle Sync thành công, cấu hình Firebase được đặt đúng vị trí và ứng dụng chạy trên thiết bị/máy ảo. Không đánh dấu dự án là có thể build cho tới khi quy trình này được xác minh.
