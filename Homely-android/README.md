# Homely Android

API contract v1.5 dùng HTTP envelope `{statusCode,message,data}`. ApiClient gỡ envelope một lần cho JSON và upload; màn hình nhận DTO/page như trước. STOMP giữ payload trực tiếp. Xem [global wrapper](../Docs%20-%20contract/GLOBAL_RESPONSE_WRAPPER.md).

Dependency được quản lý trong gradle/libs.versions.toml. Lượt test/build/lint mới nhất đạt 6 unit test, 0 lỗi lint và 2 gợi ý nâng AGP/OkHttp; giải thích tại [ANDROID_LINT_REVIEW.md](../Docs%20-%20contract/ANDROID_LINT_REVIEW.md).

Java + Android Views, minSdk 24, compile/target SDK 37. API mặc định: `http://10.0.2.2:8080/api/v1`. Build bằng Android Studio hoặc Gradle wrapper; wrapper dùng toolchain JVM khai báo trong `gradle/gradle-daemon-jvm.properties`.

```powershell
.\gradlew.bat --no-daemon --max-workers=2 :app:assembleDebug :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. Build cho điện thoại qua `adb reverse tcp:8080 tcp:8080`:

```powershell
.\gradlew.bat -PHOMELY_API_URL=http://127.0.0.1:8080/api/v1 :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug cho phép HTTP để kết nối backend local. Release cần `HOMELY_API_URL=https://.../api/v1`; manifest release không cho cleartext. Không dùng `localhost` trong emulator để gọi máy tính.

Màn hình hiện có: khám phá/tìm kiếm/phân trang, chi tiết/ảnh/phí phòng, đăng ký/đăng nhập/xác minh email, booking tenant/host, sandbox payment, bàn giao/case, lịch xem, chat STOMP + đồng bộ REST, inbox, quản lý tin/ảnh/lịch xem của chủ nhà và duyệt tin của admin. Khi lỗi mạng, màn hình hiển thị lỗi và cho thử lại; các POST nghiệp vụ giữ Idempotency-Key khi chưa nhận thành công.

Session được mã hóa bằng khóa trong Android Keystore; API tự refresh token và không gửi access token hết hạn kèm request refresh. Không có mật khẩu demo hardcode trong app. Xem tài khoản seed và bộ chạy tại [Infra](../Infra/README.md).

Chưa có `google-services.json` vẫn build/chạy được phần nghiệp vụ và inbox. Để nhận push thật, làm theo [hướng dẫn Firebase](../Docs%20-%20contract/FIREBASE_SETUP.md), build/cài lại app và cấp quyền thông báo.

Build/lint thành công chưa đồng nghĩa đã nghiệm thu toàn bộ màn hình trên thiết bị. Cần chạy kịch bản trong TEST_PLAN với backend đang hoạt động, bao gồm xoay màn hình, mất mạng, đổi tài khoản và mở từ notification.
