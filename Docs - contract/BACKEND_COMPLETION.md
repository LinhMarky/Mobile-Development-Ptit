# Hoàn thiện backend — 05/10/2026

Contract v1.6. Phạm vi: hoàn thiện backend hiện có, giữ HTTP global wrapper, MySQL outbox + FCM, thanh toán sandbox. Code mới chia theo trách nhiệm và có hướng dẫn mở rộng tại [README backend](../backend/README.md).

## Đã triển khai

| Phần | Hành vi |
| --- | --- |
| JWT | Bắt buộc Base64 secret ít nhất 64 byte, access TTL mặc định 15 phút. HTTP, service và STOMP đối chiếu ID tài khoản để token cũ không nhận quyền khi email được đăng ký lại. Hỗ trợ ID trong token v1.5 đến khi hết hạn. |
| Auth rate limit | Cửa sổ 60 giây, giới hạn theo IP kết nối và identity, bộ nhớ có giới hạn; 429 + Retry-After. Auth body trên 16 KiB trả 413. Không tin X-Forwarded-For thô. |
| Đăng ký và SMTP | Transaction gồm user + token + SMTP. SMTP lỗi rollback, trả 503 EMAIL_UNAVAILABLE; cùng email có thể đăng ký lại. Resend lỗi giữ token trước đó. |
| Xóa tài khoản | Điều kiện dùng chung khi nhận/xử lý; pending chặn thao tác thường. Job kiểm tra lại, khóa lifecycle riêng trước rooms/request, archive tin, thu hồi sessions/devices, xóa wishlist/OTP, ẩn danh profile, xóa roles. Lịch sử booking/payment/refund/case được giữ. FAILED có thể thử lại; GET /auth/delete-account trả trạng thái. |
| Retention | Batch tối đa cấu hình, mặc định 200 cho mỗi nhóm/lượt; xóa refresh/OTT/idempotency hết hạn và push SENT/FAILED/SKIPPED sau 30 ngày. Giữ processing và lịch sử nghiệp vụ. |
| FCM và metrics | Worker có các bước claim/send/finish rõ ràng; gửi ngoài transaction, metrics sau commit. Có Docker override mount credentials read-only. Actuator metrics chỉ ADMIN còn quyền trong DB. |
| Production config | Profile prod yêu cầu cấu hình riêng, MySQL xác minh TLS, SMTP STARTTLS bắt buộc, chặn demo seed và mock payment. Hibernate validate, Flyway quản lý schema. |
| CI và nghiệm thu | Workflow Maven verify + MySQL 8.4, script kiểm tra SMTP/MinIO/deletion, script tải tìm phòng, hướng dẫn chạy Docker/Firebase. |

Migration mới: V17 bổ sung index retention; V18 tạo và backfill account_lifecycle_locks. Không sửa migration đã áp dụng. Khi thêm nghiệp vụ mới chặn xóa, bổ sung điều kiện vào AccountDeletionEligibility và test tái kiểm tra/rollback.

## Kết quả kiểm chứng

| Kiểm tra | Kết quả và bằng chứng |
| --- | --- |
| Maven verify, H2 + MySQL 8.4 | **249 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**. Log `backend/target/backend-completion-mysql84-verify.log`; schema riêng `homely_completion_test`, không dùng database demo. JAR đóng gói thành công. |
| H2 + MySQL thử nghiệm trước lượt 8.4 | **249 test đạt**, log `backend/target/backend-completion-verify.log`. |
| Docker startup | **PASS**, backend health UP; MySQL 8.4, MinIO và Mailpit hoạt động. Flyway xác nhận 18 migrations, schema V18. Backend demo đang dùng `http://localhost:18082` trong project `homely-completion`. |
| HTTP nghiệp vụ | **PASS**: auth rotation/logout, catalog moderation/privacy, money DTO, booking replay/conflict, deposit guard, payment sandbox, handover, inbox. Log `backend/target/backend-completion-http.log`. |
| SMTP / storage / deletion | **PASS**: thư đăng ký tới Mailpit, token xác minh dùng được; upload/đọc PNG qua MinIO có bytes khớp; ảnh private trả 404 với guest; auth 429 có Retry-After; job hoàn tất deletion và token cũ bị từ chối. Log `backend/target/infrastructure-acceptance.log`, report `backend/target/infrastructure-results.json`. |
| Metrics | **PASS**: guest 401, tenant 403, admin đọc được homely.push.backlog. Report `backend/target/metrics-acceptance-results.json`. |
| Tải tìm phòng | **PASS**: 100 request, 4 worker, 0 lỗi, p95 **67 ms**, max 2518 ms. Report `backend/target/backend-load-results.json`. Dataset demo nhỏ, có request đầu khởi tạo truy vấn; không suy ra khả năng chịu tải thực tế. |
| OpenAPI | Xuất từ Docker backend: **1.6.0, 83 paths**, có GET trạng thái deletion, giữ x-stomp và webhook mock có điều kiện; toàn bộ local references được kiểm tra bởi Infra/ExportContract.java. File [openapi.yaml](openapi.yaml), bản JSON tại `backend/target/backend-completion-openapi.json`. |

Test bổ sung kiểm tra key, rate limit đồng thời, SMTP rollback/retry, pending deletion, ID khi email dùng lại (HTTP/service/STOMP), preservation giao dịch, refund FAILED, report chưa xử lý, rollback processor, retention và production guard. Bộ workflow/auth/retention chạy cả H2 và MySQL.

Android không được thay đổi trong lượt hoàn thiện backend này. Kết quả trước đó: 6 unit test, APK build, lint 0 error/2 warning theo [ANDROID_LINT_REVIEW.md](ANDROID_LINT_REVIEW.md); không suy ra đã nghiệm thu E2E thiết bị.

## Việc chủ dự án cần làm tiếp

1. Tạo Firebase project, thêm app đúng package, cung cấp google-services.json cho Android và service account riêng cho backend. Làm theo [FIREBASE_SETUP.md](FIREBASE_SETUP.md); kiểm tra push foreground/background/logout trên điện thoại.
2. Chạy E2E Android với các tài khoản tenant/host/admin: tìm phòng, lịch xem, chat, booking, cọc sandbox, bàn giao, review, report/case.
3. Chuẩn bị môi trường và dữ liệu demo có thể chạy lại theo [Infra/README.md](../Infra/README.md), trình bày rõ sandbox và kết quả test.
4. Khi triển khai thật: chọn storage được duy trì, cấu hình HTTPS/SMTP/database/Firebase riêng, bổ sung backup và thử restore. Không dùng Compose demo làm cấu hình production.

## Giới hạn cần hiểu đúng

- Chưa có Firebase project/credentials, nên chưa xác nhận giao FCM đến thiết bị. Unit/integration test gateway giả chứng minh logic outbox/retry, không chứng minh Firebase delivery.
- CI đã có workflow trong repository, chưa chạy trên GitHub vì chưa publish/push.
- Rate limit lưu trong một backend process. Nếu chạy nhiều instance, cần bộ đếm dùng chung hoặc giới hạn tại gateway; proxy phải cấu hình client IP tin cậy.
- SMTP đồng bộ có timeout; chưa có email outbox. Thư và MySQL không commit nguyên tử: database lỗi sau khi thư gửi có thể tạo thư không dùng được.
- Anonymize profile và giữ lịch sử giao dịch không phải xóa mọi nội dung người dùng từng nhập vào chat/report/booking. Không tự động purge dữ liệu nghiệp vụ còn cần đối chiếu.
- Logout thu hồi refresh/device; access JWT của tài khoản vẫn active còn hiệu lực đến TTL. Tài khoản pending/deleted/suspended bị kiểm tra từ DB.
- MinIO demo build từ source chính thức ghim phiên bản vì image cũ không tải được. Repository cộng đồng đã archive; xem [nguồn/license và giới hạn](../Infra/minio/README.md).
- Startup còn cảnh báo Flyway về mức hỗ trợ MySQL 8.4, open-in-view mặc định và Thymeleaf template location. Migration/test MySQL 8.4 đã đạt; việc nâng đồng bộ Spring Boot/Flyway và thu hẹp dependency/view configuration nên thực hiện riêng với regression test, không che warning rồi coi là đã nâng cấp.
- Load script với dataset demo chỉ là kiểm tra tải ban đầu, không phải bằng chứng chịu tải production. Thanh toán và hoàn tiền vẫn là sandbox.
