# CHANGELOG

## Contract v1.6 — Hoàn thiện backend (05/10/2026)

- JWT HS512 bắt buộc secret hợp lệ; access token được ràng buộc ID tài khoản tại HTTP/STOMP để chống tái sử dụng email. Auth có rate limit theo IP và identity, 429 + Retry-After, body quá lớn trả 413.
- Đăng ký rollback nếu SMTP lỗi, trả 503 EMAIL_UNAVAILABLE; resend lỗi giữ token cũ.
- Xóa tài khoản có processor/job, kiểm tra lại điều kiện, khóa lifecycle trước resource, thu hồi session/device và anonymize profile; lưu lịch sử booking/payment. Thêm GET /auth/delete-account. V18 tạo/backfill account_lifecycle_locks.
- Retention xóa dữ liệu hết hạn theo batch; push terminal giữ 30 ngày. V17 bổ sung index. Metrics theo dõi push/retention, chỉ ADMIN truy cập metrics.
- Profile prod chặn demo/mock, cấu hình riêng; CI kiểm thử MySQL, scripts kiểm tra hạ tầng và tải, Docker FCM override. Không thay format global wrapper.
- Bằng chứng và giới hạn thực tế tại [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md); FCM thật vẫn cần Firebase project của người dùng.

## Contract v1.5 — Global HTTP response wrapper (04/10/2026)

- Theo yêu cầu người dùng, HTTP JSON chuyển sang `{statusCode,message,data}` cho cả success và error. HTTP status thật được giữ; DTO/array/page nằm trong data. Error data chứa ProblemDTO cùng code, field_errors và trace_id; Content-Type là application/json.
- HTTP 200 không DTO trả data=null. HTTP 204/205/304 và byte stream media không bọc; Swagger/OpenAPI, Actuator và STOMP giữ giao thức riêng.
- FormatRestResponse được triển khai lại với phạm vi controller ứng dụng, hỗ trợ string/null và tránh double wrap. ApiProblems dùng cùng envelope cho filter và lỗi HTTP handshake; MVC/servlet fallback được advice bọc.
- Android ApiClient gỡ envelope một lần cho request thường, login/refresh và multipart upload. Các màn hình tiếp tục nhận DTO/page; lỗi đọc data.detail.
- Snapshot idempotency cũ được gắn envelope khi replay, không chạy lại mutation; snapshot mới giữ nguyên bytes. OpenAPI mô tả envelope success/error, giữ binary và STOMP.
- Không đổi state machine, schema database hoặc thêm migration. Chi tiết và bằng chứng tại [GLOBAL_RESPONSE_WRAPPER.md](GLOBAL_RESPONSE_WRAPPER.md). Kết quả P0 v1.4 bên dưới là lịch sử trước thay đổi này.

## Contract v1.4 — Đồng bộ luồng nghiệp vụ và FCM (04/10/2026)

- REST dùng `/api/v1`, DTO trực tiếp và snake_case; VND trả chuỗi số nguyên. Bổ sung idempotency cho POST booking/payment/viewing/review/report.
- P0: loại bỏ wrapper legacy; HTTP 204 không body, media trả binary. DTO REST chỉ xuất JSON. MVC/security/filter/STOMP/servlet fallback thống nhất ProblemDTO; lỗi 405/406/415 có code riêng, 405 giữ Allow; field_errors gồm field/reason, không phản chiếu rejected value.
- URL media dùng apiPrefix cấu hình, mặc định api/v1; OpenAPI dùng cùng prefix cho auth/idempotency, đăng ký FieldErrorDTO, mô tả media binary và alias schema lỗi cũ về ProblemDTO. Không thêm migration cho P0. Bằng chứng tại P0_RESULTS.md.
- Search thống nhất trên GET listings với filters/geo/sort; cập nhật các đường dẫn profile/device/preference theo controller.
- PENDING booking 24h, giữ phòng 48h, payment attempt tối đa 15 phút; xác nhận cọc cần payment hợp lệ; late success hoàn tiền sandbox. Case quá hạn bàn giao và các quyết định hoàn/chia/giữ cọc được nối vào nghiệp vụ.
- Webhook mock yêu cầu cờ cấu hình + HMAC; mô phỏng thanh toán chỉ trong chế độ demo và kiểm tra payer.
- Media attach/đọc áp dụng ownership và visibility. Nội dung được đọc qua endpoint backend để kiểm tra lại quyền và tránh URL MinIO nội bộ không truy cập được từ điện thoại.
- FCM dùng MySQL outbox + worker, thay kế hoạch Kafka. Bổ sung unregister device và unique thiết bị active; V14–V16 bổ sung số tiền settlement, push delivery, unique token/installation.
- Payment attempt PENDING quá hạn được job chuyển EXPIRED; thêm thông báo PAYMENT_EXPIRED và PAYMENT_REFUNDED cho payer, cùng các kết quả PAYMENT_SUCCEEDED/PAYMENT_FAILED. Các thông báo payment dùng ref_type=booking.
- Thêm GET admin/listings, admin/cases, bookings/{id}, bookings/{id}/cases và viewings/{id}; endpoint case-create cho thành viên booking.
- OpenAPI xuất lại từ runtime; giữ extension STOMP, mô tả điều kiện bật webhook mock. Xem IMPLEMENTATION_STATUS.md và FIREBASE_SETUP.md cho giới hạn nghiệm thu.

**Source of Truth:** PROJECT_CONTRACT.md

> Lịch sử thay đổi hợp đồng giao thức (Contract) sau khi team đã bắt đầu phát triển. Không dùng file này để ghi log commit code, chỉ dùng để thông báo thay đổi spec/API/Database.

---

## Contract v1.0 — Initial Release

### Date
17/09/2026

### Summary
Phát hành bản đầu tiên `PROJECT_CONTRACT.md` hợp nhất v1.0.

### Scope
- 13 nhóm chức năng cốt lõi (Từ F01 đến F13).
- Toàn bộ đặc tả API: Auth, Discovery, Booking, Payment (Sandbox), Review, Chat, Moderation.
- Định dạng lỗi RFC 9457 và Pagination rules.
- Hệ thống Business Rules (BR-01 đến BR-18).
- Ma trận phân quyền.

### FE impact
Baseline để phát triển toàn bộ UI Android và ViewModel layer.

### BE impact
Baseline để dựng Database, API Controller, Kafka publisher và STOMP websocket handler.

### Migration
Không áp dụng (phiên bản đầu tiên).

---

## Contract v1.1 — Full Restoration & Schema Alignment

### Date
26/09/2026

### Changed
- **PROJECT_CONTRACT.md:** Khôi phục hoàn toàn từ file bị hỏng (~60% nội dung mất). Gộp 3 phần (Part1/Part2/Part3) vào 1 file duy nhất.
- **§1.2:** Bổ sung bảng endpoint đầy đủ cho Interaction, Booking, Payment, Chat, Notification, Admin (đánh dấu `[RECOVERED]`).
- **§2:** Bổ sung schema cho media, viewings, bookings, payments, chat, notifications, audit_logs (đánh dấu `[RECOVERED]`).
- **§3:** Khôi phục DTO definitions cho Booking, Chat.
- **§4:** Khôi phục Enum definitions đầy đủ cho tất cả modules.
- **§5:** Khôi phục BR-03 đến BR-15 (listing search, detail, viewing, booking lifecycle, concurrency, payment, media, review, chat, idempotency, jobs, report, cases).
- **§7.3:** Thêm Error Code Catalog chi tiết.
- **listing_fees schema:** Đổi `fee_name` → `fee_code`, thêm `fee_mode`, đổi `unit` → `unit_name` (V5 migration).
- **wishlist_items:** Đổi FK từ `listing_id` → `room_id` (V5 migration).
- **listings:** Thêm UNIQUE(room_id) (V5 migration).
- **User roles:** Thêm bảng `roles` + `user_roles`, xóa cột `is_host` (V6 migration).
- **FormatRestResponse:** Xóa bỏ response wrapper — API trả DTO trực tiếp.

### Reason
File PROJECT_CONTRACT.md trước đó bị truncated và ghép sai do trích xuất từ transcript.jsonl. Tham khảo `codex24th9.md` §4.1 và `project_assessment.md` §2.

### FE impact
- Wishlist API path chuyển sang `/wishlist/{roomId}` thay vì `/{listingId}`.
- Fee DTO dùng `fee_code`, `fee_mode`, `unit_name` thay vì `fee_name`, `unit`.
- Response API không còn wrapper `{statusCode, message, data}` — nhận DTO trực tiếp.

### BE impact
- V5 migration: catalog schema alignment.
- V6 migration: user roles table.
- Xóa `FormatRestResponse.java` + `RestResponse.java`.
- Email verified check trước mutation actions.

### Migration
- Flyway V5 và V6 cần chạy trên database hiện tại.
- Existing users tự động migrate: tất cả có ROLE_TENANT, is_host=1 thêm ROLE_HOST.

---

## Contract v1.3 — Admin moderation và notification inbox

### Date
29/09/2026

### Changed
- Admin approve đặt thời điểm công bố và hạn 30 ngày; reject/suspend kiểm tra state machine. Bổ sung endpoint suspend listing và enum SUSPENDED trong code.
- Các thao tác Admin ghi audit và notification liên quan trong cùng transaction. Case resolution chỉ ghi nhận quyết định, chưa thực thi settlement.
- Khóa user đồng bộ status/cờ legacy, thu hồi refresh sessions. HTTP JWT kiểm tra lại trạng thái tài khoản và quyền admin trước idempotency replay. JWT nhận role từ DB, bao gồm ROLE_ADMIN.
- Notification list/read/read-all trả DTO trực tiếp, kiểm tra chủ sở hữu và size tối đa 50. Giữ alias mark-all-read; thêm Admin/Notification vào scope bắt buộc Idempotency-Key.

### FE impact
- Admin gửi reason/note bắt buộc qua query theo giới hạn trong ADMIN_NOTIFICATIONS.md. POST cần Idempotency-Key; xử lý 403/404/409 bằng ProblemDTO.
- Inbox nhận PageResponse/NotificationDTO trực tiếp; chuyển sang /notifications/read-all. Không coi inbox hiện tại là push hoặc case decision là tiền đã hoàn.

### Migration
Không thêm migration trong lượt này; dùng schema V12–V13 hiện có. Phase 8 chưa hoàn tất outbox/Kafka/FCM.

---

## Contract v1.2 — Phase 7 Chat

### Date
27/09/2026

### Changed
- CHAT01–CHAT05: REST create-or-open, danh sách/chi tiết hội thoại, history cursor và read marker; DTO trả trực tiếp dạng snake_case.
- CHAT01 trả 201 cả khi mở lại; POST yêu cầu `Idempotency-Key` không trắng, tối đa 128 ký tự. Replay trả snapshot, kiểm tra lại ACTIVE/membership trước cache.
- BR-11: UNIQUE(room, tenant), snapshot room_title, điều kiện tạo mới với listing PUBLISHED còn hạn, cấm tự chat, giữ quyền truy cập hội thoại cũ khi listing ẩn/hết hạn.
- TEXT qua STOMP `/ws` với access JWT ở HTTP handshake. Chỉ ba personal queues messages/acks/errors; MessageDTO ACK và ProblemDTO lỗi. Membership, verified khi gửi, token expiry và ACTIVE được kiểm tra; broker MESSAGE không chuyển tới session hết hạn/vô hiệu.
- Sequence và `client_message_id` UUID chuẩn hóa bảo đảm idempotency dữ liệu; retry khác nội dung trả 409. Phát realtime sau commit.
- Cursor before/after loại trừ biên, data ASC; `has_more` theo hướng, `next_cursor` chỉ về tin cũ. Read marker đơn điệu, unread không đếm tin tự gửi.
- Rate limit trong một process: REST 120 request/phút, STOMP SEND 60/phút mặc định. Simple broker giới hạn một backend instance; reconnect dùng REST resync.
- Bổ sung OpenAPI chat và `backend/CHAT.md`. IMAGE/SYSTEM, around_sequence và offline push chưa triển khai.

### Reason
Làm rõ GAP-02/GAP-03 và chốt giao thức chat để client có thể tích hợp đúng với backend Phase 7.

### FE impact
- Dùng `/api/v1/conversations`, PageResponse `{data,total_elements,total_pages,current_page,page_size}`; history dùng `{data,next_cursor,has_more}`.
- Lưu UUID ổn định cho mỗi lần soạn gửi; ghép ACK bằng client_message_id, hợp nhất tin theo id hoặc `(conversation_id,sequence)`.
- Cung cấp HTTP Authorization cho native WebSocket, subscribe personal queues và REST catch-up sau reconnect. Không dùng next_cursor làm cursor đồng bộ tin mới.
- Gửi read marker sau khi tin thực sự hiển thị/được đọc; không xem ACK gửi là read receipt. Lỗi chat không đảm bảo correlation theo client_message_id.

### BE impact
- Kiểm soát quyền trước REST replay và ở service/STOMP; sequence, message và preview lưu cùng transaction, phát sự kiện sau commit.
- Cấu hình `homely.chat.rest-requests-per-minute`, `homely.chat.messages-per-minute`, `homely.chat.allowed-origins`; chưa dùng nhiều backend instances.

### Migration
- Flyway `V11__chat_tables.sql` tạo conversations/messages/read_markers cùng khóa ngoại, unique keys và chỉ mục; chạy sau V1–V10 hiện có.
- Không sửa checksum migrations cũ. Tài liệu này mô tả thay đổi contract, không thay thế báo cáo kết quả chạy migration/kiểm thử.

---

*Format cho các lần cập nhật sau:*

```md
## Contract v1.x

### Date
[Ngày cập nhật]

### Changed
[ID API / Tên BR / DTO / Enum thay đổi]

### Reason
[Lý do thay đổi, hoặc dẫn link tới CONTRACT_GAPS]

### FE impact
[FE cần làm gì, ví dụ: "Bổ sung field XYZ vào UI"]

### BE impact
[BE cần làm gì, ví dụ: "Tạo migration thêm cột XYZ, sửa logic tại BR-05"]

### Migration
[Các bước migrate dữ liệu cũ nếu có]
```
