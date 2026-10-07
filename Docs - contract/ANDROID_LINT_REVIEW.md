# Giải thích và xử lý cảnh báo Android lint — 04/10/2026

26 cảnh báo của lượt global wrapper là cảnh báo Android, không phải 26 lỗi backend. Sau lượt dọn này, báo cáo mới có **0 error, 2 warning**. Không dùng baseline, disable lint hay SuppressLint để che kết quả.

## Các nhóm cảnh báo

| Nhóm | Trước | Sau | Ý nghĩa và cách xử lý |
| --- | --- | --- | --- |
| SetTextI18n | 10 | 0 | Text trạng thái được viết trực tiếp trong setText, khó quản lý/ngôn ngữ hóa. Chuyển 10 text và trạng thái loading vào strings.xml; MainActivity đọc resource. |
| UseTomlInstead | 6 | 0 | Version dependency khai báo rải rác trong build.gradle.kts. Chuyển desugar, OkHttp, Firebase Messaging, ViewModel/LiveData và JSON-java vào libs.versions.toml. |
| UnusedResources | 3 | 0 | Layout activity_main mẫu và màu black/white không được tham chiếu. Đã kiểm tra và bỏ resource thừa; MainActivity vẫn dựng UI bằng Java như trước. |
| ApplySharedPref | 2 | 0 | commit() được dùng nhưng bỏ qua kết quả ghi. Giữ ghi đồng bộ cho clear/installation, kiểm tra boolean và log lỗi không có token. Đồng bộ creation của installation để caller đồng thời dùng cùng ID. Không đổi save token sang apply(); save vẫn kiểm tra commit và báo lỗi I/O. |
| AndroidGradlePluginVersion | 1 | 1 | Công cụ thông báo có AGP mới hơn. Hiện vẫn giữ AGP 9.3.2 với Gradle 9.5.0. |
| GradleDependency | 2 | 0 | ViewModel/LiveData có bản ổn định mới. Đã nâng cả hai lên Lifecycle 2.11.0, rồi chạy test/build/lint. |
| NewerVersionAvailable | 2 | 1 | JSON-java dùng cho unit test đã nâng lên 20260814; OkHttp runtime vẫn giữ 4.12.0. |
| **Tổng** | **26** | **2** | Hai warning còn lại là gợi ý nâng version, không làm build thất bại. |

## Quyết định giữ hai phiên bản

- **AGP 9.3.2 → 9.4.1:** Lint cũng gợi ý patch 9.3.3. Bản 9.4 yêu cầu Gradle tối thiểu 9.6.0; wrapper hiện là 9.5.0. Lượt này giữ toolchain đã kiểm chứng, chưa nâng AGP/Gradle. Tham chiếu [tài liệu tương thích AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes).
- **OkHttp 4.12.0 → 5.5.0:** Đây là đổi major version của transport HTTP/WebSocket. Giữ 4.12.0 đang chạy, tách việc nâng transport khỏi lượt dọn resource/cấu hình khi Android HTTP/reconnect/multipart chưa được nghiệm thu trên thiết bị. Không xem “có version mới” là bằng chứng bản đang dùng có lỗi bảo mật hoặc logic.

Lifecycle 2.11.0 đã được xác minh từ [release notes chính thức](https://developer.android.com/jetpack/androidx/releases/lifecycle); JSON-java 20260814 từ [release của tác giả](https://github.com/stleary/JSON-java/releases/tag/20260814). JSON-java này chỉ dùng cho JVM unit test, không thay thư viện org.json của Android runtime. Theo [SharedPreferences.Editor](https://developer.android.com/reference/android/content/SharedPreferences.Editor), commit ghi đồng bộ và trả kết quả; apply ghi bất đồng bộ và không báo lỗi ghi. Kiểm tra kết quả commit là lựa chọn triển khai của dự án cho dữ liệu phiên/thiết bị.

## Bằng chứng mới nhất

- `testDebugUnitTest assembleDebug lintDebug`: **BUILD SUCCESSFUL in 2m**, 51 task thực hiện; [log](../Homely-android/lint-cleanup-verify.log).
- **6 unit test, 0 failure, 0 error, 0 skipped**: ApiEnvelopeTest 5 và ExampleUnitTest 1. Parser object/token/array/page/null/error vẫn đạt với JSON-java mới.
- [Lint report](../Homely-android/app/build/reports/lint-results-debug.html): 0 error, 2 warning; hai warning ở libs.versions.toml cho AGP và OkHttp.
- [APK debug mới](../Homely-android/app/build/outputs/apk/debug/app-debug.apk) đã được tạo.
- Không chạy lại backend vì lượt này chỉ sửa Android. Mốc backend 205 test và HTTP workflow trước đó nằm tại [GLOBAL_RESPONSE_WRAPPER.md](GLOBAL_RESPONSE_WRAPPER.md).

Các text UI khác qua helper Java chưa được chuyển toàn bộ sang resource; không suy ra app đã hoàn chỉnh đa ngôn ngữ chỉ từ việc hết SetTextI18n. Chưa nghiệm thu E2E Android/FCM thật trong lượt dọn lint này.
