# 🏢 Homely Rental System — Infrastructure & Deployment

![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![MySQL 8.4](https://img.shields.io/badge/MySQL-8.4-blue)
![MinIO](https://img.shields.io/badge/MinIO-Storage-red)
![Mailpit](https://img.shields.io/badge/Mailpit-SMTP-yellow)

Tài liệu này cung cấp hướng dẫn toàn diện về cài đặt, vận hành và gỡ lỗi toàn bộ hạ tầng của hệ thống **Homely** (Room Rental System) thông qua **Docker Compose**.

---

## 🛠 1. Yêu cầu hệ thống (Prerequisites)

- **Docker Desktop** (Windows/Mac) hoặc **Docker Engine** (Linux) phải được cài đặt và đang ở trạng thái **Running**. Khuyến nghị cấp phát tối thiểu **4GB RAM** cho Docker để chạy mượt mà Spring Boot, MySQL và MinIO.
- **Git** để quản lý và đồng bộ source code.
- **PowerShell** (nếu dùng Windows) để thực thi các file kịch bản tự động `.ps1`.
- Đảm bảo các port sau đang **trống** trên máy tính của bạn:
  - `8080` (hoặc `8082`): API Backend
  - `3306`: MySQL Database
  - `9000` & `9001`: MinIO Object Storage
  - `8025` & `1025`: Mailpit SMTP server

---

## 🚀 2. Hướng dẫn sử dụng Docker Compose chi tiết

Toàn bộ hệ thống chạy qua các containers độc lập, giao tiếp với nhau bằng mạng ảo của Docker.

### 2.1. Khởi tạo môi trường
Di chuyển vào thư mục `Infra` trên terminal và chạy script tạo file môi trường:
```powershell
.\New-DemoEnv.ps1
```
> Script này sẽ tự động sinh file `.env` (chứa mật khẩu ngẫu nhiên cho MySQL, MinIO, JWT Secret...). Không bao giờ commit file `.env` lên Git để tránh lộ thông tin nhạy cảm.

### 2.2. Các lệnh Docker Compose cốt lõi

**Khởi động hệ thống:**
```powershell
docker compose up --build -d
```
- Lệnh này sẽ kéo (pull) image của MySQL và Mailpit về, đồng thời tự động build (biên dịch) image cho Backend (từ `../backend/Dockerfile`) và MinIO (từ `minio/Dockerfile`).
- Cờ `-d` (detached) giúp container chạy ngầm, không khóa terminal của bạn.

**Kiểm tra trạng thái (Status):**
```powershell
docker compose ps
```
- Dùng lệnh này để xem danh sách các container. Chú ý cột trạng thái phải là `Running` (hoặc `Up`). Nếu thấy `Exit`, hãy xem log.

**Xem log (Nhật ký chạy):**
```powershell
docker compose logs -f backend
```
- Theo dõi tiến trình khởi động của Spring Boot. Nó sẽ chờ các dịch vụ khác (DB, Storage) báo `healthy` rồi mới bắt đầu.
- Để xem log của DB: `docker compose logs -f mysql`

**Dừng hệ thống (Tạm thời):**
```powershell
docker compose stop
```
- Dừng container nhưng không xóa chúng.

**Tắt và dọn dẹp hoàn toàn (Quan trọng khi lỗi nặng):**
```powershell
docker compose down
```
- Lệnh này xóa sạch container và network ảo.
- **Lưu ý:** Nếu bạn muốn **xóa trắng luôn cả dữ liệu (Database, ảnh đã up)** để làm lại từ đầu, hãy thêm cờ `-v`:
  ```powershell
  docker compose down -v
  ```

---

## 🔧 3. Cấu trúc dịch vụ & Port Mapping

Hệ thống được quy định cụ thể trong file `compose.yaml` với các ánh xạ port (Port Mapping) từ container ra ngoài máy thật như sau:

| Tên Dịch Vụ | Port trong Container | Port ở Máy ngoài (Host) | Công dụng | Dữ liệu bền vững (Volume) |
| --- | --- | --- | --- | --- |
| **Backend** | `8080` | `8080` (hoặc cấu hình) | Chứa API Spring Boot | N/A |
| **MySQL** | `3306` | `3307` | Database chính | `mysql_data` |
| **MinIO** | `9000` (API), `9001` (Console)| `9000`, `9001` | Lưu file/ảnh. Web UI ở `:9001` | `minio_data` |
| **Mailpit** | `1025` (SMTP), `8025` (UI) | `1025`, `8025` | Chặn email OTP. Xem thư tại `:8025` | N/A |

*(Volume giúp dữ liệu của MySQL và MinIO không bị mất đi ngay cả khi bạn xóa container, trừ khi bạn cố tình chạy `docker compose down -v`)*

---

## 🧪 4. Tự động hóa kiểm thử (Testing)

Đứng tại thư mục `Infra`, chạy:

1. **Kiểm tra Hạ tầng (`Test-Infrastructure.ps1`)**:
   Xác minh DB, MinIO upload, Mailpit và Rate Limit.
   ```powershell
   .\Test-Infrastructure.ps1 -BaseUrl http://localhost:8080/api/v1
   ```

2. **Kiểm tra Luồng Nghiệp Vụ (`Test-Demo.ps1`)**:
   Chạy luồng API thực tế (tạo tài khoản, tạo phòng, sandbox payment).
   ```powershell
   $env:DEMO_PASSWORD = (Get-Content .env | Select-String "DEMO_PASSWORD=").Line.Split("=")[1]
   .\Test-Demo.ps1 -BaseUrl http://localhost:8080/api/v1
   ```

---

## ⚠️ 5. Các lỗi Docker thường gặp & Cách khắc phục

**Lỗi 1: `unable to prepare context: path ".../infra/minio" not found`**
- **Nguyên nhân**: Bị thiếu thư mục `minio` do Git đã ẩn nó đi.
- **Cách sửa**: Đã được patch trên nhánh `dev`. Hãy chạy `git pull` để nhận bản mới nhất.

**Lỗi 2: `ports are not available: exposing port TCP 127.0.0.1:8080...`**
- **Nguyên nhân**: Port `8080` của bạn đang bị phần mềm khác chiếm giữ (Zalo, Skype, IIS, Tomcat...).
- **Cách sửa**: Mở file `Infra/.env`, thêm/sửa dòng `API_PORT=8082`. Chạy lại `docker compose up -d`. Lúc này API sẽ chạy ở `localhost:8082`.

**Lỗi 3: Backend bị tắt ngay lập tức (Exit 1)**
- **Nguyên nhân**: Có thể cấu hình `JWT_SECRET` bị sai định dạng.
- **Cách sửa**: Chạy lệnh `docker compose logs backend` để đọc lỗi. Nếu do cấu hình, hãy xóa file `.env` đi, chạy lại `.\New-DemoEnv.ps1`, và sau đó `docker compose down` rồi khởi động lại hệ thống.

**Lỗi 4: Docker Desktop bị treo hoặc báo lỗi kết nối Daemon**
- **Nguyên nhân**: Môi trường WSL2 hoặc Hyper-V trên Windows bị nghẽn.
- **Cách sửa**: Tắt Docker Desktop. Mở Task Manager tắt hẳn các tiến trình Docker (hoặc chạy `wsl --shutdown` trên PowerShell) rồi bật lại Docker Desktop.
