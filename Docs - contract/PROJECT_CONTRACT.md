# PROJECT CONTRACT — Homely Room Rental System

> Bản 1.6 bổ sung auth rate limit, đăng ký nhất quán khi SMTP lỗi và lifecycle xóa tài khoản; HTTP envelope giữ format v1.5. Xem [BACKEND_COMPLETION.md](BACKEND_COMPLETION.md), [GLOBAL_RESPONSE_WRAPPER.md](GLOBAL_RESPONSE_WRAPPER.md) và [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). [openapi.yaml](openapi.yaml) xuất từ backend, giữ STOMP và webhook mock có điều kiện. Các phần `[RECOVERED]` chưa triển khai không phải cam kết đã hoàn thành.

**Phiên bản contract:** 1.6  
**Ngày phát hành:** 17/09/2026  
**Cập nhật lần cuối:** 05/10/2026  
**Cơ sở:** PROJECT_SPEC_FOR_CLAUDE v1.0

> **Tài liệu Source of Truth** cho toàn bộ dự án. Mọi thay đổi API, DTO, Enum, Business Rule, Schema đều phải cập nhật tại đây.
>
> Các phần đánh dấu `[RECOVERED]` được khôi phục từ code, BE_SPEC, FE_SPEC và context — cần owner xác nhận.

---

## §1. API Protocol

### 1.1 Mục tiêu sản phẩm và phạm vi

App phục vụ người tìm phòng (ưu tiên sinh viên/người đi làm quanh trường hoặc nơi làm việc) và chủ trọ quản lý các phòng độc lập. Hành trình hoàn chỉnh:

**Tìm phòng → kiểm tra thông tin và chi phí → lưu/chat → hẹn xem nếu cần → yêu cầu thuê → chủ duyệt giữ phòng → khách xác nhận/cọc thử nghiệm → xác nhận bàn giao → đánh giá.**

#### 13 nhóm chức năng bắt buộc

| Mã  | Nhóm chức năng       | Kết quả người dùng cần đạt |
| --- | -------------------- | --- |
| F01 | Tài khoản và vai trò | Xem công khai không cần đăng nhập; tài khoản có thể vừa tìm phòng vừa cho thuê |
| F02 | Khám phá             | Xem phòng còn trống, mới cập nhật, gần điểm đã chọn |
| F03 | Chi tiết phòng       | Xem ảnh, giá, phí, tiện ích, vị trí, đánh giá và chủ phòng |
| F04 | Lưu và so sánh       | Lưu phòng yêu thích vào Wishlist để xem lại |
| F05 | Nhắn tin             | Chat trực tiếp giữa khách và chủ trọ |
| F06 | Hẹn xem phòng       | Đặt lịch xem phòng thực tế |
| F07 | Yêu cầu thuê        | Gửi yêu cầu giữ phòng, chủ duyệt hoặc từ chối |
| F08 | Giữ phòng và cọc    | Đặt cọc thử nghiệm (sandbox), giữ phòng có thời hạn |
| F09 | Bàn giao             | Xác nhận bàn giao phòng hai bên |
| F10 | Đánh giá             | Đánh giá sau khi hoàn thành thuê |
| F11 | Thông báo            | Push notification cho các sự kiện quan trọng |
| F12 | Quản trị             | Admin duyệt tin, xử lý tranh chấp, suspend |
| F13 | Báo cáo vi phạm     | Report nội dung, tin nhắn hoặc hành vi |

### 1.2 API Endpoints

#### Auth Module (AUTH01–AUTH12)

| Code   | Method | Path                             | Actor    | Mô tả |
| ------ | ------ | -------------------------------- | -------- | --- |
| AUTH01 | POST   | /api/v1/auth/register            | Guest    | Đăng ký tài khoản |
| AUTH02 | POST   | /api/v1/auth/login               | Guest    | Đăng nhập, trả access + refresh token |
| AUTH03 | POST   | /api/v1/auth/refresh             | Any      | Refresh access token |
| AUTH04 | POST   | /api/v1/auth/logout              | Auth     | Đăng xuất, revoke refresh token |
| AUTH05 | POST   | /api/v1/auth/verify-email/resend      | Auth     | Gửi email xác minh |
| AUTH06 | POST   | /api/v1/auth/verify-email        | Guest    | Xác minh email bằng OTT |
| AUTH07 | GET    | /api/v1/profile             | Auth     | Lấy thông tin profile |
| AUTH08 | PATCH  | /api/v1/profile             | Auth     | Cập nhật profile |
| AUTH09 | POST   | /api/v1/auth/enable-host         | Auth+Verified | Kích hoạt vai trò HOST |
| AUTH10 | PUT   | /api/v1/profile/device-token        | Auth     | Đăng ký FCM device token |
| AUTH11 | GET/PATCH | /api/v1/profile/notification-preferences | Auth   | Quản lý notification preferences |
| AUTH12 | POST/GET | /api/v1/auth/delete-account      | Auth     | Yêu cầu xóa và xem trạng thái yêu cầu của chính mình |

#### Catalog Module — Host (HOST01–HOST06)

| Code   | Method | Path                                | Actor | Mô tả |
| ------ | ------ | ----------------------------------- | ----- | --- |
| HOST01 | POST   | /api/v1/rooms                       | Host  | Tạo phòng mới |
| HOST02 | PATCH  | /api/v1/rooms/{id}                  | Host  | Sửa phòng (optimistic locking) |
| HOST03 | GET    | /api/v1/rooms/my                    | Host  | Danh sách phòng của host |
| HOST04 | GET    | /api/v1/rooms/{id}                  | Host  | Chi tiết phòng |
| HOST05 | POST   | /api/v1/rooms/{id}/mark-rented      | Host  | Đánh dấu đã cho thuê ngoài app |
| HOST06 | POST   | /api/v1/rooms/{id}/mark-available   | Host  | Đánh dấu phòng trống lại |

#### Catalog Module — Listing (CAT01–CAT14)

| Code  | Method | Path                              | Actor       | Mô tả |
| ----- | ------ | --------------------------------- | ----------- | --- |
| CAT01 | POST   | /api/v1/listings/room/{roomId}    | Host        | Tạo listing (DRAFT) |
| CAT02 | PATCH  | /api/v1/listings/{id}             | Host        | Sửa listing |
| CAT03 | POST   | /api/v1/listings/{id}/submit      | Host        | Gửi duyệt |
| CAT04 | POST   | /api/v1/listings/{id}/hide        | Host        | Ẩn tin đang PUBLISHED |
| CAT05 | GET    | /api/v1/listings                  | Public      | Tìm kiếm listings |
| CAT06 | GET | /api/v1/listings?latitude=...&longitude=...&sort=nearest | Public | Tìm theo vị trí, chung API search |
| CAT07 | GET    | /api/v1/listings/{id}             | Public      | Chi tiết listing |
| CAT08 | PUT    | /api/v1/listings/{id}/fees        | Host        | Set danh sách fees |
| CAT09 | GET    | /api/v1/listings/{id}/fees        | Public      | Xem fees |
| CAT10 | PUT | /api/v1/listings/{id}/fees | Host | Thay cả danh sách, bỏ phần tử để xóa fee |
| CAT11 | POST   | /api/v1/wishlist/{roomId}         | Auth        | Lưu phòng vào wishlist |
| CAT12 | DELETE | /api/v1/wishlist/{roomId}         | Auth        | Bỏ lưu phòng |
| CAT13 | GET    | /api/v1/wishlist                  | Auth        | Danh sách đã lưu |
| CAT14 | GET    | /api/v1/amenities                 | Public      | Danh sách tiện ích |
| GET   | GET    | /api/v1/listings/my               | Host        | Danh sách tin của host |

#### Interaction Module (VIEW01–VIEW08, REV01–REV04, RPT01–RPT02) [RECOVERED]

| Code    | Method | Path                                    | Actor       | Mô tả |
| ------- | ------ | --------------------------------------- | ----------- | --- |
| VIEW01  | GET    | /api/v1/viewing-slots/{roomId}          | Public      | Lịch trống xem phòng |
| VIEW02  | POST   | /api/v1/viewing-slots                   | Host        | Tạo slot xem phòng |
| VIEW03  | DELETE | /api/v1/viewing-slots/{id}              | Host        | Xóa slot |
| VIEW04  | GET    | /api/v1/viewings/my                     | Auth        | Danh sách hẹn xem |
| VIEW05  | POST   | /api/v1/viewings                        | Tenant+V    | Đặt hẹn xem |
| VIEW06  | POST   | /api/v1/viewings/{id}/confirm           | Host        | Xác nhận hẹn |
| VIEW07  | POST   | /api/v1/viewings/{id}/cancel            | Auth        | Hủy hẹn |
| VIEW08  | POST   | /api/v1/viewings/{id}/complete          | Host        | Hoàn thành buổi xem |
| REV01   | POST   | /api/v1/reviews                         | Tenant+V    | Tạo đánh giá |
| REV02   | GET    | /api/v1/reviews/room/{roomId}           | Public      | Xem đánh giá phòng |
| REV03   | PATCH  | /api/v1/reviews/{id}                    | Owner       | Sửa đánh giá |
| REV04   | DELETE | /api/v1/reviews/{id}                    | Owner       | Xóa đánh giá |
| RPT01   | POST   | /api/v1/reports                         | Auth+V      | Tạo báo cáo vi phạm |
| RPT02   | GET    | /api/v1/reports/my                      | Auth        | Xem báo cáo đã tạo |

#### Booking Module (BOOK01–BOOK09, CASE01–CASE03) [RECOVERED]

| Code    | Method | Path                                    | Actor       | Mô tả |
| ------- | ------ | --------------------------------------- | ----------- | --- |
| BOOK01  | POST   | /api/v1/bookings                        | Tenant+V    | Tạo yêu cầu thuê (Idempotency-Key) |
| BOOK02  | GET    | /api/v1/bookings/my                     | Tenant      | Danh sách booking của tenant |
| BOOK03  | GET    | /api/v1/bookings/host                   | Host        | Danh sách booking cho host |
| BOOK04  | POST   | /api/v1/bookings/{id}/approve           | Host        | Duyệt yêu cầu thuê |
| BOOK05  | POST   | /api/v1/bookings/{id}/reject            | Host        | Từ chối yêu cầu thuê |
| BOOK06  | POST   | /api/v1/bookings/{id}/confirm-deposit   | Tenant      | Xác nhận đã cọc |
| BOOK07  | POST   | /api/v1/bookings/{id}/handover-tenant   | Tenant      | Tenant xác nhận bàn giao |
| BOOK08  | POST   | /api/v1/bookings/{id}/handover-host     | Host        | Host xác nhận bàn giao |
| BOOK09  | POST   | /api/v1/bookings/{id}/cancel            | Auth        | Hủy booking |
| CASE01  | POST   | /api/v1/booking-cases                   | Auth        | Mở case tranh chấp |
| CASE02  | GET    | /api/v1/booking-cases/{bookingId}       | Auth        | Xem case |
| CASE03  | POST   | /api/v1/booking-cases/{id}/resolve      | Admin       | Giải quyết case |

#### Payment Module (PAY01–PAY03) [RECOVERED]

| Code  | Method | Path                                    | Actor       | Mô tả |
| ----- | ------ | --------------------------------------- | ----------- | --- |
| PAY01 | POST   | /api/v1/payments                        | Tenant      | Tạo thanh toán cọc (Mock) |
| PAY02 | POST   | /api/v1/payments/webhook                | System      | Nhận webhook từ payment provider |
| PAY03 | GET    | /api/v1/payments/booking/{bookingId}    | Auth        | Xem trạng thái thanh toán |

#### Chat Module (CHAT01–CHAT05) [PHASE7]

| Code   | Method | Path                                    | Actor  | Mô tả |
| ------ | ------ | --------------------------------------- | ------ | --- |
| CHAT01 | POST   | /api/v1/conversations                   | Auth+V | Tạo/mở conversation |
| CHAT02 | GET    | /api/v1/conversations                   | Auth   | Danh sách conversations |
| CHAT03 | GET    | /api/v1/conversations/{id}              | Member | Chi tiết conversation |
| CHAT04 | GET    | /api/v1/conversations/{id}/messages     | Member | Lịch sử tin nhắn (cursor) |
| CHAT05 | POST   | /api/v1/conversations/{id}/read-marker  | Member | Đánh dấu đã đọc |
| CHAT-WS| STOMP  | /ws (SEND /app/chat.send)               | Member | Gửi tin nhắn realtime |

Các endpoint HTTP chat dùng envelope `{statusCode,message,data}`; DTO/cursor page nằm trong data. Payload STOMP giữ DTO trực tiếp. Tất cả thao tác yêu cầu tài khoản `ACTIVE`; tạo conversation và gửi tin yêu cầu email đã xác minh. Chỉ tenant/host đã lưu trên conversation được đọc, gửi hoặc cập nhật read marker; vai trò ADMIN không tự cấp quyền đọc hội thoại riêng.

#### Notification & Admin (NOTIF01–03, ADMIN01–04) [RECOVERED]

| Code    | Method | Path                                    | Actor | Mô tả |
| ------- | ------ | --------------------------------------- | ----- | --- |
| NOTIF01 | GET    | /api/v1/notifications                   | Auth  | Danh sách thông báo |
| NOTIF02 | POST   | /api/v1/notifications/{id}/read         | Auth  | Đánh dấu đã đọc |
| NOTIF03 | POST   | /api/v1/notifications/read-all          | Auth  | Đánh dấu tất cả đã đọc |
| ADMIN01 | POST   | /api/v1/admin/listings/{id}/approve     | Admin | Duyệt tin |
| ADMIN02 | POST   | /api/v1/admin/listings/{id}/reject      | Admin | Từ chối tin |
| ADMIN03 | POST   | /api/v1/admin/listings/{id}/suspend     | Admin | Tạm ngưng tin |
| ADMIN04 | POST   | /api/v1/admin/users/{id}/suspend        | Admin | Suspend user |
| ADMIN05 | POST   | /api/v1/admin/cases/{id}/resolve        | Admin | Ghi kết luận BookingCase, chưa chuyển tiền |
| ADMIN06 | POST   | /api/v1/admin/reports/{id}/resolve      | Admin | Xử lý báo cáo |
| ADMIN07 | POST   | /api/v1/admin/reviews/{id}/hide         | Admin | Ẩn review kèm lý do |
| ADMIN08 | POST   | /api/v1/admin/users/{id}/unsuspend      | Admin | Mở khóa tài khoản |

#### Phạm vi Admin/Notification v1.3 (29/09/2026)

- Hộp thông báo chỉ trả dữ liệu của người gọi; `GET /notifications/unread-count` là endpoint bổ sung. `/notifications/mark-all-read` là alias legacy của `/notifications/read-all`. List dùng page >= 0, size 1..50, mặc định 20, thứ tự created_at DESC rồi id DESC. DTO/page nằm trong envelope.data.
- Admin và notification POST cần Idempotency-Key; ACTIVE và quyền admin hiện tại trong DB được kiểm tra trước replay. JWT role lấy từ các role đã cấp, gồm ROLE_ADMIN. Khóa user đồng bộ status/cờ suspended, thu hồi refresh sessions; mở khóa không hồi sinh session đã thu hồi hoặc user DELETED.
- Duyệt tin chỉ từ PENDING_REVIEW, đặt published_at và expires_at = published_at + 30 ngày, snapshot terms_version của Room. Từ chối chỉ từ PENDING_REVIEW; tạm ngưng chỉ từ PUBLISHED/HIDDEN sang SUSPENDED. Host không tự submit tin SUSPENDED; chưa có API khôi phục tin tạm ngưng.
- `reason` là query parameter bắt buộc, không trắng, tối đa 500 ký tự cho reject/suspend/hide. Case resolve cần `decision` và `note` tối đa 2000; report resolve cần `note` tối đa 1000. Thành công trả 200 body rỗng; trạng thái không cho phép trả 409.
- Thay đổi trạng thái, audit và thông báo liên quan cùng transaction. Case resolution mới ghi nhận decision, người xử lý và ghi chú; chưa thực thi hoàn tiền/chia tiền. Push FCM/Kafka/outbox và các producer sự kiện khác chưa hoàn thành.
- Chi tiết request/response, quyền, kiểm chứng và giới hạn: [backend/ADMIN_NOTIFICATIONS.md](../backend/ADMIN_NOTIFICATIONS.md).

### 1.3 Request/Response Protocol

- **Base URL:** `/api/v1`
- **Content-Type:** `application/json`
- **Global HTTP JSON response:** `{statusCode, message, data}`. `statusCode` là integer trùng HTTP status thật; `message` là thông báo, `data` là DTO/object/array hoặc null. Ba field envelope giữ đúng tên trên; DTO bên trong giữ snake_case. Không đổi lỗi 4xx/5xx thành HTTP 200.
- **Success response:** DTO/object/array nằm trong `data`; PageResponse hoặc MessagePageDTO nằm nguyên trong `data`, không gộp metadata ra envelope. HTTP 200 không có DTO trả `data: null`. HTTP 204/205/304 không body; endpoint media/content trả binary với Content-Type của file.
- **Auth:** `Authorization: Bearer <access_token>`
- **Idempotency:** POST mutation endpoints gửi header `Idempotency-Key: <UUID>`
- **Pagination:** Spring Pageable — query params `page` (0-based), `size` (default 20, max 50), `sort`
- **Money:** `DECIMAL(18,0)` — response JSON trả chuỗi số nguyên VND, ví dụ `"3000000"`, không dấu thập phân
- **DateTime:** UTC ISO-8601 (`2026-09-17T10:30:00Z`)
- **Error:** HTTP `application/json`, cùng envelope; `data` chứa ProblemDTO (xem §7), `message` lấy từ detail. STOMP giữ MessageDTO/ProblemDTO trực tiếp; Swagger/OpenAPI và Actuator không bọc.

---

## §2. Data Model

### 2.1 Auth & User

#### users

`id BIGINT PK`, `email VARCHAR(255) NOT NULL UNIQUE`, `password VARCHAR(200) NOT NULL`, `full_name VARCHAR(100) NOT NULL`, `phone VARCHAR(15)?`, `avatar_url VARCHAR(512)?`, `email_verified BOOLEAN DEFAULT FALSE`, `status UserStatus DEFAULT 'ACTIVE'`, `refresh_token TEXT?` _(legacy, dùng refresh_tokens table)_, `version INT`, timestamps.

#### roles

`id BIGINT PK`, `name VARCHAR(20) NOT NULL UNIQUE` — Seed: `ROLE_TENANT`, `ROLE_HOST`, `ROLE_ADMIN`.

#### user_roles

`user_id BIGINT FK users`, `role_id BIGINT FK roles`, PK(user_id, role_id).

#### refresh_tokens

`id`, `token_hash VARCHAR(64) NOT NULL UNIQUE`, `user_id FK users`, `installation_id VARCHAR(36)`, `device_name VARCHAR(100)`, `expires_at DATETIME(3)`, `revoked BOOLEAN DEFAULT FALSE`, `created_at`.

#### one_time_tokens

`id`, `token_hash VARCHAR(64) NOT NULL UNIQUE`, `user_id FK users`, `purpose OneTimeTokenPurpose`, `used BOOLEAN DEFAULT FALSE`, `expires_at DATETIME(3)`, `created_at`.

#### device_tokens

`id`, `user_id FK users`, `token VARCHAR(4096)`, `platform VARCHAR(10) DEFAULT 'ANDROID'`, `device_name VARCHAR(100)`, `installation_id VARCHAR(36)`, `active BOOLEAN DEFAULT TRUE`, timestamps. UNIQUE(user_id, installation_id).

#### notification_preferences

`id`, `user_id FK users UNIQUE`, `transaction_push BOOLEAN DEFAULT TRUE`, `transaction_email BOOLEAN DEFAULT TRUE`, `chat_push BOOLEAN DEFAULT TRUE`, `recommendation_push BOOLEAN DEFAULT TRUE`, `version INT`, timestamps.

#### account_deletion_requests

`id`, `user_id FK users UNIQUE`, `reason VARCHAR(1000) DEFAULT ''`, `status DeletionStatus DEFAULT 'PENDING'`, `failure_code VARCHAR(100)?`, `completed_at DATETIME(3)?`, timestamps.

#### account_lifecycle_locks (Auth, V18)

`user_id BIGINT PK FK users`. Tạo cùng User; migration backfill tài khoản cũ. Thao tác ghi của actor và processor xóa khóa hàng này trước resource, tránh khóa `users` gây xung đột với FK của người khác.

#### auth_email_jobs [RECOVERED]

Chưa triển khai bảng/job này. Bản hiện tại gửi SMTP đồng bộ với timeout; đăng ký rollback khi gửi lỗi, trả 503 `EMAIL_UNAVAILABLE`.

`id`, `user_id FK users`, `one_time_token_id FK one_time_tokens`, `encrypted_token_payload TEXT?`, `purpose OneTimeTokenPurpose`, `status DeliveryStatus DEFAULT 'PENDING'`, `attempt_count INT DEFAULT 0`, `next_attempt_at DATETIME(3)?`, `expires_at DATETIME(3)`, timestamps.

### 2.2 Catalog

#### amenities

`id BIGINT PK`, `name VARCHAR(100) NOT NULL UNIQUE`, `icon VARCHAR(100)?`, `category VARCHAR(50)?`.

#### rooms

`id BIGINT PK`, `host_id BIGINT FK users`, `unit_code VARCHAR(30) NOT NULL`, `room_type RoomType`, `area_m2 DECIMAL(6,1)`, `max_occupants INT DEFAULT 1`, `address_line VARCHAR(300)?`, `province_code VARCHAR(20)?`, `province_name VARCHAR(100)?`, `ward_code VARCHAR(20)?`, `ward_name VARCHAR(100)?`, `latitude DOUBLE?`, `longitude DOUBLE?`, `availability RoomAvailability DEFAULT 'AVAILABLE'`, `terms_version INT DEFAULT 1`, `version INT`, timestamps. UNIQUE(host_id, unit_code).

#### room_amenities

PK(room_id, amenity_id), FK rooms, FK amenities.

#### listings

`id BIGINT PK`, `room_id BIGINT FK rooms UNIQUE`, `title VARCHAR(150) DEFAULT ''`, `description VARCHAR(5000) DEFAULT ''`, `rent_vnd DECIMAL(18,0)?`, `deposit_vnd DECIMAL(18,0)?`, `status ListingStatus DEFAULT 'DRAFT'`, `terms_version INT DEFAULT 1`, `published_at DATETIME(3)?`, `expires_at DATETIME(3)?`, `version INT`, timestamps.

**Constraint:** UNIQUE(room_id) — 1 Room = 1 Listing (BR-01).

#### listing_fees

`id BIGINT PK`, `listing_id FK listings CASCADE`, `fee_code VARCHAR(50) NOT NULL`, `fee_mode VARCHAR(20) NOT NULL DEFAULT 'FIXED'`, `amount_vnd DECIMAL(18,0) NOT NULL`, `unit_name VARCHAR(50)?`, `note VARCHAR(500)?`, `sort_order INT DEFAULT 0`.

#### wishlist_items

`id BIGINT PK`, `user_id FK users CASCADE`, `room_id FK rooms CASCADE`, `created_at`. UNIQUE(user_id, room_id).

### 2.3 Media [RECOVERED]

#### media

`id BIGINT PK`, `uploader_id FK users`, `purpose MediaPurpose NOT NULL`, `resource_type VARCHAR(50)?`, `resource_id BIGINT?`, `content_type VARCHAR(100) NOT NULL`, `file_size_bytes BIGINT NOT NULL`, `storage_key VARCHAR(512) NOT NULL UNIQUE`, `original_filename VARCHAR(255)?`, `width INT?`, `height INT?`, `duration_secs INT?`, `status MediaStatus DEFAULT 'READY'`, `version INT`, timestamps.

### 2.4 Interaction [RECOVERED]

#### viewing_slots

`id`, `room_id FK rooms`, `host_id FK users`, `start_at DATETIME(3) NOT NULL`, `end_at DATETIME(3) NOT NULL`, `status ViewingSlotStatus DEFAULT 'OPEN'`, timestamps.

#### viewings

`id`, `viewing_slot_id FK viewing_slots`, `tenant_id FK users`, `room_id FK rooms`, `host_id FK users`, `status ViewingStatus DEFAULT 'REQUESTED'`, `note VARCHAR(1000)?`, `cancelled_reason VARCHAR(1000)?`, `completed_at DATETIME(3)?`, `version INT`, timestamps.

#### reviews

`id`, `booking_id FK bookings UNIQUE`, `reviewer_id FK users`, `room_id FK rooms`, `rating INT NOT NULL` (1–5), `comment VARCHAR(2000)?`, `visible BOOLEAN DEFAULT TRUE`, `hide_reason VARCHAR(500)?`, `version INT`, timestamps.

#### reports

`id`, `reporter_id FK users`, `target_type VARCHAR(50) NOT NULL`, `target_id BIGINT NOT NULL`, `reason_code VARCHAR(50) NOT NULL`, `description VARCHAR(2000)?`, `status ReportStatus DEFAULT 'PENDING'`, `resolved_by BIGINT? FK users`, `resolution_note VARCHAR(1000)?`, `resolved_at DATETIME(3)?`, `version INT`, timestamps.

#### listing_status_history, booking_status_history, viewing_status_history

Mỗi bảng: `id`, FK tương ứng, `from_status VARCHAR(30)?`, `to_status VARCHAR(30) NOT NULL`, `actor_id BIGINT? FK users`, `actor_type ActorType NOT NULL`, `reason VARCHAR(1000)?`, `trace_id VARCHAR(64) NOT NULL`, `created_at`; append-only.

### 2.5 Booking [RECOVERED]

#### bookings

`id`, `tenant_id FK users`, `host_id FK users`, `room_id FK rooms`, `listing_id FK listings`, `status BookingStatus DEFAULT 'PENDING'`, `desired_move_in DATE?`, `occupant_count INT DEFAULT 1`, `rent_vnd DECIMAL(18,0) NOT NULL`, `deposit_vnd DECIMAL(18,0) DEFAULT 0`, `note VARCHAR(1000)?`, `terms_snapshot JSON NOT NULL`, `snapshot_schema_version INT DEFAULT 1`, `request_expires_at DATETIME(3) NOT NULL`, `hold_expires_at DATETIME(3)?`, `handover_due_at DATETIME(3)?`, `tenant_terms_accepted_at DATETIME(3)?`, `tenant_handover_confirmed_at DATETIME(3)?`, `host_handover_confirmed_at DATETIME(3)?`, `allocated_payment_id BIGINT? FK payments UNIQUE`, `completed_at DATETIME(3)?`, `cancelled_at DATETIME(3)?`, `last_reason VARCHAR(1000)?`, `version INT`, timestamps.

**Index:** (tenant_id, status, created_at, id), (host_id, status, created_at, id), (status, request_expires_at), (status, hold_expires_at), (status, handover_due_at).

**Generated columns STORED:**
- `active_room_id = CASE WHEN status IN ('APPROVED','CONFIRMED') THEN room_id ELSE NULL END`; UNIQUE(active_room_id).
- `open_room_id = CASE WHEN status IN ('PENDING','APPROVED','CONFIRMED') THEN room_id ELSE NULL END`; UNIQUE(tenant_id, open_room_id).

MySQL cho nhiều NULL ở unique index.

#### booking_cases

`id`, `booking_id FK bookings`, `type BookingCaseType NOT NULL`, `opened_by BIGINT? FK users`, `description TEXT NOT NULL`, `status CaseStatus DEFAULT 'OPEN'`, `decision BookingCaseDecision?`, `resolution_note TEXT?`, `resolved_by BIGINT? FK users`, `resolved_at DATETIME(3)?`, `version INT`, timestamps.

Generated `open_booking_id=booking_id` khi OPEN/IN_REVIEW, NULL otherwise; UNIQUE(open_booking_id).

#### case_evidence

PK(case_id, media_id), `uploaded_by FK users`, `created_at`. Media purpose REPORT_EVIDENCE.

### 2.6 Payment & Refund [RECOVERED]

#### payments

`id`, `booking_id FK bookings`, `payer_id FK users`, `provider PaymentProvider DEFAULT 'MOCK_SANDBOX'`, `provider_payment_id VARCHAR(100) UNIQUE`, `amount_vnd DECIMAL(18,0) NOT NULL`, `currency CHAR(3) DEFAULT 'VND'`, `status PaymentStatus DEFAULT 'PENDING'`, `expires_at DATETIME(3) NOT NULL`, `succeeded_at DATETIME(3)?`, `failure_code VARCHAR(100)?`, `is_test BOOLEAN DEFAULT TRUE`, `version INT`, timestamps.

Generated `pending_booking_id=booking_id` khi PENDING, NULL otherwise; UNIQUE(pending_booking_id).

#### payment_webhook_receipts

`id`, `provider PaymentProvider`, `event_id CHAR(36) NOT NULL`, `payment_id FK payments`, `payload_hash BINARY(32) NOT NULL`, `outcome WebhookEventType`, `processed_at DATETIME(3) NOT NULL`, `created_at`; UNIQUE(provider, event_id).

#### refunds

`id`, `payment_id FK payments UNIQUE`, `provider_refund_id VARCHAR(100) UNIQUE`, `amount_vnd DECIMAL(18,0) NOT NULL`, `reason RefundReason`, `status RefundStatus DEFAULT 'PENDING'`, `attempt_count INT DEFAULT 0`, `next_attempt_at DATETIME(3)?`, `last_error_code VARCHAR(100)?`, `completed_at DATETIME(3)?`, `version INT`, timestamps.

Chỉ hoàn toàn phần trong v1; amount=payment amount.

### 2.7 Chat [PHASE7]

#### conversations

`id`, `room_id FK rooms`, `tenant_id FK users`, `host_id FK users`, `room_title VARCHAR(150)` _(snapshot khi tạo)_, `last_message_at DATETIME(3)?`, `last_message_preview VARCHAR(200)?`, `sequence BIGINT DEFAULT 0`, `version INT`, timestamps. UNIQUE(room_id, tenant_id).

`sequence` là sequence lớn nhất đã commit trong conversation, bắt đầu từ 0 khi chưa có tin. `room_title` lấy từ listing lúc mở hội thoại; đổi/ẩn/archive listing không thay tên lịch sử.

#### messages

`id`, `conversation_id FK conversations`, `sender_id FK users`, `content TEXT NOT NULL`, `client_message_id VARCHAR(36) NOT NULL`, `sequence BIGINT NOT NULL`, `content_type MessageContentType DEFAULT 'TEXT'`, `created_at`. UNIQUE(conversation_id, client_message_id), UNIQUE(conversation_id, sequence).

#### read_markers

PK(conversation_id, user_id), `last_read_sequence BIGINT NOT NULL DEFAULT 0`, `updated_at`. Marker chỉ tăng và không vượt `conversations.sequence`.

### 2.8 Notification & Admin [RECOVERED]

#### notifications

`id`, `user_id FK users`, `type NotificationType NOT NULL`, `title VARCHAR(200) NOT NULL`, `body VARCHAR(500)?`, `target_type VARCHAR(50)?`, `target_id VARCHAR(50)?`, `read BOOLEAN DEFAULT FALSE`, `created_at`. Index(user_id, read, created_at).

#### outbox_events

`id`, `aggregate_type VARCHAR(50) NOT NULL`, `aggregate_id VARCHAR(50) NOT NULL`, `event_type VARCHAR(100) NOT NULL`, `payload JSON NOT NULL`, `status OutboxStatus DEFAULT 'PENDING'`, `created_at`. Index(status, created_at).

#### audit_logs

`id`, `actor_id BIGINT? FK users`, `actor_type ActorType NOT NULL`, `action VARCHAR(100) NOT NULL`, `target_type VARCHAR(50) NOT NULL`, `target_id VARCHAR(50) NOT NULL`, `reason TEXT?`, `redacted_changes JSON NOT NULL`, `trace_id VARCHAR(64) NOT NULL`, `created_at`; append-only.

#### daily_listing_metrics

PK(listing_id, metric_date, event_type), `event_count BIGINT DEFAULT 0`, `updated_at`.

#### idempotency_keys

`id`, `idempotency_key VARCHAR(512) NOT NULL UNIQUE`, `status VARCHAR(20) DEFAULT 'PROCESSING'`, `response_status_code INT?`, `response_body TEXT?`, `expires_at DATETIME(3) NOT NULL`, `created_at`.

### 2.9 Lưu trữ và xóa dữ liệu

- Listing đã dùng: archive, không xóa cứng.
- Booking, Payment, Refund, Review, status history không cascade theo User/Room.
- Delete account chỉ được nhận khi không có: Booking PENDING/APPROVED/CONFIRMED, Viewing REQUESTED/CONFIRMED, Payment PENDING, case mở, refund chưa SUCCEEDED, Room RENTED/HELD, report chưa giải quyết do user tạo.
- POST delete-account trả status PENDING. Job tối đa 50 yêu cầu/lượt, mặc định 30 giây; kiểm tra lại blockers trong transaction riêng. PENDING/PROCESSING chặn sử dụng tài khoản, trừ xem/gửi lại yêu cầu xóa và logout. Chỉ request FAILED mới được gửi lại, dùng cùng hàng theo user_id.
- Trước anonymize: archive tin, thu hồi sessions, vô hiệu DeviceToken, xóa Wishlist và mã xác minh, thay email/tên/password, xóa phone/avatar/legacy refresh token/roles. Không hard-delete user hoặc lịch sử giao dịch. COMPLETED ghi completed_at và bỏ reason; blockers phát sinh sau nhận yêu cầu đưa request về FAILED/DELETION_BLOCKED, không ẩn danh một phần.
- GET delete-account trả AccountDeletionResponse gồm id, status, failure_code, created_at, completed_at; 404 nếu chưa có. Sau khi hoàn tất, bearer cũ không còn giải được tài khoản đã đổi email; không tiếp tục dùng endpoint này để đọc lịch sử.
- Retention: media chưa gắn 24h; token hết hạn và snapshot idempotency COMPLETED hết hạn được dọn; push SENT/FAILED/SKIPPED quá 30 ngày được dọn. Mỗi nhóm tối đa 200 hàng/lượt mặc định 60 giây. Không dọn PROCESSING hoặc lịch sử audit/case/payment/booking/refund.

---

## §3. DTO Definitions

Tất cả field bắt buộc trừ field có `?`. PATCH chỉ cho sửa whitelist. Field không nêu không được ghi từ client.

### Auth DTOs

| DTO                            | Trường và điều kiện |
| ------------------------------ | --- |
| RegisterRequest                | email:Email, password:Text(12,128) không trim, full_name:Text(2,100); không role |
| LoginRequest                   | email:Email, password:string, installation_id:Uuid, device_name:Text(1,100) |
| RefreshRequest                 | refresh_token:string 43..512 |
| EmailRequest                   | email:Email |
| OneTimeTokenRequest            | token:string 43..512 |
| ProfileUpdateRequest           | full_name?:Text(2,100), phone?:Text(9,15) `^[0-9]+$`, avatar_url?:Url(0,512) |
| NotificationPreferencesRequest | expected_version, transaction_push:boolean, transaction_email:boolean, chat_push:boolean, recommendation_push:boolean |
| DeviceTokenRequest             | token:Text(1,4096), platform:ANDROID, device_name:Text(1,100), installation_id:Uuid |
| DeletionRequest                | current_password:string, reason?:Text(0,1000) |

### Catalog DTOs

| DTO                  | Trường và điều kiện |
| -------------------- | --- |
| RoomCreateRequest    | unit_code:Text(1,30) `[A-Z0-9_-]+`, room_type:RoomType, area_m2:number 2..1000, max_occupants:integer 1..10, address:AddressInput, location:GeoPoint, amenity_ids:Id[] tối đa 30 |
| RoomPatch            | expected_version; room_type?, area_m2?, max_occupants?, address?, location?, amenity_ids? cùng kiểu RoomCreate; unit_code và host không sửa được |
| AddressInput         | line:Text(5,300), province_code:Text(1,20), province_name:Text(1,100), ward_code?:Text(1,20)\|null, ward_name?:Text(1,100)\|null; ward_code và ward_name cùng có hoặc cùng null |
| GeoPoint             | latitude:number (-90..90), longitude:number (-180..180) |
| ListingWriteRequest  | title?:Text(0,150) mặc định "", description?:Text(0,5000) mặc định "", rent_vnd?:Money min 0, deposit_vnd?:Money min 0, expected_version?:int |
| FeeInput             | fee_code:Text(1,50), fee_mode:Text(1,20) default "FIXED", amount_vnd:Money min 0, unit_name?:Text(0,50), note?:Text(0,500), sort_order:int default 0 |
| GeocodeRequest       | address:Text(5,300) |

### Booking DTOs [RECOVERED]

| DTO                  | Trường và điều kiện |
| -------------------- | --- |
| BookingCreateRequest | room_id:Id, desired_move_in?:Date (future), occupant_count:int 1..10, note?:Text(0,1000) — Idempotency-Key required |
| BookingActionRequest | reason?:Text(0,1000) |
| CaseCreateRequest    | booking_id:Id, type:BookingCaseType, description:Text(10,5000) |
| CaseResolveRequest   | decision:BookingCaseDecision, resolution_note:Text(10,5000) |

### Chat DTOs [PHASE7]

| DTO                  | Trường và điều kiện |
| -------------------- | --- |
| ConversationCreateRequest | room_id:Id |
| MessageSendFrame     | conversation_id:Id, content:Text(1,5000) không toàn khoảng trắng, client_message_id:Uuid, content_type?:TEXT default TEXT |
| MessageCursorQuery   | limit:int 1..50 default 20, before_sequence?:long > 0, after_sequence?:long >= 0; không truyền cả hai |
| ReadMarkerRequest    | last_read_sequence:long >= 0 |
| ConversationDTO      | id, room_id, tenant_id, host_id, room_title:string, last_message_at:DateTime\|null, last_message_preview:string\|null, sequence:long, last_read_sequence:long, unread_count:long, created_at:DateTime |
| MessageDTO           | id, conversation_id, sender_id, content:string, client_message_id:Uuid, sequence:long >= 1, content_type:TEXT, created_at:DateTime |
| MessagePageDTO       | data:MessageDTO[], next_cursor:long\|null, has_more:boolean; data luôn ASC theo sequence |
| ReadMarkerDTO        | conversation_id, user_id, last_read_sequence:long, updated_at:DateTime |

ID và sequence trả JSON integer (`int64`); field dùng `snake_case`. `last_read_sequence` và `unread_count` trong ConversationDTO thuộc người gọi, không phải của thành viên còn lại. HTTP danh sách conversation có envelope.data là PageResponse: `{data, total_elements, total_pages, current_page, page_size}`.

CHAT01 trả HTTP 201 cho cả tạo mới và mở lại. CHAT02 dùng `page >= 0` default 0, `size` 1..50 default 20. CHAT01 và CHAT05 yêu cầu `Idempotency-Key` không rỗng/toàn khoảng trắng, dài tối đa 128 ký tự. Replay trả snapshot response đã lưu, có thể cũ hơn trạng thái hiện tại; client GET lại khi cần trạng thái mới. Trạng thái tài khoản và quyền thành viên vẫn được kiểm tra trước khi trả response cache.

Trong MessagePageDTO, `has_more` mô tả hướng đang yêu cầu: tin cũ hơn cho truy vấn mặc định/`before_sequence`, tin mới hơn cho `after_sequence`. Với truy vấn mặc định/`before_sequence`, `next_cursor` bằng sequence nhỏ nhất trong `data` nếu còn tin cũ hơn, nếu không là null. Với `after_sequence`, `next_cursor` luôn null; dùng sequence cuối của `data` làm `after_sequence` kế tiếp khi đồng bộ tin mới.

---

## §4. Enum Definitions

### Auth Enums

| Enum | Giá trị |
| --- | --- |
| UserStatus | `ACTIVE`, `SUSPENDED`, `DELETED` |
| RoleName | `ROLE_TENANT`, `ROLE_HOST`, `ROLE_ADMIN` |
| OneTimeTokenPurpose | `EMAIL_VERIFY`, `PASSWORD_RESET` |
| DeletionStatus | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| DeliveryStatus | `PENDING`, `SENT`, `FAILED` |

### Catalog Enums

| Enum | Giá trị |
| --- | --- |
| RoomType | `PHONG_TRO`, `CHUNG_CU_MINI`, `HOMESTAY`, `KTX`, `NGUYEN_CAN` |
| RoomAvailability | `AVAILABLE`, `HELD`, `RENTED` |
| ListingStatus | `DRAFT`, `PENDING_REVIEW`, `PUBLISHED`, `HIDDEN`, `REJECTED`, `SUSPENDED`, `EXPIRED`, `ARCHIVED` |
| FeeMode | `FIXED`, `PER_PERSON`, `METERED` |

**ListingStatus Sets:**
- `EDITABLE`: {DRAFT, HIDDEN, REJECTED, EXPIRED}
- `SUBMITTABLE`: {DRAFT, HIDDEN, REJECTED, EXPIRED}

### Media Enums [RECOVERED]

| Enum | Giá trị |
| --- | --- |
| MediaPurpose | `ROOM_PHOTO`, `ROOM_VIDEO`, `AVATAR`, `CHAT_IMAGE`, `REPORT_EVIDENCE` |
| MediaStatus | `READY`, `ATTACHED`, `DELETED` |

### Interaction Enums [RECOVERED]

| Enum | Giá trị |
| --- | --- |
| ViewingSlotStatus | `OPEN`, `BOOKED`, `CANCELLED` |
| ViewingStatus | `REQUESTED`, `CONFIRMED`, `COMPLETED`, `CANCELLED_BY_TENANT`, `CANCELLED_BY_HOST`, `NO_SHOW` |
| ReportStatus | `PENDING`, `REVIEWING`, `RESOLVED`, `DISMISSED` |

### Booking Enums [RECOVERED]

| Enum | Giá trị |
| --- | --- |
| BookingStatus | `PENDING`, `APPROVED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `EXPIRED` |
| BookingCaseType | `DEPOSIT_DISPUTE`, `HANDOVER_OVERDUE`, `NO_SHOW`, `OTHER` |
| CaseStatus | `OPEN`, `IN_REVIEW`, `RESOLVED`, `CLOSED` |
| BookingCaseDecision | `REFUND_TENANT`, `FORFEIT_DEPOSIT`, `SPLIT`, `NO_ACTION` |
| ActorType | `SYSTEM`, `TENANT`, `HOST`, `ADMIN` |

### Payment Enums [RECOVERED]

| Enum | Giá trị |
| --- | --- |
| PaymentProvider | `MOCK_SANDBOX`, `VNPAY`, `MOMO` |
| PaymentStatus | `PENDING`, `SUCCEEDED`, `FAILED`, `EXPIRED` |
| RefundReason | `HOST_REJECTED`, `BOOKING_EXPIRED`, `CASE_DECISION`, `SYSTEM_ERROR` |
| RefundStatus | `PENDING`, `SUCCEEDED`, `FAILED` |
| WebhookEventType | `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `REFUND_SUCCESS`, `REFUND_FAILED` |

### Chat Enums [RECOVERED]

| Enum | Giá trị |
| --- | --- |
| MessageContentType | `TEXT`, `IMAGE`, `SYSTEM` |
| NotificationType | `BOOKING_STATUS`, `PAYMENT_STATUS`, `MESSAGE`, `VIEWING_STATUS`, `LISTING_STATUS`, `SYSTEM` |
| OutboxStatus | `PENDING`, `SENT`, `FAILED` |

Phase 7 chỉ nhận/gửi `TEXT`. `IMAGE` và `SYSTEM` là giá trị dành cho phần phát triển tiếp theo; client gửi hai loại này phải nhận lỗi validation, không được lưu như TEXT hoặc tin hệ thống giả.

---

## §5. Business Rules

### 5.1 BR-01 Danh tính phòng và vai trò

1. Một Room là một phòng vật lý, chỉ có một suất thuê tại một thời điểm.
2. Một user được có TENANT+HOST. Chọn chế độ trên Android chỉ đổi menu; quyền từ server.
3. Cấm chủ đặt phòng, hẹn xem, mở hội thoại tự nhắn hoặc đánh giá phòng của chính mình → `SELF_ACTION_NOT_ALLOWED`.
4. Room.host_id và unit_code bất biến trong v1.
5. Room với APPROVED/CONFIRMED không được sửa thông số. Khi Room chưa giữ chỗ, sửa thông số → tăng `terms_version` → Listing về DRAFT. Snapshot Booking cũ không đổi.
6. ACTIVE không đồng nghĩa email đã xác minh. UI không dùng nhãn "phòng đảm bảo" chỉ dựa vào duyệt tin.

### 5.2 BR-02 Vòng đời tin đăng (Listing Lifecycle)

| Từ | Hành động | Sang | Điều kiện |
| --- | --- | --- | --- |
| (new) | createListing | DRAFT | Host tạo listing cho room chưa có listing |
| DRAFT | submit | PENDING_REVIEW | Đã có title + rent_vnd |
| HIDDEN | submit | PENDING_REVIEW | Host muốn đăng lại |
| REJECTED | submit | PENDING_REVIEW | Host sửa rồi gửi lại |
| EXPIRED | submit | PENDING_REVIEW | Host gia hạn |
| PENDING_REVIEW | approve | PUBLISHED | Admin duyệt; set published_at, expires_at = +30 ngày |
| PENDING_REVIEW | reject | REJECTED | Admin từ chối kèm lý do |
| PUBLISHED | hide | HIDDEN | Host tự ẩn |
| PUBLISHED | suspend | SUSPENDED | Admin xử phạt |
| PUBLISHED | expire (job) | EXPIRED | expires_at <= now |
| DRAFT, HIDDEN, REJECTED, EXPIRED | edit | giữ nguyên | Host sửa nội dung |

**EDITABLE set:** {DRAFT, HIDDEN, REJECTED, EXPIRED}  
**SUBMITTABLE set:** {DRAFT, HIDDEN, REJECTED, EXPIRED}

### 5.3 BR-03 Tìm kiếm và lọc [RECOVERED]

- Mặc định chỉ hiện Listing PUBLISHED + Room AVAILABLE + chưa hết hạn.
- Bộ lọc: query text (title/description), room_type, min/max rent_vnd, min/max area_m2, province_code, amenity_ids, sort (rent_asc, rent_desc, newest, nearest).
- Geo search: bán kính km từ tọa độ.

### 5.4 BR-04 Chi tiết phòng [RECOVERED]

- Chỉ trả listing PUBLISHED cho public (getListingDetail kiểm tra status).
- Trả kèm: Room info, fees, amenities, host summary, review stats.
- Room availability hiển thị nhưng không ẩn listing (xem được trạng thái HELD/RENTED).

### 5.5 BR-05 Hẹn xem phòng [RECOVERED]

- Tenant (verified) đặt lịch xem từ slot OPEN do host tạo.
- Slot phải >= 24h trong tương lai.
- Mỗi tenant chỉ có 1 viewing REQUESTED/CONFIRMED cho 1 phòng cùng lúc.
- Host confirm hoặc cancel.
- Tenant có thể cancel trước 2h.

### 5.6 BR-06 Booking Lifecycle [RECOVERED]

| Từ | Hành động | Sang | Điều kiện |
| --- | --- | --- | --- |
| (new) | createBooking | PENDING | Room AVAILABLE; tenant != host; verified; snapshot terms |
| PENDING | approve (host) | APPROVED | set hold_expires_at = +48h |
| PENDING | reject (host) | CANCELLED | set last_reason |
| PENDING | expire (job) | EXPIRED | request_expires_at <= now |
| PENDING | cancel (tenant) | CANCELLED | tenant hủy |
| APPROVED | confirmDeposit | CONFIRMED | payment SUCCEEDED; set handover_due_at |
| APPROVED | expire (job) | EXPIRED | hold_expires_at <= now; refund if paid |
| APPROVED | cancel | CANCELLED | either party; refund if paid |
| CONFIRMED | handoverBoth | COMPLETED | cả hai xác nhận; completed_at |
| CONFIRMED | expire (job) | → open case | handover_due_at quá hạn |
| CONFIRMED | cancel | CANCELLED | only via case resolution |

### 5.7 BR-07 Concurrency & Optimistic Locking [RECOVERED]

- `@Version` bắt buộc trên: Room, Listing, Booking, Payment, Review.
- 2 tenant đặt cùng 1 phòng → generated column `active_room_id` UNIQUE → chỉ 1 APPROVED/CONFIRMED tại một thời điểm.
- `OptimisticLockException` → 409 CONFLICT.

### 5.8 BR-08 Payment Sandbox [RECOVERED]

- Mock payment provider, `is_test = true` cho v1.
- Tạo payment → PENDING, expires trong 15 phút.
- Webhook `PAYMENT_SUCCESS` → allocate cho booking → CONFIRMED.
- Late settlement: payment thành công sau booking hết hạn → auto refund.
- Dedup webhook bằng UNIQUE(provider, event_id).

### 5.9 BR-09 Media Upload [RECOVERED]

- Upload qua MultipartFile, BE validate magic bytes + kích thước.
- Max: 10 MB/ảnh, 50 MB/video. Formats: JPEG, PNG, WEBP, MP4.
- Max 15 ảnh + 1 video per room.
- Sinh UUID storage key, upload lên MinIO.
- Status: READY → ATTACHED khi gắn vào resource.
- Job dọn: xóa READY > 24h.

### 5.10 BR-10 Review [RECOVERED]

- Chỉ tạo review sau Booking COMPLETED.
- 1 booking = 1 review (UNIQUE booking_id).
- Rating 1–5, comment optional.
- Reviewer phải là tenant của booking đó.
- Admin có thể hide/restore review kèm lý do.

### 5.11 BR-11 Chat [PHASE7]

1. Conversation giữa người mở hội thoại (tenant) và host của một room, UNIQUE(room_id, tenant_id). Cấm tự mở với phòng của mình (`SELF_ACTION_NOT_ALLOWED`). Gọi mở lại trả conversation hiện có; không tạo một thread mới.
2. Chỉ tạo conversation mới khi listing PUBLISHED và `expires_at > now`. `expires_at = null` không đủ điều kiện tạo mới. Hội thoại đã tồn tại vẫn đọc/gửi được khi listing ẩn, hết hạn hoặc archive, miễn người thao tác còn ACTIVE và là thành viên; gửi tin vẫn yêu cầu verified.
3. Lưu `room_title` snapshot khi tạo. Hai thành viên cố định theo conversation; client không được gán `sender_id`, host, tenant, sequence hoặc timestamp.
4. Gửi tin TEXT qua STOMP. Server cấp sequence tăng trong mỗi conversation và lưu message, sequence, last_message_at/preview trong cùng transaction. Chỉ phát message và ACK sau commit; rollback không phát dữ liệu chưa lưu.
5. `client_message_id` là UUID do client tạo và giữ nguyên khi retry; server chuẩn hóa UUID về chữ thường. Trùng `(conversation_id, client_message_id)` với cùng sender, content và content_type trả lại message đã lưu qua ACK, không tăng sequence và không phát lại message mới. Trùng khóa nhưng khác sender/nội dung/loại tin trả conflict 409; không ghi đè bản cũ.
6. History dùng cursor sequence loại trừ biên: `before_sequence = s` chỉ lấy sequence `< s`; `after_sequence = s` chỉ lấy `> s`. Không truyền cả hai. Không cursor: lấy tối đa `limit` tin mới nhất. before: lấy tối đa `limit` tin gần biên nhất về phía cũ. after: lấy tối đa `limit` tin đầu tiên phía mới. Mọi kết quả `data` đều ASC theo sequence, kể cả window mới nhất.
7. Marker riêng từng user/conversation. Request vượt sequence mới nhất bị từ chối; request thấp hơn marker hiện tại giữ nguyên marker. Cập nhật đồng thời phải giữ tính đơn điệu. `unread_count` đếm tin do thành viên còn lại gửi sau marker, không tính tin của chính người gọi.
8. Broker realtime chỉ phục vụ session đang kết nối. Sau reconnect, client dùng REST `after_sequence` để khôi phục tin đã commit và hợp nhất theo `(conversation_id, sequence)` hoặc `id`. ACK ứng dụng trên `chat.acks` không phải STOMP `ACK` frame và không xác nhận người nhận đã đọc.

Các quyết định có giới hạn của lát cắt này được ghi tại CONTRACT_GAPS, mục Phase 7; không bao gồm `around_sequence`, chat ảnh, push offline hoặc đồng bộ broker nhiều instance. Flyway V11 tạo ba bảng chat sau V1–V10 hiện có. Hướng dẫn tích hợp client: [backend/CHAT.md](../backend/CHAT.md).

### 5.12 BR-12 Idempotency

- Bắt buộc cho **tất cả POST mutation endpoints**.
- Key = `Idempotency-Key` header + `user_id` + `request_path` + `body_hash`.
- Lần 1: lưu PROCESSING → xử lý → COMPLETED + lưu response.
- Lần 2 trùng key: trả response lần 1.
- Key PROCESSING đang xử lý → 409 CONFLICT.
- TTL: 24h.

### 5.13 BR-13 Scheduled Jobs

| Job | Tần suất | Hành động |
| --- | --- | --- |
| Expire Booking PENDING | Mỗi phút | `request_expires_at <= NOW` → EXPIRED |
| Expire Booking APPROVED | Mỗi phút | `hold_expires_at <= NOW` → EXPIRED, giải phóng Room |
| Handover Overdue | Mỗi phút | CONFIRMED + `handover_due_at` quá hạn → mở BookingCase |
| Listing Expiration | Mỗi giờ | PUBLISHED + `expires_at <= NOW` → EXPIRED |
| Orphan Media Cleanup | Hàng ngày | READY > 24h → xóa khỏi storage + DB |
| Outbox Publisher | Mỗi 5 giây | PENDING → gửi Kafka → SENT |

### 5.14 BR-14 Report [RECOVERED]

- User (verified) báo cáo: listing, review, message, user.
- reason_code: SPAM, SCAM, INAPPROPRIATE, DUPLICATE, OTHER.
- Admin review và resolve/dismiss.

### 5.15 BR-15 Booking Cases (Dispute) [RECOVERED]

- Mở case khi có tranh chấp về cọc, bàn giao, no-show.
- Chỉ 1 case OPEN/IN_REVIEW per booking.
- Admin resolve với decision: REFUND_TENANT, FORFEIT_DEPOSIT, SPLIT, NO_ACTION.

---

## §6. Security

### 6.1 SEC-01 Authentication

- JWT HMAC-SHA512 (HS512) — single secret key.
- `JWT_SECRET` bắt buộc, Base64 hợp lệ và giải mã được ít nhất 64 byte. Không có secret mặc định cho ứng dụng.
- Access token: chứa `token_type: "access"`, `user_id`, roles, user info; TTL mặc định 900 giây. HTTP và STOMP đối chiếu ID với tài khoản hiện tại để email đăng ký lại không nhận quyền từ token cũ. Token trước v1.6 có `user.id` vẫn được kiểm tra ID đến khi hết hạn.
- Refresh token: chứa `token_type: "refresh"`, không có roles/user; TTL mặc định 7 ngày.
- **SecurityConfig phải reject token có `token_type != "access"` trên resource server endpoints.**
- Auth rate limit trong bộ nhớ một backend, cửa sổ mặc định 60 giây; kiểm tra cả IP kết nối và identity (email/token/tài khoản), không tin `X-Forwarded-For` thô. Giới hạn IP/identity: login 60/10, register 20/3, refresh 120/30, verify-email 60/10, resend 20/1. Vượt giới hạn trả 429 `RATE_LIMITED` cùng `Retry-After`; body auth trên 16 KiB trả 413 `REQUEST_TOO_LARGE`. Cấu hình qua `homely.auth.rate-limit.*`.

### 6.2 SEC-02 Session Management

- Refresh tokens hash SHA-256 lưu bảng `refresh_tokens`.
- Hỗ trợ multi-device sessions (mỗi installation_id 1 session).
- Token rotation: refresh → revoke cũ + cấp mới.
- Logout: revoke refresh token hiện tại.
- Logout-all: revoke tất cả sessions.

### 6.3 SEC-03 Email Verification

- OTT hash SHA-256, TTL 15 phút, single-use.
- Gửi SMTP đồng bộ với timeout. Đăng ký gồm user + token + gửi thư trong một transaction; SMTP lỗi trả 503 `EMAIL_UNAVAILABLE` và rollback để cùng email có thể thử lại. Resend lỗi không làm mất token cũ. Chưa triển khai email outbox; SMTP không thể commit nguyên tử cùng MySQL nếu database lỗi sau khi thư đã gửi.
- Một số action yêu cầu verified: booking, viewing, review, chat send, enable host.

### 6.4 SEC-04 Permission Matrix

| Hành động | Guest | Tenant | Host | Admin |
| --- | --- | --- | --- | --- |
| Xem public/search/map | Có | Có | Có | Có |
| Sửa hồ sơ, phiên, cài đặt | Không | Chính mình | Chính mình | Chính mình |
| Wishlist, booking, viewing | Không | Chính mình + verified | Nếu đồng thời TENANT | Không mạo danh |
| Đăng/sửa tin | Không | Không | Phòng/tin của mình | Duyệt/ẩn qua admin |
| Approve/reject thuê | Không | Không | Chủ đúng Booking | Chỉ case resolution |
| Handover confirm | Không | Bên thuê | Chủ đúng Booking | Kết luận case + audit |
| Review | Không | Booking COMPLETED | Không tự đánh giá | Hide/restore + lý do |
| Chat | Không | Thành viên | Thành viên | Message bị report |
| Metrics, suspend | Không | Không | Không | Có |

### 6.5 SEC-05 Kênh truyền và STOMP

- HTTPS/WSS ở release/demo. Cleartext chỉ local emulator (`10.0.2.2`) debug.
- Không cookie-auth browser v1.
- Spring bảo vệ handshake + inbound SEND/SUBSCRIBE; interceptor kiểm tra JWT/membership.
- Rate limit cả REST và STOMP.
- Maps API key theo môi trường/package/signing. Secrets chỉ server.

#### Giao thức STOMP Phase 7

- Native WebSocket handshake tại `/ws`, cùng host với API, dùng HTTP header `Authorization: Bearer <access_token>`. Không truyền token qua URL; không dựa vào cookie hay CONNECT header để thay thế handshake. Dùng WSS ngoài môi trường local.
- Chỉ nhận access JWT hợp lệ; refresh JWT bị từ chối. Session giữ danh tính từ handshake. Server kiểm tra thời hạn access token và trạng thái ACTIVE cho thao tác inbound và trước khi chuyển broker MESSAGE tới session; session không còn hợp lệ bị đóng. Hết hạn phải lấy access token mới rồi reconnect.
- Client chỉ SEND JSON `MessageSendFrame` tới `/app/chat.send`.
- Client chỉ SUBSCRIBE ba destination riêng: `/user/queue/chat.messages`, `/user/queue/chat.acks`, `/user/queue/chat.errors`. Không subscribe broker queue/topic chung hoặc đường dẫn user khác; không SEND trực tiếp tới broker destinations.
- `chat.messages` dành cho message đã commit; `chat.acks` dành cho kết quả gửi/retry của sender. Cả hai trả `MessageDTO` trực tiếp; client đối chiếu ACK bằng `client_message_id`. Lỗi domain/validation trên `chat.errors` trả `ProblemDTO`, không cam kết chứa `client_message_id` để ghép với lần gửi. Payload được mô tả tại OpenAPI `x-stomp`. Lỗi CONNECT/SUBSCRIBE hoặc xác thực transport có thể trả STOMP ERROR và đóng connection.
- Membership kiểm tra ở mỗi lần thao tác conversation; biết ID hoặc có role HOST/ADMIN không đủ quyền. User destinations dùng principal do server xác thực, không dùng tên user từ payload client.
- Rate limit theo tài khoản, lưu trong bộ nhớ của một backend: REST mặc định 120 request/phút (`homely.chat.rest-requests-per-minute`), STOMP SEND mặc định 60 lần/phút (`homely.chat.messages-per-minute`), bao gồm retry. REST vượt giới hạn trả 429 `RATE_LIMITED` với `Retry-After: 60`.
- Simple broker chạy trong process, chỉ triển khai một backend instance trong Phase 7. Không cam kết push bền vững/offline; REST history là nguồn khôi phục sau mất kết nối. Origins WebSocket cấu hình bằng `homely.chat.allowed-origins`; mặc định same-origin, native client không gửi Origin được hỗ trợ.

### 6.6 SEC-06 Password Storage

- Encoder: Argon2id (contract gốc) / BCrypt (tạm chấp nhận cho v1).
- Không trim password, min 12 chars.

---

## §7. Error Format

### 7.1 ProblemDTO Schema

Trong HTTP, các trường ProblemDTO nằm ở `RestResponse.data`; response dùng `application/json`. Đây là các trường Problem Details trong envelope, không phải document RFC 9457 ở root. STOMP error payload giữ ProblemDTO trực tiếp.

| Field | Kiểu | Quy tắc |
| --- | --- | --- |
| type | string | `urn:problem:` + code lowercase, `_` → `-` |
| title | string | Tên lỗi ngắn ổn định |
| status | integer | Trùng HTTP response status |
| detail | string | Thông báo dễ hiểu; không stack trace/SQL/token |
| instance | string | Path request, không query chứa token |
| code | string | UPPER_SNAKE_CASE |
| field_errors | array | Luôn có; `[]` nếu không lỗi theo field |
| trace_id | string | Server tạo |
| timestamp | DateTime | UTC server |

### 7.2 FieldError

| Field | Kiểu | Quy tắc |
| --- | --- | --- |
| field | string | Tên trường (dot notation cho nested) |
| reason | string | Lý do dễ hiểu; không chứa giá trị bị từ chối, token hoặc thông tin nội bộ |

### 7.3 Error Code Catalog

| HTTP | Code | Mô tả |
| --- | --- | --- |
| 400 | VALIDATION_FAILED | Input validation lỗi |
| 400 | INVALID_JSON | JSON parse error |
| 401 | AUTHENTICATION_REQUIRED | Chưa cung cấp access token |
| 401 | INVALID_ACCESS_TOKEN | Access token sai hoặc hết hạn |
| 401 | INVALID_CREDENTIALS | Sai email/password |
| 400 | VALIDATION_FAILED | Request refresh sai, session đã thu hồi/hết hạn hoặc installation không khớp |
| 403 | FORBIDDEN | Không có quyền |
| 403 | EMAIL_NOT_VERIFIED | Email chưa xác minh |
| 403 | ACCOUNT_INACTIVE | Tài khoản không ACTIVE hoặc bị suspended |
| 404 | RESOURCE_NOT_FOUND | Không tìm thấy resource |
| 405 | METHOD_NOT_ALLOWED | HTTP method không được hỗ trợ; có header Allow |
| 406 | NOT_ACCEPTABLE | Media type response được yêu cầu không được hỗ trợ |
| 415 | UNSUPPORTED_MEDIA_TYPE | Content-Type của request không được hỗ trợ |
| 409 | CONFLICT | Optimistic lock / duplicate |
| 409 | ROOM_NOT_AVAILABLE | Phòng đã có người giữ |
| 409 | TERMS_CHANGED | Điều khoản đã thay đổi |
| 409 | LISTING_EXISTS | Room đã có listing |
| 409 | SELF_ACTION_NOT_ALLOWED | Tự thao tác trên resource mình sở hữu |
| 409 | INVALID_STATUS_TRANSITION | Chuyển trạng thái không hợp lệ |
| 422 | BUSINESS_RULE_VIOLATION | Vi phạm business rule |
| 429 | RATE_LIMITED | Quá giới hạn request |
| 500 | INTERNAL_ERROR | Lỗi server (ẩn chi tiết) |

---

## §8. Pagination

```json
{
  "statusCode": 200,
  "message": "OK",
  "data": {
    "data": [],
    "total_elements": 0,
    "total_pages": 0,
    "current_page": 0,
    "page_size": 20
  }
}
```

- Page 0-based.
- Size default 20, max 50.
- Chat messages dùng cursor pagination (before_sequence / after_sequence).

---

## §9. References

| Link | Mục đích |
| --- | --- |
| [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html) | Java/dependency baseline |
| [Hibernate optimistic locking](https://docs.hibernate.org/orm/6.6/userguide/html_single/#locking-optimistic) | @Version, không phải pessimistic |
| [MySQL 8.4 Spatial Index](https://dev.mysql.com/doc/refman/8.4/en/creating-spatial-indexes.html) | Spatial index + khoảng cách |
| [Spring Security 6.5 JWT](https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html) | JWT Resource Server |
| [Spring Security 6.5 WebSocket](https://docs.spring.io/spring-security/reference/6.5/servlet/integrations/websocket.html) | WebSocket security |
| [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457.html) | Error response format |
| [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) | Argon2id parameters |
| [OWASP File Upload](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html) | Upload validation practices |

---

*Kết thúc PROJECT_CONTRACT v1.6. Mọi thay đổi giao thức phải cập nhật phiên bản contract.*
> **Cập nhật 03/10/2026:** Hiện trạng triển khai và các quyết định thay thế mô tả cũ nằm tại [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). FCM dùng MySQL outbox; hướng dẫn tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Các mốc kiểm chứng trước đây là lịch sử, không phải nghiệm thu bản hiện tại.
