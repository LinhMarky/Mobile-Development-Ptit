# 🚀 Backend Homely (Spring Boot)

![Java 17](https://img.shields.io/badge/Java-17-orange)
![Spring Boot 3.3.x](https://img.shields.io/badge/Spring_Boot-3.3.x-green)
![Testing](https://img.shields.io/badge/Tests-249_Passing-brightgreen)
![Architecture](https://img.shields.io/badge/Architecture-Modular_Monolith-purple)

Hệ thống Backend cung cấp toàn bộ API, luồng nghiệp vụ (Business Rules), và giao tiếp Realtime (WebSocket) cho nền tảng tìm và thuê phòng **Homely**.

---

## 🏗 Kiến trúc (Architecture)

Dự án được xây dựng theo chuẩn **Modular Monolith** nhằm đảm bảo sự cân bằng giữa tốc độ phát triển và khả năng mở rộng (Scale). Codebase được chia thành các package độc lập theo Domain Driven Design (DDD):
- `auth`: Quản lý tài khoản, JWT Token, Rate Limiting, Phân quyền.
- `catalog`: Quản lý danh sách phòng (Listings, Rooms), tìm kiếm, thuộc tính tiện ích (Amenities).
- `booking`: Quản lý hợp đồng, luồng đặt phòng, vòng đời thuê.
- `chat`: Giao tiếp Realtime qua STOMP WebSocket, quản lý tin nhắn, lịch sử chat.
- `payment`: Giao dịch, tiền cọc, refund, sandbox payment.
- `media`: Xử lý lưu trữ file đa phương tiện (Upload MinIO/S3).
- `notification`: Outbox Pattern & Firebase Cloud Messaging (FCM).
- `admin` & `interaction`: Quản trị hệ thống, Review, Report tranh chấp.

Mỗi module đều tuân thủ kiến trúc nhiều lớp (Controller -> Service -> Repository), DTO cách ly hoàn toàn entity, tránh leak dữ liệu nội bộ.

---

## 🛡 Tính năng kỹ thuật nổi bật

1. **Idempotency & Concurrency Guard**: Mọi request `POST` đột biến (Mutation) đều yêu cầu `Idempotency-Key` để chống double-click. Sử dụng Pessimistic Locking và Row-level lock (`AccountLifecycleLock`) để đảm bảo không bị race-condition.
2. **Global Response Wrapper**: Mọi kết quả HTTP được gói gọn vào chuẩn `{statusCode, message, data}`, hỗ trợ `ProblemDTO` chuẩn xác định (RFC 9457) cho exception.
3. **Smart Rate Limiting**: Limit API thông minh dựa trên cả `IP` và `Identity` (băm token) bằng bộ lọc in-memory, chống Brute-force triệt để.
4. **Outbox Pattern & Workers**: Tách biệt logic gửi Push Notification (FCM) khỏi Transaction chính bằng cách lưu thông báo xuống database (Outbox) và xử lý ngầm (Worker).

---

## 💻 Hướng dẫn Build & Chạy nội bộ

1. **Chạy qua Docker Compose (Khuyên dùng cho Frontend/App Developer)**:
   Di chuyển vào thư mục `Infra` và làm theo [Infra/README.md](../Infra/README.md).

2. **Chạy thủ công (Dành cho Backend Developer)**:
   - Cài đặt Java 17+ và Maven.
   - Set các biến môi trường thiết yếu: `JWT_SECRET` (chuỗi Base64 dài >64 bytes), `MYSQL_USER`, `MYSQL_PASSWORD`, `MINIO_ACCESS_KEY`...
   - Chạy lệnh build:
     ```bash
     mvn clean install -DskipTests
     ```
   - Chạy ứng dụng qua Spring Boot:
     ```bash
     mvn spring-boot:run
     ```

3. **Chạy Unit/Integration Tests**:
   Dự án sở hữu độ bao phủ test cực cao với 249 tests.
   ```bash
   mvn verify
   ```
   *(Để chạy full test tích hợp với database thật, cần cung cấp biến `HOMELY_TEST_MYSQL_URL` trỏ tới 1 schema kiểm thử riêng).*

---

## 📜 Quy tắc Đóng góp (Contribution & Expansion)

Khi phát triển thêm tính năng cho Backend, mọi kỹ sư cần tuân thủ tuyệt đối các nguyên tắc sau:

1. **Transaction & Locking**: Các thao tác thay đổi trạng thái user phải gọi `AccountAccessService` ở **bên trong transaction**, trước khi cấp bất kỳ khóa resource nào (Booking, Room).
2. **Account Deletion Rule**: Bất kỳ nghiệp vụ mới nào sinh ra dữ liệu liên kết tới `User` đều phải bổ sung điều kiện xóa vào `AccountDeletionEligibility` và `AccountDeletionProcessor`.
3. **No Distributed Transaction Trap**: Việc gửi email SMTP hoặc Push FCM **không được** đặt vào chung Database Transaction. Luôn hoàn tất DB commit trước khi đẩy network call.
4. **API Contract**: Mọi thay đổi DTO/Schema phải được cập nhật ở OpenAPI và tài liệu [PROJECT_CONTRACT.md](../Docs%20-%20contract/PROJECT_CONTRACT.md).
