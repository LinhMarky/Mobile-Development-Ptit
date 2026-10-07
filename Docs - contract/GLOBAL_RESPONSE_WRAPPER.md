# Global HTTP response wrapper — Contract v1.5

> **Android sau dọn lint:** 6 unit test vẫn đạt, APK build thành công; lint hiện 0 error, 2 warning. Báo cáo 26 warning trong bằng chứng wrapper bên dưới là mốc trước dọn. Chi tiết tại [ANDROID_LINT_REVIEW.md](ANDROID_LINT_REVIEW.md).

Ngày 04/10/2026, theo yêu cầu người dùng, backend và Android chuyển đồng bộ từ DTO trực tiếp sang envelope chung cho HTTP JSON. Contract hiện tại nằm tại PROJECT_CONTRACT.md §1.3/§7/§8; kết quả P0 v1.4 trong P0_RESULTS.md là lịch sử trước thay đổi này.

## Giao thức

Mọi HTTP JSON của controller ứng dụng có đúng ba trường `statusCode`, `message`, `data`. `statusCode` giữ camelCase theo tên envelope đã chốt, là integer trùng HTTP status thật. DTO trong `data` vẫn dùng snake_case và tiền VND vẫn là chuỗi số nguyên. Success message là reason phrase của HTTP status; error message lấy từ ProblemDTO.detail.

HTTP 201:

```json
{"statusCode":201,"message":"Created","data":{"id":7,"title":"Phòng A"}}
```

HTTP 200 với phân trang:

```json
{"statusCode":200,"message":"OK","data":{"data":[],"total_elements":0,"total_pages":0,"current_page":0,"page_size":20}}
```

HTTP 401:

```json
{
  "statusCode": 401,
  "message": "Authentication is required",
  "data": {
    "type": "urn:problem:authentication-required",
    "title": "Unauthorized",
    "status": 401,
    "detail": "Authentication is required",
    "instance": "/api/v1/profile",
    "code": "AUTHENTICATION_REQUIRED",
    "field_errors": [],
    "trace_id": "aef4c490-28ec-47ee-8523-9dc4d8c39089",
    "timestamp": "2026-10-04T14:25:00Z"
  }
}
```

HTTP lỗi vẫn là 4xx/5xx, Content-Type `application/json`, Cache-Control `no-store`; không đổi thành 200. ProblemDTO giữ code, field_errors, trace_id và timestamp trong data. Đây là Problem Details bên trong envelope, không gắn nhãn `application/problem+json` cho document root đã bọc.

HTTP 200 không có DTO trả `{"statusCode":200,"message":"OK","data":null}`. HTTP 204/205/304 không body; media/content trả byte stream nguyên gốc. Swagger/OpenAPI, Actuator và STOMP có giao thức riêng: STOMP vẫn trả MessageDTO/ProblemDTO trực tiếp, không thêm envelope HTTP.

## Triển khai

- `FormatRestResponse` bọc response một lần tại controller thuộc package ứng dụng; hỗ trợ DTO, array, page, null và StringHttpMessageConverter. Payload đã là RestResponse được giữ nguyên. Converter cho binary/resource/stream không bị bọc.
- `ApiProblems` giữ nơi tạo ProblemDTO chung. Filter security/account/idempotency/chat và lỗi HTTP handshake ghi envelope trực tiếp; lỗi MVC/servlet fallback được advice bọc. HTTP status, Allow, WWW-Authenticate và Retry-After được giữ.
- Idempotency lưu response sau khi serialize envelope; replay snapshot mới giữ nguyên bytes. Snapshot JSON hoặc success rỗng trước v1.5 được bọc khi replay, không lặp lại mutation hoặc xóa claim.
- Android `ApiEnvelope` gỡ data tại ApiClient cho request thường, login/refresh và upload multipart. UI nhận DTO/page như trước; array được chuyển thành object data ở nội bộ cho màn hình danh sách. Lỗi đọc data.detail, có fallback cho lỗi proxy không phải JSON. Envelope thiếu field hoặc statusCode không khớp HTTP bị từ chối.
- OpenAPI v1.5 mô tả success envelope, RestResponseProblemDTO và RestResponseVoid, giữ schema media binary và extension STOMP. Không đổi database/state machine, không thêm migration.

## Kiểm chứng

- Backend Maven `verify`: **205 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**, hoàn tất 21:25:11 ngày 04/10/2026. [Log](../backend/target/wrapper-verify.log). Auth/business workflow chạy trên H2 và MySQL thật; Flyway V1–V16 được kiểm chứng.
- `FormatRestResponseTest` thêm 4 test cho string converter + status/Location, tránh bọc hai lần, null200/204/304 và byte array binary. ResponseContractTest, GlobalExceptionTest, AdminNotificationHttpTest, ChatControllerTest và AuthIntegrationTest kiểm chứng envelope HTTP thực tế cùng quyền truy cập/validation.
- IdempotencyFilterTest thêm 2 test cho snapshot cũ dạng DTO/page/rỗng; test replay mới giữ bytes và không chạy mutation lần hai.
- Android **6 unit test đạt**, trong đó ApiEnvelopeTest có 5 test cho token/object, array/page, null/204, lỗi/fallback và envelope sai. `testDebugUnitTest assembleDebug lintDebug` **BUILD SUCCESSFUL**; lint 0 error, 26 warning. [Log](../Homely-android/wrapper-android-verify.log), [lint report](../Homely-android/app/build/reports/lint-results-debug.html), [APK debug](../Homely-android/app/build/outputs/apk/debug/app-debug.apk).
- HTTP trên JAR mới và database demo riêng: **PASS** auth rotation/logout, catalog moderation/privacy, money DTO, booking replay/conflict, deposit guard, sandbox payment, handover và inbox; Booking ID 5. Script kiểm tra cả envelope success/error. [Log](../backend/target/wrapper-http-acceptance.log).
- [openapi.yaml](openapi.yaml) xuất từ runtime **1.5.0**: **83 paths** (82 runtime + webhook mock có điều kiện); mọi local reference được kiểm tra trước khi ghi. Endpoint /v3/api-docs không bị bọc; schema login có statusCode/message/data; media giữ JPEG/PNG/WebP/MP4 binary.

Chưa nghiệm thu E2E Android hoặc FCM thật; Firebase vẫn cần cấu hình theo [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Thanh toán và hoàn tiền là sandbox. API v1.5 và Android mới cần được dùng cùng nhau; client cũ nhận DTO trực tiếp cần nâng cấp parser.
