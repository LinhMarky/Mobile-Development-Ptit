# Kết quả xử lý P0 — 04/10/2026

> **Mốc lịch sử v1.4:** Sau nghiệm thu P0 bên dưới, người dùng yêu cầu global response wrapper. Giao thức hiện tại là v1.5, xem [GLOBAL_RESPONSE_WRAPPER.md](GLOBAL_RESPONSE_WRAPPER.md). Quyết định bỏ wrapper và kết quả 199 test dưới đây mô tả bản trước thay đổi; không dùng làm format API hiện tại.

Đã xử lý cả ba mục C1/C2/C3 trong `danhgiatoandienngay4thang10.md`, đối chiếu contract v1.4 và client Android. Đề xuất bật `FormatRestResponse.supports()` trong bản đánh giá cũ không phù hợp: client nhận DTO trực tiếp; bật wrapper sẽ đổi giao thức đang dùng.

## Thay đổi và logic

| Mục | Kết quả | Kiểm chứng |
| --- | --- | --- |
| C1 — Response wrapper | Xóa FormatRestResponse, RestResponse, ApiMessage; controller dùng Operation cho mô tả API. Object/array giữ dạng gốc, PageResponse phẳng, 204 rỗng, media trả byte stream. | ResponseContractTest; kiểm tra các class cũ không có trong JAR. |
| C2 — Lỗi | ApiProblems tạo ProblemDTO chung cho MVC, security, account/idempotency/chat filters, handshake và STOMP. ApiErrorController bao phủ servlet fallback. HTTP lỗi là application/problem+json và no-store; 405 giữ Allow, 406/415 có code đúng. | ResponseContractTest, GlobalExceptionTest, IdempotencyFilterTest, chat tests; servlet fallback được kiểm tra trên H2 và MySQL. |
| C3 — URL media | MediaService dùng apiPrefix cấu hình, mặc định api/v1. OpenAPI dùng cùng prefix cho auth/idempotency. URL media vẫn qua kiểm tra ownership/visibility mỗi lần đọc. | MediaUrlTest: 5 trường hợp; HTTP với custom/v2, upload, bytes, tin ẩn và media riêng. |

Lỗi giữ 9 trường: `type`, `title`, `status`, `detail`, `instance`, `code`, `timestamp`, `field_errors`, `trace_id`. Field error gồm `field` và `reason`. Không đưa rejected value, exception/SQL hay query chứa token vào response. Lỗi 5xx trả thông báo chung. Normal request tới `/error` vẫn cần xác thực; chỉ servlet dispatcher ERROR được phép đi tới fallback để giữ status gốc.

REST DTO chỉ xuất JSON; yêu cầu XML cho DTO trả 406. Các converter phục vụ byte stream/resource vẫn được giữ cho media. Các lỗi nghiệp vụ vẫn giữ status/code riêng; chuẩn hóa định dạng không đổi state machine booking/payment hay quyền truy cập.

OpenAPI bổ sung schema FieldErrorDTO còn thiếu, mô tả nội dung media dạng binary và giữ STOMP extension. ChatProblem/ProblemDetails là alias của ProblemDTO. Prefix nên cấu hình chuẩn, ví dụ `api/v1` hoặc `custom/v2`; URL là đường dẫn tương đối để Android ghép với host backend.

## Bằng chứng nghiệm thu

- Maven `verify`: **199 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**, hoàn tất 18:32:11 ngày 04/10/2026. [Log](../backend/target/p0-final-verify.log). Auth/business workflow chạy trên cả H2 và MySQL thật; suite có kiểm tra Flyway V1–V16.
- Bổ sung 19 lượt test so với mốc P1/P2: ResponseContractTest 11, MediaUrlTest 5, OpenApiContractTest 1, servlet fallback 1 trên H2 và 1 trên MySQL.
- HTTP trên JAR mới và database demo riêng: **PASS** auth rotation/logout, moderation/privacy, VND DTO, booking replay/conflict, deposit guard, payment sandbox, handover và inbox. Booking ID 4. [Log](../backend/target/p0-http-acceptance.log).
- JAR mới khởi động thành công trên cổng kiểm thử 18081, validate 16 migrations và schema database. [Log runtime](../backend/target/p0-runtime.log).
- [openapi.yaml](openapi.yaml) xuất lại từ runtime: **83 paths** (82 runtime + webhook mock có điều kiện), giữ `x-stomp`, mọi local `$ref` được kiểm tra trước khi ghi. Media 200 mô tả JPEG/PNG/WebP/MP4 dạng binary.
- Kiểm tra nội dung JAR: có ApiProblems/ApiErrorController/ApiWebConfig; không còn ba class legacy nêu ở C1.

Chạy lại test từ thư mục `backend` với Maven đi kèm; thiết lập `HOMELY_TEST_MYSQL_URL`, `HOMELY_TEST_MYSQL_USER`, `HOMELY_TEST_MYSQL_PASSWORD` trỏ tới schema test riêng để chạy các bài MySQL. Hướng dẫn môi trường và demo tại [Infra/README.md](../Infra/README.md). Không dùng database dữ liệu thật cho integration test.

## Giới hạn

P0 backend đã được kiểm chứng trong phạm vi trên. Chưa nghiệm thu E2E Android hoặc giao FCM thật do chưa có Firebase project/cấu hình; hướng dẫn tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Thanh toán/hoàn tiền vẫn là sandbox. Các kết quả P1/P2 trước đó giữ tại [P1_P2_RESULTS.md](P1_P2_RESULTS.md).
