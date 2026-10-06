# BACKEND SPECIFICATIONS (BE_SPEC)

**Vai trò:** Tài liệu hướng dẫn kiến trúc, thiết kế database, xử lý luồng (concurrency) và event-driven cho hệ thống Spring Boot.  
**Tham chiếu Business Rules & API:** Mọi logic nghiệp vụ cốt lõi phải tuân theo `PROJECT_CONTRACT.md`.

**HTTP hiện tại (v1.6):** FormatRestResponse bọc JSON cho controller ứng dụng; ApiProblems bọc lỗi của filter/handshake. Service vẫn trả DTO; không bọc ở từng controller hoặc service. STOMP, media binary, no-content và endpoint framework có giao thức riêng. Chi tiết các lớp mới, transaction, cấu hình và kiểm thử tại [README backend](../backend/README.md) và [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md).

---

## 1. Backend Architecture

- **Platform:** Java 17 + Spring Boot 3.3.2
- **Kiến trúc:** Modular Monolith (chia package theo chức năng).
- **Cơ sở dữ liệu:** MySQL 8.4 (Chính) + MinIO (S3) (Lưu file).
- **ORM / Data Access:** Spring Data JPA / Hibernate. Quản lý schema bằng Flyway.
- **Bảo mật:** Spring Security theo BOM của Spring Boot 3.3.2; JWT HS512, bắt buộc secret riêng, kiểm tra trạng thái và ID tài khoản, auth rate limit.
- **Thông báo:** MySQL outbox + worker Firebase Admin SDK; không dùng Kafka.
- **Realtime:** Spring WebSocket / STOMP.

---

## 2. Module Responsibilities (Phân chia gói)

| Package | Trách nhiệm |
|---|---|
| `com.homely.rental.auth` | User, Role, Session, JWT, Email Verification, account deletion. |
| `com.homely.rental.catalog` | Listing, Room, Location, Fee, Wishlist, Tiện ích. |
| `com.homely.rental.interaction` | Hẹn xem phòng, đánh giá, report. |
| `com.homely.rental.booking` | Booking, bàn giao, tranh chấp. |
| `com.homely.rental.payment` | Thanh toán cọc, hoàn tiền, webhook sandbox. |
| `com.homely.rental.chat` | Conversation, Message, STOMP, marker. |
| `com.homely.rental.notification` | Inbox + push_deliveries cùng transaction; FCM ngoài transaction. |
| `com.homely.rental.media` | Upload, S3/MinIO, gắn ảnh vào resource. |
| `com.homely.rental.admin` | Case resolution, suspend user/listing, audit. |
| `com.homely.rental.common` | Response/error, idempotency, auditing, retention, config. |

---

## 3. Transaction Boundaries & Concurrency Rules

- **Quy tắc `@Transactional`:**
  - Chỉ áp dụng `@Transactional` ở mức Service layer.
  - Không bọc API call external (như push FCM hoặc gọi API nhà mạng) bên trong database transaction để tránh treo connection.
  - Dùng MySQL outbox: domain, inbox và `push_deliveries` chung transaction. Worker có lease/retry; xem FIREBASE_SETUP.md.

- **Optimistic Locking (`@Version`):**
  - **Bắt buộc trên các entity:** `Room`, `Listing`, `Booking`, `Payment`.
  - **Mục đích:** Xử lý BR-07 (Tranh chấp dữ liệu đồng thời).
  - **Ví dụ luồng Booking:** 2 tenant cùng đặt chung 1 phòng ở cùng khoảnh khắc. Transaction nào commit trước sẽ tăng `version` của `Room`. Transaction sau khi update `Room.active_booking_id` sẽ gặp `OptimisticLockException` và báo lỗi 409 `ROOM_NOT_AVAILABLE`.

- **Pessimistic Locking:**
  - Dùng khi thực thi Webhook thanh toán (Payment Webhook) để tránh 2 request webhook duplicate chạm vào cùng một Payment record cùng lúc (chữ ký `event_id` giúp dedup, khóa Pessimistic giúp chặn race condition).

---

## 4. Idempotency (BR-12)

- Phải triển khai cho **tất cả các endpoint POST thay đổi trạng thái hệ thống** (Tạo Booking, Approve/Reject, Thanh toán, Handover).
- Dùng `Idempotency-Key` (header) + `principal.id` + `request_path` + `body_hash` lưu vào bảng `idempotency_keys`.
- **Trạng thái:**
  - Lần 1: Lưu `PROCESSING` (nếu đã có `PROCESSING` → chặn 409). Xong luồng → `COMPLETED` và lưu response body.
  - Lần 2 (trùng key): Trả về nguyên body của lần 1 với mã 200/201.

---

## 5. Cấu hình Job / Scheduler (BR-13)

Sử dụng `@Scheduled` (cron jobs) hoặc Delay Queue.
- **Expire Booking Request:** Job chạy quét các Booking ở trạng thái `PENDING` có `request_expires_at <= NOW`. Chuyển sang `EXPIRED`. (Chạy mỗi phút).
- **Expire Held Booking (Deposit Timeout):** Quét Booking `APPROVED` có `hold_expires_at <= NOW`. Chuyển sang `EXPIRED`, giải phóng Room.
- **Handover Overdue:** Quét Booking `CONFIRMED` quá `handover_due_at`, tự động mở BookingCase `HANDOVER_OVERDUE`.
- **Listing Expiration:** Quét Listing `PUBLISHED` hết hạn (30 ngày), tự đổi sang `EXPIRED`.

---

## 6. Lớp Security & Quyền

- Chặn theo Role: `@PreAuthorize("hasRole('HOST')")`.
- Ownership check: `@PreAuthorize("@securityService.isRoomOwner(authentication, #roomId)")`.
- JWT Token Validation ở Filter layer. Token bị revoked (logout) kiểm tra qua bảng `refresh_tokens`. (Tham chiếu ma trận phân quyền trong PROJECT_CONTRACT).

---

## 7. Xử lý File Upload (MinIO)

- Upload trực tiếp file lên BE (MultipartFile). BE kiểm tra magic bytes, kích thước, định dạng.
- Sinh UUID làm object key. Upload lên S3/MinIO lấy path.
- Trả về DTO cho FE. Sau đó khi FE gửi POST tạo Listing, BE kiểm tra file có state `READY` và đổi sang state gắn liền với `Listing`.
- Job dọn dẹp hàng ngày xóa các file `READY` quá 24h mà không được gắn vào resource nào.

---

## 8. Global Error Mapping

Bắt mọi exception tại `@ControllerAdvice` và format theo `RFC 9457`:
- `MethodArgumentNotValidException` → 400 VALIDATION_FAILED + `field_errors`.
- `OptimisticLockException` → 409 CONFLICT (kèm detail).
- `AccessDeniedException` → 403 (kiểm tra nếu thiếu verify email thì mã riêng).
- `Exception` chung → 500 INTERNAL_ERROR (chỉ log, ẩn stack trace ra ngoài).
> **Cập nhật 03/10/2026:** Hiện trạng triển khai và các quyết định thay thế mô tả cũ nằm tại [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). FCM dùng MySQL outbox; hướng dẫn tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Các mốc kiểm chứng trước đây là lịch sử, không phải nghiệm thu bản hiện tại.
