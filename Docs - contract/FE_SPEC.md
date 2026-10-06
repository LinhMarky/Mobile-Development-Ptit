# FRONTEND SPECIFICATIONS (FE_SPEC)

**Vai trò:** Tài liệu hướng dẫn thiết kế, kiến trúc và UI Behavior cho ứng dụng Android Native.  
**Tham chiếu Business Rules & API:** Mọi logic nghiệp vụ và cấu trúc dữ liệu phải tuân theo `PROJECT_CONTRACT.md`.

**HTTP hiện tại (v1.6):** ApiClient dùng ApiEnvelope để lấy payload từ data một lần, bao gồm login/refresh/upload. UI nhận DTO/page; lỗi đọc data.detail/code. ChatSocket giữ payload STOMP trực tiếp. Auth có thể trả 429 kèm Retry-After hoặc 503 EMAIL_UNAVAILABLE. Sau yêu cầu xóa, 403 ACCOUNT_DELETION_PENDING chặn thao tác thường; GET /auth/delete-account vẫn trả trạng thái. Khi xóa hoàn tất, token không còn hợp lệ. Backend đã có API trạng thái; màn hình theo dõi chuyên biệt chưa được bổ sung trong lượt hoàn thiện backend này.

---

## 1. FE Architecture

- **Platform:** Android Native (Java)
- **UI Framework:** XML Layouts + ViewBinding
- **Networking:** Retrofit + OkHttp
- **Image Loading:** Glide
- **Maps:** Google Maps SDK
- **Realtime:** STOMP over WebSocket (dùng thư viện STOMP client hỗ trợ Java)
- **Architecture Pattern:** MVVM (Model-View-ViewModel)
- **State Management:** LiveData hoặc StateFlow
- **Naming Conventions:**
  - XML IDs: `snake_case` với tiền tố thành phần (`btn_submit`, `tv_title`, `rv_list`, `edt_search`).
  - Java Classes: `PascalCase`.

---

## 2. Authentication & Session

- **Flow cơ bản:** Splash → Kiểm tra Token → (Có Token hợp lệ) → Main / (Không Token) → Khách (Guest mode) hoặc Login.
- **Lưu trữ Token:** Access Token (thời gian sống ngắn) và Refresh Token (sống dài) lưu trong EncryptedSharedPreferences.
- **Xử lý 401 (TOKEN_EXPIRED):**
  - Retrofit Authenticator tự động lấy `refresh_token` gọi API `AUTH03`.
  - Nếu thành công, lưu token mới và retry request gốc với Idempotency-Key/client_message_id nguyên vẹn.
  - Nếu thất bại (`401 INVALID_REFRESH_TOKEN`), tự động xóa local session, đẩy user về màn hình Login và hiển thị toast thông báo "Phiên đăng nhập hết hạn".
- **Xử lý 403 (EMAIL_NOT_VERIFIED):**
  - Đẩy user sang `SCREEN-AUTH-VERIFY`, không tự động retry request.
- **Logout:** Gọi API `AUTH04`, xóa token local, ngắt STOMP connection, hủy subscribe FCM, điều hướng về Guest mode.

---

## 3. Navigation & User Flows

- **App Structure (Bottom Navigation):**
  - **Khám phá** (Tìm kiếm, Bản đồ)
  - **Đã lưu** (Wishlist)
  - **Tin nhắn** (Danh sách Conversation)
  - **Lịch trình** (Viewings, Bookings)
  - **Cá nhân** (Profile, Switch Host/Tenant mode)

- **Chế độ Host / Tenant:**
  - Nút chuyển chế độ ở tab Cá nhân.
  - Đổi chế độ = Đổi UI Bottom Navigation. Không phải đổi token (quyền được BE tự động cấp nếu User là Host hợp lệ).

---

## 4. Screen Catalog & Screen Specifications

Mọi màn hình đều phải tuân thủ xử lý 4 trạng thái UI: `Loading`, `Content`, `Empty`, `Error/Offline`.

### SCREEN-SEARCH-LIST (Danh sách tìm kiếm)
- **Purpose:** Hiển thị phòng trống theo bộ lọc.
- **Actor:** Guest, Tenant.
- **Entry points:** Bottom Nav (Khám phá).
- **API:** `CAT05 (GET /listings)`.
- **UI Sections:**
  - Thanh tìm kiếm (Text, Quận/Huyện).
  - Nút Filter (mở Modal lọc).
  - Danh sách thẻ phòng (RecyclerView) chứa giá, diện tích, location, hình cover.
- **Actions:**
  - Chạm vào thẻ → `SCREEN-ROOM-DETAIL`.
  - Bấm icon tim → Gọi `CAT11` (Save) / `CAT12` (Unsave).
- **Business Rule:** Source: PROJECT_CONTRACT BR-03.
- **Error Handling:** 422 Filter lỗi (hiện snackbar). Mất mạng hiện layout offline.

### SCREEN-ROOM-DETAIL (Chi tiết phòng)
- **Purpose:** Xem chi tiết toàn bộ, phí, cọc, tiện ích, review.
- **Actor:** Guest, Tenant.
- **API:** `CAT07 (GET /listings/{id})`.
- **UI Sections:**
  - Carousel ảnh, Nút Save, Nút Share.
  - Thông tin giá, diện tích, rule.
  - Danh sách fee chi tiết (tính tổng minh họa nếu có occupant_count).
  - Chủ phòng (Avatar, Tên, Rate).
  - Bản đồ mini.
  - Call To Action (CTA): "Hẹn xem" (Nửa phụ) và "Yêu cầu thuê" (Nửa chính).
- **Business Rule:** Source: PROJECT_CONTRACT BR-04.
- **CTA Conditions:** Nếu `availability != AVAILABLE`, disable CTA và ghi "Đang giữ / Đã thuê".

### SCREEN-VIEWING-SCHEDULE (Hẹn xem)
- **Purpose:** Tenant chọn slot thời gian rảnh của Host để đặt hẹn.
- **Actor:** Tenant (Verified).
- **API:** `VIEW01 (GET viewing-slots)`, `VIEW05 (POST viewings)`.
- **UI Sections:** Calendar week view, danh sách slot `OPEN` trong ngày.
- **Actions:** Chọn slot → Nhập note → Submit.
- **Business Rule:** Source: PROJECT_CONTRACT BR-05.

### SCREEN-BOOKING-CREATE (Tạo yêu cầu thuê)
- **Purpose:** Gửi yêu cầu giữ phòng tới Host.
- **Actor:** Tenant (Verified).
- **API:** `BOOK01 (POST /bookings)` + Yêu cầu gửi kèm Idempotency-Key.
- **UI Sections:**
  - Tóm tắt snapshot tiền (Rent, Deposit, Fees).
  - Chọn ngày dọn vào (từ lịch).
  - Số lượng người, số lượng xe.
  - Submit button.
- **Business Rule:** Source: PROJECT_CONTRACT BR-06, BR-07.
- **Error Handling:** Trả về 409 `ROOM_NOT_AVAILABLE` hoặc `TERMS_CHANGED` → Tải lại dữ liệu chi tiết, không tự động force user đồng ý.

### SCREEN-CHAT (Phòng chat)
- **Purpose:** Nhắn tin giữa Tenant và Host.
- **Actor:** Tenant, Host.
- **API (REST):** `CHAT04 (GET messages)`.
- **API (WebSocket):** Gửi frame STOMP lên `/ws`.
- **UI Sections:** Toolbar (Room info, Host info), Message List, Input bar.
- **Realtime Chat Rule:**
  - Tin nhắn do mình gửi hiển thị ngay "Đang gửi" (màu nhạt).
  - Có ACK từ broker → Hiển thị bình thường.
  - Lỗi mạng → Retry button (giữ nguyên `client_message_id`).
- **Business Rule:** Source: PROJECT_CONTRACT BR-11.

---

## 5. UI State & Action Permission Matrix

- Hành động Mutation (Tạo, Sửa, Xóa) bắt buộc:
  - Tài khoản phải **Verified**.
  - Kiểm tra kết nối mạng trước khi request.
  - Vô hiệu hóa (Disable) nút bấm ngay sau khi click, hiển thị indicator (chống spam click) nhưng gửi kèm `Idempotency-Key`.
- Hành động chỉ dành cho người tạo/sở hữu (Ví dụ: Edit Review, Cancel Booking) chỉ được hiển thị button nếu `user_id` khớp với `actor_id` trong DTO.

---

## 6. Form Validation (FE UX)

*Dù FE có validation, Backend vẫn là chốt chặn cuối.*

- **Email:** Regex email hợp lệ.
- **Password:** >= 12 ký tự (chưa cần chữ hoa/đặc biệt nếu BE không ép, nhưng tuân thủ BR).
- **Money:** Ngăn nhập ký tự không phải số. FE tự động format `#,### ₫` khi hiển thị, nhưng khi gọi API phải gỡ format, gửi đúng string số nguyên (`"3000000"`).
- **Ngày tháng:** Không cho phép chọn ngày quá khứ, giới hạn hiển thị calendar theo quy tắc ở `BR-05` và `BR-06`.

---

## 7. Error Handling (Client Side)

Ánh xạ từ `RFC 9457 ProblemDTO` của Backend:

| Lỗi BE (HTTP + Code) | Xử lý trên UI Android |
|---|---|
| `400 INVALID_JSON` | Crash/Log Analytics (Lỗi dev). |
| `400 VALIDATION_FAILED` | Highlight các TextInputs bị lỗi dựa vào mảng `field_errors`. Hiển thị thông báo. |
| `401 TOKEN_EXPIRED` | Authenticator tự động lấy token mới. |
| `401 INVALID_CREDENTIALS`| "Tên đăng nhập hoặc mật khẩu không đúng." |
| `403 EMAIL_NOT_VERIFIED` | Hiển thị modal "Vui lòng xác minh email để tiếp tục", nút dẫn ra Email Verify. |
| `404 RESOURCE_NOT_FOUND` | Hiển thị màn hình Empty State "Dữ liệu không tồn tại hoặc đã bị xóa". |
| `409 ROOM_NOT_AVAILABLE` | Popup: "Phòng này đã có người giữ. Vui lòng chọn phòng khác." Load lại trang. |
| `409 TERMS_CHANGED` | Popup: "Chủ phòng vừa cập nhật giá/điều kiện. Vui lòng xem lại." Load lại trang. |
| `429 RATE_LIMITED` | "Bạn thao tác quá nhanh. Vui lòng thử lại sau X giây." (Đọc Retry-After). |
| `500 INTERNAL_ERROR` | "Hệ thống đang gặp sự cố. Vui lòng thử lại sau." |
| Mất mạng (Timeout/IOException) | "Không có kết nối mạng. Vui lòng kiểm tra lại." |

---

## 8. Notification & Deep Linking

- **FCM (Firebase Cloud Messaging):** Service nhận push ở chế độ nền.
- Cấu trúc Payload chứa `target_type` và `target_id`.
- **Deep Link Navigation:**
  - `type == MESSAGE` → Mở `SCREEN-CHAT` với `conversation_id`.
  - `type == BOOKING_STATUS` → Mở `SCREEN-BOOKING-DETAIL` với `booking_id`.
  - `type == NEW_MATCHING_LISTING` → Mở `SCREEN-ROOM-DETAIL` với `listing_id`.
- **Quy tắc:** Bấm vào Push không bao giờ thực hiện thẳng mutation API, chỉ dùng để mở màn hình xem trạng thái.
> **Cập nhật 03/10/2026:** Hiện trạng triển khai và các quyết định thay thế mô tả cũ nằm tại [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md). FCM dùng MySQL outbox; hướng dẫn tại [FIREBASE_SETUP.md](FIREBASE_SETUP.md). Các mốc kiểm chứng trước đây là lịch sử, không phải nghiệm thu bản hiện tại.
