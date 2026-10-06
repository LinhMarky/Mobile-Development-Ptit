# 🏢 Homely Rental System — Infrastructure & Deployment

![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![MySQL 8.4](https://img.shields.io/badge/MySQL-8.4-blue)
![MinIO](https://img.shields.io/badge/MinIO-Storage-red)
![Mailpit](https://img.shields.io/badge/Mailpit-SMTP-yellow)

Tài liệu này cung cấp hướng dẫn cài đặt và vận hành toàn bộ hạ tầng của hệ thống **Homely** (Room Rental System) trên môi trường Local/Demo thông qua Docker Compose.

---

## 🛠 Yêu cầu hệ thống (Prerequisites)

- **Docker Desktop** (trên Windows/Mac) hoặc **Docker Engine** (trên Linux) phải đang ở trạng thái Running.
- **Git** để quản lý source code.
- **PowerShell** (nếu chạy trên Windows) để thực thi các script tự động.
- Trống các port: `8080` (hoặc `8082` tuỳ cấu hình API), `3306` (cho MySQL), `9000/9001` (cho MinIO), `8025/1025` (cho Mailpit).

---

## 🚀 Khởi động nhanh (Quick Start)

Mở Terminal (khuyến nghị PowerShell) tại thư mục `Infra` và chạy lần lượt các bước sau:

### Bước 1: Khởi tạo biến môi trường
Script sau sẽ tự động sinh file `.env` chứa các mật khẩu ngẫu nhiên và thông tin bảo mật cần thiết cho toàn bộ Docker Compose.
```powershell
.\New-DemoEnv.ps1
```
*(Nếu báo lỗi port 8080 bị trùng, bạn có thể mở file `.env` sinh ra và đổi `API_PORT=8082`)*

### Bước 2: Build & Start các services
Chạy lệnh sau để khởi động hạ tầng ngầm (MySQL, MinIO, Mailpit) và build image cho Backend.
```powershell
docker compose up --build -d
```

### Bước 3: Theo dõi Log
Backend sẽ tự động chờ cho đến khi MySQL và MinIO chuyển sang trạng thái `healthy` rồi mới khởi động Spring Boot. Quá trình này mất khoảng 1-2 phút.
```powershell
docker compose logs -f backend
```

---

## 🧪 Kịch bản Kiểm thử (Testing)

Dự án cung cấp sẵn các script PowerShell để tự động hoá việc kiểm tra hệ thống. Vẫn đứng tại thư mục `Infra`, chạy:

1. **Kiểm tra Hạ tầng (`Test-Infrastructure.ps1`)**:
   Kiểm tra kết nối Database, upload/đọc file từ MinIO, hệ thống gửi email (SMTP Mailpit) và rate limit.
   ```powershell
   .\Test-Infrastructure.ps1 -BaseUrl http://localhost:8080/api/v1
   ```

2. **Kiểm tra Nghiệp vụ (`Test-Demo.ps1`)**:
   Chạy luồng End-to-End thực tế: Đăng ký, đăng nhập, verify email, tạo phòng, duyệt yêu cầu thuê, thanh toán mô phỏng (sandbox) và chat.
   ```powershell
   $env:DEMO_PASSWORD = (Get-Content .env | Select-String "DEMO_PASSWORD=").Line.Split("=")[1]
   .\Test-Demo.ps1 -BaseUrl http://localhost:8080/api/v1
   ```

---

## 🔧 Cấu trúc dịch vụ (Services Architecture)

- **MySQL (`mysql:8.4`)**: Database chính. Dữ liệu được lưu tại volume `mysql_data`.
- **MinIO (`homely-minio-source`)**: Dịch vụ Object Storage tương thích S3, dùng để lưu trữ ảnh phòng và avatar. Web UI quản lý tại `http://localhost:9001`. Dữ liệu lưu tại `minio_data`.
- **Mailpit (`axllent/mailpit`)**: Server chặn email (Catch-all SMTP) dành cho môi trường dev. Giúp bạn xem email OTP đăng ký tài khoản mà không cần gửi email thật. Web UI tại `http://localhost:8025`.
- **Backend (`homely-demo-backend`)**: Ứng dụng Spring Boot cốt lõi. Giao tiếp nội bộ với các dịch vụ trên thông qua Docker network.

---

## ⚠️ Khắc phục sự cố thường gặp (Troubleshooting)

1. **Lỗi `unable to prepare context: path ".../infra/minio" not found`**
   - **Nguyên nhân**: Quên không commit thư mục `minio` do bị vướng `.gitignore`.
   - **Cách sửa**: Đã được xử lý, bạn chỉ cần `git pull` bản mới nhất.

2. **Lỗi `ports are not available: exposing port TCP 127.0.0.1:8080`**
   - **Nguyên nhân**: Port 8080 đã bị ứng dụng khác trên máy (VD: Tomcat, Jenkins, Skype) chiếm dụng.
   - **Cách sửa**: Mở file `Infra/.env`, thêm/sửa dòng `API_PORT=8082` và chạy lại `docker compose up -d`. Khi test, nhớ đổi `-BaseUrl` sang `8082`.

3. **Backend bị tắt ngay lập tức (Exit 1)**
   - Hãy dùng `docker compose logs backend` để xem chi tiết. Thường do `JWT_SECRET` trong file `.env` bị thiếu hoặc sai định dạng. Hãy xóa `.env` và chạy lại `New-DemoEnv.ps1`.
