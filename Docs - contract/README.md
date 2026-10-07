# Tài liệu Homely

Backend dùng Java 17, Spring Boot 3.3.2, MySQL 8.4, JWT HS512, Flyway, MinIO/S3 và FCM qua MySQL outbox. Không dùng Kafka.

- [Mục lục và giao thức hiện tại](00_README.md).
- [Contract v1.6](PROJECT_CONTRACT.md) và [OpenAPI](openapi.yaml).
- [Hoàn thiện backend và kết quả kiểm chứng](BACKEND_COMPLETION.md).
- [Kiến trúc code, transaction, cấu hình và cách mở rộng](../backend/README.md).
- [Chạy Docker demo](../Infra/README.md).
- [Cấu hình Firebase](FIREBASE_SETUP.md).

`JWT_SECRET` bắt buộc là Base64 của ít nhất 64 byte ngẫu nhiên. Dùng hướng dẫn chạy bên trên để chuẩn bị đầy đủ MySQL, SMTP và storage; không chỉ tạo database rồi chạy app.
