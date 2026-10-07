# TEST PLAN & ACCEPTANCE CRITERIA

**Vai trò:** Tài liệu hướng dẫn kịch bản kiểm thử (Test Scenarios), Acceptance Criteria và các bài test đồng thời (Concurrency) để QA và Developer nghiệm thu chất lượng hệ thống.  
**Tham chiếu Business Rules:** Tuân thủ tuyệt đối `PROJECT_CONTRACT.md`.

## Hoàn thiện backend v1.6 ngày 05/10/2026

Kết quả mới: **249 test đạt**, MySQL 8.4 + H2, JAR thành công; HTTP nghiệp vụ, SMTP/MinIO/deletion và metrics Docker PASS. Tải demo 100 request/4 worker, 0 lỗi, p95 67 ms. Chi tiết và giới hạn tại [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md). Các trường hợp bổ sung:

- Secret thiếu/sai/ngắn bị từ chối; JWT phải khớp ID kể cả khi email được dùng lại. Kiểm tra HTTP, transaction service và STOMP.
- Rate limit IP + identity, body replay, bỏ qua X-Forwarded-For thô, request đồng thời; HTTP 429 + Retry-After và 413.
- SMTP lỗi không giữ user đăng ký dang dở; đăng ký lại cùng email được. Resend lỗi giữ token trước đó.
- Deletion pending chặn profile/refresh/mutation nhưng cho status/logout; kiểm tra lại blocker; lỗi rollback; retry FAILED; giữ booking/payment; FAILED refund và report chưa xử lý vẫn chặn.
- Retention chỉ xóa bản ghi hết hạn/terminal, giữ processing và giao dịch. Cả H2 và MySQL riêng.
- Profile prod từ chối demo/mock. Docker kiểm tra SMTP/MinIO/ảnh private/rate limit/deletion job; tải tìm phòng đo lỗi và p95.

## Lịch sử kiểm chứng global wrapper v1.5 ngày 04/10/2026

Mốc v1.5: **205 test backend, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**. Android 6 unit test đạt, build APK + lint thành công. Các số liệu cũ bên dưới là lịch sử. Chi tiết tại [GLOBAL_RESPONSE_WRAPPER.md](GLOBAL_RESPONSE_WRAPPER.md).

Android sau dọn cảnh báo có **0 error, 2 warning**, chỉ còn gợi ý cập nhật AGP/OkHttp. Chi tiết và log riêng tại [ANDROID_LINT_REVIEW.md](ANDROID_LINT_REVIEW.md).

| Bộ kiểm tra | Phạm vi P0 |
| --- | --- |
| FormatRestResponseTest — 4 test | String converter, status/Location, không double wrap, null200/204/304, byte array binary. |
| ResponseContractTest — 11 test | Object/array/page trong envelope; 204 rỗng; media URL và bytes với custom/v2; upload; hidden/private media; lỗi bọc từ MVC/security/account/idempotency; 405/406/415 và validation. |
| IdempotencyFilterTest — 15 test | Snapshot mới replay giữ nguyên bytes; snapshot legacy DTO/page/rỗng được bọc và không chạy lại mutation; conflict/scope/authorization vẫn đúng. |
| ApiEnvelopeTest — 5 test Android | Token/object, array/page, null/204, lỗi/fallback và envelope không hợp lệ. |
| MediaUrlTest — 5 trường hợp | Prefix mặc định, tùy chỉnh và slash khi sinh đường dẫn. |
| OpenApiContractTest — 1 test | Schema ProblemDTO + FieldErrorDTO, prefix tùy chỉnh, public/auth và Idempotency-Key. |
| AuthIntegrationTest + AuthMysqlIntegrationTest | Servlet ERROR dispatch giữ 404/500/503 và ProblemDTO, không lộ attributes nhạy cảm; request thường tới /error vẫn cần xác thực. |
| Các suite lỗi/chat/idempotency hiện có | Giữ status/code, header và quyền truy cập sau khi dùng ApiProblems chung. |
| HTTP trên JAR + OpenAPI | Luồng demo PASS; 83 paths, mọi local reference hợp lệ và media schema binary. |

---

## Phạm vi kiểm chứng thực tế ngày 29/09/2026

Các kịch bản bên dưới là mục tiêu nghiệm thu, không phải danh sách đã chạy thành công. Các mâu thuẫn C01/C03 và ma trận lỗi chung vẫn được ghi tại `CONTRACT_GAPS.md`.

Lượt backend `verify` gần nhất đạt **74 test, 0 thất bại, 0 lỗi, 0 bỏ qua**. Phần Admin/Notification bổ sung 19 test:

| Bộ test | Số test | Phạm vi |
| --- | --- | --- |
| `AdminWorkflowTest` | 10 | Trạng thái listing/case/report/review/user; audit và inbox cùng transaction; rollback; duyệt đồng thời; quyền sở hữu thông báo; thu hồi refresh session |
| `AdminNotificationHttpTest` | 6 | JWT/quyền admin hiện tại, tài khoản bị khóa, kiểm tra quyền trước replay, validation/query, idempotency, DTO trực tiếp và alias read-all |
| `AdminAccountAuthTest` | 3 | JWT chứa role được cấp; cờ khóa legacy chặn password login và refresh |

55 test còn lại bao phủ chat, lỗi chung và HTTP idempotency, gồm test Flyway khi thiết lập `HOMELY_TEST_MYSQL_URL` cho schema test riêng. Concurrency service dùng H2; MySQL 8.0.45 kiểm chứng migration V1–V13 và khởi động JAR. Chưa nghiệm thu MySQL 8.4, Kafka/FCM, refund từ quyết định case hoặc E2E Android. Chi tiết và lệnh chạy tại [codex24th9.md](codex24th9.md), mục 9, và [hướng dẫn Admin/Notification](../backend/ADMIN_NOTIFICATIONS.md).

---

## 1. Unit & Integration Test (Backend)

Phải bao phủ các bài toán khó nhất, đặc biệt là concurrency (BR-07).

### Concurrency & Race Condition Tests
1. **C01 - Double Booking:** 2 Tenant cùng bấm tạo Booking (BOOK01) cho 1 phòng tại cùng 1 mili-giây.
   - *Expected:* Chỉ 1 request được 201 Created. Request kia nhận 409 `ROOM_NOT_AVAILABLE`.
2. **C02 - Double Approval:** Host bấm duyệt (BOOK04) 2 Booking PENDING của 2 người khác nhau cho cùng 1 phòng.
   - *Expected:* 1 request thành công, trạng thái Booking thứ nhất thành APPROVED, Room thành HELD. Request thứ 2 gặp `OptimisticLockException` trên Room, trả về 409.
3. **C03 - Double Payment:** Hệ thống nhận được 2 Webhook thanh toán thành công (khác event_id nhưng cùng payment_id).
   - *Expected:* Webhook đầu xử lý thành công, Payment thành SUCCEEDED. Webhook sau bị khóa (Pessimistic) hoặc check trạng thái đã SUCCEEDED thì ghi nhận duplicated, không allocate lại booking, đưa vào hàng đợi Refund.
4. **C04 - Payment vs Expiration:** Job hết hạn (Expire Hold) kích hoạt đúng lúc Webhook payment vừa tới.
   - *Expected:* Dùng Version field của Booking. Nếu job đổi Booking thành EXPIRED trước, Payment webhook đánh dấu thanh toán là SUCCEEDED muộn (Late Settlement), không cứu Booking, kích hoạt luồng hoàn tiền.

---

## 2. API Test Matrix (Toàn bộ Endpoints)

QA dùng Postman/RestAssured kiểm tra các lỗi tiêu chuẩn cho *mọi* API:
- `401 Unauthorized` nếu gửi request không có token hoặc token hết hạn.
- `403 Forbidden` nếu thao tác trái quyền Role (VD: Tenant cố tạo Room).
- `403 Forbidden` nếu gọi API cần xác minh email mà user chưa xác minh.
- `400 Bad Request` nếu body thiếu field bắt buộc (title, rent_vnd...). Envelope phải có statusCode đúng HTTP status; data chứa ProblemDTO với code và field_errors.
- `422 Unprocessable Entity` nếu format dữ liệu sai (ngày tháng không hợp lệ).
- `404 Not Found` nếu truyền ID (room_id, booking_id) không tồn tại hoặc của người khác (Ownership rule).
- `409 Conflict` nếu thiếu/trùng `Idempotency-Key` (đối với POST).

---

## 3. End-to-End (E2E) Scenarios

Đây là các luồng User Journey đầy đủ mô phỏng thực tế.

### E2E-01: Happy Path Đặt phòng & Bàn giao
1. **Host** tạo Room, submit Listing.
2. **Admin** duyệt Listing sang PUBLISHED.
3. **Tenant 1** tìm kiếm, thấy Listing, gửi yêu cầu BOOKING (Move_in ngày X).
4. **Host** nhận thông báo, mở app, Bấm APPROVE.
5. **Tenant 1** thấy Booking APPROVED, chọn thanh toán cọc Sandbox.
6. Hệ thống mô phỏng Webhook thành công → Booking chuyển sang CONFIRMED.
7. Đến ngày X, **Host** và **Tenant 1** cùng bấm "Đã nhận phòng" / "Đã bàn giao".
8. Booking chuyển COMPLETED, Room chuyển RENTED.
9. **Tenant 1** viết Review 5 sao. Review hiển thị công khai trên Listing.

### E2E-02: Timeout & Expiration
1. **Tenant 1** tạo BOOKING.
2. Quá 24h **Host** không duyệt.
3. Hệ thống chạy Job: Booking tự động chuyển EXPIRED. Tiền không bị trừ.
4. Room vẫn giữ nguyên trạng thái AVAILABLE.

### E2E-03: Tranh chấp (Booking Case)
1. **Tenant** đã cọc (Booking CONFIRMED).
2. Đến hẹn bàn giao, phòng không như mô tả.
3. **Tenant** bấm tạo BOOKING CASE (Lý do: Lừa đảo, ảnh rác).
4. **Admin** xem Case, quyết định CANCEL_BOOKING và Refund.
5. Tiền trả về Tenant, Room tự động nhả về AVAILABLE.

### E2E-04: Late Settlement (Thanh toán muộn)
1. Booking APPROVED chờ thanh toán cọc trong 48h.
2. **Tenant** vào màn hình thanh toán, nhưng chần chừ không nhập mã OTP của ngân hàng.
3. Quá 48h, Job chạy → Booking chuyển EXPIRED, Room trở về AVAILABLE.
4. 15 phút sau, **Tenant** hoàn tất nhập OTP, ngân hàng đẩy Webhook SUCCEEDED.
5. BE kiểm tra thấy Booking đã EXPIRED.
6. Hệ thống đánh dấu Payment = SUCCEEDED nhưng tạo một lệnh REFUND hoàn tiền ngay lập tức (Lý do: LATE_PAYMENT).

---

## 4. State Transition Tests

**Kiểm thử tự động P1/P2 ngày 04/10/2026:** BookingServiceTest bao phủ các chuyển trạng thái/ownership; AuthIntegrationTest và AuthMysqlIntegrationTest bao phủ đăng ký, xác minh, rotation/logout và request đồng thời. BusinessWorkflowTest/BusinessMysqlTest kiểm tra payment expiration, late webhook, số lượng notification và rollback. Kết quả toàn suite: 180 test đạt; xem [P1_P2_RESULTS.md](P1_P2_RESULTS.md).

Dành cho QA manual: Kiểm tra UI bắt buộc phải phản chiếu đúng State (tham chiếu State Machine trong `PROJECT_CONTRACT.md`).
- **Phòng HELD:** Trên map/search UI không được phép có nút "Đặt phòng".
- **Listing DRAFT:** UI của Tenant không thể search thấy.
- **Booking CANCELLED:** Cả Host và Tenant không được nhìn thấy nút Confirm Handover.

---

## 5. Security & Access Control Tests

1. Thử xóa tài khoản khi đang có Booking CONFIRMED → BE phải chặn (DeletionStatus = BLOCKED).
2. Tenant cố tình lấy URL gọi API duyệt phòng (HOST action) → 403 Forbidden.
3. User A cố gọi API đọc tin nhắn chat của User B → 403 / 404 (Ownership/Membership).
4. Mọi API POST gửi ảnh/media lên cloud cần check S3 url xem có public mà không cần signature không (yêu cầu là bucket private, dùng signed URL 5 phút).
> **Cập nhật 03/10/2026:** Hiện trạng triển khai và các quyết định thay thế mô tả cũ nằm tại [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). FCM dùng MySQL outbox; hướng dẫn tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Các mốc kiểm chứng trước đây là lịch sử, không phải nghiệm thu bản hiện tại.
