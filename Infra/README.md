# 🏢 Homely Rental System — Infrastructure & Deployment

![Java 17](https://img.shields.io/badge/Java-17-orange)
![Spring Boot 3.3.x](https://img.shields.io/badge/Spring_Boot-3.3.x-green)
![MySQL 8.4](https://img.shields.io/badge/MySQL-8.4-blue)
![MinIO](https://img.shields.io/badge/MinIO-Storage-red)

Tài liệu này cung cấp hướng dẫn cài đặt và vận hành hệ thống **Homely** (Room Rental System) trên môi trường Local/Demo bằng Docker Compose. Hệ thống bao gồm Backend (Spring Boot), Database (MySQL 8.4), Storage (MinIO) và Mock Email (Mailpit).

---

## 🛠 Yêu cầu hệ thống (Prerequisites)

- **Docker Desktop** / **Docker Engine** (đang chạy với Linux containers).
- **PowerShell** (nếu chạy trên Windows).
- **Java 17+ & Maven** (nếu muốn build thủ công không qua Docker).

---

## 🚀 Khởi động nhanh (Quick Start)

Mở Terminal (PowerShell) tại thư mục `Infra` và chạy lần lượt các lệnh sau:

1. **Khởi tạo môi trường:**
   ```powershell
   # Sinh file .env với các biến bảo mật tự động
   .\New-DemoEnv.ps1
   ```

2. **Kiểm tra file cấu hình (Tùy chọn):**
   ```powershell
   docker compose config --quiet
   ```

3. **Khởi động toàn bộ hệ thống:**
   ```powershell
   docker compose up --build -d
   ```

4. **Theo dõi log Backend:**
   ```powershell
   docker compose logs -f backend
   ```
> **Lưu ý:** Backend sẽ tự động chờ MySQL và MinIO `healthy` rồi mới bắt đầu chạy. Thời gian tải ban đầu có thể mất 1-2 phút.

---

## 🌍 Các dịch vụ và Cổng (Services & Ports)

Sau khi hệ thống khởi động thành công, bạn có thể truy cập các dịch vụ qua các địa chỉ sau:

| Dịch vụ | Địa chỉ truy cập | Ghi chú |
|---------|-----------------|---------|
| **Backend API Health** | [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) | Trạng thái server |
| **Swagger UI (API Docs)** | [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) | Tài liệu API |
| **Mailpit (Mock Email)** | [http://localhost:8025](http://localhost:8025) | Hộp thư test (Mã OTP, email xác nhận) |
| **MinIO Console** | [http://localhost:9001](http://localhost:9001) | Quản lý File/Ảnh (User/Pass trong `.env`) |

*Lưu ý: Nếu cổng `8080` bị trùng, bạn có thể đổi cổng bằng cách cấu hình `$env:API_PORT='18082'` trước khi chạy Docker Compose.*

---

## 👥 Tài khoản Demo (Sandbox Data)

Khi khởi chạy, hệ thống sẽ tự động tạo dữ liệu mẫu (Seed Data) nếu biến `DEMO_SEED=true` được bật. Mật khẩu chung cho tất cả các tài khoản nằm trong biến `DEMO_PASSWORD` ở file `.env`.

Danh sách tài khoản test:
- 👑 **Admin**: `admin@homely.test` (Quản trị, duyệt tin)
- 🏠 **Host**: `host@homely.test` (Chủ nhà, quản lý phòng, booking)
- 🧑‍💼 **Tenant**: `tenant@homely.test` (Khách thuê phòng)

> Dữ liệu seed bao gồm: 3 phòng, 2 tin công khai, 1 tin chờ duyệt và 1 lịch hẹn xem phòng. Seed an toàn và sẽ tự bỏ qua nếu phát hiện email đã tồn tại. Muốn test luồng tạo mới, bạn cứ dùng email tùy ý và check hộp thư Mailpit để lấy mã OTP nhé.

---

## 🧪 Chạy Script Kiểm thử (Testing)

Dự án cung cấp sẵn các công cụ tự động (Dev Tooling) để kiểm thử luồng nghiệp vụ. Chạy các lệnh sau trong PowerShell:

1. **Kiểm tra luồng Hạ tầng (Tạo user, upload ảnh, gửi mail, rate limit):**
   ```powershell
   .\Test-Infrastructure.ps1 -BaseUrl 'http://localhost:8080/api/v1'
   ```

2. **Kiểm tra tải trọng Backend (Load Testing):**
   ```powershell
   .\Test-BackendLoad.ps1 -BaseUrl 'http://localhost:8080/api/v1'
   ```

3. **Kiểm thử luồng Đặt phòng (Booking/Payment/Handover):**
   ```powershell
   .\Test-Demo.ps1 -BaseUrl 'http://localhost:8080/api/v1'
   ```
   *(Yêu cầu thiết lập `$env:DEMO_PASSWORD` trong Terminal trùng với `.env`)*

---

## 📱 Kết nối với App Android

Xem chi tiết tại: [Hướng dẫn Android](../Homely-android/README.md).
- **Emulator mặc định:** `http://10.0.2.2:8080/api/v1`
- **Thiết bị thật (cắm cáp USB):** Chạy lệnh `adb reverse tcp:8080 tcp:8080` và gọi API qua `http://127.0.0.1:8080/api/v1`

---

## ⚙️ Hướng dẫn Deploy lên Production (Môi trường thật)

Nếu muốn đưa dự án ra thực tế, cần lưu ý:
1. Tắt chế độ Demo (`DEMO_ENABLED=false`, `DEMO_SEED=false`).
2. Tự cấu hình biến môi trường thật (`MYSQL_*`, `JWT_SECRET`, `MINIO_*`, `SMTP_*`).
3. Dùng giao thức HTTPS và thay Mailpit bằng SMTP Provider thật (như SendGrid, Gmail).
4. Tích hợp Firebase Cloud Messaging để Push Notification (xem chi tiết [FIREBASE_SETUP.md](../Docs%20-%20contract/FIREBASE_SETUP.md)).
5. Kết nối ví điện tử hoặc cổng thanh toán thật (VNPay/Momo) thay vì dùng Sandbox Payment.
