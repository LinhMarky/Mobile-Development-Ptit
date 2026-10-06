# Phạm vi và bằng chứng triển khai — 05/10/2026

Tài liệu này cập nhật hiện trạng sau yêu cầu khắc phục các điểm yếu ở mục 10 của `codex24th9.md`. Không dùng số endpoint hoặc trạng thái hoàn thành cũ để suy ra đã nghiệm thu sản phẩm.

## Quyết định áp dụng

| Nội dung | Hành vi hiện tại |
| --- | --- |
| Kiến trúc | Java 17 / Spring Boot 3.3.2 / MySQL / MinIO; MySQL outbox + Firebase Admin SDK gửi FCM. Không dùng Kafka; không thêm lưu nhu cầu tìm phòng. |
| REST | Contract v1.6, mặc định `/api/v1`, cấu hình qua apiPrefix. HTTP JSON có envelope `{statusCode,message,data}`; DTO/page/array nằm trong data, DTO giữ snake_case. 200 không DTO có data=null; 204/205/304 rỗng, media binary. PageResponse bên trong data giữ page 0-based, size tối đa 50. |
| Lỗi | ApiProblems tạo ProblemDTO chung; HTTP application/json envelope, data chứa ProblemDTO, no-store; field_errors gồm field/reason; trace_id + timestamp; 405/406/415 giữ semantics. STOMP giữ ProblemDTO trực tiếp. |
| Tiền | JSON response là chuỗi số nguyên VND, ví dụ `"3000000"`; phép tính trong backend dùng BigDecimal, database DECIMAL(18,0). |
| Idempotency | POST rooms/listings/wishlist/conversations/admin/notifications/bookings/payments/viewings/viewing-slots/reviews/reports cần key. Retry cùng key+payload replay response đã bọc; payload khác 409. Snapshot legacy được bọc khi replay mà không chạy mutation lần hai. GET lấy trạng thái mới; auth/multipart không dùng filter này. |
| Tìm kiếm | Một endpoint GET `/listings`: query, room_type, min/max_rent_vnd, min/max_area_m2, province_code, amenity_ids, latitude/longitude/radius_km và sort. `nearest` cần tọa độ. Không có endpoint riêng `/listings/nearby`. |
| Booking | PENDING hết hạn sau 24 giờ; APPROVED giữ phòng 48 giờ; payment attempt tối đa 15 phút và không vượt hạn giữ. Sau cọc có 7 ngày để bàn giao. Quá hạn bàn giao mở một case. |
| Thanh toán | Chỉ payment SUCCEEDED đúng booking/amount và được phân bổ mới xác nhận cọc. Webhook muộn sinh refund sandbox, không hồi sinh booking hết hạn/hủy. UUID event + hash payload chống xử lý trùng. |
| Hết hạn payment | Job mặc định 60 giây/lượt, tối đa 100 candidate; mỗi payment một transaction, khóa room → booking → payment và kiểm tra lại trạng thái/deadline. Không giải phóng hold còn hiệu lực khi chỉ một lượt payment hết hạn. |
| Thông báo nghiệp vụ | BookingTransitions ghi inbox cho hai thành viên khi đổi trạng thái. Payment gửi kết quả cho payer, dùng tham chiếu booking; webhook replay không tạo trùng thông báo. Inbox + outbox cùng transaction với nghiệp vụ. |
| Webhook mock | Chỉ đăng ký route khi MOCK_PAYMENT_ENABLED=true; bắt buộc HMAC-SHA256 trên raw body qua X-Mock-Signature. Secret tối thiểu 32 ký tự. Không nuốt mọi lỗi thành HTTP 200. |
| Demo payment | POST `/payments/{id}/simulate` chỉ khi DEMO_ENABLED=true và chỉ payer của payment được gọi; không thu tiền thật. |
| Case | REFUND_TENANT hoàn toàn bộ; FORFEIT_DEPOSIT hoàn 0; SPLIT cần tenant_refund_vnd nguyên, lớn hơn 0 và nhỏ hơn deposit. Lưu phần tenant/host, kết thúc booking, giải phóng phòng cùng transaction. NO_ACTION chỉ ghi kết luận. Phần host retained là sổ ghi nhận sandbox, không chuyển khoản thật. |
| Media | Attach kiểm tra host/verified/room ownership/purpose; không sửa ảnh khi phòng HELD. Người ngoài không xem ảnh tin ẩn. URL `/{apiPrefix}/media/{id}/content` dùng cấu hình và được kiểm tra quyền mỗi lần đọc. |
| Điều khoản | Sửa listing/fees cần trạng thái cho sửa; phòng HELD khóa thay đổi. Booking snapshot bao gồm fees. Khóa room dùng chung cho booking/catalog/viewing để tuần tự hóa mutation. |
| FCM | Inbox + delivery cùng transaction, worker gửi ngoài transaction, retry/lease/invalid token/preferences; Android kiểm tra recipient và mở màn tương ứng. Chưa có Firebase project nên chưa nghiệm thu giao push thật. |
| Auth | Secret JWT bắt buộc, access TTL 15 phút, ID tài khoản kiểm tra ở HTTP/service/STOMP. SMTP lỗi rollback đăng ký, trả 503; resend lỗi giữ token cũ. Auth có giới hạn IP/identity. Refresh rotation khóa actor trước session và giữ installation. Logout thu hồi refresh session và device; access JWT còn theo TTL nếu tài khoản vẫn active. |
| Người dùng hiện tại | Các service dùng UserResolver + AccountAccessService, kiểm tra tài khoản trong database; principal chưa authenticated/anonymous không được giải thành user. |
| Xóa tài khoản | Bộ điều kiện dùng chung khi nhận/xử lý, gồm report chưa giải quyết và refund chưa SUCCEEDED. Pending chặn mutation; job khóa lifecycle và rooms, kiểm tra lại rồi anonymize/thu hồi sessions và devices/archive tin. FAILED có thể thử lại; lịch sử giao dịch giữ nguyên. |
| Retention / metrics | Batch 200/nhóm, xóa refresh/OTT/idempotency hết hạn và push terminal sau 30 ngày; giữ lịch sử. Metrics push/retention qua Actuator, ADMIN được kiểm tra từ DB. |

## Kiểm chứng

- Hoàn thiện v1.6: **249 test backend đạt, 0 failure/error/skipped, Maven verify + JAR thành công**, gồm H2 và MySQL 8.4 riêng. Docker health/SMTP/MinIO/deletion/HTTP/metrics PASS; 100 request tìm phòng, 4 worker, 0 lỗi, p95 67 ms trên dataset demo. Xem [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md) và các log/report được dẫn ở đó. Các mốc v1.5/P0/P1/P2 bên dưới được giữ làm lịch sử.
- Lượt dọn lint Android mới nhất: **6 unit test đạt, APK build thành công, lint 0 error và 2 warning**, giảm từ 26. Còn hai gợi ý nâng AGP/OkHttp; không tắt rule. Xem [ANDROID_LINT_REVIEW.md](ANDROID_LINT_REVIEW.md) và `Homely-android/lint-cleanup-verify.log`. Lifecycle đã nâng 2.11.0, JSON-java unit test 20260814; không đổi HTTP contract v1.5.
- Mốc v1.5 mới nhất: **205 test backend, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**; Android **6 unit test đạt**, build APK + lint thành công (0 error, 26 warning). Xem [GLOBAL_RESPONSE_WRAPPER.md](GLOBAL_RESPONSE_WRAPPER.md), `backend/target/wrapper-verify.log` và `Homely-android/wrapper-android-verify.log`.
- HTTP trên JAR wrapper mới **PASS**: auth, catalog, booking/idempotency, deposit/payment/handover và inbox; kiểm tra envelope success/error, log `backend/target/wrapper-http-acceptance.log`. OpenAPI 1.5.0 có 83 paths, đã kiểm tra local references; endpoint OpenAPI không bị bọc.
- Các mốc P0/P1/P2 bên dưới là lịch sử trước chuyển đổi HTTP envelope.
- Lượt P0 mới nhất: **199 test, 0 failure, 0 error, 0 skipped; BUILD SUCCESS**, log `backend/target/p0-final-verify.log`. Bao gồm 19 lượt bổ sung cho response/error/servlet fallback, media prefix và OpenAPI. Xem [P0_RESULTS.md](P0_RESULTS.md).
- Lượt P1/P2 ngày 04/10: **180 test, 0 failure, 0 error, 0 skipped**; auth và business workflow đều chạy trên H2 lẫn MySQL thật. Xem [P1_P2_RESULTS.md](P1_P2_RESULTS.md) và log `backend/target/p1-p2-verify.log`. Bước repackage ban đầu bị JAR đang mở; đã dừng helper kiểm thử và đóng gói lại thành công, log `backend/target/p1-p2-package.log`.
- Android build + lint lượt sau sửa ảnh/lịch xem/refresh: BUILD SUCCESS; lint còn cảnh báo, không lỗi. APK tại `Homely-android/app/build/outputs/apk/debug/app-debug.apk`.
- Compose đã qua `docker compose config --quiet`; điều này chỉ xác nhận cấu hình, chưa chứng minh mọi container đã chạy.
- MySQL 8.0.45 kiểm thử đã qua Flyway V1–V16 và schema validation; ánh xạ CHAR của event_id/currency đã được sửa và kiểm chứng. Chưa nghiệm thu toàn bộ Compose dùng MySQL 8.4.
- HTTP workflow trên JAR P0 mới đã **PASS**, log `backend/target/p0-http-acceptance.log`: auth, catalog moderation/privacy, money DTO, booking replay/conflict, deposit guard, sandbox payment, handover và inbox. Không dùng kết quả này để suy ra đã kiểm thử Android/FCM thật.
- OpenAPI hiện tại xuất từ runtime wrapper: success/error envelope, FieldErrorDTO, media binary; giữ STOMP extension. Alias ChatProblem/ProblemDetails vẫn trỏ về ProblemDTO cho payload STOMP.
- Android connected test trước đó vướng emulator không có window focus; chưa nghiệm thu E2E Android hoặc FCM trên điện thoại.

## Hướng dẫn

- [Chạy và seed demo](../Infra/README.md).
- [Build/chạy Android](../Homely-android/README.md).
- [Cấu hình và nghiệm thu Firebase](FIREBASE_SETUP.md).
- [Global response wrapper và nâng cấp client](GLOBAL_RESPONSE_WRAPPER.md).
- [Giải thích cảnh báo Android và kết quả dọn lint](ANDROID_LINT_REVIEW.md).
- OpenAPI phản ánh bản REST đang chạy; extension `x-stomp` mô tả chat TEXT. Kafka, transfer tiền thật và tìm kiếm đã lưu không thuộc phạm vi hiện tại.

P0/P1/P2 trong bản đánh giá ngày 04/10 đã hoàn tất trong phạm vi backend nêu trên. Nghiệm thu sản phẩm vẫn cần demo/E2E Android; push thật cần bổ sung cấu hình Firebase của người dùng.
